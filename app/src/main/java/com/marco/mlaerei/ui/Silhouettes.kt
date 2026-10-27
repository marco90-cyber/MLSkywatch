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
    JUMBO(1.35f),     // 747: gobba del ponte superiore
    SUPER(1.4f),      // A380: ponte doppio per tutta la lunghezza
    TRANSPORT(0.95f), // A400M, C-130: ala alta, quattro eliche
    FIGHTER(0.45f),   // caccia militare
    GLIDER(0.45f),    // aliante
    MDTYPE(0.9f),     // motori in coda: MD-80, 717, 727
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
    private val quad = setOf("A124", "A225", "A342", "A343", "A345", "A346", "IL96", "C5M", "C17", "B52")
    private val jumbo = setOf("B741", "B742", "B743", "B744", "B748", "B74S", "B74R", "B74F")
    private val transport = setOf("A400", "C130", "C30J", "C160", "AN12", "IL76")
    private val mdtype = setOf(
        "MD80", "MD81", "MD82", "MD83", "MD87", "MD88", "MD90", "B712", "DC91", "DC93", "DC95",
        "B721", "B722", "T154", "TU34", "DC9",
    )
    private val fighterPrefixes = listOf(
        "F16", "F15", "F18", "F22", "F35", "F5E", "F5F", "EUFI", "TORN", "RFAL", "GRIP", "M346", "T38", "A10",
        "MIG", "SU2", "SU3", "FA18", "HAWK", "A29", "F4", "TEX2", "AJET", "T6",
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
        "AN26", "AN24", "AC90", "AC95", "BE9L", "BE9T",
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
                t == "A388" -> return Shape.SUPER
                t in jumbo -> return Shape.JUMBO
                t in heli -> return Shape.HELI
                t in transport -> return Shape.TRANSPORT
                t in mdtype -> return Shape.MDTYPE
                t == "GLID" || t == "ULAC" -> return Shape.GLIDER
                fighterPrefixes.any { t.startsWith(it) } && t.length <= 5 && a.category != "A3" -> return Shape.FIGHTER
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

    private enum class Kind { FAR, BODY, DETAIL, GEAR }
    private class Part(val path: Path, val kind: Kind = Kind.BODY)

    private val cache = HashMap<Shape, List<Part>>()

    private fun partsOf(shape: Shape): List<Part> = cache.getOrPut(shape) {
        when (shape) {
            Shape.NARROW -> airliner(half = 0.052f, fin = 0.14f, engines = listOf(0.02f), engH = 0.055f)
            Shape.WIDE -> airliner(half = 0.068f, fin = 0.17f, engines = listOf(0.0f), engH = 0.07f)
            Shape.QUAD -> airliner(half = 0.07f, fin = 0.17f, engines = listOf(-0.03f, 0.12f), engH = 0.065f)
            Shape.JUMBO -> airliner(half = 0.068f, fin = 0.18f, engines = listOf(-0.03f, 0.12f), engH = 0.065f) +
                Part(Path().apply { addOval(Rect(0.06f, -0.068f * 1.75f, 0.42f, -0.068f * 0.35f)) })
            Shape.SUPER -> airliner(half = 0.075f, fin = 0.17f, engines = listOf(-0.03f, 0.12f), engH = 0.065f) +
                Part(Path().apply { addOval(Rect(-0.34f, -0.075f * 1.6f, 0.40f, -0.075f * 0.2f)) }) +
                Part(poly(0.30f, -0.075f * 1.25f, 0.38f, -0.075f * 0.95f, 0.30f, -0.075f * 0.95f), Kind.DETAIL)
            Shape.REGIONAL -> tailEngineJet(half = 0.042f, fin = 0.17f)
            Shape.MDTYPE -> tailEngineJet(half = 0.052f, fin = 0.19f)
            Shape.BIZJET -> tailEngineJet(half = 0.05f, fin = 0.15f)
            Shape.TURBOPROP -> propPlane(half = 0.05f, fin = 0.15f, wide = true)
            Shape.LIGHT -> propPlane(half = 0.065f, fin = 0.15f, wide = false)
            Shape.TRANSPORT -> transportPlane()
            Shape.FIGHTER -> fighter()
            Shape.GLIDER -> glider()
            Shape.HELI -> helicopter()
        }
    }

    private operator fun List<Part>.plus(p: Part): List<Part> = ArrayList(this).also { it.add(p) }

    /**
     * Disegna la sagoma centrata in [center], lunga [widthPx], rivolta a destra o a sinistra.
     * [detail] = colore di finestrini e dettagli (di solito il colore dello sfondo, un po' trasparente);
     * [gearDown] = carrello fuori (decollo, atterraggio o a terra).
     */
    fun DrawScope.drawSilhouette(
        shape: Shape,
        center: Offset,
        widthPx: Float,
        facingRight: Boolean,
        color: Color,
        detail: Color = Color.Transparent,
        gearDown: Boolean = false,
    ) {
        val parts = partsOf(shape)
        val showDetail = widthPx > 50f * density
        withTransform({
            translate(center.x, center.y)
            scale(if (facingRight) widthPx else -widthPx, widthPx, Offset.Zero)
        }) {
            for (p in parts) if (p.kind == Kind.FAR) drawPath(p.path, color.copy(alpha = color.alpha * 0.55f))
            for (p in parts) if (p.kind == Kind.BODY) drawPath(p.path, color)
            if (gearDown) for (p in parts) if (p.kind == Kind.GEAR) drawPath(p.path, color)
            if (showDetail) for (p in parts) if (p.kind == Kind.DETAIL) drawPath(p.path, detail)
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

    /** Finestrini (file di puntini), parabrezza e presa d'aria del motore. */
    private fun windows(half: Float, from: Float, to: Float, count: Int): List<Part> {
        val out = ArrayList<Part>()
        val r = half * 0.075f
        for (i in 0 until count) {
            val x = from + (to - from) * i / (count - 1)
            out.add(Part(oval(x - r, -half * 0.42f - r, x + r, -half * 0.42f + r), Kind.DETAIL))
        }
        out.add(Part(poly(0.40f, -half * 0.62f, 0.47f, -half * 0.30f, 0.40f, -half * 0.30f), Kind.DETAIL))
        return out
    }

    private fun gear(half: Float, mainX: Float, noseX: Float): List<Part> {
        val out = ArrayList<Part>()
        val len = 0.05f
        for (x in listOf(mainX, mainX - 0.045f)) {
            out.add(Part(poly(x - 0.004f, half, x + 0.004f, half, x + 0.004f, half + len, x - 0.004f, half + len), Kind.GEAR))
            out.add(Part(oval(x - 0.014f, half + len - 0.012f, x + 0.014f, half + len + 0.016f), Kind.GEAR))
        }
        out.add(Part(poly(noseX - 0.004f, half, noseX + 0.004f, half, noseX + 0.004f, half + len * 0.85f, noseX - 0.004f, half + len * 0.85f), Kind.GEAR))
        out.add(Part(oval(noseX - 0.012f, half + len * 0.85f - 0.01f, noseX + 0.012f, half + len * 0.85f + 0.014f), Kind.GEAR))
        return out
    }

    /** Aereo di linea: ali basse, ala lontana, winglet, motori sotto l'ala. */
    private fun airliner(half: Float, fin: Float, engines: List<Float>, engH: Float): List<Part> {
        val parts = ArrayList<Part>()
        // ala e piano di coda dal lato lontano (più chiari)
        parts.add(Part(poly(0.10f, -half + 0.01f, -0.10f, -half - 0.075f, -0.17f, -half - 0.075f, -0.03f, -half + 0.01f), Kind.FAR))
        parts.add(Part(poly(-0.40f, -half + 0.01f, -0.52f, -half - 0.035f, -0.55f, -half - 0.035f, -0.47f, -half + 0.01f), Kind.FAR))
        parts.add(Part(fuselage(half)))
        parts.add(Part(poly(-0.24f, -half + 0.005f, -0.43f, -half - fin, -0.50f, -half - fin, -0.47f, -half + 0.005f)))
        parts.add(Part(poly(0.10f, half - 0.01f, -0.14f, half + 0.075f, -0.21f, half + 0.075f, -0.05f, half - 0.01f)))
        // winglet
        parts.add(Part(poly(-0.205f, half + 0.075f, -0.235f, half + 0.012f, -0.222f, half + 0.012f, -0.185f, half + 0.075f)))
        for (x in engines) {
            parts.add(Part(oval(x, half - 0.005f, x + 0.15f, half + engH)))
            parts.add(Part(oval(x + 0.135f, half + 0.004f, x + 0.152f, half + engH - 0.004f), Kind.DETAIL))
        }
        parts.addAll(windows(half, -0.30f, 0.32f, 22))
        parts.addAll(gear(half, 0.0f, 0.30f))
        return parts
    }

    /** Jet regionale o executive: coda a T e motori dietro. */
    private fun tailEngineJet(half: Float, fin: Float): List<Part> {
        val parts = ArrayList<Part>()
        parts.add(Part(poly(0.08f, -half + 0.01f, -0.08f, -half - 0.06f, -0.14f, -half - 0.06f, -0.02f, -half + 0.01f), Kind.FAR))
        parts.add(Part(fuselage(half)))
        parts.add(Part(poly(-0.28f, -half + 0.005f, -0.44f, -half - fin, -0.50f, -half - fin, -0.48f, -half + 0.005f)))
        parts.add(Part(poly(-0.54f, -half - fin - 0.02f, -0.40f, -half - fin - 0.02f, -0.39f, -half - fin + 0.005f, -0.53f, -half - fin + 0.005f)))
        parts.add(Part(poly(0.08f, half - 0.01f, -0.10f, half + 0.06f, -0.16f, half + 0.06f, -0.03f, half - 0.01f)))
        parts.add(Part(poly(-0.14f, half + 0.06f, -0.158f, half + 0.02f, -0.145f, half + 0.02f, -0.10f, half + 0.06f)))
        parts.add(Part(oval(-0.46f, -half - 0.01f, -0.26f, half * 0.2f)))
        parts.add(Part(oval(-0.285f, -half + 0.004f, -0.262f, half * 0.2f - 0.004f), Kind.DETAIL))
        parts.addAll(windows(half, -0.20f, 0.30f, 14))
        parts.addAll(gear(half, 0.02f, 0.30f))
        return parts
    }

    /** Aereo a elica: ala alta, elica davanti. */
    private fun propPlane(half: Float, fin: Float, wide: Boolean): List<Part> {
        val parts = ArrayList<Part>()
        parts.add(Part(fuselage(half)))
        parts.add(Part(poly(-0.26f, -half + 0.005f, -0.44f, -half - fin, -0.50f, -half - fin, -0.48f, -half + 0.005f)))
        parts.add(Part(poly(0.14f, -half - 0.025f, -0.12f, -half - 0.025f, -0.12f, -half + 0.01f, 0.14f, -half + 0.01f)))
        // piano di coda
        parts.add(Part(poly(-0.40f, -half + 0.01f, -0.52f, -half - 0.02f, -0.52f, -half + 0.025f, -0.38f, -half + 0.03f)))
        if (wide) {
            parts.add(Part(oval(0.00f, -half - 0.065f, 0.22f, -half - 0.005f)))
            parts.add(Part(poly(0.225f, -half - 0.125f, 0.240f, -half - 0.125f, 0.240f, -half + 0.065f, 0.225f, -half + 0.065f)))
            parts.addAll(windows(half, -0.25f, 0.0f, 9))
        } else {
            parts.add(Part(poly(0.505f, -0.13f, 0.535f, -0.13f, 0.535f, 0.13f, 0.505f, 0.13f)))
            // carrello fisso
            parts.add(Part(oval(-0.01f, half + 0.02f, 0.05f, half + 0.065f), Kind.GEAR))
            parts.add(Part(oval(0.34f, half + 0.02f, 0.40f, half + 0.065f), Kind.GEAR))
        }
        if (wide) parts.addAll(gear(half, 0.02f, 0.32f))
        return parts
    }

    /** Trasporto militare/cargo: ala alta, quattro eliche, coda a T. */
    private fun transportPlane(): List<Part> {
        val half = 0.075f
        val parts = ArrayList<Part>()
        parts.add(Part(fuselage(half)))
        parts.add(Part(poly(-0.26f, -half + 0.005f, -0.44f, -half - 0.20f, -0.50f, -half - 0.20f, -0.48f, -half + 0.005f)))
        parts.add(Part(poly(-0.54f, -half - 0.22f, -0.38f, -half - 0.22f, -0.38f, -half - 0.195f, -0.54f, -half - 0.195f)))
        parts.add(Part(poly(0.18f, -half - 0.03f, -0.14f, -half - 0.03f, -0.14f, -half + 0.015f, 0.18f, -half + 0.015f)))
        for (x in listOf(-0.02f, 0.13f)) {
            parts.add(Part(oval(x, -half - 0.075f, x + 0.12f, -half - 0.02f)))
            parts.add(Part(poly(x + 0.12f, -half - 0.115f, x + 0.132f, -half - 0.115f, x + 0.132f, -half + 0.02f, x + 0.12f, -half + 0.02f)))
        }
        parts.add(Part(poly(0.40f, -half * 0.55f, 0.48f, -half * 0.2f, 0.40f, -half * 0.2f), Kind.DETAIL))
        parts.addAll(gear(half, 0.0f, 0.34f))
        return parts
    }

    /** Caccia: fusoliera sottile, ala a delta, doppia deriva, tettuccio. */
    private fun fighter(): List<Part> {
        val parts = ArrayList<Part>()
        parts.add(Part(poly(0.50f, 0.012f, 0.30f, -0.032f, 0.05f, -0.045f, -0.40f, -0.04f, -0.50f, -0.025f, -0.50f, 0.03f, -0.10f, 0.048f, 0.30f, 0.030f)))
        parts.add(Part(poly(0.16f, 0.015f, -0.30f, 0.115f, -0.42f, 0.115f, -0.28f, 0.02f)))
        parts.add(Part(poly(-0.28f, -0.04f, -0.44f, -0.17f, -0.50f, -0.17f, -0.50f, -0.035f)))
        parts.add(Part(poly(-0.40f, 0.02f, -0.54f, 0.07f, -0.54f, 0.085f, -0.44f, 0.04f)))
        parts.add(Part(oval(0.12f, -0.075f, 0.34f, -0.02f)))
        parts.add(Part(oval(0.18f, -0.062f, 0.31f, -0.028f), Kind.DETAIL))
        return parts
    }

    /** Aliante: fusoliera sottile con tettuccio e ali lunghissime. */
    private fun glider(): List<Part> {
        val parts = ArrayList<Part>()
        parts.add(Part(fuselage(0.032f)))
        parts.add(Part(poly(-0.30f, -0.03f, -0.46f, -0.13f, -0.50f, -0.13f, -0.49f, -0.03f)))
        parts.add(Part(poly(-0.44f, -0.12f, -0.56f, -0.12f, -0.56f, -0.105f, -0.44f, -0.105f)))
        parts.add(Part(poly(0.12f, -0.045f, 0.10f, -0.062f, -0.20f, -0.062f, -0.22f, -0.045f)))
        parts.add(Part(oval(0.10f, -0.055f, 0.30f, -0.005f)))
        parts.add(Part(oval(0.14f, -0.046f, 0.27f, -0.012f), Kind.DETAIL))
        return parts
    }

    private fun helicopter(): List<Part> {
        val parts = ArrayList<Part>()
        parts.add(Part(oval(0.02f, -0.12f, 0.50f, 0.10f)))                       // cabina
        parts.add(Part(oval(0.28f, -0.095f, 0.47f, 0.02f), Kind.DETAIL))        // vetro
        parts.add(Part(poly(0.12f, -0.07f, -0.45f, -0.045f, -0.45f, -0.015f, 0.12f, 0.0f))) // trave di coda
        parts.add(Part(poly(-0.38f, -0.04f, -0.46f, -0.17f, -0.50f, -0.17f, -0.47f, -0.02f))) // deriva
        parts.add(Part(poly(-0.30f, -0.17f, 0.50f, -0.17f, 0.50f, -0.155f, -0.30f, -0.155f))) // rotore principale
        parts.add(Part(poly(0.19f, -0.165f, 0.22f, -0.165f, 0.22f, -0.115f, 0.19f, -0.115f))) // albero rotore
        parts.add(Part(poly(0.06f, 0.115f, 0.44f, 0.115f, 0.44f, 0.13f, 0.06f, 0.13f)))      // pattini
        parts.add(Part(poly(0.12f, 0.09f, 0.135f, 0.09f, 0.135f, 0.12f, 0.12f, 0.12f)))
        parts.add(Part(poly(0.37f, 0.09f, 0.385f, 0.09f, 0.385f, 0.12f, 0.37f, 0.12f)))
        parts.add(Part(poly(-0.49f, -0.215f, -0.47f, -0.215f, -0.47f, -0.12f, -0.49f, -0.12f))) // rotore di coda
        return parts
    }
}
