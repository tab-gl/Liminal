import 'package:material_ui/material_ui.dart';

import '../services/haptics_service.dart';
import '../theme/sproutly_motion.dart';
import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';

/// The base surface of the interface.
///
/// One large radius, one tightened corner, a hairline border instead of a heavy
/// shadow, and a 2% scale-in on press when the card is tappable. The shape is
/// what makes a grid of these read as Sproutly rather than as a list of boxes.
class SproutlyCard extends StatefulWidget {
  const SproutlyCard({
    required this.child,
    super.key,
    this.onTap,
    this.onLongPress,
    this.padding = const EdgeInsets.all(16),
    this.color,
    this.borderColor,
    this.elevated = false,
    this.clip = true,
    this.tightenedCorner = true,
  });

  final Widget child;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final EdgeInsets padding;
  final Color? color;
  final Color? borderColor;
  final bool elevated;
  final bool clip;
  final bool tightenedCorner;

  @override
  State<SproutlyCard> createState() => _SproutlyCardState();
}

class _SproutlyCardState extends State<SproutlyCard> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final bool interactive = widget.onTap != null || widget.onLongPress != null;
    final BorderRadius radius = SproutlyShapes.cardRadius(tightened: widget.tightenedCorner);

    return AnimatedScale(
      scale: _pressed && !SproutlyMotion.reduceMotion(context) ? 0.985 : 1,
      duration: SproutlyMotion.resolve(context, SproutlyMotion.pressFeedback),
      curve: SproutlyMotion.standard,
      child: AnimatedContainer(
        duration: SproutlyMotion.resolve(context, SproutlyMotion.effectsFast),
        decoration: BoxDecoration(
          color: widget.color ?? theme.colorScheme.surfaceContainerLowest,
          borderRadius: radius,
          border: Border.all(
            color: widget.borderColor ?? theme.colorScheme.outlineVariant.withValues(alpha: 0.9),
          ),
          boxShadow: widget.elevated
              ? <BoxShadow>[
                  BoxShadow(
                    color: context.tokens.cardShadow,
                    blurRadius: 24,
                    offset: const Offset(0, 10),
                  ),
                ]
              : null,
        ),
        child: ClipRRect(
          borderRadius: radius,
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: interactive
                  ? () {
                      Haptics.tap();
                      widget.onTap?.call();
                    }
                  : null,
              onLongPress: widget.onLongPress,
              onHighlightChanged: (bool value) {
                if (!interactive) return;
                setState(() => _pressed = value);
              },
              child: Padding(padding: widget.padding, child: widget.child),
            ),
          ),
        ),
      ),
    );
  }
}
