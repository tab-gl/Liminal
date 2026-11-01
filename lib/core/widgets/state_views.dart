import 'package:material_ui/material_ui.dart';
import 'package:shimmer/shimmer.dart';

import '../errors/app_exception.dart';
import 'shape_morph_loader.dart';
import 'skeleton.dart';
import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';
import 'sproutly_button.dart';

/// Empty state. Every list in Sproutly has one, with a way out of it.
class EmptyView extends StatelessWidget {
  const EmptyView({
    required this.title,
    required this.message,
    super.key,
    this.icon = Icons.spa_outlined,
    this.actionLabel,
    this.onAction,
    this.compact = false,
  });

  final String title;
  final String message;
  final IconData icon;
  final String? actionLabel;
  final VoidCallback? onAction;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: 32, vertical: compact ? 24 : 48),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Container(
              height: compact ? 56 : 76,
              width: compact ? 56 : 76,
              decoration: BoxDecoration(
                color: theme.colorScheme.primaryContainer.withValues(alpha: 0.55),
                borderRadius: SproutlyShapes.radiusXl,
              ),
              child: Icon(
                icon,
                size: compact ? 26 : 34,
                color: theme.colorScheme.onPrimaryContainer,
              ),
            ),
            SizedBox(height: compact ? 14 : 20),
            Text(title, style: theme.textTheme.titleMedium, textAlign: TextAlign.center),
            const SizedBox(height: 6),
            Text(
              message,
              style: theme.textTheme.bodySmall,
              textAlign: TextAlign.center,
            ),
            if (actionLabel != null && onAction != null) ...<Widget>[
              SizedBox(height: compact ? 16 : 22),
              SproutlyButton(
                label: actionLabel!,
                onPressed: onAction,
                variant: SproutlyButtonVariant.tonal,
                size: SproutlyButtonSize.small,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Error state that distinguishes a retryable failure from a permanent one.
class ErrorStateView extends StatelessWidget {
  const ErrorStateView({required this.error, super.key, this.onRetry});

  final Object error;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final AppException exception = error is AppException
        ? error as AppException
        : const AppException.unknown();
    final bool canRetry = exception.retryable && onRetry != null;

    return EmptyView(
      icon: exception.code == AppErrorCode.network
          ? Icons.wifi_off_rounded
          : exception.code == AppErrorCode.permissionDenied
              ? Icons.lock_outline_rounded
              : Icons.error_outline_rounded,
      title: exception.code == AppErrorCode.network ? 'You are offline' : 'Something went wrong',
      message: exception.message,
      actionLabel: canRetry ? 'Try again' : null,
      onAction: canRetry ? onRetry : null,
    );
  }
}

/// Skeleton for a card list, so the first paint is never an empty box.
class CardListSkeleton extends StatelessWidget {
  const CardListSkeleton({super.key, this.count = 5});

  final int count;

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      physics: const NeverScrollableScrollPhysics(),
      shrinkWrap: true,
      itemCount: count,
      separatorBuilder: (_, __) => const SizedBox(height: 12),
      itemBuilder: (BuildContext context, int index) => const AppCardSkeleton(),
    );
  }
}

/// Inline spinner used inside buttons and small containers.
class SproutlyLoader extends StatelessWidget {
  const SproutlyLoader({super.key, this.size = 22, this.stroke = 2.4});

  final double size;
  final double stroke;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: size,
      width: size,
      child: CircularProgressIndicator(strokeWidth: stroke, color: context.colors.primary),
    );
  }
}

/// Full-bleed loading state for a screen whose first paint has nothing to show.
/// Uses the morphing loader rather than a spinner so the identity survives the
/// empty moment.
class ScreenLoader extends StatelessWidget {
  const ScreenLoader({super.key, this.label});

  final String? label;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: ShapeMorphLoader(size: 46, label: label),
    );
  }
}
