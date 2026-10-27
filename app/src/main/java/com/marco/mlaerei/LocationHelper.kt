package com.marco.mlaerei

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper

/** Posizione tramite LocationManager (nessuna dipendenza da Google Play Services). */
class LocationHelper(
    context: Context,
    private val onProviderChange: () -> Unit = {},
    private val onLocation: (Location) -> Unit,
) : LocationListener {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var running = false

    /** true se almeno GPS o rete (posizione approssimata) sono attivi nelle impostazioni del telefono. */
    fun isAnyEnabled(): Boolean = try {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (e: Exception) {
        true
    }

    @SuppressLint("MissingPermission")
    fun start() {
        stop()
        running = true
        var best: Location? = null
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                if (!manager.isProviderEnabled(provider)) continue
                val last = manager.getLastKnownLocation(provider)
                if (last != null && (best == null || last.time > best.time)) best = last
                manager.requestLocationUpdates(provider, 15_000L, 200f, this, Looper.getMainLooper())
            } catch (e: SecurityException) {
                // permesso preciso non concesso per questo provider
            } catch (e: IllegalArgumentException) {
                // provider non disponibile
            }
        }
        if (best != null) onLocation(best)
    }

    fun stop() {
        if (!running) return
        running = false
        try {
            manager.removeUpdates(this)
        } catch (e: SecurityException) {
        }
    }

    override fun onLocationChanged(location: Location) {
        onLocation(location)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
        onProviderChange()
    }

    override fun onProviderDisabled(provider: String) {
        onProviderChange()
    }
}
