package app.tab.store.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * WCAG contrast helpers. Every colour the accent system produces is pushed
 * through these so text stays readable whatever accent someone picks and
 * whichever of light or dark they are in.
 */
object Contrast {
    const val TEXT = 4.5f
    const val LARGE_OR_UI = 3f

    /** Contrast ratio between two colours, from 1 (identical) to 21 (black on white). */
    fun ratio(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05f) / (lo + 0.05f)
    }

    /**
     * Moves [fg] towards black or white, whichever direction increases contrast
     * against [bg], until it reaches [min]. Returns [fg] untouched when it already
     * passes, so a good accent keeps its character.
     */
    fun ensure(fg: Color, bg: Color, min: Float = TEXT): Color {
        if (ratio(fg, bg) >= min) return fg
        val towards = if (bg.luminance() > 0.5f) Color.Black else Color.White
        var out = fg
        var step = 0
        while (ratio(out, bg) < min && step < 24) {
            step++
            out = lerp(fg, towards, step / 24f)
        }
        return out
    }

    /**
     * Black or white, whichever reads better on [bg]. Pure black rather than ink:
     * with black and white the worse of the two is never below 4.58:1 whatever the
     * background, so a button label always passes. Near-black does not (4.35).
     */
    fun on(bg: Color): Color =
        if (ratio(Color.Black, bg) >= ratio(Color.White, bg)) Color.Black else Color.White
}
