# 📱 Virtual Steer Android Companion App

Virtual Steer is an ultra-low latency, high-precision virtual steering wheel controller that transforms your Android smartphone into a wireless racing wheel. By utilizing your device's built-in motion sensors, it emulates a physical steering axis and transmits control inputs to a Windows host PC over UDP, feeding directly into a virtual Xbox 360 controller via the ViGEmBus driver.

---

## ⚡ Core Features & Recent Upgrades

* **🎮 Drag-and-Drop Layout Editor:** Fully customizable button positioning. Tap **"Adjust Button Positions"** in Settings to enter edit mode, drag buttons (`Pause`, `Cam`, `Lights`, `GEAR-`, `HBRAKE`, `GEAR+`, and `📻 RADIO`) anywhere on the screen, and click **"Save & Exit"** to persist your layout.
* **📏 135° Steering Limits:** Reconfigured default handling sensitivity to map to 135° steering by default for optimal control.
* **📻 Compact Radio Channel Control:** Includes a dedicated, compact `📻 RADIO` station button that cycles channels by pulsing `DpadRight` (standard radio button for NFS, Forza, and GTA V).
* **🔋 Live Battery Tracking:** Direct integration with Android's `BatteryManager` service to display real-time mobile battery status on the main dashboard and diagnostics.
* **⚙️ Detailed Pedals & Sensor Settings:**
  * **Pedal Tuning:** Set custom Pedal Deadzone thresholds, Smoothing filters to reduce jitter, and Invert pedal orientations.
  * **Sensor Source Selection:** Choose between **Rotation Vector** (Gyroscope-assisted sensor fusion) or **Accelerometer** (Gravity tilt roll fallback for budget devices).
* **🧹 Clutter-Free UI:** Hidden layout adjustments, steering angles, and detailed latency metrics from the main driving HUD to keep focus purely on the road.
* **🏎️ Zero-Configuration Auto-Discovery:** Broadcasting UDP packets find your PC receiver instantly on local Wi-Fi or when using **Android Hotspot**.

---

## 🛠️ Build & Installation Guide

You can compile a standalone `.apk` directly from this repository using **Android Studio** or the **Gradle command line**.

### Prerequisites
* **Java Development Kit (JDK 17)** or higher.
* **Android SDK** (API Level 34+ recommended).

### 1. Build via Command Line (CLI)
Open your terminal in the `virtual_steer` project directory:

* **Compile Debug APK (Fastest, for testing):**
  ```cmd
  .\gradlew.bat assembleDebug
  ```
  *Your compiled package will be saved to:*  
  📂 `app/build/outputs/apk/debug/app-debug.apk`

* **Compile Release APK (Optimized):**
  ```cmd
  .\gradlew.bat assembleRelease
  ```
  *Your compiled package will be saved to:*  
  📂 `app/build/outputs/apk/release/app-release-unsigned.apk`

* **Clean Build Cache:**
  ```cmd
  .\gradlew.bat clean
  ```

### 2. Build via Android Studio
1. Open Android Studio and select **File > Open**.
2. Select the directory: `C:\Users\Shubham\.gemini\antigravity\scratch\VirtualSteerReceiver\virtual_steer`.
3. Wait for the Gradle sync to finish.
4. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)** in the top menu.
5. A popup will appear in the bottom-right corner when complete. Click **Locate** to find the output APK.

---

## 🏎️ Connection & Setup Checklist

For the lowest possible latency and minimum interference, **Mobile Hotspot Mode** is highly recommended.

1. **Enable Mobile Hotspot** on your Android phone.
2. **Connect your Windows PC** to this mobile hotspot network.
3. Open the **Virtual Steer Windows Receiver** on your PC and click **Start Listening**.
4. Launch the **Virtual Steer Android App**. It should automatically detect and pair with your PC.
   * *If auto-discovery is blocked by a firewall, go to **Settings > Network** in the app and manually enter your PC's IP address.*
5. **Calibrate Steering:** Hold the phone level in your preferred driving angle and tap **CALIBRATE (Set Center)**.
6. Verify your inputs on PC using Windows Game Controllers setting (**`joy.cpl`**).

---

## 📊 Communication Protocol (v1.1)

Data is serialized into a highly compacted **24-byte binary packet** for maximum network throughput:

| Offset | Data Type | Field | Description |
| :--- | :--- | :--- | :--- |
| **0** | Byte | Header | `0x56` (representing ASCII 'V') |
| **1** | Byte | Version | `0x01` |
| **2** | Short | Sequence | Packet sequence ID (for loss detection) |
| **4** | Float | Steering | Left/Right axis `[-1.0, 1.0]` |
| **8** | Float | Throttle | Gas pedal `[0.0, 1.0]` |
| **12** | Float | Brake | Brake pedal `[0.0, 1.0]` |
| **16** | Float | Clutch | Clutch pedal `[0.0, 1.0]` |
| **20** | UShort | Buttons | 16-bit bitfield mapped to Xbox digital buttons |
| **22** | Short | CRC | 16-bit Checksum (calculated over offsets 0-21) |

### Action Button Bitfield Maps
* **Bit 0:** Handbrake
* **Bit 1:** Gear Up
* **Bit 2:** Gear Down
* **Bit 3:** Pause
* **Bit 4:** Horn
* **Bit 5:** Camera
* **Bit 6:** Headlights
* **Bit 7:** D-pad Up (Menu Navigate)
* **Bit 8:** D-pad Down (Menu Navigate)
* **Bit 9:** D-pad Left (Menu Navigate)
* **Bit 10:** D-pad Right / Radio (pulsed via dedicated HUD button)
* **Bit 11:** Left Shoulder / LB (Xbox game mappings)
* **Bit 12:** Right Shoulder / RB (Xbox game mappings)
* **Bit 13:** Back / Select (Xbox game mappings)
