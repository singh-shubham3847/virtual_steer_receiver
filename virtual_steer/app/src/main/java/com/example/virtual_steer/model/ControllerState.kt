package com.example.virtual_steer.model

data class ControllerState(

    // Analog
    val steering: Float = 0f,
    val throttle: Float = 0f,
    val brake: Float = 0f,
    val clutch: Float = 0f,

    // Buttons
    val handbrake: Boolean = false,
    val gearUp: Boolean = false,
    val gearDown: Boolean = false,
    val pause: Boolean = false,
    val horn: Boolean = false,
    val headlights: Boolean = false,
    val camera: Boolean = false,
    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false,
    val lb: Boolean = false,
    val rb: Boolean = false,
    val back: Boolean = false,

    // Telemetry
    val connected: Boolean = false,
    val latency: Int = 0
)