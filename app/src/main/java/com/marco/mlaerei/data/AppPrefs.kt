package com.marco.mlaerei.data

import android.content.Context

class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var radiusKm: Int
        get() = sp.getInt(KEY_RADIUS, DEFAULT_RADIUS_KM)
        set(value) = sp.edit().putInt(KEY_RADIUS, value).apply()

    var showGround: Boolean
        get() = sp.getBoolean(KEY_GROUND, false)
        set(value) = sp.edit().putBoolean(KEY_GROUND, value).apply()

    var refreshSec: Int
        get() = sp.getInt(KEY_REFRESH, DEFAULT_REFRESH_SEC)
        set(value) = sp.edit().putInt(KEY_REFRESH, value).apply()

    var headingUp: Boolean
        get() = sp.getBoolean(KEY_HEADING_UP, false)
        set(value) = sp.edit().putBoolean(KEY_HEADING_UP, value).apply()

    var imperial: Boolean
        get() = sp.getBoolean(KEY_IMPERIAL, false)
        set(value) = sp.edit().putBoolean(KEY_IMPERIAL, value).apply()

    /** 0 = come il sistema, 1 = chiaro, 2 = scuro */
    var themeMode: Int
        get() = sp.getInt(KEY_THEME, 0)
        set(value) = sp.edit().putInt(KEY_THEME, value).apply()

    companion object {
        /**
         * Raggio predefinito: un aereo di linea in quota si distingue a occhio nudo,
         * con aria limpida, fino a circa 50 km.
         */
        const val DEFAULT_RADIUS_KM = 50
        const val MIN_RADIUS_KM = 5
        const val MAX_RADIUS_KM = 250
        const val DEFAULT_REFRESH_SEC = 10

        private const val KEY_RADIUS = "radius_km"
        private const val KEY_GROUND = "show_ground"
        private const val KEY_REFRESH = "refresh_sec"
        private const val KEY_HEADING_UP = "heading_up"
        private const val KEY_IMPERIAL = "imperial"
        private const val KEY_THEME = "theme_mode"
    }
}
