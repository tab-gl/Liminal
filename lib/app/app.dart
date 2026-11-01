import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../core/providers/app_providers.dart';
import '../core/providers/settings_provider.dart';
import '../core/theme/sproutly_theme.dart';
import 'router.dart';

/// Root widget.
///
/// Sproutly uses the decoupled `material_ui` MaterialApp and layers its own
/// expressive design system on top, so every screen keeps full control of
/// shape, motion and haptics. The M3E package supplies three specific
/// components (see core/widgets/m3e/expressive.dart); everything else is
/// Sproutly's own. Rationale and the version check are in
/// docs/DESIGN_SYSTEM.md.
class SproutlyApp extends ConsumerWidget {
  const SproutlyApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ThemeMode themeMode = ref.watch(themeModeProvider);

    return MaterialApp.router(
      title: 'Sproutly',
      debugShowCheckedModeBanner: false,
      theme: SproutlyTheme.light(),
      darkTheme: SproutlyTheme.dark(),
      themeMode: themeMode,
      routerConfig: ref.watch(routerProvider),
      builder: (BuildContext context, Widget? child) {
        // Clamp text scaling so a very large accessibility setting does not
        // shatter the expressive layouts; 1.6x still respects the intent.
        final MediaQueryData media = MediaQuery.of(context);
        return MediaQuery(
          data: media.copyWith(
            textScaler: media.textScaler.clamp(minScaleFactor: 0.9, maxScaleFactor: 1.6),
          ),
          child: child ?? const SizedBox.shrink(),
        );
      },
    );
  }
}
