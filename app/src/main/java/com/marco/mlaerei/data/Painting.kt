package com.marco.mlaerei.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException

enum class PaintingStyle(val label: String, val promptText: String) {
    POSTER("Poster d'epoca", "vintage travel poster illustration, bold flat colors, retro art print"),
    WATERCOLOR("Acquerello", "delicate watercolor painting, soft washes of color, visible paper texture"),
    ENGRAVING("Incisione", "fine ink engraving, detailed crosshatching, antique scientific plate"),
    ARTDECO("Art déco", "art deco illustration, elegant geometric shapes, navy and gold palette"),
    PENCIL("Matita", "detailed graphite pencil drawing, soft shading"),
    PIXEL("Pixel art", "pixel art, retro 16-bit game style"),
}

enum class Backdrop(val label: String, val promptText: String) {
    PAPER("Carta bianca", "on a plain off-white paper background"),
    CLEAR("Cielo sereno", "flying in a clear blue sky with a few small clouds"),
    SUNSET("Tramonto", "flying through a glowing orange and pink sunset sky"),
    NIGHT("Notte", "flying at night under a starry sky"),
    STORM("Tempesta", "flying through dramatic dark storm clouds"),
}

/**
 * Dipinti generati dall'AI con Pollinations (endpoint gratuito, senza chiave).
 * Il servizio anonimo accetta circa una richiesta ogni 15 secondi, quindi le richieste
 * sono messe in coda e ogni dipinto viene salvato sul telefono per non rifarlo.
 */
class PaintingRepository(private val dir: File) {

    private val mutex = Mutex()
    private var lastRequestAt = 0L

    fun buildPrompt(a: Aircraft, d: AircraftDetails?, style: PaintingStyle, backdrop: Backdrop): String {
        val operator = d?.airline ?: d?.owner
        val model = d?.model ?: a.typeDescription ?: a.typeCode ?: "aircraft"
        val subject = buildString {
            append("side view of ")
            if (operator != null) append("a $operator ")
            else append("an ")
            append(model)
            append(" aircraft in its real livery")
        }
        return "${style.promptText}, $subject, ${backdrop.promptText}, centered, wide shot, " +
            "no text, no letters, no watermark"
    }

    fun cachedFile(prompt: String): File? {
        val f = fileFor(prompt)
        return if (f.exists() && f.length() > MIN_BYTES) f else null
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun count(): Int = dir.listFiles()?.size ?: 0

    suspend fun getOrCreate(prompt: String): File = withContext(Dispatchers.IO) {
        cachedFile(prompt)?.let { return@withContext it }
        mutex.withLock {
            cachedFile(prompt)?.let { return@withContext it }
            var attempt = 0
            while (true) {
                val wait = lastRequestAt + MIN_GAP_MS - System.currentTimeMillis()
                if (wait > 0) delay(wait)
                lastRequestAt = System.currentTimeMillis()
                try {
                    return@withContext download(prompt)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    attempt++
                    if (attempt >= 3) throw e
                    delay(20_000L)
                }
            }
            throw IOException("dipinto non disponibile")
        }
    }

    private fun download(prompt: String): File {
        val seed = (sha1(prompt).take(8).toLong(16) and 0x7FFFFFFF)
        val encoded = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
        val url = "https://image.pollinations.ai/prompt/$encoded" +
            "?width=768&height=480&seed=$seed&model=flux&nologo=true"

        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 120_000
            conn.setRequestProperty("User-Agent", "MLSkywatch/1.0 (app Android personale)")
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            val type = conn.contentType ?: ""
            if (!type.startsWith("image")) throw IOException("risposta non valida")

            dir.mkdirs()
            val target = fileFor(prompt)
            val tmp = File(dir, target.name + ".tmp")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (tmp.length() <= MIN_BYTES) {
                tmp.delete()
                throw IOException("immagine vuota")
            }
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw IOException("salvataggio non riuscito")
            }
            return target
        } finally {
            conn.disconnect()
        }
    }

    private fun fileFor(prompt: String) = File(dir, "p_" + sha1(prompt) + ".jpg")

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val MIN_GAP_MS = 16_000L
        private const val MIN_BYTES = 5_000L
    }
}
