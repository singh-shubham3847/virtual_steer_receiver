using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using VirtualSteerReceiver.Utils;

namespace VirtualSteerReceiver.Network
{
    /// <summary>
    /// Handles device discovery for Virtual Steer.
    /// 
    /// Protocol:
    ///   1. Android broadcasts "DISCOVER_VIRTUAL_STEER" to UDP 255.255.255.255:4445
    ///   2. Windows listens on port 4445, receives the broadcast
    ///   3. Windows replies directly to the Android sender with a JSON payload + legacy payload
    ///   4. Additionally, Windows proactively broadcasts its presence every 2 seconds
    ///      so Android can discover it passively without sending a discovery request.
    /// </summary>
    public sealed class DiscoveryBroadcaster : IDisposable
    {
        private UdpClient? _socket;
        private CancellationTokenSource? _cts;
        private Task? _listenerTask;
        private Task? _broadcastTask;

        public bool IsBroadcasting { get; private set; }
        public string LastPayload { get; private set; } = string.Empty;
        public string LastJsonPayload { get; private set; } = string.Empty;

        public void Start(int serverPort, string? pcName = null)
        {
            if (IsBroadcasting)
            {
                return;
            }

            string hostName = SanitizeHostName(string.IsNullOrWhiteSpace(pcName) ? Dns.GetHostName() : pcName.Trim());
            LastPayload = $"VIRTUAL_STEER_SERVER:{serverPort}:{hostName}";
            LastJsonPayload = BuildJsonPayload(serverPort, hostName);

            _cts = new CancellationTokenSource();
            CancellationToken token = _cts.Token;

            // Use a single socket for both listening and sending.
            // This avoids port conflicts and ensures replies come from the correct port.
            try
            {
                _socket = new UdpClient();
                _socket.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
                _socket.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.Broadcast, true);
                _socket.Client.Bind(new IPEndPoint(IPAddress.Any, Protocol.DiscoveryPort));
            }
            catch (SocketException ex) when (ex.SocketErrorCode == SocketError.AddressAlreadyInUse)
            {
                Logger.Instance.Warn($"Discovery port {Protocol.DiscoveryPort} already in use. Trying with ephemeral port for broadcast-only mode.");
                try
                {
                    _socket?.Dispose();
                    _socket = new UdpClient();
                    _socket.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.Broadcast, true);
                    // Can't listen for discovery requests, but can still broadcast presence
                }
                catch (Exception innerEx)
                {
                    Logger.Instance.Error($"Failed to create discovery socket: {innerEx.Message}");
                    return;
                }
            }
            catch (Exception ex)
            {
                Logger.Instance.Error($"Failed to start discovery: {ex.Message}");
                return;
            }

            IsBroadcasting = true;

            // Start listener for "DISCOVER_VIRTUAL_STEER" requests from Android
            _listenerTask = Task.Run(async () =>
            {
                await ListenForDiscoveryRequests(serverPort, hostName, token);
            }, token);

            // Start proactive broadcaster so Android can find us passively
            _broadcastTask = Task.Run(async () =>
            {
                await BroadcastPresence(token);
            }, token);

