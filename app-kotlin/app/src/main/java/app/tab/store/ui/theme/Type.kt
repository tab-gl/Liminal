package app.tab.store.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.tab.store.R

/**
 * DM Sans, shipped as one variable font (wght 100-1000, opsz 9-40, OFL).
 *
 * Compose picks weights from the variable axis on API 26+; below that the
 * platform synthesises them, which is why the family declares the same file for
 * each weight rather than four static assets.
 */
val DmSans = FontFamily(
    Font(R.font.dmsans, weight = FontWeight.Light),
    Font(R.font.dmsans, weight = FontWeight.Normal),
    Font(R.font.dmsans, weight = FontWeight.Medium),
    Font(R.font.dmsans, weight = FontWeight.SemiBold),
    Font(R.font.dmsans, weight = FontWeight.Bold),
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = DmSans,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

val SproutlyTypography = Typography(
    displayLarge = style(52, 56, FontWeight.Bold, -1.6),
    displayMedium = style(42, 48, FontWeight.Bold, -1.2),
    displaySmall = style(34, 40, FontWeight.SemiBold, -0.8),
    headlineLarge = style(30, 36, FontWeight.SemiBold, -0.6),
    headlineMedium = style(26, 32, FontWeight.SemiBold, -0.4),
    headlineSmall = style(22, 28, FontWeight.SemiBold, -0.2),
    titleLarge = style(20, 26, FontWeight.SemiBold),
    titleMedium = style(16, 22, FontWeight.SemiBold),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 21, FontWeight.Normal),
    bodySmall = style(12, 18, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.2),
    labelSmall = style(11, 14, FontWeight.SemiBold, 0.4),
)
