package com.marco.mlaerei.ui

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Fmt {
    private val IT: Locale = Locale.ITALY

    fun int(n: Int): String = String.format(IT, "%,d", n)

    /** true = unità imperiali (miglia, piedi, mph, °F); false = sistema metrico. */
    @Volatile
    var imperial: Boolean = false

    const val KM_PER_MI = 1.609344
    const val FT_PER_M = 3.280839895

    val distUnit: String get() = if (imperial) "mi" else "km"

    /** Distanza con una cifra decimale sotto 10, intera sopra. */
    fun km(d: Double): String {
        val v = if (imperial) d / KM_PER_MI else d
        return if (v < 10) String.format(IT, "%.1f %s", v, distUnit)
        else String.format(IT, "%,d %s", v.roundToInt(), distUnit)
    }

    /** Distanza intera (es. raggio di ricerca) data in km. */
    fun distWhole(km: Double): String {
        val v = if (imperial) km / KM_PER_MI else km
        return String.format(IT, "%,d %s", v.roundToInt(), distUnit)
    }

    fun altitude(meters: Int): String =
        if (imperial) "${int((meters * FT_PER_M).roundToInt())} ft" else "${int(meters)} m"

    fun speed(kmh: Int): String =
        if (imperial) "${int((kmh / KM_PER_MI).roundToInt())} mph" else "${int(kmh)} km/h"

    fun temperature(c: Double): String =
        if (imperial) "${(c * 9.0 / 5.0 + 32.0).roundToInt()}°F" else "${c.roundToInt()}°C"

    /** Punto cardinale in italiano (N, NE, E, SE, S, SO, O, NO). */
    fun cardinal(deg: Double): String {
        val names = arrayOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        return names[(((deg + 22.5) % 360.0) / 45.0).toInt().coerceIn(0, 7)]
    }

    fun degrees(deg: Double): String = "${deg.roundToInt() % 360}° ${cardinal(deg)}"

    fun elevation(deg: Double?): String? = when {
        deg == null -> null
        deg >= 80 -> "quasi sopra di te"
        deg >= 1 -> "${deg.roundToInt()}° sopra l'orizzonte"
        deg > -1 -> "all'orizzonte"
        else -> "sotto l'orizzonte"
    }

    private fun vrate(fpm: Int): String =
        if (imperial) "${int(fpm)} ft/min" else "${int((fpm * 0.3048).roundToInt())} m/min"

    fun vertRate(fpm: Int?): String? = when {
        fpm == null -> null
        abs(fpm) < 100 -> "in volo livellato"
        fpm > 0 -> "in salita, ${vrate(fpm)}"
        else -> "in discesa, ${vrate(abs(fpm))}"
    }
}
