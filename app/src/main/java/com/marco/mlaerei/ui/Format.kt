package com.marco.mlaerei.ui

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Fmt {
    private val IT: Locale = Locale.ITALY

    fun int(n: Int): String = String.format(IT, "%,d", n)

    fun km(d: Double): String =
        if (d < 10) String.format(IT, "%.1f km", d) else String.format(IT, "%,d km", d.roundToInt())

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

    fun vertRate(fpm: Int?): String? = when {
        fpm == null -> null
        abs(fpm) < 100 -> "in volo livellato"
        fpm > 0 -> "in salita, ${int(fpm)} ft/min"
        else -> "in discesa, ${int(abs(fpm))} ft/min"
    }
}
