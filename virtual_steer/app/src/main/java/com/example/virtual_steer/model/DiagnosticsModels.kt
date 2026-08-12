package com.example.virtual_steer.model

data class SteeringDiagnostics(
    val rawAngle: Float = 0f,
    val calibrationOffset: Float = 0f,
    val calibratedAngle: Float = 0f,
    val deadZoneApplied: Float = 0f,
    val smoothedAngle: Float = 0f,
    val finalOutput: Float = 0f,
    val percentage: Float = 0f
)

data class PedalDiagnostics(
    val rawValue: Float = 0f,
    val deadZoneApplied: Float = 0f,
    val curveOutput: Float = 0f,
    val smoothedValue: Float = 0f,
    val finalOutput: Float = 0f,
    val percentage: Float = 0f
)

data class SensorDiagnostics(
    val rotationFreq: Float = 0f,
    val gyroFreq: Float = 0f,
    val accelFreq: Float = 0f,
    val lastUpdateMs: Long = 0L
)

data class NetworkDiagnostics(
    val connected: Boolean = false,
    val targetIp: String = "N/A",
    val port: Int = 0,
    val packetRate: Int = 0,
    val latencyMs: Int = 0,
    val packetLoss: Float = 0f,
    val packetsSent: Long = 0,
    val packetsLost: Long = 0,
    val reconnectCount: Int = 0
)

data class SystemDiagnostics(
    val steering: SteeringDiagnostics = SteeringDiagnostics(),
    val throttle: PedalDiagnostics = PedalDiagnostics(),
    val brake: PedalDiagnostics = PedalDiagnostics(),
    val sensors: SensorDiagnostics = SensorDiagnostics(),
    val network: NetworkDiagnostics = NetworkDiagnostics(),
    val fps: Int = 0,
    val battery: Int = 0,
    val cpuUsage: Float = 0f,
    val temperature: Float = 0f
)
