package com.marco.mlaerei.ui

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marco.mlaerei.UiState
import com.marco.mlaerei.data.Aircraft
import com.marco.mlaerei.data.WeatherKind
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_ALT_M = 13_500.0
private val Ink = Color(0xFF2B3A47)
private val Accent = Color(0xFF1565C0)

// ---------------------------------------------------------------------- schermata

@Composable
fun SkyScreen(state: UiState, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val weather = state.weather
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
        ) {
            WeatherBackdrop(weather?.kind, Modifier.fillMaxSize())
            PlanesLayer(
                aircraft = state.aircraft,
                radiusKm = state.radiusKm,
                selectedHex = state.selected?.hex,
                onSelect = onSelect,
                modifier = Modifier.fillMaxSize(),
            )
            val chip = if (weather == null) {
                "Meteo non disponibile"
            } else {
                weather.kind.label + (weather.temperatureC?.let { " · ${it.roundToInt()}°C" } ?: "")
            }
            Text(
                text = chip,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF37474F),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            if (state.aircraft.isEmpty()) {
                Text(
                    text = "Nessun aereo entro ${state.radiusKm} km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF546E7A),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        state.selected?.let { SkySummary(it, state) }

        Text(
            "Da sinistra a destra: Nord, Est, Sud, Ovest. Più in alto = più alto in quota, " +
                "più grande = più vicino. Tocca un aereo per selezionarlo.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SkySummary(a: Aircraft, state: UiState) {
    val d = state.details
    val model = d?.model ?: a.typeDescription ?: a.typeCode
    val who = d?.airline ?: d?.owner
    val line1 = listOfNotNull(a.callsign, model, who).joinToString(" · ")
    val alt = when {
        a.onGround -> "a terra"
        a.altitudeM != null -> "${Fmt.int(a.altitudeM!!)} m"
        else -> null
    }
    val line2 = listOfNotNull(
        alt,
        "${Fmt.km(a.distanceKm)} verso ${Fmt.degrees(a.bearingDeg)}",
        a.speedKmh?.let { "${Fmt.int(it)} km/h" },
        Fmt.elevation(a.elevationDeg),
    ).joinToString(" · ")

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(line1, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                line2,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --------------------------------------------------------------------- aerei

private data class Placed(
    val a: Aircraft,
    val shape: Shape,
    val center: Offset,
    val widthPx: Float,
    val facingRight: Boolean,
    val alpha: Float,
)

/**
 * Posizione di ogni aereo: da sinistra a destra la direzione (Nord, Est, Sud, Ovest),
 * in verticale la quota, e la grandezza dice quanto è vicino. Dal più lontano al più vicino.
 */
private fun place(list: List<Aircraft>, size: Size, radiusKm: Int, dp: Float): List<Placed> {
    val groundY = size.height * 0.90f
    val topY = size.height * 0.08f
    return list.sortedByDescending { it.distanceKm }.map { a ->
        val shape = Silhouettes.shapeOf(a)
        val t = Math.pow((a.distanceKm / radiusKm).coerceIn(0.0, 1.0), 0.6).toFloat()
        val base = (120f - (120f - 26f) * t) * dp
        val width = (base * shape.lengthFactor).coerceAtLeast(18f * dp)

        val known = a.onGround || a.altitudeM != null
        val altM = when {
            a.onGround -> 0.0
            a.altitudeM != null -> a.altitudeM!!.toDouble()
            else -> 6000.0
        }
        val frac = (altM / MAX_ALT_M).coerceIn(0.0, 1.0).toFloat()
        val y = min(groundY - frac * (groundY - topY), groundY - width * 0.08f)
        val x = (a.bearingDeg / 360.0 * size.width).toFloat()
        val facingRight = (a.trackDeg ?: 90.0) in 0.0..180.0

        Placed(a, shape, Offset(x, y), width, facingRight, if (known) 1f else 0.45f)
    }
}

@Composable
private fun PlanesLayer(
    aircraft: List<Aircraft>,
    radiusKm: Int,
    selectedHex: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dens = LocalDensity.current
    val gridPaint = remember(dens) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF90A4AE.toInt()
            textSize = with(dens) { 10.sp.toPx() }
            textAlign = Paint.Align.LEFT
        }
    }
    val compassPaint = remember(dens) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF546E7A.toInt()
            textSize = with(dens) { 11.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }
    val labelPaint = remember(dens) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF263238.toInt()
            textSize = with(dens) { 10.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }
    val selectedPaint = remember(dens) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1565C0.toInt()
            textSize = with(dens) { 11.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    Canvas(
        modifier = modifier.pointerInput(aircraft, radiusKm) {
            detectTapGestures { tap ->
                val placed = place(
                    aircraft,
                    Size(size.width.toFloat(), size.height.toFloat()),
                    radiusKm,
                    density,
                )
                var best: Placed? = null
                var bestD = Float.MAX_VALUE
                for (p in placed) {
                    val d = (p.center - tap).getDistance()
                    val reach = max(40f * density, p.widthPx * 0.5f)
                    if (d < reach && d <= bestD) {
                        bestD = d
                        best = p
                    }
                }
                best?.let { onSelect(it.a.hex) }
            }
        },
    ) {
        val w = size.width
        val h = size.height
        val dp = density
        val groundY = h * 0.90f
        val topY = h * 0.08f
        val native = drawContext.canvas.nativeCanvas

        // linee di quota
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f * dp, 8f * dp))
        for (km in listOf(3, 6, 9, 12)) {
            val y = groundY - (km * 1000.0 / MAX_ALT_M).toFloat() * (groundY - topY)
            drawLine(Color(0x22000000), Offset(0f, y), Offset(w, y), strokeWidth = dp, pathEffect = dash)
            native.drawText("$km km", 10f * dp, y - 4f * dp, gridPaint)
        }

        // terra e punti cardinali
        drawRect(Color(0xFFDDE3E8), Offset(0f, groundY), Size(w, h - groundY))
        drawLine(Color(0xFFB0BEC5), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 1.5f * dp)
        val names = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO", "N")
        for ((i, name) in names.withIndex()) {
            val x = (i * 45f / 360f * w).coerceIn(12f * dp, w - 12f * dp)
            drawLine(Color(0xFFB0BEC5), Offset(x, groundY), Offset(x, groundY + 6f * dp), strokeWidth = dp)
            native.drawText(name, x, groundY + 20f * dp, compassPaint)
        }

        // aerei (dal più lontano al più vicino)
        val placed = place(aircraft, size, radiusKm, dp)
        val labelled = placed.takeLast(8).map { it.a.hex }.toSet()
        for (p in placed) {
            val selected = p.a.hex == selectedHex
            val color = (if (selected) Accent else Ink).copy(alpha = p.alpha)
            with(Silhouettes) {
                drawSilhouette(p.shape, p.center, p.widthPx, p.facingRight, color)
            }
            if (selected || p.a.hex in labelled) {
                native.drawText(
                    p.a.callsign,
                    p.center.x,
                    p.center.y + p.widthPx * 0.17f + 12f * dp,
                    if (selected) selectedPaint else labelPaint,
                )
            }
        }
    }
}

// --------------------------------------------------------------- sfondo del meteo

private val cloudSpots = listOf(
    Triple(0.05f, 0.14f, 1.0f),
    Triple(0.30f, 0.30f, 0.8f),
    Triple(0.55f, 0.10f, 1.2f),
    Triple(0.80f, 0.26f, 0.9f),
    Triple(0.15f, 0.48f, 0.7f),
    Triple(0.65f, 0.45f, 1.0f),
    Triple(0.92f, 0.58f, 0.8f),
    Triple(0.40f, 0.62f, 0.9f),
)

private fun DrawScope.cloud(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(color, r, Offset(cx, cy))
    drawCircle(color, r * 0.75f, Offset(cx - r, cy + r * 0.25f))
    drawCircle(color, r * 0.85f, Offset(cx + r, cy + r * 0.2f))
    drawCircle(color, r * 0.7f, Offset(cx + r * 0.2f, cy - r * 0.5f))
    drawRoundRect(color, Offset(cx - r * 1.75f, cy + r * 0.15f), Size(r * 3.5f, r * 0.9f), CornerRadius(r * 0.45f))
}

/** Sfondo bianco che cambia con il meteo: sole, nuvole, nebbia, pioggia, temporale, neve. */
@Composable
fun WeatherBackdrop(kind: WeatherKind?, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "weather")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(90_000, easing = LinearEasing)),
        label = "drift",
    )
    val fall by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "fall",
    )
    val snow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8_000, easing = LinearEasing)),
        label = "snow",
    )
    val flash by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "flash",
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val dp = density

        val bg = when (kind) {
            WeatherKind.CLOUDY -> Color(0xFFEEF1F3)
            WeatherKind.FOG -> Color(0xFFE9ECEE)
            WeatherKind.RAIN -> Color(0xFFE4E9ED)
            WeatherKind.STORM -> Color(0xFFD5DBE1)
            WeatherKind.SNOW -> Color(0xFFF6F9FC)
            else -> Color.White
        }
        drawRect(bg)

        // sole
        if (kind == WeatherKind.SUNNY || kind == WeatherKind.PARTLY) {
            val sc = Offset(w * 0.80f, h * 0.16f)
            val sr = min(w, h) * 0.10f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x66FFD54F), Color(0x00FFD54F)),
                    center = sc,
                    radius = sr * 3.2f,
                ),
                radius = sr * 3.2f,
                center = sc,
            )
            drawCircle(Color(0xFFFFD54F), sr, sc)
        }

        // nuvole
        val cloudCount = when (kind) {
            WeatherKind.PARTLY -> 3
            WeatherKind.CLOUDY, WeatherKind.RAIN, WeatherKind.STORM -> 8
            WeatherKind.SNOW -> 5
            else -> 0
        }
        val cloudColor = when (kind) {
            WeatherKind.PARTLY -> Color(0xFFE3E8EC)
            WeatherKind.CLOUDY -> Color(0xFFC9D1D8)
            WeatherKind.RAIN -> Color(0xFFB0BAC4)
            WeatherKind.STORM -> Color(0xFF8F9BA8)
            else -> Color(0xFFDCE4EC)
        }
        for (i in 0 until cloudCount) {
            val (xf, yf, sc) = cloudSpots[i]
            val r = min(w, h) * 0.07f * sc
            val span = w + 8f * r
            val x = ((xf + drift) % 1f) * span - 4f * r
            cloud(x, h * yf, r, cloudColor)
        }

        // nebbia
        if (kind == WeatherKind.FOG) {
            for (yf in listOf(0.22f, 0.42f, 0.62f, 0.80f)) {
                val bandH = h * 0.18f
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0x00B0BEC5), Color(0x77B0BEC5), Color(0x00B0BEC5)),
                        startY = h * yf - bandH / 2f,
                        endY = h * yf + bandH / 2f,
                    ),
                    topLeft = Offset(0f, h * yf - bandH / 2f),
                    size = Size(w, bandH),
                )
            }
        }

        // pioggia
        if (kind == WeatherKind.RAIN || kind == WeatherKind.STORM) {
            val n = if (kind == WeatherKind.STORM) 130 else 90
            for (i in 0 until n) {
                val rx = ((i * 7919) % 1000) / 1000f
                val ry = ((i * 104729) % 1000) / 1000f
                val x = rx * w
                val y = ((ry + fall) % 1f) * h
                drawLine(Color(0x889DB0C2), Offset(x, y), Offset(x - 4f * dp, y + 14f * dp), strokeWidth = 1.5f * dp)
            }
        }

        // fulmine
        if (kind == WeatherKind.STORM && (flash in 0.50f..0.52f || flash in 0.56f..0.575f)) {
            drawRect(Color.White.copy(alpha = 0.6f))
        }

        // neve
        if (kind == WeatherKind.SNOW) {
            for (i in 0 until 60) {
                val rx = ((i * 7919) % 1000) / 1000f
                val ry = ((i * 104729) % 1000) / 1000f
                val sway = kotlin.math.sin((ry + snow) * 6.2832f) * 8f * dp
                val x = rx * w + sway
                val y = ((ry + snow) % 1f) * h
                drawCircle(Color(0xFFB7C7D9), radius = 2.5f * dp, center = Offset(x, y))
            }
        }
    }
}
