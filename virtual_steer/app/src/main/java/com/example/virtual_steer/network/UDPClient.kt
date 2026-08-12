package com.example.virtual_steer.network

import android.util.Log
import com.example.virtual_steer.model.ControllerState
import com.example.virtual_steer.model.NetworkConfig
import com.example.virtual_steer.model.NetworkDiagnostics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference

/**
 * Robust, low-latency UDP client for transmitting ControllerState at a fixed rate.
 */
class UDPClient(
    private val scope: CoroutineScope
) {
    private val TAG = "UDPClient"

    private var socket: DatagramSocket? = null
    private var senderJob: Job? = null
    private val serializer = PacketSerializer()

    private val latestState = AtomicReference<ControllerState>()
    private val targetAddress = AtomicReference<InetAddress>()
    private val targetPort = AtomicReference(4444)
    private val config = AtomicReference<NetworkConfig>(NetworkConfig())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastPacket = MutableStateFlow<ByteArray?>(null)
    val lastPacket: StateFlow<ByteArray?> = _lastPacket.asStateFlow()

    private val _diagnostics = MutableStateFlow(NetworkDiagnostics())
    val diagnostics: StateFlow<NetworkDiagnostics> = _diagnostics.asStateFlow()

    /**
     * Update network configuration (packet rate, heartbeats).
     */
    fun updateConfig(newConfig: NetworkConfig) {
        val oldConfig = config.getAndSet(newConfig)
        if (oldConfig.packetRate != newConfig.packetRate && _isConnected.value) {
            restartSenderLoop()
        }
    }

    /**
     * Connect to the specified IP and port and start the transmission loop.
     */
    fun connect(ipAddress: String, port: Int) {
        if (_isConnected.value) {
            disconnect()
        }

        scope.launch(Dispatchers.IO) {
            try {
                val address = InetAddress.getByName(ipAddress)
                targetAddress.set(address)
                targetPort.set(port)

                socket = DatagramSocket()
                _isConnected.value = true
                _diagnostics.value = NetworkDiagnostics(
                    connected = true,
                    targetIp = ipAddress,
                    port = port,
                    packetRate = 0
                )
                
                startSenderLoop()
                Log.d(TAG, "Connected to $ipAddress:$port")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to $ipAddress:$port", e)
                _isConnected.value = false
            }
        }
    }

    /**
     * Stop the transmission loop and close the socket.
     */
    fun disconnect() {
        senderJob?.cancel()
        senderJob = null
        
        socket?.close()
        socket = null
        
        _isConnected.value = false
        _diagnostics.value = _diagnostics.value.copy(connected = false, packetRate = 0)
        Log.d(TAG, "Disconnected")
    }

    /**
     * Update the latest state to be sent in the next transmission cycle.
     */
    fun updateControllerState(state: ControllerState) {
        latestState.set(state)
    }

    private fun restartSenderLoop() {
        senderJob?.cancel()
        startSenderLoop()
    }

    private fun startSenderLoop() {
        val intervalMs = (1000f / config.get().packetRate.coerceAtLeast(1)).toLong()
        
        senderJob = scope.launch(Dispatchers.IO) {
            var lastHeartbeat = 0L
            var packetsSent = 0L
            var rateWindowStart = System.currentTimeMillis()
            var packetsInWindow = 0
            
            while (isActive) {
                val state = latestState.get()
                val addr = targetAddress.get()
                val port = targetPort.get()
                val currentConfig = config.get()

                if (addr != null) {
                    try {
                        val now = System.currentTimeMillis()
                        val shouldSendState = state != null
                        val shouldSendHeartbeat = now - lastHeartbeat >= currentConfig.heartbeatInterval

                        if (shouldSendState || shouldSendHeartbeat) {
                            val data = serializer.serialize(state ?: ControllerState())
                            val packet = DatagramPacket(data, data.size, addr, port)
                            socket?.send(packet)
                            _lastPacket.value = data
                            packetsSent++
                            packetsInWindow++
                            
                            if (shouldSendHeartbeat) {
                                lastHeartbeat = now
                            }

                            if (now - rateWindowStart >= 1000L) {
                                _diagnostics.value = _diagnostics.value.copy(
                                    connected = true,
                                    targetIp = addr.hostAddress ?: "N/A",
                                    port = port,
                                    packetRate = packetsInWindow,
                                    packetsSent = packetsSent
                                )
                                packetsInWindow = 0
                                rateWindowStart = now
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Transmission error", e)
                    }
                }
                delay(intervalMs)
            }
        }
    }
}
