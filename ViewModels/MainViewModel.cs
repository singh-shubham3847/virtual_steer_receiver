using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using System.Windows.Media;
using VirtualSteerReceiver.Models;
using VirtualSteerReceiver.Network;
using VirtualSteerReceiver.Services;
using VirtualSteerReceiver.Utils;

namespace VirtualSteerReceiver.ViewModels
{
    public sealed class MainViewModel : INotifyPropertyChanged, IDisposable
    {
        private readonly ReceiverService _receiverService;

        private int _port = Protocol.ControllerPort;
        private string _hostDisplayName = $"Virtual Steer - {Dns.GetHostName()}";
        private bool _isListening;
        private string _connectionStatusText = "OFFLINE";
        private SolidColorBrush _connectionStatusBrush = new(Color.FromRgb(0x6B, 0x72, 0x80)); // Gray
        private string _localIpAddress = "Detecting...";
        private int _packetsPerSecond;
        private long _totalPackets;
        private ushort _sequenceNumber;
        private long _packetLoss;
        private long _invalidPackets;
        private long _crcFailures;
        private string _sourceIp = "Disconnected";
        private string _lastPacketTimeText = "N/A";
        private long _lastUiUpdateTimeMs;

        // Advanced Network Stats
        private double _currentPingMs;
        private double _averagePingMs;
        private double _maxPingMs;
        private double _minPingMs;
        private double _jitterMs;
        private double _bandwidthKbps;
        private long _bytesPerSecond;
        private long _droppedPackets;
        private long _reconnectCount;

        // Controller telemetry properties
        private float _steering;
        private float _throttle;
        private float _brake;
        private float _clutch;
        private bool _handbrake;
        private bool _gearUp;
        private bool _gearDown;
        private bool _pause;
        private bool _horn;
        private bool _camera;
        private bool _headlights;
        private bool _dpadUp;
        private bool _dpadDown;
        private bool _dpadLeft;
        private bool _dpadRight;
        private bool _lb;
        private bool _rb;
        private bool _back;

        // 100 Hz Test Simulator Stream
        private bool _isTestStreamRunning;
        private CancellationTokenSource? _testStreamCts;
        private bool _showDiagnostics;

        public MainViewModel()
        {
            _receiverService = new ReceiverService();
            _receiverService.ControllerStateUpdated += OnControllerStateUpdated;
            _receiverService.TelemetryUpdated += OnTelemetryUpdated;

            Logger.Instance.LogEntryAdded += OnLogEntryAdded;

            ToggleListenCommand = new RelayCommand(ToggleListen);
            ClearLogsCommand = new RelayCommand(ClearLogs);
            ToggleTestStreamCommand = new RelayCommand(ToggleTestStream);
            ToggleDiagnosticsCommand = new RelayCommand(ToggleDiagnostics);

            DetectLocalIpAddress();

            Logger.Instance.Info("Virtual Steer Companion Receiver initialized.");
            Logger.Instance.Info($"Local PC IP Address: {LocalIpAddress} (Use this IP on your Android App)");
        }

        public ICommand ToggleListenCommand { get; }
        public ICommand ClearLogsCommand { get; }
        public ICommand ToggleTestStreamCommand { get; }
        public ICommand ToggleDiagnosticsCommand { get; }

        public bool ShowDiagnostics
        {
            get => _showDiagnostics;
            set => SetProperty(ref _showDiagnostics, value);
        }

        public ObservableCollection<LogEntry> Logs { get; } = new ObservableCollection<LogEntry>();

        public int Port
        {
            get => _port;
            set
            {
                if (SetProperty(ref _port, value))
                {
                    OnPropertyChanged(nameof(DiscoveryPayloadPreview));
                    ((RelayCommand)ToggleListenCommand).RaiseCanExecuteChanged();
                }
            }
        }

        public string HostDisplayName
        {
            get => _hostDisplayName;
            set
            {
                string sanitized = string.IsNullOrWhiteSpace(value)
                    ? $"Virtual Steer - {Dns.GetHostName()}"
                    : value.Replace(':', '-').Trim();

                if (SetProperty(ref _hostDisplayName, sanitized))
                {
                    OnPropertyChanged(nameof(DiscoveryPayloadPreview));
                    OnPropertyChanged(nameof(PairingStatusText));
                }
            }
        }

        public string DiscoveryPayloadPreview => $"VIRTUAL_STEER_SERVER:{Port}:{HostDisplayName}";
        public string PairingStatusText => IsListening
            ? $"Visible to the mobile app as \"{HostDisplayName}\""
            : $"Start listening to make \"{HostDisplayName}\" appear on the mobile app";

