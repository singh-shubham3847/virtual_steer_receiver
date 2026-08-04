using System;
using System.Net;
using System.Threading;
using VirtualSteerReceiver.Models;
using VirtualSteerReceiver.Network;
using VirtualSteerReceiver.Utils;

namespace VirtualSteerReceiver.Services
{
    public sealed record TelemetryStats
    {
        public bool IsConnected { get; init; }
        public long TotalPacketsReceived { get; init; }
        public int PacketsPerSecond { get; init; }
        public ushort CurrentSequenceNumber { get; init; }
        public long PacketLossCount { get; init; }
        public long InvalidPacketsCount { get; init; }
        public long CrcFailuresCount { get; init; }
        public DateTime? LastPacketTime { get; init; }
        public string SourceIp { get; init; } = "Disconnected";

        // Advanced Network Stats
        public double CurrentPingMs { get; init; }
        public double AveragePingMs { get; init; }
        public double MaxPingMs { get; init; }
        public double MinPingMs { get; init; }
        public double JitterMs { get; init; }
        public double BandwidthKbps { get; init; }
        public long BytesPerSecond { get; init; }
        public long DroppedPackets { get; init; }
        public long ReconnectCount { get; init; }

        // Raw packet for inspector
        public byte[]? LastRawPacket { get; init; }
    }

    public sealed class ReceiverService : IDisposable
    {
        private readonly UDPServer _server;
        private readonly DiscoveryBroadcaster _broadcaster;
        private readonly VirtualControllerService _virtualController;
        private readonly System.Threading.Timer _watchdogTimer;
        private readonly System.Threading.Timer _rateTimer;

        private ControllerState _currentState = ControllerState.Empty;
        public ControllerState CurrentState => _currentState;

        // Observer event for UI and Xbox Controller
        public event Action<ControllerState>? ControllerStateUpdated;
        public event Action<TelemetryStats>? TelemetryUpdated;

        private bool _isConnected;
        private bool _hasConnectedOnce;
        private long _totalPackets;
        private int _packetsThisSecond;
        private int _packetsPerSecond;
        private long _bytesThisSecond;
        private long _bytesPerSecond;

        private ushort _lastSequenceNumber;
        private bool _hasReceivedFirstPacket;
        private long _packetLossCount;
        private long _invalidPacketsCount;
        private long _crcFailuresCount;
        private long _reconnectCount;

        private DateTime? _lastPacketTime;
        private double _currentPingMs;
        private double _maxPingMs;
        private double _minPingMs;
        private double _pingSum;
        private long _pingSamplesCount;
        private double _avgPingMs;
        private double _previousPingMs;
        private double _jitterMs;

        private string _sourceIp = "Disconnected";
        private byte[]? _lastRawPacket;
        private DateTime _lastTelemetryPublishTime = DateTime.MinValue;

        public ReceiverService()
        {
            _server = new UDPServer();
            _server.PacketReceived += OnRawPacketReceived;
            _server.ErrorOccurred += OnServerError;

            _broadcaster = new DiscoveryBroadcaster();
            _virtualController = new VirtualControllerService();

            // Watchdog timer ticks every 100ms to check 500ms timeout
            _watchdogTimer = new System.Threading.Timer(OnWatchdogTick, null, Timeout.Infinite, Timeout.Infinite);

            // Rate timer ticks every 1 second to update Packets/sec and Bytes/sec
            _rateTimer = new System.Threading.Timer(OnRateTimerTick, null, Timeout.Infinite, Timeout.Infinite);
        }

        public bool IsListening => _server.IsListening;
        public int Port => _server.Port;

        public void Start(int port = Protocol.ControllerPort, string? hostDisplayName = null)
        {
            ResetStats();
            _server.Start(port);
            _broadcaster.Start(port, hostDisplayName);
            _virtualController.Start();

            // Start timers
            _watchdogTimer.Change(100, 100);
            _rateTimer.Change(1000, 1000);

            Logger.Instance.Info($"🎮 UDP Receiver started on port {port}");
            Logger.Instance.Info($"💡 Ensure Windows Firewall allows UDP port {port} (inbound) and {Protocol.DiscoveryPort} (inbound+outbound)");
            Logger.Instance.Info($"📡 Discovery broadcasting on port {Protocol.DiscoveryPort}");

            PublishTelemetry();
        }

        public void Stop()
        {
            // Stop timers
            _watchdogTimer.Change(Timeout.Infinite, Timeout.Infinite);
            _rateTimer.Change(Timeout.Infinite, Timeout.Infinite);

            _broadcaster.Stop();
            _server.Stop();
            _virtualController.Stop();
            _isConnected = false;
            _sourceIp = "Disconnected";
            Logger.Instance.Info("⏹ UDP Receiver stopped");
            PublishTelemetry();
        }

        private void ResetStats()
        {
            _totalPackets = 0;
            _packetsThisSecond = 0;
            _packetsPerSecond = 0;
            _bytesThisSecond = 0;
            _bytesPerSecond = 0;
            _lastSequenceNumber = 0;
            _hasReceivedFirstPacket = false;
            _hasConnectedOnce = false;
            _packetLossCount = 0;
            _invalidPacketsCount = 0;
            _crcFailuresCount = 0;
            _reconnectCount = 0;
            _lastPacketTime = null;
            _currentPingMs = 0;
            _maxPingMs = 0;
            _minPingMs = 0;
            _pingSum = 0;
            _pingSamplesCount = 0;
            _avgPingMs = 0;
            _previousPingMs = 0;
            _jitterMs = 0;
            _isConnected = false;
            _sourceIp = "Disconnected";
            _lastRawPacket = null;
            _lastTelemetryPublishTime = DateTime.MinValue;
        }

