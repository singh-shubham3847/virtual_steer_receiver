using System;
using VirtualSteerReceiver.Models;

namespace VirtualSteerReceiver.Network
{
    public enum ParseStatus
    {
        Success,
        InvalidSize,
        InvalidHeader,
        InvalidVersion,
        CrcMismatch
    }

    public readonly struct ParseResult
    {
        public ParseStatus Status { get; }
        public ControllerState? State { get; }

        public ParseResult(ParseStatus status, ControllerState? state = null)
        {
            Status = status;
            State = state;
        }
    }

    public static class PacketParser
    {
        public static ParseResult Parse(ReadOnlySpan<byte> buffer)
        {
            if (buffer.Length != Protocol.PACKET_SIZE)
            {
                return new ParseResult(ParseStatus.InvalidSize);
            }

            if (buffer[0] != Protocol.HEADER)
            {
                return new ParseResult(ParseStatus.InvalidHeader);
            }

            if (buffer[1] != Protocol.VERSION)
            {
                return new ParseResult(ParseStatus.InvalidVersion);
            }

            ushort expectedCrc = BitConverter.ToUInt16(buffer.Slice(22, 2));
            ushort computedCrc = Protocol.CalculateCrc16(buffer.Slice(0, 22));

            if (expectedCrc != computedCrc)
            {
                return new ParseResult(ParseStatus.CrcMismatch);
            }

            ushort sequenceNumber = BitConverter.ToUInt16(buffer.Slice(2, 2));
            float steering = BitConverter.ToSingle(buffer.Slice(4, 4));
            float throttle = BitConverter.ToSingle(buffer.Slice(8, 4));
            float brake = BitConverter.ToSingle(buffer.Slice(12, 4));
            float clutch = BitConverter.ToSingle(buffer.Slice(16, 4));
            ushort buttonsRaw = BitConverter.ToUInt16(buffer.Slice(20, 2));

            var buttons = (Protocol.ButtonFlags)buttonsRaw;

            var state = new ControllerState
            {
                SequenceNumber = sequenceNumber,
                Steering = Math.Clamp(steering, -1.0f, 1.0f),
                Throttle = Math.Clamp(throttle, 0.0f, 1.0f),
                Brake = Math.Clamp(brake, 0.0f, 1.0f),
                Clutch = Math.Clamp(clutch, 0.0f, 1.0f),
                Handbrake = buttons.HasFlag(Protocol.ButtonFlags.Handbrake),
                GearUp = buttons.HasFlag(Protocol.ButtonFlags.GearUp),
                GearDown = buttons.HasFlag(Protocol.ButtonFlags.GearDown),
                Pause = buttons.HasFlag(Protocol.ButtonFlags.Pause),
                Horn = buttons.HasFlag(Protocol.ButtonFlags.Horn),
                Camera = buttons.HasFlag(Protocol.ButtonFlags.Camera),
                Headlights = buttons.HasFlag(Protocol.ButtonFlags.Headlights),
                DpadUp = buttons.HasFlag(Protocol.ButtonFlags.DpadUp),
                DpadDown = buttons.HasFlag(Protocol.ButtonFlags.DpadDown),
                DpadLeft = buttons.HasFlag(Protocol.ButtonFlags.DpadLeft),
                DpadRight = buttons.HasFlag(Protocol.ButtonFlags.DpadRight),
                LB = buttons.HasFlag(Protocol.ButtonFlags.LB),
                RB = buttons.HasFlag(Protocol.ButtonFlags.RB),
                Back = buttons.HasFlag(Protocol.ButtonFlags.Back),
                Timestamp = DateTime.UtcNow
            };

            return new ParseResult(ParseStatus.Success, state);
        }
    }
}
