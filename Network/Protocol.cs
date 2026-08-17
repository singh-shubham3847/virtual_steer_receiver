using System;

namespace VirtualSteerReceiver.Network
{
    public static class Protocol
    {
        public const int PACKET_SIZE = 32;
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

        // Fast Lookup Table for CRC-16-CCITT (poly 0x1021)
        private static readonly ushort[] CrcTable;

        static Protocol()
        {
            CrcTable = new ushort[256];
            for (int i = 0; i < 256; i++)
            {
                ushort value = (ushort)(i << 8);
                for (int bit = 0; bit < 8; bit++)
                {
                    bool topBit = (value & 0x8000) != 0;
                    value = (ushort)(value << 1);
                    if (topBit)
                    {
                        value ^= 0x1021;
                    }
                }
                CrcTable[i] = value;
            }
        }

        /// <summary>
        /// Fast Lookup-Table-based CRC-16 calculation.
        /// </summary>
        public static ushort CalculateCrc16(ReadOnlySpan<byte> buffer)
        {
            ushort crc = 0x0000;
            for (int i = 0; i < buffer.Length; i++)
            {
                byte index = (byte)((crc >> 8) ^ buffer[i]);
                crc = (ushort)((crc << 8) ^ CrcTable[index]);
            }
            return crc;
        }

        /// <summary>
        /// Fast Lookup-Table-based legacy CRC-16 calculation (init 0xFFFF).
        /// </summary>
        public static ushort CalculateLegacyCrc16(ReadOnlySpan<byte> buffer)
        {
            ushort crc = 0xFFFF;
            for (int i = 0; i < buffer.Length; i++)
            {
                byte index = (byte)((crc >> 8) ^ buffer[i]);
                crc = (ushort)((crc << 8) ^ CrcTable[index]);
            }
            return crc;
        }
    }
}