        public bool IsListening
        {
            get => _isListening;
            private set
            {
                if (SetProperty(ref _isListening, value))
                {
                    OnPropertyChanged(nameof(PairingStatusText));
                }
            }
        }

        public string LocalIpAddress
        {
            get => _localIpAddress;
            private set => SetProperty(ref _localIpAddress, value);
        }

        public string ConnectionStatusText
        {
            get => _connectionStatusText;
            private set => SetProperty(ref _connectionStatusText, value);
        }

        public SolidColorBrush ConnectionStatusBrush
        {
            get => _connectionStatusBrush;
            private set => SetProperty(ref _connectionStatusBrush, value);
        }

        public bool IsTestStreamRunning
        {
            get => _isTestStreamRunning;
            private set
            {
                if (SetProperty(ref _isTestStreamRunning, value))
                {
                    OnPropertyChanged(nameof(TestStreamButtonText));
                }
            }
        }

        public string TestStreamButtonText => IsTestStreamRunning ? "⏹ Stop 100Hz Test Stream" : "⚡ Start 100Hz Test Stream";

        public int PacketsPerSecond
        {
            get => _packetsPerSecond;
            private set => SetProperty(ref _packetsPerSecond, value);
        }

        public long TotalPackets
        {
            get => _totalPackets;
            private set => SetProperty(ref _totalPackets, value);
        }

        public ushort SequenceNumber
        {
            get => _sequenceNumber;
            private set => SetProperty(ref _sequenceNumber, value);
        }

        public long PacketLoss
        {
            get => _packetLoss;
            private set => SetProperty(ref _packetLoss, value);
        }

        public long InvalidPackets
        {
            get => _invalidPackets;
            private set => SetProperty(ref _invalidPackets, value);
        }

        public long CrcFailures
        {
            get => _crcFailures;
            private set => SetProperty(ref _crcFailures, value);
        }

        public string SourceIp
        {
            get => _sourceIp;
            private set => SetProperty(ref _sourceIp, value);
        }

        public string LastPacketTimeText
        {
            get => _lastPacketTimeText;
            private set => SetProperty(ref _lastPacketTimeText, value);
        }

        // Advanced Network Stats Properties
        public double CurrentPingMs
        {
            get => _currentPingMs;
            private set { if (SetProperty(ref _currentPingMs, value)) OnPropertyChanged(nameof(CurrentPingText)); }
        }
        public string CurrentPingText => $"{_currentPingMs:0.0} ms";

        public double AveragePingMs
        {
            get => _averagePingMs;
            private set { if (SetProperty(ref _averagePingMs, value)) OnPropertyChanged(nameof(AveragePingText)); }
        }
        public string AveragePingText => $"{_averagePingMs:0.0} ms";

        public double MaxPingMs
        {
            get => _maxPingMs;
            private set { if (SetProperty(ref _maxPingMs, value)) OnPropertyChanged(nameof(MaxPingText)); }
        }
        public string MaxPingText => $"{_maxPingMs:0.0} ms";

        public double MinPingMs
        {
            get => _minPingMs;
            private set { if (SetProperty(ref _minPingMs, value)) OnPropertyChanged(nameof(MinPingText)); }
        }
        public string MinPingText => $"{_minPingMs:0.0} ms";

        public double JitterMs
        {
            get => _jitterMs;
            private set { if (SetProperty(ref _jitterMs, value)) OnPropertyChanged(nameof(JitterText)); }
        }
        public string JitterText => $"{_jitterMs:0.0} ms";

        public double BandwidthKbps
        {
            get => _bandwidthKbps;
            private set { if (SetProperty(ref _bandwidthKbps, value)) OnPropertyChanged(nameof(BandwidthText)); }
        }
        public string BandwidthText => $"{_bandwidthKbps:0.00} Kbps";

        public long BytesPerSecond
        {
            get => _bytesPerSecond;
            private set { if (SetProperty(ref _bytesPerSecond, value)) OnPropertyChanged(nameof(BytesPerSecondText)); }
        }
        public string BytesPerSecondText => $"{_bytesPerSecond:N0} B/s";

        public long DroppedPackets
        {
            get => _droppedPackets;
            private set => SetProperty(ref _droppedPackets, value);
        }

        public long ReconnectCount
        {
            get => _reconnectCount;
            private set => SetProperty(ref _reconnectCount, value);
        }

        // Controller Telemetry
        public float Steering
        {
            get => _steering;
            private set
            {
                if (SetProperty(ref _steering, value))
                {
                    OnPropertyChanged(nameof(SteeringText));
                    OnPropertyChanged(nameof(SteeringGaugeValue));
                    OnPropertyChanged(nameof(SteeringDirectionText));
                }
            }
        }

