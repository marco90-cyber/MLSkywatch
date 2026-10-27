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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marco.mlaerei.data.Aircraft
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val RadarBackground = Color(0xFF0D1B2A)
private val RadarLines = Color(0xFF2E4A62)
private val RadarSweep = Color(0xFF4FC3F7)
private val RadarText = 0xFF9FB6CC.toInt()

fun altitudeColor(a: Aircraft): Color {
    if (a.onGround) return Color(0xFF9E9E9E)
    val ft = a.altitudeFt ?: return Color(0xFFB0BEC5)
    return when {
        ft < 3_000 -> Color(0xFFFF7043)
        ft < 10_000 -> Color(0xFFFFCA28)
        ft < 25_000 -> Color(0xFF66BB6A)
        else -> Color(0xFF42A5F5)
    }
}

private fun project(a: Aircraft, center: Offset, radius: Float, radiusKm: Int): Offset {
    val f = (a.distanceKm / radiusKm).toFloat().coerceAtMost(1f) * radius
    val b = Math.toRadians(a.bearingDeg)
    return Offset(center.x + f * sin(b).toFloat(), center.y - f * cos(b).toFloat())
}

@Composable
fun RadarView(
    aircraft: List<Aircraft>,
    radiusKm: Int,
    selectedHex: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "sweep")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing)),
        label = "angle",
    )
    val density = LocalDensity.current
    val ringPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RadarText
            textSize = with(density) { 11.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }
    val labelPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 11.sp.toPx() }
            textAlign = Paint.Align.LEFT
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(RadarBackground)
            .pointerInput(aircraft, radiusKm) {
                detectTapGestures { tap ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = min(size.width, size.height) / 2f * 0.9f
                    var nearest: Aircraft? = null
                    var nearestDist = Float.MAX_VALUE
                    for (a in aircraft) {
                        val d = (project(a, c, r, radiusKm) - tap).getDistance()
                        if (d < nearestDist) {
                            nearestDist = d
                            nearest = a
                        }
                    }
                    if (nearest != null && nearestDist < 48.dp.toPx()) onSelect(nearest.hex)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = min(size.width, size.height) / 2f * 0.9f
            val line = 1.dp.toPx()

            // anelli e croce
            for (i in 1..3) {
                drawCircle(RadarLines, radius = r * i / 3f, center = c, style = Stroke(line))
            }
            drawLine(RadarLines, Offset(c.x - r, c.y), Offset(c.x + r, c.y), line)
            drawLine(RadarLines, Offset(c.x, c.y - r), Offset(c.x, c.y + r), line)

            // scansione
            rotate(degrees = sweep, pivot = c) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1f to RadarSweep.copy(alpha = 0.35f),
                        center = c,
                    ),
                    radius = r,
                    center = c,
                )
            }

            // etichette: punti cardinali e distanze degli anelli
            val native = drawContext.canvas.nativeCanvas
            val pad = 4.dp.toPx()
            native.drawText("N", c.x, c.y - r - pad, ringPaint)
            native.drawText("S", c.x, c.y + r + pad + ringPaint.textSize * 0.8f, ringPaint)
            native.drawText("E", c.x + r + pad + ringPaint.textSize * 0.45f, c.y + ringPaint.textSize * 0.35f, ringPaint)
            native.drawText("O", c.x - r - pad - ringPaint.textSize * 0.45f, c.y + ringPaint.textSize * 0.35f, ringPaint)
            for (i in 1..3) {
                val km = (radiusKm * i / 3.0).roundToInt()
                val rr = r * i / 3f * 0.7071f
                native.drawText("$km km", c.x + rr + 2.dp.toPx(), c.y - rr, labelPaint.apply { alpha = 150 })
            }
            labelPaint.alpha = 255

            // io al centro
            drawCircle(Color.White, radius = 4.dp.toPx(), center = c)
            drawCircle(Color.White.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = c, style = Stroke(line))

            // aerei
            val showAllLabels = aircraft.size <= 12
            val selected = aircraft.firstOrNull { it.hex == selectedHex } ?: aircraft.firstOrNull()
            for (a in aircraft) {
                val p = project(a, c, r, radiusKm)
                val isSel = a.hex == selected?.hex
                val s = (if (isSel) 11.dp else 8.dp).toPx()
                val color = altitudeColor(a)

                if (isSel) {
                    drawCircle(color.copy(alpha = 0.25f), radius = s * 2.2f, center = p)
                    drawCircle(color, radius = s * 2.2f, center = p, style = Stroke(line * 1.5f))
                }

                val path = Path().apply {
                    moveTo(p.x, p.y - s)
                    lineTo(p.x + s * 0.65f, p.y + s * 0.75f)
                    lineTo(p.x, p.y + s * 0.35f)
                    lineTo(p.x - s * 0.65f, p.y + s * 0.75f)
                    close()
                }
                rotate(degrees = (a.trackDeg ?: 0.0).toFloat(), pivot = p) {
                    drawPath(path, color)
                }

                if (isSel || showAllLabels) {
                    native.drawText(a.callsign, p.x + s * 1.3f, p.y - s * 0.6f, labelPaint)
                }
            }
        }
    }
}
