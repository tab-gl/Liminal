import 'dart:ui' show ImageFilter;

import 'package:material_ui/material_ui.dart';

import '../services/haptics_service.dart';
import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';

/// One action in the [SproutlyActionBar].
class SproutlyAction {
  const SproutlyAction({
    required this.icon,
    required this.label,
    required this.onPressed,
    this.emphasized = false,
  });

  final IconData icon;
  final String label;
  final VoidCallback onPressed;

  /// Emphasised actions get the filled treatment (the primary one per bar).
  final bool emphasized;
}

/// A floating action bar for a pushed route.
///
/// Same visual language as the navigation island - detached, blurred, inset -
/// so a detail screen feels like part of the same app rather than a web page
/// with a toolbar bolted on. Built here rather than taken from the M3E package
/// so the surface, spacing and press feedback stay under our control.
class SproutlyActionBar extends StatelessWidget {
  const SproutlyActionBar({required this.actions, super.key});

  final List<SproutlyAction> actions;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final double bottomInset = MediaQuery.paddingOf(context).bottom;

    return Padding(
      padding: EdgeInsets.fromLTRB(18, 0, 18, bottomInset > 0 ? 10 : 14),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(SproutlyShapes.xxl),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
          child: Container(
            height: 64,
            padding: const EdgeInsets.symmetric(horizontal: 8),
            decoration: BoxDecoration(
              color: theme.colorScheme.surfaceContainerLowest
                  .withValues(alpha: context.isDark ? 0.84 : 0.9),
              borderRadius: BorderRadius.circular(SproutlyShapes.xxl),
              border: Border.all(color: theme.colorScheme.outlineVariant),
              boxShadow: <BoxShadow>[
                BoxShadow(
                  color: context.tokens.cardShadow,
                  blurRadius: 26,
                  spreadRadius: -6,
                  offset: const Offset(0, 12),
                ),
              ],
            ),
            child: Row(
              children: <Widget>[
                for (final SproutlyAction action in actions)
                  Expanded(child: _ActionSlot(action: action)),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ActionSlot extends StatelessWidget {
  const _ActionSlot({required this.action});

  final SproutlyAction action;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final Color foreground = action.emphasized
        ? theme.colorScheme.onPrimary
        : theme.colorScheme.onSurface;
    final Color background = action.emphasized
        ? theme.colorScheme.primary
        : Colors.transparent;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 3, vertical: 8),
      child: Semantics(
        button: true,
        label: action.label,
        child: Material(
          color: background,
          borderRadius: SproutlyShapes.radiusPill,
          child: InkWell(
            borderRadius: SproutlyShapes.radiusPill,
            onTap: () {
              Haptics.tap();
              action.onPressed();
            },
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Icon(action.icon, size: 18, color: foreground),
                const SizedBox(width: 6),
                Flexible(
                  child: Text(
                    action.label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelMedium!.copyWith(
                      color: foreground,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
