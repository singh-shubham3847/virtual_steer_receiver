package com.example.virtual_steer.model

import kotlinx.serialization.Serializable

@Serializable
enum class PedalResponseCurve {
    LINEAR, RACING, AGGRESSIVE
}

@Serializable
data class PedalConfig(
    val throttleCurve: PedalResponseCurve = PedalResponseCurve.RACING,
    val brakeCurve: PedalResponseCurve = PedalResponseCurve.RACING,
    val deadZone: Float = 0.05f,
    val smoothing: Float = 0.20f,
    val invert: Boolean = false,
    val precision: Float = 0.001f,
    val throttleMin: Float = 0f,
    val throttleMax: Float = 1f,
    val brakeMin: Float = 0f,
    val brakeMax: Float = 1f
)
