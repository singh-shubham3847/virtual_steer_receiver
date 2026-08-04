using System;
using VirtualSteerReceiver.Models;
using VirtualSteerReceiver.Network;

namespace VirtualSteerReceiver.Tests
{
    public static class TestRunner
    {
        public static void Main(string[] args)
        {
            Console.WriteLine("==================================================");
            Console.WriteLine(" Virtual Steer Receiver - Protocol Unit Tests");
            Console.WriteLine("==================================================");

            int passed = 0;
            int total = 0;

            void Assert(bool condition, string testName)
            {
                total++;
                if (condition)
                {
                    passed++;
                    Console.WriteLine($"[PASS] {testName}");
                }
                else
                {
                    Console.WriteLine($"[FAIL] {testName}");
                }
            }

            // Test 1: Valid Packet Decoding
            byte[] packet = CreateValidPacket(101, 0.432f, 0.817f, 0.000f, 0.250f, Protocol.ButtonFlags.Handbrake | Protocol.ButtonFlags.Horn | Protocol.ButtonFlags.Headlights);
            ParseResult res1 = PacketParser.Parse(packet);
            Assert(res1.Status == ParseStatus.Success, "Valid Packet Status");
            Assert(res1.State?.SequenceNumber == 101, "Sequence Number Decoding");
            Assert(Math.Abs((res1.State?.Steering ?? 0f) - 0.432f) < 0.001f, "Steering Float Decoding");
            Assert(Math.Abs((res1.State?.Throttle ?? 0f) - 0.817f) < 0.001f, "Throttle Float Decoding");
            Assert(Math.Abs((res1.State?.Clutch ?? 0f) - 0.250f) < 0.001f, "Clutch Float Decoding");
            Assert(res1.State?.Handbrake == true, "Handbrake Button Flag Decoding");
            Assert(res1.State?.Horn == true, "Horn Button Flag Decoding");
            Assert(res1.State?.Headlights == true, "Headlights Button Flag Decoding");
            Assert(res1.State?.GearUp == false, "GearUp Unset Button Flag");
            Assert(Protocol.CalculateCrc16("123456789"u8) == 0x31C3, "Android CRC-16/XMODEM Known Value");
            Assert(Protocol.CalculateCrc16(ReadOnlySpan<byte>.Empty) == 0x0000, "Android CRC-16/XMODEM Empty Value");

            // Test 2: Invalid Header
            byte[] badHeader = (byte[])packet.Clone();
            badHeader[0] = 0xFF;
            ParseResult res2 = PacketParser.Parse(badHeader);
            Assert(res2.Status == ParseStatus.InvalidHeader, "Invalid Header Detection");

            // Test 3: Invalid Version
            byte[] badVersion = (byte[])packet.Clone();
            badVersion[1] = 0x99;
            ParseResult res3 = PacketParser.Parse(badVersion);
            Assert(res3.Status == ParseStatus.InvalidVersion, "Invalid Version Detection");

            // Test 4: CRC Checksum Corruption
            byte[] corrupted = (byte[])packet.Clone();
            corrupted[10] ^= 0xFF; // Corrupt throttle byte
            ParseResult res4 = PacketParser.Parse(corrupted);
            Assert(res4.Status == ParseStatus.CrcMismatch, "CRC-16 Checksum Corruption Detection");

            // Test 5: Invalid Size
            byte[] badSize = new byte[20];
            ParseResult res5 = PacketParser.Parse(badSize);
            Assert(res5.Status == ParseStatus.InvalidSize, "Invalid Packet Size Detection");

            Console.WriteLine("--------------------------------------------------");
            Console.WriteLine($"Results: {passed} / {total} tests passed.");
            Console.WriteLine("==================================================");

            if (passed != total)
            {
                Environment.Exit(1);
            }
        }

        private static byte[] CreateValidPacket(ushort seq, float steering, float throttle, float brake, float clutch, Protocol.ButtonFlags buttons)
        {
            byte[] buf = new byte[Protocol.PACKET_SIZE];
            buf[0] = Protocol.HEADER;
            buf[1] = Protocol.VERSION;
            BitConverter.GetBytes(seq).CopyTo(buf, 2);
            BitConverter.GetBytes(steering).CopyTo(buf, 4);
            BitConverter.GetBytes(throttle).CopyTo(buf, 8);
            BitConverter.GetBytes(brake).CopyTo(buf, 12);
            BitConverter.GetBytes(clutch).CopyTo(buf, 16);
            buf[20] = (byte)buttons;
            buf[21] = 0;

            ushort crc = Protocol.CalculateCrc16(buf.AsSpan(0, 22));
            BitConverter.GetBytes(crc).CopyTo(buf, 22);
            return buf;
        }
    }
}
