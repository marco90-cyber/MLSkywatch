package com.marco.mlaerei.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil
import kotlin.math.min

class AircraftRepository {

    private data class RouteInfo(
        val airline: String?,
        val originCity: String?,
        val originCode: String?,
        val destCity: String?,
        val destCode: String?,
    )

    private data class AircraftInfo(
        val model: String?,
        val manufacturer: String?,
        val owner: String?,
        val photoUrl: String?,
    )

    private data class PhotoInfo(val url: String, val link: String?, val photographer: String?)

    // ---------------------------------------------------------------- voli vicini

    /**
     * Scarica gli aerei entro [radiusKm] da (lat, lon).
     * Fonte principale adsb.lol, di riserva adsb.fi (stesso formato).
     */
    suspend fun fetchNearby(
        lat: Double,
        lon: Double,
        radiusKm: Int,
        includeGround: Boolean,
    ): List<Aircraft> = withContext(Dispatchers.IO) {
        val nm = min(250, ceil(radiusKm / 1.852).toInt().coerceAtLeast(1))
        val urls = listOf(
            "https://api.adsb.lol/v2/point/$lat/$lon/$nm",
            "https://opendata.adsb.fi/api/v2/lat/$lat/lon/$lon/dist/$nm",
        )
        var lastError: Exception? = null
        for (url in urls) {
            try {
                val body = httpGet(url)
                return@withContext parseAircraft(body, lat, lon, radiusKm, includeGround)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("nessuna fonte disponibile")
    }

    private fun parseAircraft(
        body: String,
        userLat: Double,
        userLon: Double,
        radiusKm: Int,
        includeGround: Boolean,
    ): List<Aircraft> {
        val arr: JSONArray = JSONObject(body).optJSONArray("ac") ?: JSONArray()
        val out = ArrayList<Aircraft>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (!o.has("lat") || !o.has("lon")) continue
            val lat = o.optDouble("lat", Double.NaN)
            val lon = o.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue

            val altRaw = o.opt("alt_baro")
            val onGround = altRaw is String && altRaw.equals("ground", ignoreCase = true)
            val altFt = (altRaw as? Number)?.toInt()
            if (onGround && !includeGround) continue

            val distance = Geo.distanceKm(userLat, userLon, lat, lon)
            if (distance > radiusKm) continue

            val hex = o.str("hex")?.uppercase() ?: continue
            val flight = o.str("flight")
            val reg = o.str("r")
            val callsign = flight ?: reg ?: hex

            val track = (o.opt("track") as? Number)?.toDouble()
                ?: (o.opt("true_heading") as? Number)?.toDouble()
            val rate = (o.opt("baro_rate") as? Number)?.toInt()
                ?: (o.opt("geom_rate") as? Number)?.toInt()

            out.add(
                Aircraft(
                    hex = hex,
                    callsign = callsign,
                    hasCallsign = flight != null,
                    registration = reg,
                    typeCode = o.str("t"),
                    typeDescription = o.str("desc"),
                    lat = lat,
                    lon = lon,
                    altitudeFt = altFt,
                    onGround = onGround,
                    speedKt = (o.opt("gs") as? Number)?.toDouble(),
                    trackDeg = track,
                    vertRateFpm = rate,
                    squawk = o.str("squawk"),
                    distanceKm = distance,
                    bearingDeg = Geo.bearingDeg(userLat, userLon, lat, lon),
                )
            )
        }
        out.sortBy { it.distanceKm }
        return out
    }

    // ------------------------------------------------------------ dettagli aereo

    /** Rotta (adsbdb), dati dell'aeromobile (adsbdb) e foto (planespotters.net). Non lancia mai errori di rete. */
    suspend fun fetchDetails(a: Aircraft): AircraftDetails = coroutineScope {
        val routeJob = async(Dispatchers.IO) {
            if (a.hasCallsign) safe { fetchRoute(a.callsign) } else null
        }
        val infoJob = async(Dispatchers.IO) { safe { fetchAircraftInfo(a.hex) } }
        val photoJob = async(Dispatchers.IO) { safe { fetchPhoto(a.hex) } }

        val route = routeJob.await()
        val info = infoJob.await()
        val photo = photoJob.await()

        AircraftDetails(
            airline = route?.airline,
            originCity = route?.originCity,
            originCode = route?.originCode,
            destCity = route?.destCity,
            destCode = route?.destCode,
            model = info?.model,
            manufacturer = info?.manufacturer,
            owner = info?.owner,
            photoUrl = photo?.url ?: info?.photoUrl,
            photoLink = photo?.link,
            photographer = photo?.photographer,
            photoSource = when {
                photo != null -> "planespotters.net"
                info?.photoUrl != null -> "airport-data.com"
                else -> null
            },
        )
    }

    private fun fetchRoute(callsign: String): RouteInfo? {
        val url = "https://api.adsbdb.com/v0/callsign/" + URLEncoder.encode(callsign, "UTF-8")
        val route = JSONObject(httpGet(url))
            .optJSONObject("response")
            ?.optJSONObject("flightroute") ?: return null
        val origin = route.optJSONObject("origin")
        val dest = route.optJSONObject("destination")
        return RouteInfo(
            airline = route.optJSONObject("airline")?.str("name"),
            originCity = origin?.str("municipality"),
            originCode = origin?.str("iata_code") ?: origin?.str("icao_code"),
            destCity = dest?.str("municipality"),
            destCode = dest?.str("iata_code") ?: dest?.str("icao_code"),
        )
    }

    private fun fetchAircraftInfo(hex: String): AircraftInfo? {
        val ac = JSONObject(httpGet("https://api.adsbdb.com/v0/aircraft/$hex"))
            .optJSONObject("response")
            ?.optJSONObject("aircraft") ?: return null
        return AircraftInfo(
            model = ac.str("type"),
            manufacturer = ac.str("manufacturer"),
            owner = ac.str("registered_owner"),
            photoUrl = ac.str("url_photo"),
        )
    }

    private fun fetchPhoto(hex: String): PhotoInfo? {
        val photos = JSONObject(httpGet("https://api.planespotters.net/pub/photos/hex/$hex"))
            .optJSONArray("photos") ?: return null
        val first = photos.optJSONObject(0) ?: return null
        val src = first.optJSONObject("thumbnail_large")?.str("src")
            ?: first.optJSONObject("thumbnail")?.str("src")
            ?: return null
        return PhotoInfo(
            url = src,
            link = first.str("link"),
            photographer = first.str("photographer"),
        )
    }

    // ------------------------------------------------------------------- utilità

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private inline fun <T> safe(block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun JSONObject.str(key: String): String? {
        val v = optString(key, "").trim()
        return if (v.isEmpty() || v == "null") null else v
    }

    companion object {
        private const val USER_AGENT = "MLSkywatch/1.0 (app Android personale)"
    }
}
