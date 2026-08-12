package com.example.virtual_steer.engine

import com.example.virtual_steer.model.PedalConfig
import com.example.virtual_steer.model.PedalDiagnostics
import com.example.virtual_steer.model.PedalResponseCurve
import kotlin.math.pow
import kotlin.math.roundToInt

class PedalProcessor(
    private var config: PedalConfig = PedalConfig(),
    private val isBrake: Boolean = false
) {
    private var smoothedValue = 0f

    fun updateConfig(newConfig: PedalConfig) {
        config = newConfig
    }

    fun process(rawValue: Float, curve: PedalResponseCurve): PedalDiagnostics {
        // Step 1: Apply Min/Max Calibration
        val min = if (isBrake) config.brakeMin else config.throttleMin
        val max = if (isBrake) config.brakeMax else config.throttleMax
        
        var value = if (max > min) {
            (rawValue - min) / (max - min)
        } else {
            rawValue
        }
        value = value.coerceIn(0f, 1f)

        // Step 2: Invert if enabled
        if (config.invert) {
            value = 1f - value
        }

        // Step 3: Dead zone
        value = if (value < config.deadZone) 0f else {
            (value - config.deadZone) / (1f - config.deadZone)
        }
        value = value.coerceIn(0f, 1f)
        val deadZoneValue = value

        // Step 4: Response Curve
        val curveOutput = applyCurve(value, curve)
        value = curveOutput

        // Step 5: Smoothing (Low-pass)
        smoothedValue += (value - smoothedValue) * config.smoothing
        
        // Step 6: Apply Precision
        val finalOutput = if (config.precision > 0) {
            val factor = 1f / config.precision
            (smoothedValue * factor).roundToInt() / factor
        } else {
            smoothedValue
        }.coerceIn(0f, 1f)

        return PedalDiagnostics(
            rawValue = rawValue,
            deadZoneApplied = deadZoneValue,
            curveOutput = curveOutput,
            smoothedValue = smoothedValue,
            finalOutput = finalOutput,
            percentage = finalOutput * 100f
        )
    }

    private fun applyCurve(value: Float, curve: PedalResponseCurve): Float {
        return when (curve) {
            PedalResponseCurve.LINEAR -> value
            PedalResponseCurve.RACING -> {
                if (value < 0.5f) 2f * value * value else 1f - 2f * (1f - value) * (1f - value)
            }
            PedalResponseCurve.AGGRESSIVE -> value.pow(0.7f)
        }
    }

    fun reset() {
        smoothedValue = 0f
    }
}