        public string SteeringText => _steering.ToString("+0.000;-0.000;0.000");
        public string SteeringDirectionText => _steering switch
        {
            < -0.05f => $"◀ LEFT ({Math.Abs(_steering * 100):0}%)",
            > 0.05f => $"RIGHT ▶ ({_steering * 100:0}%)",
            _ => "CENTER (0%)"
        };
        public double SteeringGaugeValue => (_steering + 1.0) / 2.0 * 100.0;

        public float Throttle
        {
            get => _throttle;
            private set
            {
                if (SetProperty(ref _throttle, value))
                {
                    OnPropertyChanged(nameof(ThrottleText));
                    OnPropertyChanged(nameof(ThrottleGaugeValue));
                }
            }
        }

        public string ThrottleText => _throttle.ToString("0.000");
        public double ThrottleGaugeValue => _throttle * 100.0;

        public float Brake
        {
            get => _brake;
            private set
            {
                if (SetProperty(ref _brake, value))
                {
                    OnPropertyChanged(nameof(BrakeText));
                    OnPropertyChanged(nameof(BrakeGaugeValue));
                }
            }
        }

        public string BrakeText => _brake.ToString("0.000");
        public double BrakeGaugeValue => _brake * 100.0;

        public float Clutch
        {
            get => _clutch;
            private set
            {
                if (SetProperty(ref _clutch, value))
                {
                    OnPropertyChanged(nameof(ClutchText));
                    OnPropertyChanged(nameof(ClutchGaugeValue));
                }
            }
        }

        public string ClutchText => _clutch.ToString("0.000");
        public double ClutchGaugeValue => _clutch * 100.0;

        public bool Handbrake
        {
            get => _handbrake;
            private set { if (SetProperty(ref _handbrake, value)) OnPropertyChanged(nameof(HandbrakeText)); }
        }
        public string HandbrakeText => Handbrake ? "ON" : "OFF";

        public bool GearUp
        {
            get => _gearUp;
            private set { if (SetProperty(ref _gearUp, value)) OnPropertyChanged(nameof(GearUpText)); }
        }
        public string GearUpText => GearUp ? "ON" : "OFF";

        public bool GearDown
        {
            get => _gearDown;
            private set { if (SetProperty(ref _gearDown, value)) OnPropertyChanged(nameof(GearDownText)); }
        }
        public string GearDownText => GearDown ? "ON" : "OFF";

        public bool Pause
        {
            get => _pause;
            private set { if (SetProperty(ref _pause, value)) OnPropertyChanged(nameof(PauseText)); }
        }
        public string PauseText => Pause ? "ON" : "OFF";

        public bool Horn
        {
            get => _horn;
            private set { if (SetProperty(ref _horn, value)) OnPropertyChanged(nameof(HornText)); }
        }
        public string HornText => Horn ? "ON" : "OFF";

        public bool Camera
        {
            get => _camera;
            private set { if (SetProperty(ref _camera, value)) OnPropertyChanged(nameof(CameraText)); }
        }
        public string CameraText => Camera ? "ON" : "OFF";

        public bool Headlights
        {
            get => _headlights;
            private set { if (SetProperty(ref _headlights, value)) OnPropertyChanged(nameof(HeadlightsText)); }
        }
        public string HeadlightsText => Headlights ? "ON" : "OFF";

        public bool DpadUp
        {
            get => _dpadUp;
            private set { if (SetProperty(ref _dpadUp, value)) OnPropertyChanged(nameof(DpadUpText)); }
        }
        public string DpadUpText => DpadUp ? "ON" : "OFF";

        public bool DpadDown
        {
            get => _dpadDown;
            private set { if (SetProperty(ref _dpadDown, value)) OnPropertyChanged(nameof(DpadDownText)); }
        }
        public string DpadDownText => DpadDown ? "ON" : "OFF";

        public bool DpadLeft
        {
            get => _dpadLeft;
            private set { if (SetProperty(ref _dpadLeft, value)) OnPropertyChanged(nameof(DpadLeftText)); }
        }
        public string DpadLeftText => DpadLeft ? "ON" : "OFF";

        public bool DpadRight
        {
            get => _dpadRight;
            private set { if (SetProperty(ref _dpadRight, value)) OnPropertyChanged(nameof(DpadRightText)); }
        }
        public string DpadRightText => DpadRight ? "ON" : "OFF";

        public bool LB
        {
            get => _lb;
            private set { if (SetProperty(ref _lb, value)) OnPropertyChanged(nameof(LBText)); }
        }
        public string LBText => LB ? "ON" : "OFF";

