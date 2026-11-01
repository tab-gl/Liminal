import 'dart:ui' show ImageFilter;

import 'package:hugeicons/hugeicons.dart';
import 'package:material_ui/material_ui.dart';

import '../services/haptics_service.dart';
import '../theme/sproutly_motion.dart';
import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';

/// A navigation destination.
///
/// [icon] is a Hugeicons icon definition (`HugeIcons.strokeRoundedX`), not an
/// `IconData`: Hugeicons are SVG path sets rendered by [HugeIcon], which is why
/// the type is `List<List<dynamic>>` rather than Material's glyph lookup.
class SproutlyDestination {
  const SproutlyDestination({
    required this.label,
    required this.icon,
    this.emphasized = false,
  });

  final String label;
  final List<List<dynamic>> icon;

  /// Publish is rendered as the raised centre action rather than a peer tab.
  final bool emphasized;
}

/// The floating navigation island.
///
/// Structure: a detached surface inset from every screen edge, floating over the
/// content (the shell uses `extendBody`), with a blurred translucent fill so the
/// feed scrolling underneath stays visible as a colour wash. Five slots, a lime
/// capsule that springs behind the active slot, and Hugeicons at 1.7 stroke
/// weight - thickening to 2.2 when selected, so selection reads even for someone
/// who cannot distinguish the container colour.
class SproutlyNavBar extends StatelessWidget {
  const SproutlyNavBar({
    required this.currentIndex,
    required this.onSelected,
    super.key,
    this.badges = const <int, int>{},
  });

  final int currentIndex;
  final ValueChanged<int> onSelected;

  /// destination index -> unread count
  final Map<int, int> badges;

  static const double islandHeight = 74;
  static const double inset = 18;

  static const List<SproutlyDestination> destinations = <SproutlyDestination>[
    SproutlyDestination(
      label: 'Discover',
      icon: HugeIcons.strokeRoundedCompass01,
    ),
    SproutlyDestination(
      label: 'Categories',
      icon: HugeIcons.strokeRoundedDashboardSquare01,
    ),
    SproutlyDestination(
      label: 'Publish',
      icon: HugeIcons.strokeRoundedAdd01,
      emphasized: true,
    ),
    SproutlyDestination(
      label: 'Activity',
      icon: HugeIcons.strokeRoundedBell,
    ),
    SproutlyDestination(
      label: 'Profile',
      icon: HugeIcons.strokeRoundedUser,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final double bottomInset = MediaQuery.paddingOf(context).bottom;
    final Color surface = theme.colorScheme.surfaceContainerLowest;

    return Padding(
      padding: EdgeInsets.fromLTRB(inset, 0, inset, bottomInset > 0 ? 10 : 18),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(SproutlyShapes.xxl),
        child: BackdropFilter(
          // The blur is what makes it read as floating rather than as a bar
          // bolted to the bottom edge.
          filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
          child: Container(
            height: islandHeight,
            decoration: BoxDecoration(
              color: surface.withValues(alpha: context.isDark ? 0.82 : 0.88),
              borderRadius: BorderRadius.circular(SproutlyShapes.xxl),
              border: Border.all(
                color: theme.colorScheme.outlineVariant.withValues(alpha: 0.9),
              ),
              boxShadow: <BoxShadow>[
                BoxShadow(
                  color: context.tokens.cardShadow,
                  blurRadius: 32,
                  spreadRadius: -4,
                  offset: const Offset(0, 14),
                ),
                BoxShadow(
                  color: theme.colorScheme.primary.withValues(alpha: context.isDark ? 0.06 : 0.10),
                  blurRadius: 12,
                  spreadRadius: -2,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: Row(
              children: <Widget>[
                for (int i = 0; i < destinations.length; i++)
                  Expanded(
                    child: _NavSlot(
                      destination: destinations[i],
                      selected: i == currentIndex,
                      badge: badges[i] ?? 0,
                      onTap: () {
                        if (i == currentIndex) return;
                        Haptics.tap();
                        onSelected(i);
                      },
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

class _NavSlot extends StatelessWidget {
  const _NavSlot({
    required this.destination,
    required this.selected,
    required this.badge,
    required this.onTap,
  });

  final SproutlyDestination destination;
  final bool selected;
  final int badge;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final bool reduce = SproutlyMotion.reduceMotion(context);

    if (destination.emphasized) {
      return Center(
        child: Semantics(
          button: true,
          selected: selected,
          label: destination.label,
          child: AnimatedScale(
            scale: selected ? 1.08 : 1,
            duration: SproutlyMotion.resolve(context, SproutlyMotion.spatialFast),
            curve: SproutlyMotion.emphasized,
            child: Material(
              color: theme.colorScheme.primary,
              borderRadius: SproutlyShapes.radiusPill,
              elevation: 0,
              child: InkWell(
                borderRadius: SproutlyShapes.radiusPill,
                onTap: onTap,
                child: Container(
                  height: 50,
                  width: 60,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    borderRadius: SproutlyShapes.radiusPill,
                    boxShadow: <BoxShadow>[
                      BoxShadow(
                        color: theme.colorScheme.primary.withValues(alpha: 0.32),
                        blurRadius: 14,
                        offset: const Offset(0, 6),
                      ),
                    ],
                  ),
                  child: HugeIcon(
                    icon: destination.icon,
                    size: 26,
                    strokeWidth: 2.4,
                    color: theme.colorScheme.onPrimary,
                  ),
                ),
              ),
            ),
          ),
        ),
      );
    }

    return InkWell(
      borderRadius: BorderRadius.circular(SproutlyShapes.xl),
      onTap: onTap,
      child: Semantics(
        button: true,
        selected: selected,
        label: destination.label,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Stack(
              clipBehavior: Clip.none,
              children: <Widget>[
                AnimatedContainer(
                  duration: SproutlyMotion.resolve(context, SproutlyMotion.spatialFast),
                  curve: SproutlyMotion.emphasized,
                  height: 32,
                  width: selected ? 54 : 36,
                  decoration: BoxDecoration(
                    color: selected ? theme.colorScheme.primaryContainer : Colors.transparent,
                    borderRadius: SproutlyShapes.radiusPill,
                  ),
                  child: Center(
                    child: HugeIcon(
                      icon: destination.icon,
                      size: selected ? 21 : 20,
                      // Weight, not just colour: selection stays legible in
                      // high-contrast mode and to colour-blind users.
                      strokeWidth: selected ? 2.2 : 1.7,
                      color: selected
                          ? theme.colorScheme.onPrimaryContainer
                          : theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
                if (badge > 0)
                  Positioned(
                    right: selected ? 6 : 2,
                    top: -3,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                      constraints: const BoxConstraints(minWidth: 16),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.error,
                        borderRadius: SproutlyShapes.radiusPill,
                        border: Border.all(color: theme.colorScheme.surfaceContainerLowest, width: 1.5),
                      ),
                      child: Text(
                        badge > 9 ? '9+' : '$badge',
                        textAlign: TextAlign.center,
                        style: theme.textTheme.labelSmall!.copyWith(
                          color: theme.colorScheme.onError,
                          fontSize: 9,
                          height: 1.3,
                        ),
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 3),
            AnimatedDefaultTextStyle(
              duration: SproutlyMotion.resolve(context, SproutlyMotion.effectsDefault),
              style: theme.textTheme.labelSmall!.copyWith(
                fontSize: 10,
                color: selected ? theme.colorScheme.onSurface : theme.colorScheme.onSurfaceVariant,
                fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              ),
              child: Text(destination.label, maxLines: 1),
            ),
          ],
        ),
      ),
    );
  }
}
