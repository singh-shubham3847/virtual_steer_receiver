package com.example.virtual_steer.engine

import com.example.virtual_steer.model.SteeringConfig
import com.example.virtual_steer.model.SteeringDiagnostics
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class SteeringProcessor(
    private var config: SteeringConfig = SteeringConfig()
) {
    private var smoothedValue = 0f

    fun updateConfig(newConfig: SteeringConfig) {
        config = newConfig
    }

    /**
     * Process raw steering angle from RotationSensor.
     */
    fun process(rawAngle: Float): SteeringDiagnostics {
        val calibOffset = config.calibrationOffset

        // Step 1: Apply calibration and normalize to [-180, 180] to handle sensor wrap-around
        var calibrated = rawAngle - calibOffset
        calibrated = (calibrated + 180f) % 360f
        if (calibrated < 0f) {
            calibrated += 360f
        }
        calibrated -= 180f
        
        var value = calibrated

        // Step 2: Dead zone
        val deadZoneValue = if (abs(value) < config.deadZone) 0f else value
        value = deadZoneValue

        // Step 3: Sensitivity
        value *= config.sensitivity

        // Step 4: Invert steering if enabled
        if (config.invert) {
            value = -value
        }

        // Step 5: Clamp steering range
        val maxAngle = config.maxAngle
        value = value.coerceIn(-maxAngle, maxAngle)

        // Step 6: Response Curve (Power Function)
        // normalized value in range [-1, 1]
        val normalized = value / maxAngle
        val curveOutput = sign(normalized) * abs(normalized).pow(config.responseCurve)
        
        // Convert back to angle for smoothing consistency
        value = curveOutput * maxAngle

        // Step 7: Low-pass smoothing
        smoothedValue += (value - smoothedValue) * config.smoothing

        // Output as 0.0 to 1.0 range (normalized)
        val finalOutput = (smoothedValue / maxAngle).coerceIn(-1f, 1f)

        return SteeringDiagnostics(
            rawAngle = rawAngle,
            calibrationOffset = calibOffset,
            calibratedAngle = calibrated,
            deadZoneApplied = deadZoneValue,
            smoothedAngle = smoothedValue,
            finalOutput = finalOutput,
            percentage = finalOutput * 100f
        )
    }

    fun resetFilter() {
        smoothedValue = 0f
    }
}
