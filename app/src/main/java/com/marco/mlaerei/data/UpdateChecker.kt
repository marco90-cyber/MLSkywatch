package com.marco.mlaerei.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateResult {
    data class UpToDate(val versionName: String) : UpdateResult
    data class Available(val versionName: String, val downloadUrl: String, val pageUrl: String) : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

/** Cerca l'ultima versione pubblicata nelle Release di GitHub e la confronta con quella installata. */
object UpdateChecker {
    private const val LATEST = "https://api.github.com/repos/marco90-cyber/MLSkywatch/releases/latest"

    @Suppress("DEPRECATION")
    fun installed(context: Context): Pair<Long, String> {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        return code to (info.versionName ?: "?")
    }

    suspend fun check(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val (code, name) = installed(context)
            val conn = URL(LATEST).openConnection() as HttpURLConnection
            val body = try {
                conn.connectTimeout = 8_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "MLSkywatch/$name (+https://github.com/marco90-cyber/MLSkywatch)")
                val status = conn.responseCode
                if (status == 404) return@withContext UpdateResult.Failed("nessuna versione pubblicata")
                if (status == 403 || status == 429) return@withContext UpdateResult.Failed("troppe richieste a GitHub, riprova tra poco")
                if (status !in 200..299) return@withContext UpdateResult.Failed("GitHub ha risposto HTTP $status")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }

            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            val latestCode = tag.substringAfterLast('.').toLongOrNull()
                ?: return@withContext UpdateResult.Failed("versione non riconosciuta ($tag)")
            if (latestCode <= code) return@withContext UpdateResult.UpToDate(name)

            val assets = json.optJSONArray("assets")
            var apk: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.optJSONObject(i) ?: continue
                    if (a.optString("name").endsWith(".apk")) {
                        apk = a.optString("browser_download_url").takeIf { it.isNotBlank() }
                        break
                    }
                }
            }
            val page = json.optString("html_url", "https://github.com/marco90-cyber/MLSkywatch/releases/latest")
            UpdateResult.Available(tag.removePrefix("v"), apk ?: page, page)
        } catch (e: java.net.UnknownHostException) {
            UpdateResult.Failed("nessuna connessione a internet")
        } catch (e: java.net.SocketTimeoutException) {
            UpdateResult.Failed("GitHub non risponde (timeout)")
        } catch (e: Exception) {
            UpdateResult.Failed(e.message ?: "errore sconosciuto")
        }
    }
}
