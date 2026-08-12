using System;
using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;
using VirtualSteerReceiver.Models;
using VirtualSteerReceiver.Utils;

namespace VirtualSteerReceiver.Services
{
    /// <summary>
    /// Wraps ViGEmBus Xbox 360 controller emulation.
    /// Gracefully degrades if the ViGEmBus driver is not installed — the app
    /// still works as a diagnostic receiver, just without Xbox controller output.
    /// </summary>
    public sealed class VirtualControllerService : IDisposable
    {
        private ViGEmClient? _client;
        private IXbox360Controller? _controller;
        private bool _isStarted;
        private bool _driverAvailable;

        public bool IsReady => _isStarted && _controller is not null;

        public VirtualControllerService()
        {
            _driverAvailable = false;
        }

        public void Start()
        {
            if (_isStarted)
            {
                return;
            }

            try
            {
                _client = new ViGEmClient();
                _controller = _client.CreateXbox360Controller();
                _controller.Connect();
                _isStarted = true;
                _driverAvailable = true;
                Logger.Instance.Info("ViGEm Xbox 360 virtual controller started.");
            }
            catch (Nefarius.ViGEm.Client.Exceptions.VigemBusNotFoundException)
            {
                _driverAvailable = false;
                _isStarted = false;
                Logger.Instance.Warn("ViGEmBus driver is NOT installed. Xbox controller emulation disabled. Download from: https://github.com/nefarius/ViGEmBus/releases");
            }
            catch (Exception ex)
            {
                _driverAvailable = false;
                _isStarted = false;
                Logger.Instance.Warn($"ViGEm controller unavailable: {ex.Message}. Xbox controller emulation disabled.");
            }
        }

        public void Stop()
        {
            if (!_isStarted)
            {
                return;
            }

            try
            {
                if (_controller is not null)
                {
                    _controller.Disconnect();
                    _controller = null;
                }
            }
            catch (Exception ex)
            {
                Logger.Instance.Error($"Error stopping ViGEm controller: {ex.Message}");
            }
            finally
            {
                _isStarted = false;
            }
        }

        public void SubmitState(ControllerState state)
        {
            if (!_driverAvailable || !IsReady || _controller is null)
            {
                return;
            }

            try
            {
                _controller.ResetReport();

                short steeringAxis = ScaleAxis(state.Steering);
                byte throttleTrigger = ScaleTrigger(state.Throttle);
                byte brakeTrigger = ScaleTrigger(state.Brake);

                _controller.SetAxisValue(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Axis.LeftThumbX, steeringAxis);
                _controller.SetAxisValue(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Axis.LeftThumbY, (short)0);
                _controller.SetSliderValue(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Slider.RightTrigger, throttleTrigger);
                _controller.SetSliderValue(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Slider.LeftTrigger, brakeTrigger);

                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.X, state.Handbrake);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.A, state.GearUp);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.B, state.GearDown);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.Start, state.Pause);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.Y, state.Horn);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.RightThumb, state.Camera);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.LeftThumb, state.Headlights);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.Up, state.DpadUp);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.Down, state.DpadDown);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.Left, state.DpadLeft);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.Right, state.DpadRight);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.LeftShoulder, state.LB);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.RightShoulder, state.RB);
                _controller.SetButtonState(Nefarius.ViGEm.Client.Targets.Xbox360.Xbox360Button.Back, state.Back);

                _controller.SubmitReport();
            }
            catch (Exception ex)
            {
                Logger.Instance.Error($"ViGEm controller update failed: {ex.Message}");
            }
        }

        private static short ScaleAxis(float value)
        {
            value = Math.Clamp(value, -1.0f, 1.0f);
            return (short)Math.Round(value * short.MaxValue);
        }

        private static byte ScaleTrigger(float value)
        {
            return (byte)Math.Round(Math.Clamp(value, 0.0f, 1.0f) * byte.MaxValue);
        }

        public void Dispose()
        {
            Stop();
            try
            {
                if (_client is IDisposable disposable)
                {
                    disposable.Dispose();
                }
            }
            catch { /* Driver not installed, nothing to dispose */ }
        }
    }
}
