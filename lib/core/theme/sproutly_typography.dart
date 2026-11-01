import 'dart:ui' show FontVariation;

import 'package:material_ui/material_ui.dart';

import 'sproutly_colors.dart';

/// Type scale.
///
/// The family is **DM Sans**, shipped as one variable font file (optical size
/// 9-40, weight 100-1000). Every style therefore selects its instance through
/// `FontVariation` rather than shipping four static files: weight comes from the
/// `wght` axis, and `opsz` is set per size so large headlines use the display
/// cut and body copy uses the text cut. That is the whole reason a single 235 KB
/// asset can carry the scale honestly, without faux-bold.
class SproutlyTypography {
  const SproutlyTypography._();

  static const String fontFamily = 'DMSans';

  /// Fallbacks used only if the asset fails to load (widget tests, for example).
  static const List<String> _fallback = <String>[
    'DM Sans',
    'Roboto',
    'SF Pro Text',
  ];

  static TextStyle _base(
    double size,
    double height,
    FontWeight weight,
    Color color, {
    double tracking = 0,
  }) {
    // Optical size tracks the rendered size: small text wants the text cut,
    // large text wants the display cut.
    final double opsz = size.clamp(9, 40).toDouble();
    return TextStyle(
      fontSize: size,
      height: height / size,
      fontWeight: weight,
      letterSpacing: tracking,
      color: color,
      fontFamily: fontFamily,
      fontFamilyFallback: _fallback,
      fontVariations: <FontVariation>[
        FontVariation('wght', weight.value.toDouble()),
        FontVariation('opsz', opsz),
      ],
    );
  }

  static TextTheme build(Color onSurface, Color onSurfaceVariant) {
    return TextTheme(
      displayLarge: _base(52, 56, FontWeight.w700, onSurface, tracking: -1.6),
      displayMedium: _base(42, 48, FontWeight.w700, onSurface, tracking: -1.2),
      displaySmall: _base(34, 40, FontWeight.w600, onSurface, tracking: -0.8),
      headlineLarge: _base(30, 36, FontWeight.w600, onSurface, tracking: -0.6),
      headlineMedium: _base(26, 32, FontWeight.w600, onSurface, tracking: -0.4),
      headlineSmall: _base(22, 28, FontWeight.w600, onSurface, tracking: -0.2),
      titleLarge: _base(20, 26, FontWeight.w600, onSurface),
      titleMedium: _base(16, 22, FontWeight.w600, onSurface),
      titleSmall: _base(14, 20, FontWeight.w600, onSurface),
      bodyLarge: _base(16, 24, FontWeight.w400, onSurface),
      bodyMedium: _base(14, 21, FontWeight.w400, onSurface),
      bodySmall: _base(12, 18, FontWeight.w400, onSurfaceVariant),
      labelLarge: _base(14, 20, FontWeight.w600, onSurface, tracking: 0.1),
      labelMedium: _base(12, 16, FontWeight.w600, onSurfaceVariant, tracking: 0.2),
      labelSmall: _base(11, 14, FontWeight.w600, onSurfaceVariant, tracking: 0.4),
    );
  }

  /// The number style used on dashboard tiles and campaign metrics.
  static TextStyle metric(Color color, {double size = 34}) =>
      _base(size, size * 1.1, FontWeight.w700, color, tracking: -1.0);
}

/// Convenience access to the styles that do not live on TextTheme.
extension SproutlyTypeX on TextTheme {
  TextStyle get overline => labelSmall!.copyWith(
        letterSpacing: 1.2,
        fontWeight: FontWeight.w700,
      );
}

