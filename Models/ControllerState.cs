using System;

namespace VirtualSteerReceiver.Models
{
    public sealed record ControllerState
    {
        public float Steering { get; init; }
        public float Throttle { get; init; }
        public float Brake { get; init; }
        public float Clutch { get; init; }
        public bool Handbrake { get; init; }
        public bool GearUp { get; init; }
        public bool GearDown { get; init; }
        public bool Pause { get; init; }
        public bool Horn { get; init; }
        public bool Camera { get; init; }
        public bool Headlights { get; init; }
        public ushort SequenceNumber { get; init; }
        public DateTime Timestamp { get; init; } = DateTime.UtcNow;

        public static ControllerState Empty { get; } = new ControllerState();
    }
}
