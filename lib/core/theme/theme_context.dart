import 'package:material_ui/material_ui.dart';

import 'sproutly_typography.dart';

/// Short accessors so screens read `context.text.titleLarge` instead of
/// `Theme.of(context).textTheme.titleLarge` everywhere.
extension SproutlyThemeX on BuildContext {
  ThemeData get theme => Theme.of(this);

  ColorScheme get colors => Theme.of(this).colorScheme;

  TextTheme get text => Theme.of(this).textTheme;

  bool get isDark => Theme.of(this).brightness == Brightness.dark;

  bool get isCompact => MediaQuery.sizeOf(this).width < 600;

  bool get isTablet => MediaQuery.sizeOf(this).width >= 600;

  double get screenWidth => MediaQuery.sizeOf(this).width;

  /// Brand tokens. Falls back to the light set if the extension is missing
  /// (which can only happen in a bare widget test harness).
  SproutlyTokens get tokens =>
      Theme.of(this).extension<SproutlyTokens>() ?? SproutlyTokens.light;

  EdgeInsets get safeInsets => MediaQuery.paddingOf(this);
}
