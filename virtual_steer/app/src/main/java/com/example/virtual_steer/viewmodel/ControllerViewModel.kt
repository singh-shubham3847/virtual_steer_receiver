package com.example.virtual_steer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.virtual_steer.engine.ControllerEngine
import com.example.virtual_steer.model.*
import com.example.virtual_steer.network.DiscoveryClient
import com.example.virtual_steer.network.UDPClient
import com.example.virtual_steer.repository.SettingsRepository
import com.example.virtual_steer.sensors.RotationSensor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class ConnectionStatus {
    IDLE, SEARCHING, CONNECTING, CONNECTED, DISCONNECTED, RECONNECTING
}

data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.IDLE,
    val serverIp: String = "N/A",
    val serverName: String = "Unknown PC",
    val latencyMs: Int = 0,
    val packetRate: Int = 0
)

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

enum class LogLevel { INFO, WARN, ERROR }

class ControllerViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val controllerEngine = ControllerEngine()
    private val rotationSensor = RotationSensor(application)
    
    private val udpClient = UDPClient(viewModelScope)
    private val discoveryClient = DiscoveryClient(viewModelScope) { server ->
        onServerDiscovered(server)
    }

    // Processed angle for the UI/Engine
    private val _steeringAngle = MutableStateFlow(0f)
    val steeringAngle: StateFlow<Float> = _steeringAngle.asStateFlow()

    // Telemetry / Diagnostics
    val diagnostics: StateFlow<SystemDiagnostics> = controllerEngine.diagnostics

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredServers = MutableStateFlow<Map<String, DiscoveredServer>>(emptyMap())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers
        .map { it.values.toList().sortedByDescending { s -> s.lastSeen } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val eventLogs: StateFlow<List<LogEntry>> = _eventLogs.asStateFlow()

    val lastPacket: StateFlow<ByteArray?> = udpClient.lastPacket

    val isConnected: StateFlow<Boolean> = udpClient.isConnected

    private var lastRawAngle = 0f

    init {
        log("SYS", "Initializing Controller Engine v1.0")

        // Periodic battery level check
        viewModelScope.launch {
            val batteryManager = application.getSystemService(android.content.Context.BATTERY_SERVICE) as? android.os.BatteryManager
            while (true) {
                val batteryLevel = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
                controllerEngine.updateBattery(batteryLevel)
                kotlinx.coroutines.delay(10000) // check battery every 10 seconds
            }
        }

        // Observe settings and update engine and network
        var lastUseRotationVector: Boolean? = null
        viewModelScope.launch {
            settingsRepository.configFlow.collect { config ->
                controllerEngine.updateConfig(config)
                udpClient.updateConfig(config.network)
                discoveryClient.updateConfig(config.network)
                
                if (lastUseRotationVector != config.steering.useRotationVector) {
                    val wasFirst = lastUseRotationVector == null
                    lastUseRotationVector = config.steering.useRotationVector
                    if (!wasFirst) {
                        log("SNS", "Sensor type preference changed: useRotationVector=${config.steering.useRotationVector}")
                        rotationSensor.stop()
                        rotationSensor.start(config.steering.useRotationVector)
                    }
                }
                
                if (!config.network.autoDiscover &&
                    config.network.manualIp.isNotBlank() &&
                    _connectionState.value.status == ConnectionStatus.IDLE
                ) {
                    connectToPC(config.network.manualIp, config.network.udpPort, "Manual PC")
                }
            }
        }

        // Observe raw sensor data and process it
        viewModelScope.launch {
            rotationSensor.steeringAngle.collect { rawAngle ->
                lastRawAngle = rawAngle
                controllerEngine.updateSteering(rawAngle)
            }
        }
        
        // Sync steering angle and network stats for easy UI access
        viewModelScope.launch {
            diagnostics.collect { diag ->
                _steeringAngle.value = diag.steering.smoothedAngle
                _connectionState.update { it.copy(
                    latencyMs = diag.network.latencyMs,
                    packetRate = diag.network.packetRate
                ) }
            }
        }

        // Feed latest controller state to UDP client
        viewModelScope.launch {
            controllerEngine.controllerState.collect { state ->
                udpClient.updateControllerState(state)
            }
        }

        viewModelScope.launch {
            udpClient.diagnostics.collect { network ->
                controllerEngine.updateNetworkDiagnostics(network)
            }
        }
        
        // Monitor connection status
        viewModelScope.launch {
            udpClient.isConnected.collect { connected ->
                val newStatus = if (connected) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
                if (connected) log("NET", "Successfully connected to ${_connectionState.value.serverIp}")
                else if (_connectionState.value.status == ConnectionStatus.CONNECTED) log("NET", "Disconnected from server", LogLevel.WARN)

                _connectionState.update { it.copy(status = newStatus) }
            }
        }

        // Discovery cleanup loop
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _discoveredServers.update { current ->
                    current.filter { now - it.value.lastSeen < 10000 } // Keep for 10s
                }
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    private fun onServerDiscovered(server: DiscoveredServer) {
        _discoveredServers.update { current ->
            current + (server.ip to server.copy(lastSeen = System.currentTimeMillis()))
        }
    }

    fun connectToPC(ip: String, port: Int, name: String) {
        if (_connectionState.value.status == ConnectionStatus.CONNECTED && _connectionState.value.serverIp == ip) return
        
        log("NET", "Connecting to PC '$name' at $ip:$port...")
        _connectionState.update { it.copy(status = ConnectionStatus.CONNECTING, serverIp = ip, serverName = name) }
        
        // Save to settings for persistence
        viewModelScope.launch {
            settingsRepository.updateNetwork { it.copy(manualIp = ip, udpPort = port, preferredPc = name) }
        }
        
        udpClient.connect(ip, port)
    }

    private fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        _eventLogs.update { (listOf(LogEntry(tag = tag, message = message, level = level)) + it).take(100) }
    }

    fun calibrateCenter() {
        log("CAL", "Setting steering center to ${String.format(Locale.US, "%.2f°", lastRawAngle)}")
        viewModelScope.launch {
            settingsRepository.updateSteering { it.copy(calibrationOffset = lastRawAngle) }
        }
    }
    fun resetCalibration() {
        log("CAL", "Resetting steering calibration")
        viewModelScope.launch {
            settingsRepository.updateSteering { it.copy(calibrationOffset = 0f) }
        }
    }

    fun startSensor() {
        viewModelScope.launch {
            try {
                val config = settingsRepository.configFlow.first()
                log("SNS", "Starting sensor: useRotationVector=${config.steering.useRotationVector}")
                rotationSensor.start(config.steering.useRotationVector)
            } catch (e: Exception) {
                log("SNS", "Error loading sensor config, defaulting to Rotation Vector: ${e.message}", LogLevel.ERROR)
                rotationSensor.start(true)
            }
        }
    }
        
    fun stopSensor() {
        log("SNS", "Stopping Rotation Vector sensor")
        rotationSensor.stop()
    }

    fun updateBrake(value: Float) = controllerEngine.updateBrake(value)
    fun updateThrottle(value: Float) = controllerEngine.updateThrottle(value)
    fun updateClutch(value: Float) = controllerEngine.updateClutch(value)
    fun updateHandbrake(value: Boolean) = controllerEngine.updateHandbrake(value)
    fun updateGearUp(value: Boolean) = controllerEngine.updateGearUp(value)
    fun updateGearDown(value: Boolean) = controllerEngine.updateGearDown(value)
    fun updatePause(value: Boolean) = controllerEngine.updatePause(value)
    fun updateHorn(value: Boolean) = controllerEngine.updateHorn(value)
    fun updateHeadlights(value: Boolean) = controllerEngine.updateHeadlights(value)
    fun updateCamera(value: Boolean) = controllerEngine.updateCamera(value)
    fun updateDpadUp(value: Boolean) = controllerEngine.updateDpadUp(value)
    fun updateDpadDown(value: Boolean) = controllerEngine.updateDpadDown(value)
    fun updateDpadLeft(value: Boolean) = controllerEngine.updateDpadLeft(value)
    fun updateDpadRight(value: Boolean) = controllerEngine.updateDpadRight(value)
    fun updateLb(value: Boolean) = controllerEngine.updateLb(value)
    fun updateRb(value: Boolean) = controllerEngine.updateRb(value)
    fun updateBack(value: Boolean) = controllerEngine.updateBack(value)

    fun pulseDpadRight() = pulseButton(
        press = { controllerEngine.updateDpadRight(true) },
        release = { controllerEngine.updateDpadRight(false) }
    )

    fun pulseBack() = pulseButton(
        press = { controllerEngine.updateBack(true) },
        release = { controllerEngine.updateBack(false) }
    )

    fun pulsePause() = pulseButton(
        press = { controllerEngine.updatePause(true) },
        release = { controllerEngine.updatePause(false) }
    )

    fun pulseCamera() = pulseButton(
        press = { controllerEngine.updateCamera(true) },
        release = { controllerEngine.updateCamera(false) }
    )

    fun pulseHeadlights() = pulseButton(
        press = { controllerEngine.updateHeadlights(true) },
        release = { controllerEngine.updateHeadlights(false) }
    )

    private fun pulseButton(press: () -> Unit, release: () -> Unit) {
        press()
        viewModelScope.launch {
            kotlinx.coroutines.delay(120)
            release()
        }
    }
}
