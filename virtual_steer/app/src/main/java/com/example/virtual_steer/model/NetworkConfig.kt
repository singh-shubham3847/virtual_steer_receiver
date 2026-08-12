package com.example.virtual_steer.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionMode {
    WIFI, HOTSPOT, USB
}

@Serializable
data class NetworkConfig(
    val autoDiscover: Boolean = true,
    val preferredPc: String = "",
    val manualIp: String = "192.168.1.100",
    val udpPort: Int = 4444,
    val packetRate: Int = 100, // Hz
    val heartbeatInterval: Int = 10, // ms
    val reconnectDelay: Int = 2000, // ms
    val preferredConnection: ConnectionMode = ConnectionMode.WIFI,
    val connectionTimeout: Int = 5000 // ms
)
