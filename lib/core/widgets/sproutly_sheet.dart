import 'package:material_ui/material_ui.dart';

import '../services/haptics_service.dart';
import '../theme/sproutly_motion.dart';
import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';
import 'sproutly_button.dart';

/// Expressive modal sheet: rounded top, drag handle, and a title row with a
/// close action. Content scrolls inside a safe area so it never hides behind
/// the keyboard.
class SproutlySheet extends StatelessWidget {
  const SproutlySheet({
    required this.title,
    required this.child,
    super.key,
    this.subtitle,
    this.actions,
    this.scrollable = true,
  });

  final String title;
  final String? subtitle;
  final Widget child;
  final List<Widget>? actions;
  final bool scrollable;

  static Future<T?> show<T>({
    required BuildContext context,
    required String title,
    required Widget child,
    String? subtitle,
    List<Widget>? actions,
    bool isScrollControlled = true,
    bool dismissible = true,
  }) {
    Haptics.tap();
    return showModalBottomSheet<T>(
      context: context,
      isScrollControlled: isScrollControlled,
      isDismissible: dismissible,
      enableDrag: dismissible,
      backgroundColor: Colors.transparent,
      barrierColor: Colors.black.withValues(alpha: 0.42),
      builder: (BuildContext context) => SproutlySheet(
        title: title,
        subtitle: subtitle,
        actions: actions,
        child: child,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final EdgeInsets viewInsets = MediaQuery.viewInsetsOf(context);
    return Padding(
      padding: EdgeInsets.only(bottom: viewInsets.bottom),
      child: Container(
        constraints: BoxConstraints(maxHeight: MediaQuery.sizeOf(context).height * 0.86),
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          borderRadius: SproutlyShapes.sheetRadius(),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            const SizedBox(height: 10),
            Container(
              height: 4,
              width: 40,
              decoration: BoxDecoration(
                color: theme.colorScheme.outlineVariant,
                borderRadius: SproutlyShapes.radiusPill,
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 12, 6),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(title, style: theme.textTheme.titleLarge),
                        if (subtitle != null)
                          Padding(
                            padding: const EdgeInsets.only(top: 2),
                            child: Text(subtitle!, style: theme.textTheme.bodySmall),
                          ),
                      ],
                    ),
                  ),
                  SproutlyIconButton(
                    icon: Icons.close_rounded,
                    tooltip: 'Close',
                    size: 36,
                    onPressed: () => Navigator.of(context).maybePop(),
                  ),
                ],
              ),
            ),
            Flexible(
              child: scrollable
                  ? SingleChildScrollView(
                      padding: const EdgeInsets.fromLTRB(20, 8, 20, 20),
                      child: child,
                    )
                  : Padding(padding: const EdgeInsets.fromLTRB(20, 8, 20, 20), child: child),
            ),
            if (actions != null)
              Padding(
                padding: EdgeInsets.fromLTRB(20, 0, 20, 16 + MediaQuery.paddingOf(context).bottom),
                child: Row(
                  children: <Widget>[
                    for (int i = 0; i < actions!.length; i++) ...<Widget>[
                      if (i > 0) const SizedBox(width: 10),
                      Expanded(child: actions![i]),
                    ],
                  ],
                ),
              )
            else
              SizedBox(height: 12 + MediaQuery.paddingOf(context).bottom),
          ],
        ),
      ),
    );
  }
}

/// Two-action confirmation with a destructive option. Returns true when the
/// user confirms.
Future<bool> showConfirmDialog(
  BuildContext context, {
  required String title,
  required String message,
  String confirmLabel = 'Confirm',
  String cancelLabel = 'Cancel',
  bool destructive = false,
}) async {
  final ThemeData theme = context.theme;
  final bool? result = await showGeneralDialog<bool>(
    context: context,
    barrierDismissible: true,
    barrierLabel: title,
    barrierColor: Colors.black.withValues(alpha: 0.45),
    transitionDuration: SproutlyMotion.spatialFast,
    pageBuilder: (BuildContext context, Animation<double> animation, Animation<double> secondary) {
      return AlertDialog(
        backgroundColor: theme.colorScheme.surfaceContainerLowest,
        shape: RoundedRectangleBorder(borderRadius: SproutlyShapes.radiusXl),
        title: Text(title, style: theme.textTheme.titleLarge),
        content: Text(message, style: theme.textTheme.bodyMedium),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(cancelLabel),
          ),
          SproutlyButton(
            label: confirmLabel,
            size: SproutlyButtonSize.small,
            variant: destructive ? SproutlyButtonVariant.destructive : SproutlyButtonVariant.primary,
            onPressed: () {
              Haptics.warn();
              Navigator.of(context).pop(true);
            },
          ),
        ],
      );
    },
    transitionBuilder: (BuildContext context, Animation<double> animation, Animation<double> _, Widget child) {
      final CurvedAnimation curved = CurvedAnimation(parent: animation, curve: SproutlyMotion.emphasized);
      return ScaleTransition(
        scale: Tween<double>(begin: 0.92, end: 1).animate(curved),
        child: FadeTransition(opacity: curved, child: child),
      );
    },
  );
  return result ?? false;
}
