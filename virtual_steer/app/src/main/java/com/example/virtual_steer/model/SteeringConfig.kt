package com.example.virtual_steer.model

import kotlinx.serialization.Serializable

@Serializable
data class SteeringConfig(
    val sensitivity: Float = 1.0f,
    val deadZone: Float = 0.05f,
    val smoothing: Float = 0.2f,
    val maxAngle: Float = 135f,
    val invert: Boolean = false,
    val responseCurve: Float = 1.0f, // 1.0 = Linear
    val autoCalibration: Boolean = true,
    val calibrationOffset: Float = 0f,
    val useRotationVector: Boolean = true
)
