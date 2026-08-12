package com.example.virtual_steer.network

import android.util.Log
import com.example.virtual_steer.model.DiscoveredServer
import com.example.virtual_steer.model.DiscoveryResponse
import com.example.virtual_steer.model.NetworkConfig
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class DiscoveryClient(
    private val scope: CoroutineScope,
    private val onServerFound: (DiscoveredServer) -> Unit
) {
    private val TAG = "DiscoveryClient"
    private val DISCOVERY_PORT = 4445
    private val DISCOVERY_MSG = "DISCOVER_VIRTUAL_STEER"
    
    private var job: Job? = null
    private var config: NetworkConfig = NetworkConfig()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    fun updateConfig(newConfig: NetworkConfig) {
        val wasActive = job != null
        config = newConfig
        if (wasActive && !config.autoDiscover) {
            stop()
        } else if (!wasActive && config.autoDiscover) {
            start()
        }
    }

    fun start() {
        if (job != null) return

        job = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(DISCOVERY_PORT))
                }
                
                // Start a sub-job for periodic broadcasting to all active network interface broadcast addresses
                val broadcaster = launch {
                    val sendData = DISCOVERY_MSG.toByteArray()
                    while (isActive) {
                        try {
                            val broadcastAddresses = mutableListOf<InetAddress>()
                            // Always fallback to global broadcast
                            broadcastAddresses.add(InetAddress.getByName("255.255.255.255"))

                            try {
                                val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                                if (interfaces != null) {
                                    for (networkInterface in java.util.Collections.list(interfaces)) {
                                        if (networkInterface.isLoopback || !networkInterface.isUp) continue
                                        for (interfaceAddress in networkInterface.interfaceAddresses) {
                                            val broadcast = interfaceAddress.broadcast
                                            if (broadcast != null) {
                                                broadcastAddresses.add(broadcast)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to retrieve local interface broadcasts", e)
                            }

                            // De-duplicate addresses
                            val uniqueAddresses = broadcastAddresses.distinct()

                            for (broadcastAddr in uniqueAddresses) {
                                try {
                                    Log.d(TAG, "Broadcasting discovery request to $broadcastAddr...")
                                    val sendPacket = DatagramPacket(sendData, sendData.size, broadcastAddr, DISCOVERY_PORT)
                                    socket.send(sendPacket)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Broadcast failed to $broadcastAddr", e)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Discovery broadcast round failed", e)
                        }
                        delay(3000) // Every 3 seconds
                    }
                }

                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)

                Log.d(TAG, "Listening for discovery replies on port $DISCOVERY_PORT")

                while (isActive) {
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()
                    val senderIp = packet.address.hostAddress ?: continue
                    
                    // Ignore our own broadcast message
                    if (message == DISCOVERY_MSG) continue

                    Log.d(TAG, "Received discovery reply from $senderIp: $message")

                    try {
                        if (message.startsWith("{")) {
                            // Try JSON parsing
                            val response = json.decodeFromString<DiscoveryResponse>(message)
                            onServerFound(DiscoveredServer(senderIp, response.port, response.hostname))
                        } else if (message.startsWith("VIRTUAL_STEER_SERVER:")) {
                            // Try legacy string parsing
                            val parts = message.split(":")
                            val serverPort = parts.getOrNull(1)?.toIntOrNull() ?: 4444
                            val serverName = parts.getOrNull(2) ?: "Unknown PC"
                            onServerFound(DiscoveredServer(senderIp, serverPort, serverName))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse discovery message", e)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Discovery error", e)
                }
            } finally {
                socket?.close()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