        public bool RB
        {
            get => _rb;
            private set { if (SetProperty(ref _rb, value)) OnPropertyChanged(nameof(RBText)); }
        }
        public string RBText => RB ? "ON" : "OFF";

        public bool Back
        {
            get => _back;
            private set { if (SetProperty(ref _back, value)) OnPropertyChanged(nameof(BackText)); }
        }
        public string BackText => Back ? "ON" : "OFF";

        private void DetectLocalIpAddress()
        {
            try
            {
                foreach (NetworkInterface ni in NetworkInterface.GetAllNetworkInterfaces())
                {
                    if (ni.OperationalStatus == OperationalStatus.Up &&
                        (ni.NetworkInterfaceType == NetworkInterfaceType.Wireless80211 ||
                         ni.NetworkInterfaceType == NetworkInterfaceType.Ethernet))
                    {
                        foreach (UnicastIPAddressInformation ip in ni.GetIPProperties().UnicastAddresses)
                        {
                            if (ip.Address.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(ip.Address))
                            {
                                LocalIpAddress = ip.Address.ToString();
                                return;
                            }
                        }
                    }
                }
                LocalIpAddress = "127.0.0.1";
            }
            catch
            {
                LocalIpAddress = "127.0.0.1";
            }
        }

        private void OnControllerStateUpdated(ControllerState state)
        {
            long nowMs = Environment.TickCount64;
            if (nowMs - _lastUiUpdateTimeMs < 16) // Throttle UI refreshes to ~60 FPS to prevent WPF Dispatcher queue saturation at 1000Hz
            {
                return;
            }
            _lastUiUpdateTimeMs = nowMs;

            DispatchUI(() =>
            {
                Steering = state.Steering;
                Throttle = state.Throttle;
                Brake = state.Brake;
                Clutch = state.Clutch;

                Handbrake = state.Handbrake;
                GearUp = state.GearUp;
                GearDown = state.GearDown;
                Pause = state.Pause;
                Horn = state.Horn;
                Camera = state.Camera;
                Headlights = state.Headlights;
                DpadUp = state.DpadUp;
                DpadDown = state.DpadDown;
                DpadLeft = state.DpadLeft;
                DpadRight = state.DpadRight;
                LB = state.LB;
                RB = state.RB;
                Back = state.Back;
            });
        }

        private void OnTelemetryUpdated(TelemetryStats stats)
        {
            DispatchUI(() =>
            {
                if (stats.IsConnected)
                {
                    ConnectionStatusText = "CONNECTED";
                    ConnectionStatusBrush = new SolidColorBrush(Color.FromRgb(0x10, 0xB9, 0x81)); // Green
                }
                else
                {
                    ConnectionStatusText = IsListening ? "DISCONNECTED" : "OFFLINE";
                    ConnectionStatusBrush = IsListening
                        ? new SolidColorBrush(Color.FromRgb(0xEF, 0x44, 0x44)) // Red
                        : new SolidColorBrush(Color.FromRgb(0x6B, 0x72, 0x80)); // Gray
                }

                PacketsPerSecond = stats.PacketsPerSecond;
                TotalPackets = stats.TotalPacketsReceived;
                SequenceNumber = stats.CurrentSequenceNumber;
                PacketLoss = stats.PacketLossCount;
                InvalidPackets = stats.InvalidPacketsCount;
                CrcFailures = stats.CrcFailuresCount;
                SourceIp = stats.SourceIp;

                CurrentPingMs = stats.CurrentPingMs;
                AveragePingMs = stats.AveragePingMs;
                MaxPingMs = stats.MaxPingMs;
                MinPingMs = stats.MinPingMs;
                JitterMs = stats.JitterMs;
                BandwidthKbps = stats.BandwidthKbps;
                BytesPerSecond = stats.BytesPerSecond;
                DroppedPackets = stats.DroppedPackets;
                ReconnectCount = stats.ReconnectCount;

                LastPacketTimeText = stats.LastPacketTime.HasValue
                    ? stats.LastPacketTime.Value.ToString("HH:mm:ss.fff")
                    : "N/A";
            });
        }

        private void OnLogEntryAdded(LogEntry entry)
        {
            DispatchUI(() =>
            {
                Logs.Add(entry);
                while (Logs.Count > 150) Logs.RemoveAt(0);
            });
        }

        private void ToggleListen()
        {
            if (IsListening)
            {
                if (IsTestStreamRunning) StopTestStream();
                _receiverService.Stop();
                IsListening = false;
            }
            else
            {
                try
                {
                    _receiverService.Start(Port, HostDisplayName);
                    IsListening = true;
                }
                catch (Exception ex)
                {
                    Logger.Instance.Error($"Failed to start UDP Server on port {Port}: {ex.Message}");
                    IsListening = false;
                }
            }
        }

