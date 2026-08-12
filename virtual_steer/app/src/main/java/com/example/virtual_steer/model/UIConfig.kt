package com.example.virtual_steer.model

import kotlinx.serialization.Serializable

@Serializable
data class UIConfig(
    val darkTheme: Boolean = true,
    val accentColor: Int = 0xFF00E676.toInt(), // ThrottleGreen
    val hapticFeedback: Boolean = true,
    val showTelemetry: Boolean = true,
    val showFps: Boolean = false,
    val batteryIndicator: Boolean = true,
    val animations: Boolean = true,
    val alwaysOnScreen: Boolean = true,
    val landscapeLock: Boolean = true,
    val brightnessOverride: Float? = null, // null means system default
    val showRadio: Boolean = true,
    
    // Custom button positions (X and Y as fraction of screen width/height)
    val pauseX: Float = 0.90f,
    val pauseY: Float = 0.08f,
    val camX: Float = 0.80f,
    val camY: Float = 0.08f,
    val lightsX: Float = 0.70f,
    val lightsY: Float = 0.08f,
    val gearDownX: Float = 0.38f,
    val gearDownY: Float = 0.90f,
    val handbrakeX: Float = 0.50f,
    val handbrakeY: Float = 0.90f,
    val gearUpX: Float = 0.62f,
    val gearUpY: Float = 0.90f,
    val radioX: Float = 0.88f,
    val radioY: Float = 0.50f
)
