package com.example.virtual_steer.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.virtual_steer.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val STEERING_SENSITIVITY = floatPreferencesKey("steering_sensitivity")
        val STEERING_DEADZONE = floatPreferencesKey("steering_deadzone")
        val STEERING_SMOOTHING = floatPreferencesKey("steering_smoothing")
        val STEERING_MAX_ANGLE = floatPreferencesKey("steering_max_angle_f")
        val STEERING_MAX_ANGLE_OLD = intPreferencesKey("steering_max_angle")
        val STEERING_INVERT = booleanPreferencesKey("steering_invert")
        val STEERING_RESPONSE_CURVE = floatPreferencesKey("steering_response_curve")
        val STEERING_AUTO_CALIB = booleanPreferencesKey("steering_auto_calib")
        val STEERING_CALIB_OFFSET = floatPreferencesKey("steering_calib_offset")
        val STEERING_USE_ROTATION_VECTOR = booleanPreferencesKey("steering_use_rotation_vector")

        val THROTTLE_CURVE = stringPreferencesKey("throttle_curve")
        val BRAKE_CURVE = stringPreferencesKey("brake_curve")
        val PEDAL_DEADZONE = floatPreferencesKey("pedal_deadzone")
        val PEDAL_SMOOTHING = floatPreferencesKey("pedal_smoothing")
        val PEDAL_INVERT = booleanPreferencesKey("pedal_invert")
        val PEDAL_PRECISION = floatPreferencesKey("pedal_precision")
        val THROTTLE_MIN = floatPreferencesKey("throttle_min")
        val THROTTLE_MAX = floatPreferencesKey("throttle_max")
        val BRAKE_MIN = floatPreferencesKey("brake_min")
        val BRAKE_MAX = floatPreferencesKey("brake_max")

        val AUTO_DISCOVER = booleanPreferencesKey("auto_discover")
        val PREFERRED_PC = stringPreferencesKey("preferred_pc")
        val MANUAL_IP = stringPreferencesKey("manual_ip")
        val UDP_PORT = intPreferencesKey("udp_port")
        val PACKET_RATE = intPreferencesKey("packet_rate")
        val HEARTBEAT_INTERVAL = intPreferencesKey("heartbeat_interval")
        val RECONNECT_DELAY = intPreferencesKey("reconnect_delay")
        val PREFERRED_CONNECTION = stringPreferencesKey("preferred_connection")
        val TIMEOUT = intPreferencesKey("timeout")

        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val HAPTIC = booleanPreferencesKey("haptic")
        val TELEMETRY = booleanPreferencesKey("telemetry")
        val FPS = booleanPreferencesKey("fps")
        val BATTERY = booleanPreferencesKey("battery")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val ALWAYS_ON = booleanPreferencesKey("always_on")
        val LANDSCAPE_LOCK = booleanPreferencesKey("landscape_lock")
        val BRIGHTNESS_OVERRIDE = floatPreferencesKey("brightness_override")
        val SHOW_RADIO = booleanPreferencesKey("show_radio")
        val PAUSE_X = floatPreferencesKey("pause_x")
        val PAUSE_Y = floatPreferencesKey("pause_y")
        val CAM_X = floatPreferencesKey("cam_x")
        val CAM_Y = floatPreferencesKey("cam_y")
        val LIGHTS_X = floatPreferencesKey("lights_x")
        val LIGHTS_Y = floatPreferencesKey("lights_y")
        val GEAR_DOWN_X = floatPreferencesKey("gear_down_x")
        val GEAR_DOWN_Y = floatPreferencesKey("gear_down_y")
        val HANDBRAKE_X = floatPreferencesKey("handbrake_x")
        val HANDBRAKE_Y = floatPreferencesKey("handbrake_y")
        val GEAR_UP_X = floatPreferencesKey("gear_up_x")
        val GEAR_UP_Y = floatPreferencesKey("gear_up_y")
        val RADIO_X = floatPreferencesKey("radio_x")
        val RADIO_Y = floatPreferencesKey("radio_y")
    }

    val configFlow: Flow<ControllerConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            ControllerConfig(
                steering = SteeringConfig(
                    sensitivity = prefs[Keys.STEERING_SENSITIVITY] ?: 1.0f,
                    deadZone = prefs[Keys.STEERING_DEADZONE] ?: 0.05f,
                    smoothing = prefs[Keys.STEERING_SMOOTHING] ?: 0.2f,
                    maxAngle = prefs[Keys.STEERING_MAX_ANGLE] ?: (prefs[Keys.STEERING_MAX_ANGLE_OLD]?.toFloat() ?: 135f),
                    invert = prefs[Keys.STEERING_INVERT] ?: false,
                    responseCurve = prefs[Keys.STEERING_RESPONSE_CURVE] ?: 1.0f,
                    autoCalibration = prefs[Keys.STEERING_AUTO_CALIB] ?: true,
                    calibrationOffset = prefs[Keys.STEERING_CALIB_OFFSET] ?: 0f,
                    useRotationVector = prefs[Keys.STEERING_USE_ROTATION_VECTOR] ?: true
                ),
                pedals = PedalConfig(
                    throttleCurve = safeValueOf(prefs[Keys.THROTTLE_CURVE], PedalResponseCurve.RACING),
                    brakeCurve = safeValueOf(prefs[Keys.BRAKE_CURVE], PedalResponseCurve.RACING),
                    deadZone = prefs[Keys.PEDAL_DEADZONE] ?: 0.05f,
                    smoothing = prefs[Keys.PEDAL_SMOOTHING] ?: 0.20f,
                    invert = prefs[Keys.PEDAL_INVERT] ?: false,
                    precision = prefs[Keys.PEDAL_PRECISION] ?: 0.001f,
                    throttleMin = prefs[Keys.THROTTLE_MIN] ?: 0f,
                    throttleMax = prefs[Keys.THROTTLE_MAX] ?: 1f,
                    brakeMin = prefs[Keys.BRAKE_MIN] ?: 0f,
                    brakeMax = prefs[Keys.BRAKE_MAX] ?: 1f
                ),
                network = NetworkConfig(
                    autoDiscover = prefs[Keys.AUTO_DISCOVER] ?: true,
                    preferredPc = prefs[Keys.PREFERRED_PC] ?: "",
                    manualIp = prefs[Keys.MANUAL_IP] ?: "192.168.1.100",
                    udpPort = prefs[Keys.UDP_PORT] ?: 4444,
                    packetRate = prefs[Keys.PACKET_RATE] ?: 100,
                    heartbeatInterval = prefs[Keys.HEARTBEAT_INTERVAL] ?: 10,
                    reconnectDelay = prefs[Keys.RECONNECT_DELAY] ?: 2000,
                    preferredConnection = safeValueOf(prefs[Keys.PREFERRED_CONNECTION], ConnectionMode.WIFI),
                    connectionTimeout = prefs[Keys.TIMEOUT] ?: 5000
                ),
                ui = UIConfig(
                    darkTheme = prefs[Keys.DARK_THEME] ?: true,
                    accentColor = prefs[Keys.ACCENT_COLOR] ?: 0xFF00E676.toInt(),
                    hapticFeedback = prefs[Keys.HAPTIC] ?: true,
                    showTelemetry = prefs[Keys.TELEMETRY] ?: true,
                    showFps = prefs[Keys.FPS] ?: false,
                    batteryIndicator = prefs[Keys.BATTERY] ?: true,
                    animations = prefs[Keys.ANIMATIONS] ?: true,
                    alwaysOnScreen = prefs[Keys.ALWAYS_ON] ?: true,
                    landscapeLock = prefs[Keys.LANDSCAPE_LOCK] ?: true,
                    brightnessOverride = prefs[Keys.BRIGHTNESS_OVERRIDE],
                    showRadio = prefs[Keys.SHOW_RADIO] ?: true,
                    pauseX = prefs[Keys.PAUSE_X] ?: 0.90f,
                    pauseY = prefs[Keys.PAUSE_Y] ?: 0.08f,
                    camX = prefs[Keys.CAM_X] ?: 0.80f,
                    camY = prefs[Keys.CAM_Y] ?: 0.08f,
                    lightsX = prefs[Keys.LIGHTS_X] ?: 0.70f,
                    lightsY = prefs[Keys.LIGHTS_Y] ?: 0.08f,
                    gearDownX = prefs[Keys.GEAR_DOWN_X] ?: 0.38f,
                    gearDownY = prefs[Keys.GEAR_DOWN_Y] ?: 0.90f,
                    handbrakeX = prefs[Keys.HANDBRAKE_X] ?: 0.50f,
                    handbrakeY = prefs[Keys.HANDBRAKE_Y] ?: 0.90f,
                    gearUpX = prefs[Keys.GEAR_UP_X] ?: 0.62f,
                    gearUpY = prefs[Keys.GEAR_UP_Y] ?: 0.90f,
                    radioX = prefs[Keys.RADIO_X] ?: 0.88f,
                    radioY = prefs[Keys.RADIO_Y] ?: 0.50f
                )
            )
        }

    suspend fun updateSteering(transform: (SteeringConfig) -> SteeringConfig) {
        context.dataStore.edit { prefs ->
            val current = transform(SteeringConfig(
                sensitivity = prefs[Keys.STEERING_SENSITIVITY] ?: 1.0f,
                deadZone = prefs[Keys.STEERING_DEADZONE] ?: 0.05f,
                smoothing = prefs[Keys.STEERING_SMOOTHING] ?: 0.2f,
                maxAngle = prefs[Keys.STEERING_MAX_ANGLE] ?: (prefs[Keys.STEERING_MAX_ANGLE_OLD]?.toFloat() ?: 135f),
                invert = prefs[Keys.STEERING_INVERT] ?: false,
                responseCurve = prefs[Keys.STEERING_RESPONSE_CURVE] ?: 1.0f,
                autoCalibration = prefs[Keys.STEERING_AUTO_CALIB] ?: true,
                calibrationOffset = prefs[Keys.STEERING_CALIB_OFFSET] ?: 0f,
                useRotationVector = prefs[Keys.STEERING_USE_ROTATION_VECTOR] ?: true
            ))
            prefs[Keys.STEERING_SENSITIVITY] = current.sensitivity
            prefs[Keys.STEERING_DEADZONE] = current.deadZone
            prefs[Keys.STEERING_SMOOTHING] = current.smoothing
            prefs[Keys.STEERING_MAX_ANGLE] = current.maxAngle
            prefs[Keys.STEERING_INVERT] = current.invert
            prefs[Keys.STEERING_RESPONSE_CURVE] = current.responseCurve
            prefs[Keys.STEERING_AUTO_CALIB] = current.autoCalibration
            prefs[Keys.STEERING_CALIB_OFFSET] = current.calibrationOffset
            prefs[Keys.STEERING_USE_ROTATION_VECTOR] = current.useRotationVector
        }
    }

    suspend fun updatePedals(transform: (PedalConfig) -> PedalConfig) {
        context.dataStore.edit { prefs ->
            val current = transform(PedalConfig(
                throttleCurve = safeValueOf(prefs[Keys.THROTTLE_CURVE], PedalResponseCurve.RACING),
                brakeCurve = safeValueOf(prefs[Keys.BRAKE_CURVE], PedalResponseCurve.RACING),
                deadZone = prefs[Keys.PEDAL_DEADZONE] ?: 0.05f,
                smoothing = prefs[Keys.PEDAL_SMOOTHING] ?: 0.20f,
                invert = prefs[Keys.PEDAL_INVERT] ?: false,
                precision = prefs[Keys.PEDAL_PRECISION] ?: 0.001f,
                throttleMin = prefs[Keys.THROTTLE_MIN] ?: 0f,
                throttleMax = prefs[Keys.THROTTLE_MAX] ?: 1f,
                brakeMin = prefs[Keys.BRAKE_MIN] ?: 0f,
                brakeMax = prefs[Keys.BRAKE_MAX] ?: 1f
            ))
            prefs[Keys.THROTTLE_CURVE] = current.throttleCurve.name
            prefs[Keys.BRAKE_CURVE] = current.brakeCurve.name
            prefs[Keys.PEDAL_DEADZONE] = current.deadZone
            prefs[Keys.PEDAL_SMOOTHING] = current.smoothing
            prefs[Keys.PEDAL_INVERT] = current.invert
            prefs[Keys.PEDAL_PRECISION] = current.precision
            prefs[Keys.THROTTLE_MIN] = current.throttleMin
            prefs[Keys.THROTTLE_MAX] = current.throttleMax
            prefs[Keys.BRAKE_MIN] = current.brakeMin
            prefs[Keys.BRAKE_MAX] = current.brakeMax
        }
    }

    suspend fun updateNetwork(transform: (NetworkConfig) -> NetworkConfig) {
        context.dataStore.edit { prefs ->
            val current = transform(NetworkConfig(
                autoDiscover = prefs[Keys.AUTO_DISCOVER] ?: true,
                preferredPc = prefs[Keys.PREFERRED_PC] ?: "",
                manualIp = prefs[Keys.MANUAL_IP] ?: "192.168.1.100",
                udpPort = prefs[Keys.UDP_PORT] ?: 4444,
                packetRate = prefs[Keys.PACKET_RATE] ?: 100,
                heartbeatInterval = prefs[Keys.HEARTBEAT_INTERVAL] ?: 10,
                reconnectDelay = prefs[Keys.RECONNECT_DELAY] ?: 2000,
                preferredConnection = safeValueOf(prefs[Keys.PREFERRED_CONNECTION], ConnectionMode.WIFI),
                connectionTimeout = prefs[Keys.TIMEOUT] ?: 5000
            ))
            prefs[Keys.AUTO_DISCOVER] = current.autoDiscover
            prefs[Keys.PREFERRED_PC] = current.preferredPc
            prefs[Keys.MANUAL_IP] = current.manualIp
            prefs[Keys.UDP_PORT] = current.udpPort
            prefs[Keys.PACKET_RATE] = current.packetRate
            prefs[Keys.HEARTBEAT_INTERVAL] = current.heartbeatInterval
            prefs[Keys.RECONNECT_DELAY] = current.reconnectDelay
            prefs[Keys.PREFERRED_CONNECTION] = current.preferredConnection.name
            prefs[Keys.TIMEOUT] = current.connectionTimeout
        }
    }

    suspend fun updateUI(transform: (UIConfig) -> UIConfig) {
        context.dataStore.edit { prefs ->
            val current = transform(UIConfig(
                darkTheme = prefs[Keys.DARK_THEME] ?: true,
                accentColor = prefs[Keys.ACCENT_COLOR] ?: 0xFF00E676.toInt(),
                hapticFeedback = prefs[Keys.HAPTIC] ?: true,
                showTelemetry = prefs[Keys.TELEMETRY] ?: true,
                showFps = prefs[Keys.FPS] ?: false,
                batteryIndicator = prefs[Keys.BATTERY] ?: true,
                animations = prefs[Keys.ANIMATIONS] ?: true,
                alwaysOnScreen = prefs[Keys.ALWAYS_ON] ?: true,
                landscapeLock = prefs[Keys.LANDSCAPE_LOCK] ?: true,
                brightnessOverride = prefs[Keys.BRIGHTNESS_OVERRIDE],
                showRadio = prefs[Keys.SHOW_RADIO] ?: true,
                pauseX = prefs[Keys.PAUSE_X] ?: 0.90f,
                pauseY = prefs[Keys.PAUSE_Y] ?: 0.08f,
                camX = prefs[Keys.CAM_X] ?: 0.80f,
                camY = prefs[Keys.CAM_Y] ?: 0.08f,
                lightsX = prefs[Keys.LIGHTS_X] ?: 0.70f,
                lightsY = prefs[Keys.LIGHTS_Y] ?: 0.08f,
                gearDownX = prefs[Keys.GEAR_DOWN_X] ?: 0.38f,
                gearDownY = prefs[Keys.GEAR_DOWN_Y] ?: 0.90f,
                handbrakeX = prefs[Keys.HANDBRAKE_X] ?: 0.50f,
                handbrakeY = prefs[Keys.HANDBRAKE_Y] ?: 0.90f,
                gearUpX = prefs[Keys.GEAR_UP_X] ?: 0.62f,
                gearUpY = prefs[Keys.GEAR_UP_Y] ?: 0.90f,
                radioX = prefs[Keys.RADIO_X] ?: 0.88f,
                radioY = prefs[Keys.RADIO_Y] ?: 0.50f
            ))
            prefs[Keys.DARK_THEME] = current.darkTheme
            prefs[Keys.ACCENT_COLOR] = current.accentColor
            prefs[Keys.HAPTIC] = current.hapticFeedback
            prefs[Keys.TELEMETRY] = current.showTelemetry
            prefs[Keys.FPS] = current.showFps
            prefs[Keys.BATTERY] = current.batteryIndicator
            prefs[Keys.ANIMATIONS] = current.animations
            prefs[Keys.ALWAYS_ON] = current.alwaysOnScreen
            prefs[Keys.LANDSCAPE_LOCK] = current.landscapeLock
            current.brightnessOverride?.let { prefs[Keys.BRIGHTNESS_OVERRIDE] = it } ?: prefs.remove(Keys.BRIGHTNESS_OVERRIDE)
            prefs[Keys.SHOW_RADIO] = current.showRadio
            prefs[Keys.PAUSE_X] = current.pauseX
            prefs[Keys.PAUSE_Y] = current.pauseY
            prefs[Keys.CAM_X] = current.camX
            prefs[Keys.CAM_Y] = current.camY
            prefs[Keys.LIGHTS_X] = current.lightsX
            prefs[Keys.LIGHTS_Y] = current.lightsY
            prefs[Keys.GEAR_DOWN_X] = current.gearDownX
            prefs[Keys.GEAR_DOWN_Y] = current.gearDownY
            prefs[Keys.HANDBRAKE_X] = current.handbrakeX
            prefs[Keys.HANDBRAKE_Y] = current.handbrakeY
            prefs[Keys.GEAR_UP_X] = current.gearUpX
            prefs[Keys.GEAR_UP_Y] = current.gearUpY
            prefs[Keys.RADIO_X] = current.radioX
            prefs[Keys.RADIO_Y] = current.radioY
        }
    }

    private inline fun <reified T : Enum<T>> safeValueOf(name: String?, default: T): T {
        return try {
            if (name != null) enumValueOf<T>(name) else default
        } catch (e: IllegalArgumentException) {
            default
        }
    }
}
