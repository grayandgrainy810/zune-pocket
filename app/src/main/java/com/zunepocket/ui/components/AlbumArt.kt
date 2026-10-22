package com.zunepocket.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.zunepocket.data.ArtSource
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Album artwork. Shows art generated from [seed] immediately, then fades to the real artwork from
 * [source] once [ArtLoader] finds it. Sample music (null [source]) keeps the generated art.
 * [large] loads a full-screen-sized image for Now Playing.
 */
@Composable
fun AlbumArt(seed: Long, modifier: Modifier = Modifier, source: ArtSource? = null, large: Boolean = false) {
    val loader = LocalArtLoader.current
    val image by produceState(source?.let { loader?.cached(it, large) }, source, large, loader) {
        if (value == null && source != null && loader != null) value = loader.load(source, large)
    }
    Box(modifier) {
        Crossfade(targetState = image, animationSpec = tween(250), label = "albumArt") { bitmap ->
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            } else {
                GeneratedArt(seed, Modifier.matchParentSize())
            }
        }
    }
}

/** Placeholder art generated from a seed, so the app ships no copyrighted images. */
@Composable
private fun GeneratedArt(seed: Long, modifier: Modifier) {
    val spec = remember(seed) { ArtSpec(seed) }
    Canvas(modifier) { drawArt(spec) }
}

/** Slow pan-and-zoom drift, as on the Zune HD's now playing screen. */
@Composable
fun Modifier.kenBurns(): Modifier {
    val transition = rememberInfiniteTransition(label = "kenBurns")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "kenBurnsProgress",
    )
    return graphicsLayer {
        val scale = 1.12f + 0.10f * t
        scaleX = scale
        scaleY = scale
        translationX = (t - 0.5f) * size.width * 0.08f
        translationY = (0.5f - t) * size.height * 0.05f
    }
}

private class ArtSpec(seed: Long) {
    private val rnd = Random(seed)
    val style = rnd.nextInt(5)
    private val hue = rnd.nextFloat() * 360f
    private val hueShift = 30f + rnd.nextFloat() * 150f
    val background1 = Color.hsv(hue, 0.55f + rnd.nextFloat() * 0.35f, 0.35f + rnd.nextFloat() * 0.35f)
    val background2 = Color.hsv((hue + hueShift) % 360f, 0.6f + rnd.nextFloat() * 0.4f, 0.12f + rnd.nextFloat() * 0.25f)
    val accent = Color.hsv((hue + 180f + rnd.nextFloat() * 60f) % 360f, 0.5f + rnd.nextFloat() * 0.5f, 0.85f + rnd.nextFloat() * 0.15f)
    val values = FloatArray(32) { rnd.nextFloat() }
}

private fun DrawScope.drawArt(spec: ArtSpec) {
    val w = size.width
    val h = size.height
    val m = min(w, h)
    val v = spec.values
    drawRect(Brush.linearGradient(listOf(spec.background1, spec.background2), Offset.Zero, Offset(w, h)))

    when (spec.style) {
        // Sun over a striped horizon.
        0 -> {
            val horizon = h * (0.55f + v[0] * 0.15f)
            val center = Offset(w * (0.3f + v[1] * 0.4f), horizon)
            drawCircle(Brush.verticalGradient(listOf(spec.accent, spec.background1), horizon - m * 0.4f, horizon), m * (0.25f + v[2] * 0.15f), center)
            drawRect(spec.background2, Offset(0f, horizon), Size(w, h - horizon))
            var y = horizon + m * 0.03f
            var gap = m * 0.02f
            while (y < h) {
                drawRect(spec.accent.copy(alpha = 0.55f), Offset(0f, y), Size(w, gap * 0.6f))
                y += gap * 2
                gap *= 1.25f
            }
        }
        // Concentric rings.
        1 -> {
            val center = Offset(w * v[0], h * v[1])
            val rings = 6 + (v[2] * 6).toInt()
            for (i in rings downTo 1) {
                val radius = max(w, h) * 1.2f * i / rings
                drawCircle(
                    color = if (i % 2 == 0) spec.accent.copy(alpha = 0.18f + v[3] * 0.3f) else spec.background2.copy(alpha = 0.5f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = m * (0.02f + v[4] * 0.05f)),
                )
            }
        }
        // Diagonal stripes.
        2 -> rotate(-30f + v[0] * 60f) {
            val stripe = m * (0.04f + v[1] * 0.08f)
            var x = -w
            var i = 0
            while (x < w * 2) {
                drawRect(spec.accent.copy(alpha = 0.1f + v[i % 32] * 0.5f), Offset(x, -h), Size(stripe, h * 3))
                x += stripe * (1.5f + v[(i + 7) % 32] * 2f)
                i++
            }
        }
        // Block grid.
        3 -> {
            val cells = 4
            val cw = w / cells
            val ch = h / cells
            for (row in 0 until cells) for (col in 0 until cells) {
                val r = v[(row * cells + col) % 32]
                if (r > 0.45f) {
                    drawRect(spec.accent.copy(alpha = (r - 0.45f) * 1.6f), Offset(col * cw, row * ch), Size(cw, ch))
                }
            }
        }
        // Layered waves.
        else -> {
            val layers = 5
            for (layer in 0 until layers) {
                val baseY = h * (0.35f + layer * 0.13f)
                val amp = m * (0.03f + v[layer] * 0.08f)
                val freq = 1f + v[layer + 8] * 2.5f
                val phase = v[layer + 16] * 2f * PI.toFloat()
                val path = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) {
                        lineTo(x, baseY + amp * sin(phase + freq * 2f * PI.toFloat() * x / w))
                        x += w / 48f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(path, spec.accent.copy(alpha = 0.12f + layer * 0.08f))
            }
        }
    }
    // Subtle vignette so text laid over the art stays legible.
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), Offset(w / 2, h / 2), max(w, h) * 0.75f))
}
