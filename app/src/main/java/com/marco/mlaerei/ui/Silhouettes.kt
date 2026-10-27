package com.marco.mlaerei.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.marco.mlaerei.data.Aircraft

/** Famiglie di sagome disegnate di profilo. [lengthFactor] = lunghezza relativa a un aereo di linea. */
enum class Shape(val lengthFactor: Float) {
    NARROW(1.0f),
    WIDE(1.2f),
    QUAD(1.3f),
    REGIONAL(0.8f),
    BIZJET(0.55f),
    TURBOPROP(0.75f),
    LIGHT(0.4f),
    HELI(0.5f),
}

object Silhouettes {

    // ------------------------------------------------------------ riconoscimento

    private val heli = setOf(
        "R22", "R44", "R66", "EC20", "EC30", "EC35", "EC45", "EC55", "EC75", "H125", "H130", "H135",
        "H145", "H155", "H160", "H175", "H225", "AS32", "AS35", "AS50", "AS55", "AS65", "B06", "B06T",
        "B105", "B212", "B214", "B222", "B407", "B412", "B427", "B429", "B430", "B505", "A109", "A119",
        "A139", "A149", "A169", "A189", "AW09", "S61", "S76", "S92", "S58", "MI8", "MI17", "MI24",
        "KA32", "UH1", "H60", "H47", "H64", "EXPL", "EN28", "EN48", "GAZL", "LYNX", "NH90", "TIGR",
    )
    private val quad = setOf(
        "A388", "B741", "B742", "B743", "B744", "B748", "B74S", "B74R", "A124", "A225", "A342",
        "A343", "A345", "A346", "IL96", "C5M", "C17", "B52",
    )
    private val wide = setOf(
        "A306", "A30B", "A310", "A332", "A333", "A338", "A339", "A359", "A35K", "B762", "B763",
        "B764", "B772", "B773", "B77L", "B77W", "B788", "B789", "B78X", "MD11", "DC10", "L101", "IL86",
    )
    private val regional = setOf(
        "E135", "E140", "E145", "E170", "E175", "E190", "E195", "E75L", "E75S", "E290", "E295",
        "CRJ1", "CRJ2", "CRJ7", "CRJ9", "CRJX", "RJ70", "RJ85", "RJ1H", "F70", "F100", "F28", "SU95",
        "AR85", "B462", "B463",
    )
    private val turboprop = setOf(
        "AT43", "AT44", "AT45", "AT46", "AT72", "AT73", "AT75", "AT76", "DH8A", "DH8B", "DH8C",
        "DH8D", "SF34", "SB20", "JS31", "JS32", "JS41", "E120", "B190", "B350", "BE20", "BE99", "F50",
        "F27", "L410", "PC12", "C208", "TBM7", "TBM8", "TBM9", "P180", "SW4", "D328", "DHC6", "DHC7",
        "AN26", "AN24", "A400", "C130", "C30J", "AC90", "AC95", "BE9L", "BE9T",
    )
    private val bizPrefixes = listOf(
        "GLF", "GL5", "GL6", "GL7", "GLEX", "FA", "LJ", "CL3", "CL6", "C25", "C500", "C501", "C510",
        "C525", "C550", "C560", "C56X", "C650", "C680", "C68A", "C700", "C750", "E50P", "E55P", "E545",
        "E550", "H25", "PC24", "PRM1", "G150", "G280", "G200", "F2TH", "F900", "ASTR", "BE40", "HDJT",
        "SF50",
    )
    private val light = setOf(
        "C150", "C152", "C172", "C182", "C206", "C210", "C177", "C140", "PA28", "PA32", "PA34", "PA44",
        "PA46", "P28A", "P28B", "P28R", "P32R", "SR20", "SR22", "DA20", "DA40", "DA42", "DA62", "BE33",
        "BE35", "BE36", "BE55", "BE58", "M20P", "M20T", "AA5", "S22T", "TB20", "TB10", "P208", "GLID",
        "ULAC", "J3", "GA8", "C82R", "C402", "BN2P", "SIRA", "FK9",
    )

