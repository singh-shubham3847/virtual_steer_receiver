# Virtual Steer - Windows Receiver Development Guide

This guide provides the necessary technical details and code snippets to build a Windows-based receiver for the Virtual Steer Android application.

## 1. Packet Structure (C#)

To correctly parse the high-speed binary data from the Android app, use the following struct with explicit layout in C#.

```csharp
using System.Runtime.InteropServices;

[StructLayout(LayoutKind.Sequential, Pack = 1)]
public struct ControllerPacket
{
    public byte Header;      // Expected: 0x56
    public byte Version;     // Expected: 0x01
    public ushort Sequence;  // Packet sequence ID
    public float Steering;   // Range: -1.0 to 1.0
    public float Throttle;   // Range: 0.0 to 1.0
    public float Brake;      // Range: 0.0 to 1.0
    public float Clutch;     // Range: 0.0 to 1.0
    public byte Buttons;     // Bitfield (see mapping below)
    public byte Reserved;    // Padding
    public ushort CRC;       // 16-bit Checksum
}
```

### Button Bitfield Mapping:
| Bit | Feature |
| :--- | :--- |
| 0 | Handbrake |
| 1 | Gear Up |
| 2 | Gear Down |
| 3 | Pause |
| 4 | Horn |
| 5 | Camera |
| 6 | Headlights |

---

## 2. Server Discovery Protocol

The Android app uses UDP broadcast to find the PC. Your receiver must broadcast its presence on **UDP Port 4445**.

*   **Broadcast Interval**: Every 2 seconds.
*   **Payload Format**: `VIRTUAL_STEER_SERVER:<PORT>:<PC_NAME>`
*   **Example**: `VIRTUAL_STEER_SERVER:4444:MyGamingRig`

```csharp
using System.Net;
using System.Net.Sockets;
using System.Text;

// Example Broadcaster
public void StartDiscovery(string pcName, int udpPort)
{
    UdpClient client = new UdpClient();
    client.EnableBroadcast = true;
    IPEndPoint endPoint = new IPEndPoint(IPAddress.Broadcast, 4445);
    byte[] data = Encoding.ASCII.GetBytes($"VIRTUAL_STEER_SERVER:{udpPort}:{pcName}");

    Task.Run(async () => {
        while (true) {
            await client.SendAsync(data, data.Length, endPoint);
            await Task.Delay(2000);
        }
    });
}
```

---

## 3. UDP Receiver (High Frequency)

Listen for incoming controller states on **UDP Port 4444** (or the port specified in discovery).

```csharp
public void StartReceiver(int port)
{
    UdpClient receiver = new UdpClient(port);
    IPEndPoint remoteIp = new IPEndPoint(IPAddress.Any, 0);

    while (true)
    {
        byte[] data = receiver.Receive(ref remoteIp);
        if (data.Length == 24 && data[0] == 0x56) 
        {
            ControllerPacket packet = ParsePacket(data);
            // Process steering/pedal data...
        }
    }
}

private ControllerPacket ParsePacket(byte[] bytes)
{
    GCHandle handle = GCHandle.Alloc(bytes, GCHandleType.Pinned);
    try {
        return (ControllerPacket)Marshal.PtrToStructure(handle.AddrOfPinnedObject(), typeof(ControllerPacket));
    } finally {
        handle.Free();
    }
}
```

---

## 4. Virtual Controller Integration

To expose the Android inputs to Windows games, you must use a virtual driver.

### Option A: vJoy (Generic Joystick)
*   **Pros**: Highly configurable, works with legacy games.
*   **Driver**: [vJoy](http://vjoystick.sourceforge.net/)
*   **API**: `vJoyInterface.dll` (included in vJoy SDK).

### Option B: ViGEm (Xbox 360 / DualShock 4)
*   **Pros**: Native compatibility with all modern games (XInput). No configuration needed in games.
*   **Driver**: [ViGEmBus](https://github.com/ViGEm/ViGEmBus)
*   **API**: [ViGEm.NET](https://github.com/ViGEm/ViGEm.NET)

---

## 5. Implementation Tips

> [!IMPORTANT]
> **Latency**: Use `Task.Run` with `Priority.Highest` or a dedicated thread for the UDP receiver to avoid UI-thread stuttering.
> **Firewall**: Ensure your Windows Firewall allows inbound traffic on the selected UDP ports.
> **CRC Verification**: For production, always verify the 16-bit CRC to discard corrupted packets.

For the exact CRC algorithm and binary specification, refer to the [README.md](file:///C:/Users/Shubham/AndroidStudioProjects/virtual_steer/README.md).
