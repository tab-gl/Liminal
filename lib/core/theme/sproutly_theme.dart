import 'package:material_ui/material_ui.dart';

import 'sproutly_colors.dart';
import 'sproutly_component_themes.dart';
import 'sproutly_shapes.dart';
import 'sproutly_typography.dart';

/// Assembles the Sproutly ThemeData for light and dark.
///
/// Deliberately minimal: only the global surface, type scale, colour scheme and
/// brand tokens are set here. Component styling lives in the Sproutly widgets
/// (lib/core/widgets) so each one owns its expressive shape, motion and
/// haptics instead of inheriting generic Material defaults.
class SproutlyTheme {
  const SproutlyTheme._();

  static ThemeData light() => _build(SproutlyColors.lightScheme, SproutlyTokens.light);

  static ThemeData dark() => _build(SproutlyColors.darkScheme, SproutlyTokens.dark);

  static ThemeData _build(ColorScheme scheme, SproutlyTokens tokens) {
    final ThemeData base = ThemeData(
      useMaterial3: true,
      brightness: scheme.brightness,
      colorScheme: scheme,
      scaffoldBackgroundColor: scheme.surface,
      canvasColor: scheme.surface,
      textTheme: SproutlyTypography.build(scheme.onSurface, scheme.onSurfaceVariant),
      primaryColor: scheme.primary,
      dividerColor: scheme.outlineVariant,
      splashFactory: InkSparkle.splashFactory,
      visualDensity: VisualDensity.standard,
      extensions: <ThemeExtension<dynamic>>[tokens],
      // Sproutly widgets own their own shapes. These sub-themes exist for the
      // stock Material widgets the app still uses directly (snackbars, dialogs,
      // chips, menus, text buttons...) so none of them can fall back to a hard
      // default corner and break the rounded, organic silhouette.
    );
    return SproutlyComponentThemes.apply(base, scheme);
  }

  /// Shared page padding. Tablet layouts widen this in AdaptiveScaffold.
  static const EdgeInsets pagePadding = EdgeInsets.symmetric(horizontal: 20);
  static const double maxContentWidth = 720;
  static const BorderRadius cardRadius = BorderRadius.all(Radius.circular(SproutlyShapes.lg));
}