    fun shapeOf(a: Aircraft): Shape {
        if (a.category == "A7") return Shape.HELI
        val t = a.typeCode?.uppercase()
        if (t != null) {
            when {
                t in heli -> return Shape.HELI
                t in quad -> return Shape.QUAD
                t in wide -> return Shape.WIDE
                t in regional -> return Shape.REGIONAL
                t in turboprop -> return Shape.TURBOPROP
                bizPrefixes.any { t.startsWith(it) } -> return Shape.BIZJET
                t in light -> return Shape.LIGHT
            }
        }
        return when (a.category) {
            "A1" -> Shape.LIGHT
            "A5" -> Shape.WIDE
            "A3", "A4" -> Shape.NARROW
            else -> if (a.speedKt == null || a.speedKt >= 200.0) Shape.NARROW else Shape.LIGHT
        }
    }

    // ------------------------------------------------------------------ disegno

    private val cache = HashMap<Shape, List<Path>>()

    private fun partsOf(shape: Shape): List<Path> = cache.getOrPut(shape) {
        when (shape) {
            Shape.NARROW -> airliner(half = 0.052f, fin = 0.14f, engines = listOf(0.02f), engH = 0.055f)
            Shape.WIDE -> airliner(half = 0.068f, fin = 0.17f, engines = listOf(0.0f), engH = 0.07f)
            Shape.QUAD -> airliner(half = 0.07f, fin = 0.17f, engines = listOf(-0.03f, 0.12f), engH = 0.065f)
            Shape.REGIONAL -> tailEngineJet(half = 0.042f, fin = 0.17f)
            Shape.BIZJET -> tailEngineJet(half = 0.05f, fin = 0.15f)
            Shape.TURBOPROP -> propPlane(half = 0.05f, fin = 0.15f, wide = true)
            Shape.LIGHT -> propPlane(half = 0.065f, fin = 0.15f, wide = false)
            Shape.HELI -> helicopter()
        }
    }

    /** Disegna la sagoma centrata in [center], lunga [widthPx], rivolta a destra o a sinistra. */
    fun DrawScope.drawSilhouette(
        shape: Shape,
        center: Offset,
        widthPx: Float,
        facingRight: Boolean,
        color: Color,
    ) {
        val parts = partsOf(shape)
        withTransform({
            translate(center.x, center.y)
            scale(if (facingRight) widthPx else -widthPx, widthPx, Offset.Zero)
        }) {
            for (p in parts) drawPath(p, color)
        }
    }

    // ----------------------------------------------------------------- forme base
    // Coordinate unitarie: lunghezza 1 (da x = -0.5 a x = 0.5), muso a destra, y verso il basso.

    private fun poly(vararg p: Float): Path = Path().apply {
        moveTo(p[0], p[1])
        var i = 2
        while (i < p.size) {
            lineTo(p[i], p[i + 1])
            i += 2
        }
        close()
    }

    private fun oval(l: Float, t: Float, r: Float, b: Float): Path =
        Path().apply { addOval(Rect(l, t, r, b)) }

    private fun fuselage(half: Float): Path = Path().apply {
        moveTo(0.50f, 0.005f)
        cubicTo(0.49f, -half * 0.7f, 0.44f, -half, 0.36f, -half)
        lineTo(-0.30f, -half)
        cubicTo(-0.40f, -half, -0.46f, -half * 0.7f, -0.50f, -half * 0.4f)
        lineTo(-0.50f, 0f)
        cubicTo(-0.46f, half * 0.6f, -0.40f, half, -0.32f, half)
        lineTo(0.36f, half)
        cubicTo(0.44f, half, 0.49f, half * 0.7f, 0.50f, 0.005f)
        close()
    }

