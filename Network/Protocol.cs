using System;

namespace VirtualSteerReceiver.Network
{
    public static class Protocol
    {
        public const int PACKET_SIZE = 24;
        public const byte HEADER = 0x56; // 'V'
        public const byte VERSION = 0x01; // Version 1
        public const int ControllerPort = 4444;
        public const int DiscoveryPort = 4445;

        [Flags]
        public enum ButtonFlags : ushort
        {
            None       = 0,
            Handbrake  = 1 << 0,
            GearUp     = 1 << 1,
            GearDown   = 1 << 2,
            Pause      = 1 << 3,
            Horn       = 1 << 4,
            Camera     = 1 << 5,
            Headlights = 1 << 6,
            DpadUp     = 1 << 7,
            DpadDown   = 1 << 8,
            DpadLeft   = 1 << 9,
            DpadRight  = 1 << 10,
            LB         = 1 << 11,
            RB         = 1 << 12,
            Back       = 1 << 13
        }

        public static ushort CalculateCrc16(ReadOnlySpan<byte> buffer)
        {
            ushort crc = 0x0000;
            for (int i = 0; i < buffer.Length; i++)
            {
                byte value = buffer[i];
                for (int bit = 0; bit < 8; bit++)
                {
                    bool inputBit = ((value >> (7 - bit)) & 1) == 1;
                    bool topBit = ((crc >> 15) & 1) == 1;
                    crc = (ushort)(crc << 1);
                    if (topBit ^ inputBit)
                    {
                        crc ^= 0x1021;
                    }
                }
            }
            return crc;
        }

        public static ushort CalculateLegacyCrc16(ReadOnlySpan<byte> buffer)
        {
            ushort crc = 0xFFFF;
            for (int i = 0; i < buffer.Length; i++)
            {
                crc ^= (ushort)(buffer[i] << 8);
                for (int bit = 0; bit < 8; bit++)
                {
                    crc = (crc & 0x8000) != 0
                        ? (ushort)((crc << 1) ^ 0x1021)
                        : (ushort)(crc << 1);
                }
            }
            return crc;
        }
    }
}
