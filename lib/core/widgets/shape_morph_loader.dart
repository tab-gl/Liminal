import 'dart:math' as math;

import 'package:material_ui/material_ui.dart';

import '../theme/sproutly_motion.dart';
import '../theme/theme_context.dart';

/// A loading indicator that morphs between shapes.
///
/// Built on a superellipse: the outline is sampled as
/// `|cos θ|^(2/n)` / `|sin θ|^(2/n)`, and `n` animates between 2 (an ellipse)
/// and 5 (a squircle). That is a genuine continuous morph rather than a
/// cross-fade between two icons, it costs 64 points a frame, and it needs no
/// extra package.
///
/// When reduced motion is requested it renders one static shape - the loader
/// still communicates "busy" without movement.
class ShapeMorphLoader extends StatefulWidget {
  const ShapeMorphLoader({
    super.key,
    this.size = 44,
    this.color,
    this.strokeWidth = 0,
    this.label,
  });

  final double size;
  final Color? color;

  /// 0 fills the shape; above 0 draws an outline instead.
  final double strokeWidth;

  /// Optional caption under the shape, for full-screen states.
  final String? label;

  @override
  State<ShapeMorphLoader> createState() => _ShapeMorphLoaderState();
}

class _ShapeMorphLoaderState extends State<ShapeMorphLoader>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1800),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Respect a runtime change of the platform setting.
    final bool reduce = SproutlyMotion.reduceMotion(context);
    if (reduce && _controller.isAnimating) {
      _controller.stop();
      _controller.value = 0;
    } else if (!reduce && !_controller.isAnimating) {
      _controller.repeat();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final Color color = widget.color ?? context.colors.primary;
    return Semantics(
      label: widget.label ?? 'Loading',
      liveRegion: true,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          AnimatedBuilder(
            animation: _controller,
            builder: (BuildContext context, Widget? _) {
              // Ease the exponent so the morph has a hold at each extreme
              // instead of sliding through the middle at constant speed.
              final double wave = 0.5 - 0.5 * math.cos(_controller.value * 2 * math.pi);
              final double exponent = 2 + 3 * Curves.easeInOut.transform(wave);
              return CustomPaint(
                size: Size.square(widget.size),
                painter: _SuperellipsePainter(
                  exponent: exponent,
                  color: color,
                  strokeWidth: widget.strokeWidth,
                  // A slow quarter turn keeps it alive without spinning like a
                  // stock progress spinner.
                  rotation: _controller.value * math.pi / 2,
                ),
              );
            },
          ),
          if (widget.label != null) ...<Widget>[
            const SizedBox(height: 14),
            Text(widget.label!, style: context.text.labelSmall),
          ],
        ],
      ),
    );
  }
}

class _SuperellipsePainter extends CustomPainter {
  _SuperellipsePainter({
    required this.exponent,
    required this.color,
    required this.strokeWidth,
    required this.rotation,
  });

  final double exponent;
  final Color color;
  final double strokeWidth;
  final double rotation;

  static const int _segments = 64;

  @override
  void paint(Canvas canvas, Size size) {
    final double radius = math.min(size.width, size.height) / 2;
    final Offset center = Offset(size.width / 2, size.height / 2);
    final Path path = Path();

    for (int i = 0; i <= _segments; i++) {
      final double theta = (i / _segments) * 2 * math.pi + rotation;
      final double cos = math.cos(theta);
      final double sin = math.sin(theta);
      final double x =
          center.dx + radius * cos.sign * math.pow(cos.abs(), 2 / exponent);
      final double y =
          center.dy + radius * sin.sign * math.pow(sin.abs(), 2 / exponent);
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    path.close();

    final Paint paint = Paint()
      ..color = color
      ..isAntiAlias = true;
    if (strokeWidth > 0) {
      paint
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth
        ..strokeJoin = StrokeJoin.round;
    } else {
      paint.style = PaintingStyle.fill;
    }
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(_SuperellipsePainter oldDelegate) =>
      oldDelegate.exponent != exponent ||
      oldDelegate.color != color ||
      oldDelegate.strokeWidth != strokeWidth ||
      oldDelegate.rotation != rotation;
}
