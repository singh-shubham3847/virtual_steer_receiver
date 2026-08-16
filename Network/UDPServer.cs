using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using VirtualSteerReceiver.Utils;

namespace VirtualSteerReceiver.Network
{
    /// <summary>
    /// High-performance, low-latency UDP server that receives raw packets on a specified port.
    /// Uses a dedicated, high-priority background thread with synchronous blocking socket reads
    /// and pre-allocated buffers to minimize GC allocations, task overhead, and latency.
    /// </summary>
    public sealed class UDPServer : IDisposable
    {
        private Socket? _socket;
        private Thread? _listenThread;
        private volatile bool _isListening;

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
                _socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
                _socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
                
                // Increase receive buffer to handle bursts (256KB)
                _socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReceiveBuffer, 262144);
                
                _socket.Bind(new IPEndPoint(IPAddress.Any, port));
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

            _isListening = true;
            _listenThread = new Thread(ListenLoop)
            {
                IsBackground = true,
                Name = "UDPServerListenThread",
                Priority = ThreadPriority.Highest // Highest priority to minimize kernel-to-user thread scheduling latency
            };
            _listenThread.Start();
        }

        private void ListenLoop()
        {
            byte[] receiveBuffer = new byte[2048];
            EndPoint remoteEP = new IPEndPoint(IPAddress.Any, 0);

            while (_isListening && _socket != null)
            {
                try
                {
                    int bytesRead = _socket.ReceiveFrom(receiveBuffer, ref remoteEP);
                    if (bytesRead > 0)
                    {
                        // Copy to separate array for callback to allow immediate buffer reuse
                        byte[] data = new byte[bytesRead];
                        Buffer.BlockCopy(receiveBuffer, 0, data, 0, bytesRead);

                        PacketReceived?.Invoke(data, (IPEndPoint)remoteEP);
                    }
                }
                catch (SocketException ex) when (ex.SocketErrorCode == SocketError.ConnectionReset || ex.SocketErrorCode == SocketError.Interrupted)
                {
                    // ICMP port unreachable or socket interrupted (closed) — harmless, ignore
                    continue;
                }
                catch (ObjectDisposedException)
                {
                    break;
                }
                catch (Exception ex)
                {
                    if (_isListening)
                    {
                        ErrorOccurred?.Invoke(ex);
                    }
                }
            }
            _isListening = false;
        }

        public void Stop()
        {
            if (!_isListening) return;

            _isListening = false;
            
            try
            {
                _socket?.Close();
                _socket?.Dispose();
            }
            catch (Exception ex)
            {
                Logger.Instance.Error($"Error closing UDP socket: {ex.Message}");
            }
            
            _socket = null;
            _listenThread = null;
        }

        public void Dispose()
        {
            Stop();
        }
    }
}
