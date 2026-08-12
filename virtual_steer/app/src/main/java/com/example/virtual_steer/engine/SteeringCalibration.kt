package com.example.virtual_steer.engine

class SteeringCalibration {

    private var offset = 0f

    fun calibrate(currentAngle: Float) {
        offset = currentAngle
    }

    fun apply(rawAngle: Float): Float {
        return rawAngle - offset
    }

    fun reset() {
        offset = 0f
    }
}