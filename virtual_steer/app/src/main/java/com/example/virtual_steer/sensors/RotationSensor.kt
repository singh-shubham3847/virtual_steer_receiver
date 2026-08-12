package com.example.virtual_steer.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RotationSensor(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var activeSensor: Sensor? = null

    private val _steeringAngle = MutableStateFlow(0f)
    val steeringAngle: StateFlow<Float> = _steeringAngle

    fun start(useRotationVector: Boolean = true) {
        stop()
        
        activeSensor = if (useRotationVector) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        } else {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        
        // Fallback to accelerometer if rotation vector is requested but not available
        if (activeSensor == null && useRotationVector) {
            activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        
        activeSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        activeSensor = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(
                rotationMatrix,
                event.values
            )

            // Remap for Landscape (USB on the right)
            val remappedMatrix = FloatArray(9)
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                remappedMatrix
            )

            val steeringRad = -kotlin.math.atan2(remappedMatrix[6].toDouble(), -remappedMatrix[7].toDouble())
            val steeringDeg = Math.toDegrees(steeringRad).toFloat()
            _steeringAngle.value = steeringDeg
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Simple accelerometer pitch/roll projection in landscape
            val steeringRad = -kotlin.math.atan2(event.values[0].toDouble(), event.values[1].toDouble())
            val steeringDeg = Math.toDegrees(steeringRad).toFloat()
            _steeringAngle.value = steeringDeg
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}