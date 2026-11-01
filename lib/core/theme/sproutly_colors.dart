import 'package:material_ui/material_ui.dart';

/// The Sproutly palette.
///
/// Lime and Olive are the only brand hues; everything else is a neutral that
/// keeps them legible. No blues, no purples - see docs/DESIGN_SYSTEM.md.
class SproutlyColors {
  const SproutlyColors._();

  // Brand
  static const Color lime = Color(0xFFDBE6A1);
  static const Color olive = Color(0xFF3F4C06);
  static const Color oliveDeep = Color(0xFF2A3304);
  static const Color limeTint = Color(0xFFEDF2CF);

  // Neutrals
  static const Color offWhite = Color(0xFFFAF9F2);
  static const Color softNeutral = Color(0xFFF0F0E8);
  static const Color charcoal = Color(0xFF202117);
  static const Color mutedText = Color(0xFF77786B);
  static const Color border = Color(0xFFE5E6D9);

  // Dark neutrals
  static const Color ink = Color(0xFF16170F);
  static const Color inkSurface = Color(0xFF202117);
  static const Color inkHigh = Color(0xFF2A2B20);
  static const Color inkMutedText = Color(0xFFA9AB9A);

  // Semantic. Chosen for >= 4.5:1 on their intended surface.
  static const Color success = Color(0xFF2E6B34);
  static const Color successContainer = Color(0xFFD9EBCF);
  static const Color warning = Color(0xFF8A5A00);
  static const Color warningContainer = Color(0xFFF6E3BF);
  static const Color error = Color(0xFFB3261E);
  static const Color errorContainer = Color(0xFFF9DEDC);
  static const Color errorDark = Color(0xFFF2B8B5);

  static const ColorScheme lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: olive,
    onPrimary: Color(0xFFF7FBE4),
    primaryContainer: lime,
    onPrimaryContainer: oliveDeep,
    primaryFixed: lime,
    primaryFixedDim: Color(0xFFC9D68F),
    onPrimaryFixed: oliveDeep,
    onPrimaryFixedVariant: olive,
    secondary: Color(0xFF5A6430),
    onSecondary: Color(0xFFFBFCEF),
    secondaryContainer: limeTint,
    onSecondaryContainer: oliveDeep,
    secondaryFixed: limeTint,
    secondaryFixedDim: Color(0xFFD6DEA8),
    onSecondaryFixed: oliveDeep,
    onSecondaryFixedVariant: Color(0xFF454D22),
    tertiary: Color(0xFF6B6A3E),
    onTertiary: Color(0xFFFCFBEF),
    tertiaryContainer: Color(0xFFEFEED2),
    onTertiaryContainer: Color(0xFF25250C),
    error: error,
    onError: Color(0xFFFFFFFF),
    errorContainer: errorContainer,
    onErrorContainer: Color(0xFF410E0B),
    surface: offWhite,
    onSurface: charcoal,
    surfaceDim: Color(0xFFE7E6DC),
    surfaceBright: Color(0xFFFFFFFF),
    surfaceContainerLowest: Color(0xFFFFFFFF),
    surfaceContainerLow: Color(0xFFF5F4EB),
    surfaceContainer: softNeutral,
    surfaceContainerHigh: Color(0xFFE9E9DE),
    surfaceContainerHighest: Color(0xFFE1E2D3),
    onSurfaceVariant: mutedText,
    outline: Color(0xFFC9CBB8),
    outlineVariant: border,
    shadow: charcoal,
    scrim: Color(0xFF000000),
    inverseSurface: charcoal,
    onInverseSurface: softNeutral,
    inversePrimary: lime,
  );

  static const ColorScheme darkScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: lime,
    onPrimary: oliveDeep,
    primaryContainer: Color(0xFF39430A),
    onPrimaryContainer: Color(0xFFE9F2C0),
    primaryFixed: lime,
    primaryFixedDim: Color(0xFFC9D68F),
    onPrimaryFixed: oliveDeep,
    onPrimaryFixedVariant: olive,
    secondary: Color(0xFFC3CB9A),
    onSecondary: oliveDeep,
    secondaryContainer: Color(0xFF3A4220),
    onSecondaryContainer: Color(0xFFE7EDC9),
    secondaryFixed: limeTint,
    secondaryFixedDim: Color(0xFFD6DEA8),
    onSecondaryFixed: oliveDeep,
    onSecondaryFixedVariant: Color(0xFF454D22),
    tertiary: Color(0xFFD4D2A6),
    onTertiary: Color(0xFF2B2A12),
    tertiaryContainer: Color(0xFF43421F),
    onTertiaryContainer: Color(0xFFEFEED2),
    error: errorDark,
    onError: Color(0xFF601410),
    errorContainer: Color(0xFF8C1D18),
    onErrorContainer: Color(0xFFF9DEDC),
    surface: ink,
    onSurface: Color(0xFFF2F1E6),
    surfaceDim: Color(0xFF101108),
    surfaceBright: Color(0xFF34352A),
    surfaceContainerLowest: Color(0xFF101108),
    surfaceContainerLow: Color(0xFF1B1C13),
    surfaceContainer: inkSurface,
    surfaceContainerHigh: inkHigh,
    surfaceContainerHighest: Color(0xFF34352A),
    onSurfaceVariant: inkMutedText,
    outline: Color(0xFF5A5C4C),
    outlineVariant: Color(0xFF35362A),
    shadow: Color(0xFF000000),
    scrim: Color(0xFF000000),
    inverseSurface: Color(0xFFF2F1E6),
    onInverseSurface: ink,
    inversePrimary: olive,
  );
}
