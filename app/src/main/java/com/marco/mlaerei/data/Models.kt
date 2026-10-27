package com.marco.mlaerei.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double)

data class Aircraft(
    val hex: String,
    val callsign: String,
    val hasCallsign: Boolean,
    val registration: String?,
    val typeCode: String?,
    val typeDescription: String?,
    val category: String?,
    val lat: Double,
    val lon: Double,
    val altitudeFt: Int?,
    val onGround: Boolean,
    val speedKt: Double?,
    val trackDeg: Double?,
    val vertRateFpm: Int?,
    val squawk: String?,
    val distanceKm: Double,
    val bearingDeg: Double,
) {
    val altitudeM: Int? get() = altitudeFt?.let { (it * 0.3048).roundToInt() }

    val speedKmh: Int? get() = speedKt?.let { (it * 1.852).roundToInt() }

    /** Altezza apparente nel cielo, in gradi sopra l'orizzonte (tiene conto della curvatura terrestre). */
    val elevationDeg: Double?
        get() {
            val h = altitudeFt?.times(0.3048) ?: return null
            if (onGround) return null
            val d = distanceKm * 1000.0
            if (d < 1.0) return 90.0
            val drop = d * d / (2 * 6_371_000.0)
            return Math.toDegrees(atan2(h - drop, d))
        }
}

data class AircraftDetails(
    val airline: String? = null,
    val originCity: String? = null,
    val originCode: String? = null,
    val destCity: String? = null,
    val destCode: String? = null,
    val model: String? = null,
    val manufacturer: String? = null,
    val owner: String? = null,
    val photoUrl: String? = null,
    val photoLink: String? = null,
    val photographer: String? = null,
    val photoSource: String? = null,
) {
    val isEmpty: Boolean
        get() = airline == null && originCode == null && destCode == null &&
            model == null && owner == null && photoUrl == null

    companion object {
        val EMPTY = AircraftDetails()
    }
}

enum class WeatherKind(val label: String) {
    SUNNY("Soleggiato"),
    PARTLY("Poco nuvoloso"),
    CLOUDY("Nuvoloso"),
    FOG("Nebbia"),
    RAIN("Pioggia"),
    STORM("Temporale"),
    SNOW("Neve"),
}

data class WeatherInfo(
    val code: Int,
    val cloudCover: Int,
    val temperatureC: Double?,
    val isDay: Boolean,
) {
    /** Tipo di meteo ricavato dal codice WMO di Open-Meteo. */
    val kind: WeatherKind
        get() = when (code) {
            0, 1 -> WeatherKind.SUNNY
            2 -> WeatherKind.PARTLY
            3 -> WeatherKind.CLOUDY
            45, 48 -> WeatherKind.FOG
            in 51..67, in 80..82 -> WeatherKind.RAIN
            in 71..77, 85, 86 -> WeatherKind.SNOW
            in 95..99 -> WeatherKind.STORM
            else -> when {
                cloudCover >= 70 -> WeatherKind.CLOUDY
                cloudCover >= 30 -> WeatherKind.PARTLY
                else -> WeatherKind.SUNNY
            }
        }
}

object Geo {
    private const val EARTH_RADIUS_KM = 6371.0088

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lon2 - lon1)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_RADIUS_KM * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Direzione (0-360, 0 = Nord) da punto 1 a punto 2. */
    fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}
