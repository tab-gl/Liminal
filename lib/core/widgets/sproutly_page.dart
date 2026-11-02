import 'package:material_ui/material_ui.dart';

import '../theme/sproutly_theme.dart';
import '../theme/theme_context.dart';
import 'sproutly_button.dart';

/// Standard page frame: safe area, centred content with a readable max width,
/// and a header with an optional back action.
class SproutlyPage extends StatelessWidget {
  const SproutlyPage({
    required this.child,
    super.key,
    this.title,
    this.subtitle,
    this.actions,
    this.showBack = false,
    this.scrollable = true,
    this.padding = const EdgeInsets.symmetric(horizontal: 20),
    this.bottom,
    this.onRefresh,
  });

  final Widget child;
  final String? title;
  final String? subtitle;
  final List<Widget>? actions;
  final bool showBack;
  final bool scrollable;
  final EdgeInsets padding;
  final Widget? bottom;
  final Future<void> Function()? onRefresh;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    Widget body = child;
    if (scrollable) {
      body = SingleChildScrollView(
        physics: onRefresh == null ? null : const AlwaysScrollableScrollPhysics(),
        padding: EdgeInsets.only(bottom: 24 + MediaQuery.paddingOf(context).bottom),
        child: child,
      );
      if (onRefresh != null) {
        body = RefreshIndicator(
          color: theme.colorScheme.primary,
          onRefresh: onRefresh!,
          child: body,
        );
      }
    }

    return Scaffold(
      backgroundColor: theme.colorScheme.surface,
      body: SafeArea(
        bottom: false,
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: SproutlyTheme.maxContentWidth),
            child: Column(
              children: <Widget>[
                if (title != null)
                  Padding(
                    padding: EdgeInsets.fromLTRB(showBack ? 8 : 20, 8, 12, 4),
                    child: Row(
                      children: <Widget>[
                        if (showBack)
                          SproutlyIconButton(
                            icon: Icons.arrow_back_rounded,
                            tooltip: 'Back',
                            onPressed: () => Navigator.of(context).maybePop(),
                          ),
                        if (showBack) const SizedBox(width: 4),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(title!, style: theme.textTheme.headlineSmall),
                              if (subtitle != null)
                                Padding(
                                  padding: const EdgeInsets.only(top: 2),
                                  child: Text(subtitle!, style: theme.textTheme.bodySmall),
                                ),
                            ],
                          ),
                        ),
                        if (actions != null) ...actions!,
                      ],
                    ),
                  ),
                Expanded(
                  child: Padding(padding: padding, child: body),
                ),
                if (bottom != null) bottom!,
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// A single dashboard statistic: label, value, optional delta and icon.
class MetricTile extends StatelessWidget {
  const MetricTile({
    required this.label,
    required this.value,
    super.key,
    this.icon,
    this.delta,
    this.emphasis = false,
  });

  final String label;
  final String value;
  final IconData? icon;
  final String? delta;
  final bool emphasis;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: emphasis
            ? theme.colorScheme.primaryContainer
            : theme.colorScheme.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(
          color: emphasis ? Colors.transparent : theme.colorScheme.outlineVariant,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Row(
            children: <Widget>[
              if (icon != null) ...<Widget>[
                Icon(
                  icon,
                  size: 14,
                  color: emphasis
                      ? theme.colorScheme.onPrimaryContainer
                      : theme.colorScheme.onSurfaceVariant,
                ),
                const SizedBox(width: 5),
              ],
              Flexible(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelSmall!.copyWith(
                    color: emphasis
                        ? theme.colorScheme.onPrimaryContainer
                        : theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            value,
            style: theme.textTheme.headlineSmall!.copyWith(
              fontWeight: FontWeight.w700,
              letterSpacing: -0.6,
              color: emphasis ? theme.colorScheme.onPrimaryContainer : null,
            ),
          ),
          if (delta != null)
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(
                delta!,
                style: theme.textTheme.labelSmall!.copyWith(
                  color: emphasis ? theme.colorScheme.onPrimaryContainer : context.tokens.success,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
