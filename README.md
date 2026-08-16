# 🏎️ Virtual Steer: Wireless Android Gyro Wheel & Windows Receiver

Virtual Steer is an ultra-low latency, high-precision virtual steering wheel controller system. It turns your Android smartphone into a wireless racing wheel using built-in motion sensors and streams control data over UDP to a WPF Windows companion application. The receiver emulates a physical Xbox 360 controller via the **ViGEmBus** driver, feeding inputs directly into any PC racing game (Forza, Assetto Corsa, F1, GTA V, etc.).

---
## ⚡ Key Features

### 💻 Windows WPF Companion Receiver
* **⚡ Zero-Allocation Network Loop:** Fully refactored UDP listener using a raw `Socket` and a dedicated `ThreadPriority.Highest` background thread with zero-allocation `ReadOnlySpan<byte>` event dispatching, cutting CPU overhead to near-zero.
* **📈 60 FPS Dispatcher Throttling:** Smart throttle controls limit UI refreshes to a clean 60Hz. Kernel-level Xbox controller updates are still processed instantly on packet arrival (sub-millisecond latency), but the WPF Dispatcher is kept completely unburdened.
* **🧮 CRC-16 Lookup Table (LUT):** Replaced bit-by-bit computation (176 iterations) with a pre-computed 256-element lookup table (22 iterations, zero branching), improving parsing speed by 8x.
* **📊 Live Gauges Dashboard:** High-fidelity overview panel displaying connection duration, latency, packet rates, and animated gauges for Steering, Throttle, and Brake.
* **📈 Real-Time Graphing:** Canvas-drawn polyline graphs plotting Steering, Throttle, Brake, Latency, and Jitter over the last 300 packets.
* **🔍 Packet Hex Inspector:** Live hex dump viewer with ASCII decoder to trace and debug raw incoming UDP data byte-by-byte.
* **📝 System Event Log:** Internal logging terminal categorized by levels (Info, Warning, Error) with file export capabilities.
* **📦 Installer Ready:** Includes Inno Setup integration (`installer.iss`) to compile a single-file Windows installer.

---

## 🚀 Quick Start & Setup

### Requirements
* **PC:** Windows 10/11, [.NET 8.0 Runtime](https://dotnet.microsoft.com/download), and the [ViGEmBus Driver](https://github.com/nefarius/ViGEmBus/releases).
* **Phone:** Android 8.0 (API Level 26) or higher.

### Step 1: Connect Devices (Hotspot Recommended)
1. Turn on **Mobile Hotspot** on your Android device.
2. Connect your Windows PC to the phone's Hotspot network.
3. *Alternatively, connect both devices to the same local Wi-Fi network.*

### Step 2: Run Receiver & App
1. Launch `VirtualSteerReceiver.exe` on your PC and click **Start Listening** (default port `5000`).
2. Open the **Virtual Steer** app on your phone. It will broadcast and auto-pair with the PC.
3. Hold the phone level in your preferred driving angle and tap **CALIBRATE (Set Center)**.
4. Run `joy.cpl` in Windows Run to test the virtual controller axes and buttons.

---

## 📊 Communication Protocol (v1.1)

Telemetry and control signals are packed into a compact, fixed **24-byte binary packet** sent over UDP:

| Offset | Data Type | Field | Description |
| :--- | :--- | :--- | :--- |
| **0** | Byte | Header | `0x56` (ASCII 'V') |
| **1** | Byte | Version | `0x01` |
| **2** | Short | Sequence | Packet sequence ID for tracking packet loss |
| **4** | Float | Steering | Steering axis `[-1.0, 1.0]` |
| **8** | Float | Throttle | Gas pedal `[0.0, 1.0]` |
| **12** | Float | Brake | Brake pedal `[0.0, 1.0]` |
| **16** | Float | Clutch | Clutch pedal `[0.0, 1.0]` |
| **20** | UShort | Buttons | 16-bit bitfield for digital controllers button mapping |
| **22** | Short | CRC | 16-bit CRC Checksum calculated over offsets 0-21 |

### Button Mapping Bitmask (16-bit)
* **0x0001 (Bit 0):** Handbrake (Space)
* **0x0002 (Bit 1):** Gear Up (E)
* **0x0004 (Bit 2):** Gear Down (Q)
* **0x0008 (Bit 3):** Pause (Esc)
* **0x0010 (Bit 4):** Horn (H)
* **0x0020 (Bit 5):** Camera (C)
* **0x0040 (Bit 6):** Headlights (L)
* **0x0080 (Bit 7):** D-pad Up
* **0x0100 (Bit 8):** D-pad Down
* **0x0200 (Bit 9):** D-pad Left
* **0x0400 (Bit 10):** D-pad Right / Cycle Radio Station
* **0x0800 (Bit 11):** Left Shoulder (LB)
* **0x1000 (Bit 12):** Right Shoulder (RB)
* **0x2000 (Bit 13):** Back / Select Button

---

## 🛠️ Build from Source

### WPF Receiver Build
Open `VirtualSteerReceiver.sln` in Visual Studio 2022 and compile, or build via the .NET CLI:
```cmd
dotnet build --configuration Release
```
