package app.tab.store.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentThemeTest {

    private val seeds = AccentOption.entries.map { it.name to Color(if (it.hex == 0L) 0xFF4F5BD5 else it.hex or 0xFF000000L) } +
        // Awkward wallpaper colours: very light, very dark, and saturated yellow.
        listOf(
            "pale" to Color(0xFFFFF3B0),
            "near-black" to Color(0xFF050505),
            "yellow" to Color(0xFFFFEB00),
            "white" to Color.White,
        )

    private fun check(name: String, ratio: Float, min: Float, what: String) =
        assertTrue("$name $what is $ratio, needs $min", ratio >= min - 0.01f)

    @Test
    fun `every accent keeps text readable in light and dark`() {
        for ((name, seed) in seeds) {
            for (dark in listOf(false, true)) {
                val s = buildScheme(seed, dark)
                val tag = "$name/${if (dark) "dark" else "light"}"
                check(tag, Contrast.ratio(s.onBackground, s.background), 7f, "ink on canvas")
                check(tag, Contrast.ratio(s.onSurface, s.surface), 7f, "ink on surface")
                check(tag, Contrast.ratio(s.onSurfaceVariant, s.surfaceVariant), 4.5f, "muted on subtle")
                check(tag, Contrast.ratio(s.onSurfaceVariant, s.background), 4.5f, "muted on canvas")
                check(tag, Contrast.ratio(s.onPrimary, s.primary), 4.5f, "button label")
                check(tag, Contrast.ratio(s.primary, s.surface), 4.5f, "accent text on surface")
                check(tag, Contrast.ratio(s.primary, s.background), 4.5f, "accent text on canvas")
                check(tag, Contrast.ratio(s.onPrimaryContainer, s.primaryContainer), 4.5f, "chip label")
                check(tag, Contrast.ratio(s.inverseOnSurface, s.inverseSurface), 4.5f, "text on feature card")
                check(tag, Contrast.ratio(s.inversePrimary, s.inverseSurface), 4.5f, "accent on feature card")
                check(tag, Contrast.ratio(s.onError, s.error), 4.5f, "error label")
            }
        }
    }

    @Test
    fun `ensure leaves a passing colour alone and fixes a failing one`() {
        val passing = Color(0xFF111111)
        assertEquals(passing, Contrast.ensure(passing, Color.White))
        val fixed = Contrast.ensure(Color(0xFFCCCCCC), Color.White)
        assertTrue(Contrast.ratio(fixed, Color.White) >= 4.5f)
    }

    @Test
    fun `on picks the label that reads best`() {
        assertEquals(Color.White, Contrast.on(Color(0xFF101010)))
        assertEquals(Color.Black, Contrast.on(Color(0xFFFAFAFA)))
    }

    @Test
    fun `an unknown saved accent falls back to Monet`() {
        assertEquals(AccentOption.MONET, AccentOption.from("nonsense"))
        assertEquals(AccentOption.TEAL, AccentOption.from("teal"))
    }
}