        private void OnRawPacketReceived(byte[] data, IPEndPoint sender)
        {
            DateTime now = DateTime.UtcNow;
            _bytesThisSecond += data.Length;
            _lastRawPacket = data;

            ParseResult result = PacketParser.Parse(data);

            if (result.Status == ParseStatus.Success && result.State != null)
            {
                var state = result.State;
                _totalPackets++;
                _packetsThisSecond++;

                // Ping & Jitter calculation (inter-packet arrival time)
                if (_lastPacketTime.HasValue)
                {
                    double currentPing = (now - _lastPacketTime.Value).TotalMilliseconds;
                    _currentPingMs = currentPing;

                    if (_minPingMs == 0 || currentPing < _minPingMs) _minPingMs = currentPing;
                    if (currentPing > _maxPingMs) _maxPingMs = currentPing;

                    _pingSum += currentPing;
                    _pingSamplesCount++;
                    _avgPingMs = _pingSum / _pingSamplesCount;

                    if (_previousPingMs > 0)
                    {
                        double delta = Math.Abs(currentPing - _previousPingMs);
                        _jitterMs += (delta - _jitterMs) / 16.0; // RFC 3550 smoothing
                    }
                    _previousPingMs = currentPing;
                }

                _lastPacketTime = now;
                _sourceIp = sender.ToString();

                // Sequence & Packet Loss calculation
                if (_hasReceivedFirstPacket)
                {
                    int sequenceDelta = (ushort)(state.SequenceNumber - _lastSequenceNumber);
                    if (sequenceDelta > 1)
                    {
                        long missing = sequenceDelta - 1;
                        _packetLossCount += missing;
                        ushort expected = (ushort)(_lastSequenceNumber + 1);
                        Logger.Instance.Warn($"⚠️ Packet loss: {missing} dropped (got #{state.SequenceNumber}, expected #{expected})");
                    }
                }
                else
                {
                    _hasReceivedFirstPacket = true;
                }
                _lastSequenceNumber = state.SequenceNumber;

                // Connection state transition & Reconnect count
                if (!_isConnected)
                {
                    if (_hasConnectedOnce)
                    {
                        _reconnectCount++;
                        Logger.Instance.Info($"🔄 Reconnected to {sender} (reconnect #{_reconnectCount})");
                    }
                    else
                    {
                        _hasConnectedOnce = true;
                        Logger.Instance.Info($"📱 Connected to Android device at {sender}");
                    }
                    _isConnected = true;
                }

                // Update current state
                _currentState = state;
                _virtualController.SubmitState(state);

                // Fire events
                ControllerStateUpdated?.Invoke(state);

                if ((now - _lastTelemetryPublishTime).TotalMilliseconds >= 50)
                {
                    _lastTelemetryPublishTime = now;
                    PublishTelemetry();
                }
            }
            else
            {
                _invalidPacketsCount++;
                if (result.Status == ParseStatus.CrcMismatch)
                {
                    _crcFailuresCount++;
                    Logger.Instance.Warn("❌ CRC-16 checksum mismatch — packet discarded");
                }
                else
                {
                    Logger.Instance.Warn($"❌ Invalid packet ({result.Status}) — discarded");
                }

                if ((now - _lastTelemetryPublishTime).TotalMilliseconds >= 50)
                {
                    _lastTelemetryPublishTime = now;
                    PublishTelemetry();
                }
            }
        }

        private void OnWatchdogTick(object? state)
        {
            if (_isConnected && _lastPacketTime.HasValue)
            {
                double msSinceLastPacket = (DateTime.UtcNow - _lastPacketTime.Value).TotalMilliseconds;
                if (msSinceLastPacket > 500)
                {
                    _isConnected = false;
                    _sourceIp = "Disconnected (Timeout)";
                    Logger.Instance.Warn("⏱️ Connection timeout (no packets for 500ms)");
                    PublishTelemetry();
                }
            }
        }

        private void OnRateTimerTick(object? state)
        {
            _packetsPerSecond = _packetsThisSecond;
            _packetsThisSecond = 0;

            _bytesPerSecond = _bytesThisSecond;
            _bytesThisSecond = 0;

            if (_server.IsListening)
            {
                PublishTelemetry();
            }
        }

        private void PublishTelemetry()
        {
            double bandwidthKbps = (_bytesPerSecond * 8.0) / 1000.0;

            var stats = new TelemetryStats
            {
                IsConnected = _isConnected,
                TotalPacketsReceived = _totalPackets,
                PacketsPerSecond = _packetsPerSecond,
                CurrentSequenceNumber = _lastSequenceNumber,
                PacketLossCount = _packetLossCount,
                InvalidPacketsCount = _invalidPacketsCount,
                CrcFailuresCount = _crcFailuresCount,
                LastPacketTime = _lastPacketTime,
                SourceIp = _sourceIp,

                CurrentPingMs = Math.Round(_currentPingMs, 1),
                AveragePingMs = Math.Round(_avgPingMs, 1),
                MaxPingMs = Math.Round(_maxPingMs, 1),
                MinPingMs = Math.Round(_minPingMs, 1),
                JitterMs = Math.Round(_jitterMs, 1),
                BandwidthKbps = Math.Round(bandwidthKbps, 2),
                BytesPerSecond = _bytesPerSecond,
                DroppedPackets = _packetLossCount,
                ReconnectCount = _reconnectCount,

                LastRawPacket = _lastRawPacket
            };

            TelemetryUpdated?.Invoke(stats);
        }

        private void OnServerError(Exception ex)
        {
            Logger.Instance.Error($"UDP Server Error: {ex.Message}");
        }

        public void Dispose()
        {
            _watchdogTimer.Dispose();
            _rateTimer.Dispose();
            _server.Dispose();
            _broadcaster.Dispose();
            _virtualController.Dispose();
        }
    }
}
