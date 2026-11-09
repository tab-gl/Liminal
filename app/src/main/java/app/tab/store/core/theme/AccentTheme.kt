package app.tab.store.core.theme

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.tab.store.core.ui.SkColorScheme
import app.tab.store.core.ui.lightScheme

/** The wallpaper accent (Android 12+), or null where the device has none. */
fun monetSeed(context: Context, dark: Boolean): Color? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return runCatching {
        Color(context.getColor(if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600))
    }.getOrNull()
}

private val GRAPHITE_FALLBACK = Color(0xFF3F4451)

/**
 * A full colour scheme from one accent, for light or dark.
 *
 * Neutrals take a whisper of the accent so the app feels of a piece with it,
 * but every pairing is then checked: text on the canvas and on every surface is
 * at least 4.5:1, the accent used as text is at least 4.5:1 on its surface, and
 * the label on an accent button is whichever of near-black or white reads best.
 */
fun buildScheme(seed: Color?, dark: Boolean): SkColorScheme {
    val accentSeed = seed ?: GRAPHITE_FALLBACK

    val canvas = if (dark) lerp(Color(0xFF101010), accentSeed, 0.035f) else lerp(Color(0xFFFAFAF8), accentSeed, 0.025f)
    val surface = if (dark) lerp(Color(0xFF181818), accentSeed, 0.05f) else lerp(Color(0xFFFFFFFF), accentSeed, 0.01f)
    val subtle = if (dark) lerp(Color(0xFF222222), accentSeed, 0.09f) else lerp(Color(0xFFF0F0EC), accentSeed, 0.07f)

    // Ink must clear 7:1 on the canvas and still 4.5:1 on the slightly darker subtle surface.
    val ink = Contrast.ensure(
        lerp(if (dark) Color(0xFFF5F5F2) else Color(0xFF111111), accentSeed, 0.05f),
        subtle, 7f,
    )
    val inkSecondary = Contrast.ensure(lerp(ink, subtle, 0.28f), subtle, 7f)
    val inkMuted = Contrast.ensure(lerp(ink, subtle, 0.45f), subtle, Contrast.TEXT)
    val border = lerp(canvas, ink, if (dark) 0.16f else 0.12f)

    val primary = Contrast.ensure(accentSeed, subtle, Contrast.TEXT)
    val onPrimary = Contrast.on(primary)
    val primaryContainer = lerp(canvas, primary, if (dark) 0.26f else 0.14f)
    val onPrimaryContainer = Contrast.ensure(primary, primaryContainer, Contrast.TEXT)

    val error = Contrast.ensure(if (dark) Color(0xFFFF6B6B) else Color(0xFFC62828), subtle, Contrast.TEXT)
    val errorContainer = lerp(canvas, error, if (dark) 0.22f else 0.12f)

    return lightScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = ink,
        onSecondary = Contrast.on(ink),
        secondaryContainer = subtle,
        onSecondaryContainer = ink,
        tertiary = inkSecondary,
        onTertiary = Contrast.on(inkSecondary),
        tertiaryContainer = subtle,
        onTertiaryContainer = ink,
        background = canvas,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = subtle,
        onSurfaceVariant = inkMuted,
        surfaceContainer = subtle,
        surfaceContainerHigh = subtle,
        outline = border,
        outlineVariant = border,
        error = error,
        onError = Contrast.on(error),
        errorContainer = errorContainer,
        onErrorContainer = Contrast.ensure(error, errorContainer, Contrast.TEXT),
        inverseSurface = ink,
        inverseOnSurface = Contrast.on(ink),
        // The accent as it reads on the inverse (ink) surface, for feature cards.
        inversePrimary = Contrast.ensure(accentSeed, ink, Contrast.TEXT),
    )
}
