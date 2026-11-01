import 'package:material_ui/material_ui.dart';

import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';

/// Small capsule label. Used for categories, statuses and metadata.
class SproutlyPill extends StatelessWidget {
  const SproutlyPill({
    required this.label,
    super.key,
    this.icon,
    this.background,
    this.foreground,
    this.dense = false,
  });

  final String label;
  final IconData? icon;
  final Color? background;
  final Color? foreground;
  final bool dense;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final Color bg = background ?? theme.colorScheme.surfaceContainerHigh;
    final Color fg = foreground ?? theme.colorScheme.onSurfaceVariant;
    return Container(
      padding: EdgeInsets.symmetric(horizontal: dense ? 8 : 10, vertical: dense ? 3 : 5),
      decoration: BoxDecoration(color: bg, borderRadius: SproutlyShapes.radiusPill),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          if (icon != null) ...<Widget>[
            Icon(icon, size: dense ? 11 : 13, color: fg),
            const SizedBox(width: 4),
          ],
          Text(
            label,
            style: theme.textTheme.labelSmall!.copyWith(
              color: fg,
              fontSize: dense ? 10 : 11,
              letterSpacing: 0.2,
            ),
          ),
        ],
      ),
    );
  }
}

/// Paid placement. Always visible on a sponsored card - required by the
/// advertising policy in docs/ADVERTISING.md.
class SponsoredBadge extends StatelessWidget {
  const SponsoredBadge({super.key, this.label = 'Sponsored'});

  final String label;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      decoration: BoxDecoration(
        color: tokens.sponsoredBackground,
        borderRadius: SproutlyShapes.radiusPill,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Icon(Icons.campaign_outlined, size: 12, color: tokens.sponsoredForeground),
          const SizedBox(width: 4),
          Text(
            label,
            style: context.text.labelSmall!.copyWith(
              color: tokens.sponsoredForeground,
              fontSize: 10,
              letterSpacing: 0.4,
            ),
          ),
        ],
      ),
    );
  }
}

/// Pricing model chip: Free, Freemium, Paid, Open source.
class PricingBadge extends StatelessWidget {
  const PricingBadge({required this.label, super.key, this.isFree = false});

  final String label;
  final bool isFree;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return SproutlyPill(
      label: label,
      dense: true,
      background: isFree ? context.tokens.successContainer : theme.colorScheme.surfaceContainerHigh,
      foreground: isFree ? context.tokens.success : theme.colorScheme.onSurfaceVariant,
    );
  }
}

/// Moderation / campaign status chip with the right semantic colour.
class StatusChip extends StatelessWidget {
  const StatusChip({required this.status, super.key});

  final String status;

  static const Map<String, String> _labels = <String, String>{
    'draft': 'Draft',
    'pending_review': 'In review',
    'approved': 'Live',
    'rejected': 'Needs changes',
    'suspended': 'Suspended',
    'pending_approval': 'Awaiting approval',
    'active': 'Active',
    'paused': 'Paused',
    'completed': 'Completed',
    'exhausted': 'Budget spent',
    'open': 'Open',
    'resolved': 'Resolved',
  };

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final tokens = context.tokens;
    late final Color bg;
    late final Color fg;
    switch (status) {
      case 'approved':
      case 'active':
      case 'resolved':
        bg = tokens.successContainer;
        fg = tokens.success;
      case 'pending_review':
      case 'pending_approval':
      case 'paused':
      case 'open':
        bg = tokens.warningContainer;
        fg = tokens.warning;
      case 'rejected':
      case 'suspended':
        bg = theme.colorScheme.errorContainer;
        fg = theme.colorScheme.onErrorContainer;
      default:
        bg = theme.colorScheme.surfaceContainerHigh;
        fg = theme.colorScheme.onSurfaceVariant;
    }
    return SproutlyPill(
      label: _labels[status] ?? status,
      dense: true,
      background: bg,
      foreground: fg,
    );
  }
}