        private void ClearLogs()
        {
            Logs.Clear();
        }

        private void ToggleDiagnostics()
        {
            ShowDiagnostics = !ShowDiagnostics;
        }

        private void ToggleTestStream()
        {
            if (IsTestStreamRunning)
            {
                StopTestStream();
            }
            else
            {
                StartTestStream();
            }
        }

        private void StartTestStream()
        {
            if (!IsListening)
            {
                ToggleListen();
                if (!IsListening) return;
            }

            _testStreamCts = new CancellationTokenSource();
            IsTestStreamRunning = true;

            int targetPort = Port;
            Task.Run(() => Run100HzTestStreamLoop(_testStreamCts.Token, targetPort), _testStreamCts.Token);
            Logger.Instance.Info("100 Hz Synthetic UDP Telemetry Stream STARTED on loopback 127.0.0.1");
        }

        private void StopTestStream()
        {
            _testStreamCts?.Cancel();
            IsTestStreamRunning = false;
            Logger.Instance.Info("100 Hz Synthetic UDP Telemetry Stream STOPPED.");
        }

        private async Task Run100HzTestStreamLoop(CancellationToken token, int targetPort)
        {
            using var client = new UdpClient();
            ushort sequence = 1;
            double time = 0;

            using var periodicTimer = new PeriodicTimer(TimeSpan.FromMilliseconds(10));

            try
            {
                while (!token.IsCancellationRequested && await periodicTimer.WaitForNextTickAsync(token))
                {
                    time += 0.01;

                    float steering = (float)Math.Sin(time * 1.5) * 0.95f;
                    float throttle = (float)(Math.Sin(time * 3.0) * 0.5 + 0.5);
                    float brake = throttle < 0.2f ? 0.7f : 0.0f;
                    float clutch = (float)(Math.Sin(time * 2.2) * 0.5 + 0.5);

                    byte buttons = 0;
                    if (steering < -0.6f || steering > 0.6f) buttons |= (byte)Protocol.ButtonFlags.Handbrake;
                    if (throttle > 0.8f) buttons |= (byte)Protocol.ButtonFlags.GearUp;
                    if (brake > 0.5f) buttons |= (byte)Protocol.ButtonFlags.GearDown;
                    if (Math.Sin(time * 0.5) > 0.8) buttons |= (byte)Protocol.ButtonFlags.Horn;
                    if (Math.Sin(time * 0.4) < -0.8) buttons |= (byte)Protocol.ButtonFlags.Headlights;

                    byte[] packet = new byte[Protocol.PACKET_SIZE];
                    packet[0] = Protocol.HEADER;
                    packet[1] = Protocol.VERSION;

                    BitConverter.GetBytes(sequence++).CopyTo(packet, 2);
                    BitConverter.GetBytes(steering).CopyTo(packet, 4);
                    BitConverter.GetBytes(throttle).CopyTo(packet, 8);
                    BitConverter.GetBytes(brake).CopyTo(packet, 12);
                    BitConverter.GetBytes(clutch).CopyTo(packet, 16);
                    packet[20] = buttons;
                    packet[21] = 0;

                    ushort crc = Protocol.CalculateCrc16(packet.AsSpan(0, 22));
                    BitConverter.GetBytes(crc).CopyTo(packet, 22);

                    await client.SendAsync(packet, packet.Length, "127.0.0.1", targetPort);
                }
            }
            catch (OperationCanceledException)
            {
                // Stopped gracefully
            }
            catch (Exception ex)
            {
                Logger.Instance.Error($"Test Stream Error: {ex.Message}");
            }
        }

        private static void DispatchUI(Action action)
        {
            if (Application.Current != null && !Application.Current.Dispatcher.CheckAccess())
            {
                Application.Current.Dispatcher.BeginInvoke(action);
            }
            else
            {
                action();
            }
        }

        public void PropertyChangedNotify([CallerMemberName] string? propertyName = null) => OnPropertyChanged(propertyName);

        public event PropertyChangedEventHandler? PropertyChanged;
        private bool SetProperty<T>(ref T field, T value, [CallerMemberName] string? propertyName = null)
        {
            if (Equals(field, value)) return false;
            field = value;
            OnPropertyChanged(propertyName);
            return true;
        }

        private void OnPropertyChanged([CallerMemberName] string? propertyName = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }

        public void Dispose()
        {
            StopTestStream();
            _receiverService.Dispose();
        }
    }
}
