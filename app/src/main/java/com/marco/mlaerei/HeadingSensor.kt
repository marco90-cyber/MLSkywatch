package com.marco.mlaerei

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Direzione verso cui punta il telefono (gradi rispetto al Nord magnetico), dal sensore
 * di rotazione. Funziona sia con il telefono in piano sia tenuto in verticale.
 */
class HeadingSensor(
    context: Context,
    private val onHeading: (Float) -> Unit,
) : SensorEventListener {

    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotation = FloatArray(9)
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)

    private var sinAvg = 0f
    private var cosAvg = 0f
    private var primed = false
    private var lastSent = Float.NaN

    val available: Boolean get() = sensor != null

    fun start() {
        val s = sensor ?: return
        primed = false
        lastSent = Float.NaN
        manager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        manager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotation, event.values)

        // rotation[8] = quanto l'asse Z del telefono punta verso l'alto: vicino a 1 = telefono in piano
        val matrix = if (abs(rotation[8]) > 0.5f) {
            rotation
        } else {
            SensorManager.remapCoordinateSystem(
                rotation,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remapped,
            )
            remapped
        }
        SensorManager.getOrientation(matrix, orientation)
        val azimuth = orientation[0] // radianti, da -PI a PI

        // media circolare per addolcire i tremolii
        val alpha = 0.2f
        if (!primed) {
            sinAvg = sin(azimuth)
            cosAvg = cos(azimuth)
            primed = true
        } else {
            sinAvg += alpha * (sin(azimuth) - sinAvg)
            cosAvg += alpha * (cos(azimuth) - cosAvg)
        }
        val deg = ((Math.toDegrees(atan2(sinAvg, cosAvg).toDouble()) + 360.0) % 360.0).toFloat()

        var diff = if (lastSent.isNaN()) 360f else abs(deg - lastSent)
        if (diff > 180f && !lastSent.isNaN()) diff = 360f - diff
        if (diff >= 0.7f) {
            lastSent = deg
            onHeading(deg)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
