import 'package:material_ui/material_ui.dart';
import 'package:shimmer/shimmer.dart';

import '../theme/sproutly_motion.dart';
import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';

/// A shimmering placeholder block.
///
/// Two rules make skeletons feel fast rather than fake: they must match the
/// *shape* of what is loading (a card skeleton is card-shaped, not a grey bar),
/// and they must stop shimmering when the platform asks for reduced motion -
/// a pulsing block is exactly the kind of animation that setting exists for.
class Skeleton extends StatelessWidget {
  const Skeleton({
    super.key,
    this.height,
    this.width,
    this.radius,
    this.margin,
  });

  final double? height;
  final double? width;
  final BorderRadius? radius;
  final EdgeInsets? margin;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final Widget block = Container(
      height: height,
      width: width,
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHigh,
        borderRadius: radius ?? SproutlyShapes.radiusSm,
      ),
    );

    if (SproutlyMotion.reduceMotion(context)) {
      return Padding(padding: margin ?? EdgeInsets.zero, child: block);
    }

    return Padding(
      padding: margin ?? EdgeInsets.zero,
      child: Shimmer.fromColors(
        baseColor: theme.colorScheme.surfaceContainerHigh,
        highlightColor: theme.colorScheme.surfaceContainerLowest,
        period: const Duration(milliseconds: 1400),
        child: block,
      ),
    );
  }
}

/// A run of text lines, the last one short - which is what real text looks like.
class SkeletonLines extends StatelessWidget {
  const SkeletonLines({
    super.key,
    this.lines = 3,
    this.lineHeight = 11,
    this.spacing = 7,
  });

  final int lines;
  final double lineHeight;
  final double spacing;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        for (int i = 0; i < lines; i++)
          Padding(
            padding: EdgeInsets.only(bottom: i == lines - 1 ? 0 : spacing),
            child: FractionallySizedBox(
              alignment: Alignment.centerLeft,
              widthFactor: i == lines - 1 ? 0.55 : 1,
              child: Skeleton(height: lineHeight, radius: SproutlyShapes.radiusPill),
            ),
          ),
      ],
    );
  }
}

/// The feed card skeleton: icon, title, two lines, meta chips.
class AppCardSkeleton extends StatelessWidget {
  const AppCardSkeleton({super.key, this.dense = false});

  final bool dense;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: context.colors.surfaceContainerLowest,
        borderRadius: SproutlyShapes.cardRadius(),
        border: Border.all(color: context.colors.outlineVariant),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Skeleton(height: 56, width: 56, radius: BorderRadius.circular(16)),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                const Skeleton(height: 13, width: 130),
                const SizedBox(height: 8),
                const SkeletonLines(lines: 2, lineHeight: 9),
                if (!dense) ...<Widget>[
                  const SizedBox(height: 10),
                  Row(
                    children: <Widget>[
                      Skeleton(height: 16, width: 58, radius: SproutlyShapes.radiusPill),
                      const SizedBox(width: 6),
                      Skeleton(height: 16, width: 44, radius: SproutlyShapes.radiusPill),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// A horizontal rail of compact cards, for the Discover sections.
class RailSkeleton extends StatelessWidget {
  const RailSkeleton({super.key, this.count = 3});

  final int count;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const Padding(
          padding: EdgeInsets.fromLTRB(20, 18, 20, 10),
          child: Skeleton(height: 15, width: 140),
        ),
        SizedBox(
          height: 178,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            physics: const NeverScrollableScrollPhysics(),
            padding: const EdgeInsets.symmetric(horizontal: 20),
            itemCount: count,
            separatorBuilder: (_, __) => const SizedBox(width: 12),
            itemBuilder: (BuildContext context, int index) => Container(
              width: 156,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: context.colors.surfaceContainerLowest,
                borderRadius: SproutlyShapes.cardRadius(),
                border: Border.all(color: context.colors.outlineVariant),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Skeleton(height: 42, width: 42, radius: BorderRadius.circular(13)),
                      const Spacer(),
                      Skeleton(height: 12, width: 12, radius: SproutlyShapes.radiusPill),
                    ],
                  ),
                  const SizedBox(height: 12),
                  const Skeleton(height: 11, width: 100),
                  const SizedBox(height: 7),
                  const SkeletonLines(lines: 2, lineHeight: 8),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}

/// The listing detail skeleton: banner, title block, action row, body copy.
class DetailSkeleton extends StatelessWidget {
  const DetailSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Skeleton(
          height: 220,
          width: double.infinity,
          radius: BorderRadius.zero,
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              const Skeleton(height: 22, width: 190),
              const SizedBox(height: 10),
              const Skeleton(height: 12, width: 250),
              const SizedBox(height: 16),
              Row(
                children: <Widget>[
                  Skeleton(height: 22, width: 90, radius: SproutlyShapes.radiusPill),
                  const SizedBox(width: 8),
                  Skeleton(height: 22, width: 70, radius: SproutlyShapes.radiusPill),
                ],
              ),
              const SizedBox(height: 20),
              Skeleton(height: 52, width: double.infinity, radius: SproutlyShapes.radiusPill),
              const SizedBox(height: 24),
              const SkeletonLines(lines: 4),
            ],
          ),
        ),
      ],
    );
  }
}