    /** Aereo di linea: ali basse, motori sotto l'ala. */
    private fun airliner(half: Float, fin: Float, engines: List<Float>, engH: Float): List<Path> {
        val parts = ArrayList<Path>()
        parts.add(fuselage(half))
        parts.add(poly(-0.24f, -half + 0.005f, -0.43f, -half - fin, -0.50f, -half - fin, -0.47f, -half + 0.005f))
        parts.add(poly(0.10f, half - 0.01f, -0.14f, half + 0.075f, -0.21f, half + 0.075f, -0.05f, half - 0.01f))
        for (x in engines) parts.add(oval(x, half - 0.005f, x + 0.15f, half + engH))
        return parts
    }

    /** Jet regionale o executive: coda a T e motori dietro. */
    private fun tailEngineJet(half: Float, fin: Float): List<Path> {
        val parts = ArrayList<Path>()
        parts.add(fuselage(half))
        parts.add(poly(-0.28f, -half + 0.005f, -0.44f, -half - fin, -0.50f, -half - fin, -0.48f, -half + 0.005f))
        parts.add(poly(-0.54f, -half - fin - 0.02f, -0.40f, -half - fin - 0.02f, -0.39f, -half - fin + 0.005f, -0.53f, -half - fin + 0.005f))
        parts.add(poly(0.08f, half - 0.01f, -0.10f, half + 0.06f, -0.16f, half + 0.06f, -0.03f, half - 0.01f))
        parts.add(oval(-0.46f, -half - 0.01f, -0.26f, half * 0.2f))
        return parts
    }

    /** Aereo a elica: ala alta, elica davanti. */
    private fun propPlane(half: Float, fin: Float, wide: Boolean): List<Path> {
        val parts = ArrayList<Path>()
        parts.add(fuselage(half))
        parts.add(poly(-0.26f, -half + 0.005f, -0.44f, -half - fin, -0.50f, -half - fin, -0.48f, -half + 0.005f))
        parts.add(poly(0.14f, -half - 0.025f, -0.12f, -half - 0.025f, -0.12f, -half + 0.01f, 0.14f, -half + 0.01f))
        if (wide) {
            parts.add(oval(0.00f, -half - 0.065f, 0.22f, -half - 0.005f))
            parts.add(poly(0.225f, -half - 0.125f, 0.240f, -half - 0.125f, 0.240f, -half + 0.065f, 0.225f, -half + 0.065f))
        } else {
            parts.add(poly(0.505f, -0.13f, 0.535f, -0.13f, 0.535f, 0.13f, 0.505f, 0.13f))
        }
        return parts
    }

    private fun helicopter(): List<Path> {
        val parts = ArrayList<Path>()
        parts.add(oval(0.02f, -0.12f, 0.50f, 0.10f))                       // cabina
        parts.add(poly(0.12f, -0.07f, -0.45f, -0.045f, -0.45f, -0.015f, 0.12f, 0.0f)) // trave di coda
        parts.add(poly(-0.38f, -0.04f, -0.46f, -0.17f, -0.50f, -0.17f, -0.47f, -0.02f)) // deriva
        parts.add(poly(-0.30f, -0.17f, 0.50f, -0.17f, 0.50f, -0.155f, -0.30f, -0.155f)) // rotore principale
        parts.add(poly(0.19f, -0.165f, 0.22f, -0.165f, 0.22f, -0.115f, 0.19f, -0.115f)) // albero rotore
        parts.add(poly(0.06f, 0.115f, 0.44f, 0.115f, 0.44f, 0.13f, 0.06f, 0.13f))      // pattini
        parts.add(poly(0.12f, 0.09f, 0.135f, 0.09f, 0.135f, 0.12f, 0.12f, 0.12f))
        parts.add(poly(0.37f, 0.09f, 0.385f, 0.09f, 0.385f, 0.12f, 0.37f, 0.12f))
        return parts
    }
}