/// Brand-tinted extras that the Material ColorScheme has no slot for.
///
/// Deliberately flat: `heroSurface` is a single colour, not a gradient. Large
/// areas of Sproutly use flat fills with a hairline border and shape variation to
/// carry hierarchy - no gradient ramps anywhere in the interface.
@immutable
class SproutlyTokens extends ThemeExtension<SproutlyTokens> {
  const SproutlyTokens({
    required this.lime,
    required this.olive,
    required this.success,
    required this.warning,
    required this.successContainer,
    required this.warningContainer,
    required this.cardShadow,
    required this.sponsoredBackground,
    required this.sponsoredForeground,
    required this.heroSurface,
  });

  final Color lime;
  final Color olive;
  final Color success;
  final Color warning;
  final Color successContainer;
  final Color warningContainer;
  final Color cardShadow;
  final Color sponsoredBackground;
  final Color sponsoredForeground;

  /// Flat fill for hero and call-to-action surfaces. Replaces the gradient that
  /// earlier revisions used.
  final Color heroSurface;

  static const SproutlyTokens light = SproutlyTokens(
    lime: SproutlyColors.lime,
    olive: SproutlyColors.olive,
    success: SproutlyColors.success,
    warning: SproutlyColors.warning,
    successContainer: SproutlyColors.successContainer,
    warningContainer: SproutlyColors.warningContainer,
    cardShadow: Color(0x14202117),
    sponsoredBackground: SproutlyColors.limeTint,
    sponsoredForeground: SproutlyColors.oliveDeep,
    heroSurface: Color(0xFFEDF2CF),
  );

  static const SproutlyTokens dark = SproutlyTokens(
    lime: SproutlyColors.lime,
    olive: SproutlyColors.olive,
    success: Color(0xFF9BD69B),
    warning: Color(0xFFE8C07A),
    successContainer: Color(0xFF1F3A22),
    warningContainer: Color(0xFF3D2E12),
    cardShadow: Color(0x66000000),
    sponsoredBackground: Color(0xFF39430A),
    sponsoredForeground: Color(0xFFE9F2C0),
    heroSurface: Color(0xFF252C0C),
  );

  @override
  SproutlyTokens copyWith({
    Color? lime,
    Color? olive,
    Color? success,
    Color? warning,
    Color? successContainer,
    Color? warningContainer,
    Color? cardShadow,
    Color? sponsoredBackground,
    Color? sponsoredForeground,
    Color? heroSurface,
  }) {
    return SproutlyTokens(
      lime: lime ?? this.lime,
      olive: olive ?? this.olive,
      success: success ?? this.success,
      warning: warning ?? this.warning,
      successContainer: successContainer ?? this.successContainer,
      warningContainer: warningContainer ?? this.warningContainer,
      cardShadow: cardShadow ?? this.cardShadow,
      sponsoredBackground: sponsoredBackground ?? this.sponsoredBackground,
      sponsoredForeground: sponsoredForeground ?? this.sponsoredForeground,
      heroSurface: heroSurface ?? this.heroSurface,
    );
  }

  @override
  SproutlyTokens lerp(ThemeExtension<SproutlyTokens>? other, double t) {
    if (other is! SproutlyTokens) return this;
    return SproutlyTokens(
      lime: Color.lerp(lime, other.lime, t) ?? lime,
      olive: Color.lerp(olive, other.olive, t) ?? olive,
      success: Color.lerp(success, other.success, t) ?? success,
      warning: Color.lerp(warning, other.warning, t) ?? warning,
      successContainer:
          Color.lerp(successContainer, other.successContainer, t) ?? successContainer,
      warningContainer:
          Color.lerp(warningContainer, other.warningContainer, t) ?? warningContainer,
      cardShadow: Color.lerp(cardShadow, other.cardShadow, t) ?? cardShadow,
      sponsoredBackground:
          Color.lerp(sponsoredBackground, other.sponsoredBackground, t) ?? sponsoredBackground,
      sponsoredForeground:
          Color.lerp(sponsoredForeground, other.sponsoredForeground, t) ?? sponsoredForeground,
      heroSurface: Color.lerp(heroSurface, other.heroSurface, t) ?? heroSurface,
    );
  }
}
