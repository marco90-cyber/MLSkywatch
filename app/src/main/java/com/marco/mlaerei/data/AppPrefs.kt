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

    var paintingEnabled: Boolean
        get() = sp.getBoolean(KEY_PAINTING, true)
        set(value) = sp.edit().putBoolean(KEY_PAINTING, value).apply()

    var paintingStyle: PaintingStyle
        get() = PaintingStyle.values().firstOrNull { it.name == sp.getString(KEY_STYLE, null) }
            ?: PaintingStyle.POSTER
        set(value) = sp.edit().putString(KEY_STYLE, value.name).apply()

    var backdrop: Backdrop
        get() = Backdrop.values().firstOrNull { it.name == sp.getString(KEY_BACKDROP, null) }
            ?: Backdrop.CLEAR
        set(value) = sp.edit().putString(KEY_BACKDROP, value.name).apply()

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
        private const val KEY_PAINTING = "painting_enabled"
        private const val KEY_STYLE = "painting_style"
        private const val KEY_BACKDROP = "painting_backdrop"
    }
}
