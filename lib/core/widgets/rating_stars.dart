import 'package:material_ui/material_ui.dart';

import '../theme/theme_context.dart';

/// Star row with half-star support. Read-only by default; pass [onChanged] for
/// the review composer.
class RatingStars extends StatelessWidget {
  const RatingStars({
    required this.rating,
    super.key,
    this.size = 14,
    this.count,
    this.onChanged,
  });

  final double rating;
  final double size;
  final int? count;
  final ValueChanged<int>? onChanged;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final Color filled = context.isDark ? context.tokens.lime : context.tokens.olive;
    final Color empty = theme.colorScheme.outline.withValues(alpha: 0.6);

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        for (int i = 1; i <= 5; i++)
          GestureDetector(
            onTap: onChanged == null ? null : () => onChanged!(i),
            behavior: HitTestBehavior.opaque,
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: onChanged == null ? 0 : 2),
              child: Icon(
                rating >= i
                    ? Icons.star_rounded
                    : rating >= i - 0.5
                        ? Icons.star_half_rounded
                        : Icons.star_outline_rounded,
                size: onChanged == null ? size : size + 10,
                color: rating >= i - 0.5 ? filled : empty,
              ),
            ),
          ),
        if (count != null) ...<Widget>[
          const SizedBox(width: 6),
          Text(
            '($count)',
            style: theme.textTheme.labelSmall!.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
        ],
      ],
    );
  }
}
