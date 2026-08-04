using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using VirtualSteerReceiver.Utils;

namespace VirtualSteerReceiver.Network
{
    /// <summary>
    /// Low-level UDP server that receives raw packets on a specified port.
    /// Designed for high-frequency packet reception (100+ Hz).
    /// </summary>
    public sealed class UDPServer : IDisposable
    {
        private UdpClient? _udpClient;
        private CancellationTokenSource? _cts;
        private Task? _listenTask;
        private bool _isListening;

        public event Action<byte[], IPEndPoint>? PacketReceived;
        public event Action<Exception>? ErrorOccurred;

        public bool IsListening => _isListening;
        public int Port { get; private set; }

        public void Start(int port)
        {
            if (_isListening) return;

            Port = port;

            try
            {
                _udpClient = new UdpClient(AddressFamily.InterNetwork);
                _udpClient.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
                
                // Increase receive buffer to handle bursts (256KB)
                _udpClient.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReceiveBuffer, 262144);
                
                _udpClient.Client.Bind(new IPEndPoint(IPAddress.Any, port));
            }
            catch (SocketException ex) when (ex.SocketErrorCode == SocketError.AddressAlreadyInUse)
            {
                Logger.Instance.Error($"Port {port} is already in use. Close any other instance or change the port.");
                throw;
            }
            catch (SocketException ex) when (ex.SocketErrorCode == SocketError.AccessDenied)
            {
                Logger.Instance.Error($"Access denied binding to port {port}. Try running as Administrator or use a port > 1024.");
                throw;
            }

            _cts = new CancellationTokenSource();
            _isListening = true;

            _listenTask = Task.Run(() => ListenAsync(_cts.Token), _cts.Token);
        }

        private async Task ListenAsync(CancellationToken token)
        {
            while (!token.IsCancellationRequested && _udpClient != null)
            {
                try
                {
                    UdpReceiveResult result = await _udpClient.ReceiveAsync(token);
                    PacketReceived?.Invoke(result.Buffer, result.RemoteEndPoint);
                }
                catch (OperationCanceledException)
                {
                    break; // Stopped gracefully
                }
                catch (ObjectDisposedException)
                {
                    break;
                }
                catch (SocketException ex) when (ex.SocketErrorCode == SocketError.ConnectionReset)
                {
                    // ICMP port unreachable from a previous send — harmless, ignore
                    continue;
                }
                catch (Exception ex)
                {
                    ErrorOccurred?.Invoke(ex);
                }
            }
            _isListening = false;
        }

        public void Stop()
        {
            if (!_isListening) return;

            _isListening = false;
            _cts?.Cancel();
            _udpClient?.Close();
            _udpClient?.Dispose();
            _udpClient = null;
        }

        public void Dispose()
        {
            Stop();
            _cts?.Dispose();
        }
    }
}