            Logger.Instance.Info($"Discovery started on port {Protocol.DiscoveryPort}. PC name: \"{hostName}\", controller port: {serverPort}");
        }

        private async Task ListenForDiscoveryRequests(int serverPort, string hostName, CancellationToken token)
        {
            while (!token.IsCancellationRequested && _socket is not null)
            {
                try
                {
                    UdpReceiveResult result = await _socket.ReceiveAsync(token);
                    string message = Encoding.ASCII.GetString(result.Buffer).Trim();

                    // Only respond to discovery requests from Android
                    if (!message.Equals("DISCOVER_VIRTUAL_STEER", StringComparison.OrdinalIgnoreCase))
                    {
                        continue;
                    }

                    Logger.Instance.Info($"📱 Discovery request from {result.RemoteEndPoint}. Sending server info...");

                    // Send JSON response (primary format)
                    byte[] jsonResponse = Encoding.UTF8.GetBytes(BuildJsonPayload(serverPort, hostName));
                    await _socket.SendAsync(jsonResponse, jsonResponse.Length, result.RemoteEndPoint);

                    // Send legacy response (fallback format)
                    byte[] legacyResponse = Encoding.ASCII.GetBytes(LastPayload);
                    await _socket.SendAsync(legacyResponse, legacyResponse.Length, result.RemoteEndPoint);

                    Logger.Instance.Info($"✅ Discovery reply sent to {result.RemoteEndPoint}");
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                catch (ObjectDisposedException)
                {
                    break;
                }
                catch (Exception ex)
                {
                    if (!token.IsCancellationRequested)
                    {
                        Logger.Instance.Error($"Discovery listener error: {ex.Message}");
                    }
                }
            }
        }

        private async Task BroadcastPresence(CancellationToken token)
        {
            byte[] payload = Encoding.ASCII.GetBytes(LastPayload);

            try
            {
                while (!token.IsCancellationRequested && _socket is not null)
                {
                    var broadcastAddresses = new System.Collections.Generic.List<IPAddress>();
                    // Fallback to global broadcast
                    broadcastAddresses.Add(IPAddress.Broadcast);

                    try
                    {
                        foreach (var ni in System.Net.NetworkInformation.NetworkInterface.GetAllNetworkInterfaces())
                        {
                            if (ni.OperationalStatus != System.Net.NetworkInformation.OperationalStatus.Up) continue;
                            if (ni.NetworkInterfaceType == System.Net.NetworkInformation.NetworkInterfaceType.Loopback) continue;

                            var properties = ni.GetIPProperties();
                            foreach (var ip in properties.UnicastAddresses)
                            {
                                if (ip.Address.AddressFamily == AddressFamily.InterNetwork && ip.IPv4Mask != null)
                                {
                                    // Calculate subnet broadcast address: IP | ~Mask
                                    byte[] ipBytes = ip.Address.GetAddressBytes();
                                    byte[] maskBytes = ip.IPv4Mask.GetAddressBytes();
                                    byte[] broadcastBytes = new byte[4];
                                    for (int i = 0; i < 4; i++)
                                    {
                                        broadcastBytes[i] = (byte)(ipBytes[i] | ~maskBytes[i]);
                                    }
                                    var subnetBroadcast = new IPAddress(broadcastBytes);
                                    if (!broadcastAddresses.Contains(subnetBroadcast))
                                    {
                                        broadcastAddresses.Add(subnetBroadcast);
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Logger.Instance.Warn($"Failed to retrieve network interface broadcast addresses: {ex.Message}");
                    }

                    foreach (var addr in broadcastAddresses)
                    {
                        try
                        {
                            var endPoint = new IPEndPoint(addr, Protocol.DiscoveryPort);
                            await _socket.SendAsync(payload, payload.Length, endPoint);
                        }
                        catch (SocketException)
                        {
                            // Network might be temporarily unavailable
                        }
                    }

                    await Task.Delay(2000, token);
                }
            }
            catch (OperationCanceledException)
            {
                // Expected during shutdown.
            }
            catch (Exception ex)
            {
                if (!token.IsCancellationRequested)
                {
                    Logger.Instance.Error($"Discovery broadcast error: {ex.Message}");
                }
            }
        }

        private static string SanitizeHostName(string hostName)
        {
            string sanitized = hostName.Replace(':', '-').Trim();
            return sanitized.Length == 0 ? Dns.GetHostName() : sanitized;
        }

        private static string BuildJsonPayload(int serverPort, string hostName)
        {
            string escapedName = hostName.Replace("\\", "\\\\").Replace("\"", "\\\"");
            string escapedMachineName = Dns.GetHostName().Replace("\\", "\\\\").Replace("\"", "\\\"");
            return $"{{\"type\":\"VIRTUAL_STEER_SERVER\",\"hostname\":\"{escapedName}\",\"machineName\":\"{escapedMachineName}\",\"version\":\"1.0\",\"port\":{serverPort},\"status\":\"READY\",\"supportsXbox\":true}}";
        }

        public void Stop()
        {
            if (!IsBroadcasting)
            {
                return;
            }

            _cts?.Cancel();

            // Close the socket to unblock any pending ReceiveAsync
            _socket?.Close();
            _socket?.Dispose();
            _socket = null;

            try
            {
                _listenerTask?.Wait(1000);
                _broadcastTask?.Wait(1000);
            }
            catch (AggregateException)
            {
                // Expected during cancellation
            }

            IsBroadcasting = false;
            Logger.Instance.Info("Discovery stopped.");
        }

        public void Dispose()
        {
            Stop();
            _cts?.Dispose();
        }
    }
}
