import 'package:cached_network_image/cached_network_image.dart';
import 'package:material_ui/material_ui.dart';
import 'package:shimmer/shimmer.dart';

import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';

/// Network image with a branded placeholder and a graceful failure state.
///
/// Application icons are square with an expressive radius; screenshots keep
/// their aspect ratio inside a rounded clip.
class AppImage extends StatelessWidget {
  const AppImage({
    required this.url,
    super.key,
    this.height,
    this.width,
    this.fit = BoxFit.cover,
    this.radius,
    this.icon = Icons.apps_rounded,
    this.isIcon = false,
  });

  final String? url;
  final double? height;
  final double? width;
  final BoxFit fit;
  final BorderRadius? radius;
  final IconData icon;
  final bool isIcon;

  @override
  Widget build(BuildContext context) {
    final BorderRadius r = radius ?? SproutlyShapes.radiusMd;
    final Widget fallback = _Fallback(icon: icon, isIcon: isIcon);

    if (url == null || url!.isEmpty) {
      return SizedBox(
        height: height,
        width: width,
        child: ClipRRect(borderRadius: r, child: fallback),
      );
    }

    return SizedBox(
      height: height,
      width: width,
      child: ClipRRect(
        borderRadius: r,
        child: CachedNetworkImage(
          imageUrl: url!,
          fit: fit,
          fadeInDuration: const Duration(milliseconds: 180),
          placeholder: (BuildContext context, String _) => _Skeleton(
            height: height,
            width: width,
          ),
          errorWidget: (BuildContext context, String _, Object __) => fallback,
        ),
      ),
    );
  }
}

class _Skeleton extends StatelessWidget {
  const _Skeleton({this.height, this.width});

  final double? height;
  final double? width;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return Shimmer.fromColors(
      baseColor: theme.colorScheme.surfaceContainerHigh,
      highlightColor: theme.colorScheme.surfaceContainerLowest,
      child: Container(height: height, width: width, color: theme.colorScheme.surfaceContainerHigh),
    );
  }
}

class _Fallback extends StatelessWidget {
  const _Fallback({required this.icon, required this.isIcon});

  final IconData icon;
  final bool isIcon;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return Container(
      decoration: BoxDecoration(
        color: theme.colorScheme.primaryContainer,
      ),
      alignment: Alignment.center,
      child: Icon(
        isIcon ? icon : Icons.broken_image_outlined,
        color: theme.colorScheme.onPrimaryContainer.withValues(alpha: 0.7),
        size: isIcon ? 22 : 20,
      ),
    );
  }
}
