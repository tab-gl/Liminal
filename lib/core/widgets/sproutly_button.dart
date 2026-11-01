import 'package:material_ui/material_ui.dart';

import '../services/haptics_service.dart';
import '../theme/sproutly_motion.dart';
import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';

enum SproutlyButtonVariant { primary, tonal, outline, ghost, destructive }

enum SproutlyButtonSize { small, medium, large }

/// The Sproutly button.
///
/// Expressive details that separate it from a stock Material button:
///  * the container scales down 4% on press and springs back (spatial motion),
///  * the fill shifts to a deeper tone while held,
///  * a light haptic fires on tap,
///  * a loading state swaps the label for a spinner without changing width,
///  * disabled and busy states are visually distinct from each other.
class SproutlyButton extends StatefulWidget {
  const SproutlyButton({
    required this.label,
    required this.onPressed,
    super.key,
    this.icon,
    this.trailingIcon,
    this.variant = SproutlyButtonVariant.primary,
    this.size = SproutlyButtonSize.medium,
    this.isLoading = false,
    this.expand = false,
    this.semanticLabel,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final IconData? trailingIcon;
  final SproutlyButtonVariant variant;
  final SproutlyButtonSize size;
  final bool isLoading;
  final bool expand;
  final String? semanticLabel;

  @override
  State<SproutlyButton> createState() => _SproutlyButtonState();
}

class _SproutlyButtonState extends State<SproutlyButton> {
  bool _pressed = false;

  bool get _enabled => widget.onPressed != null && !widget.isLoading;

  double get _height {
    switch (widget.size) {
      case SproutlyButtonSize.small:
        return 40;
      case SproutlyButtonSize.medium:
        return 52;
      case SproutlyButtonSize.large:
        return 60;
    }
  }

  EdgeInsets get _padding {
    switch (widget.size) {
      case SproutlyButtonSize.small:
        return const EdgeInsets.symmetric(horizontal: 16);
      case SproutlyButtonSize.medium:
        return const EdgeInsets.symmetric(horizontal: 22);
      case SproutlyButtonSize.large:
        return const EdgeInsets.symmetric(horizontal: 28);
    }
  }

  TextStyle _labelStyle(ThemeData theme) {
    final double size = widget.size == SproutlyButtonSize.small ? 13 : 15;
    return theme.textTheme.labelLarge!.copyWith(fontSize: size, letterSpacing: 0.1);
  }

  ({Color background, Color foreground, Color? border}) _colors(ThemeData theme) {
    final ColorScheme scheme = theme.colorScheme;
    switch (widget.variant) {
      case SproutlyButtonVariant.primary:
        return (background: scheme.primary, foreground: scheme.onPrimary, border: null);
      case SproutlyButtonVariant.tonal:
        return (
          background: scheme.primaryContainer,
          foreground: scheme.onPrimaryContainer,
          border: null,
        );
      case SproutlyButtonVariant.outline:
        return (
          background: Colors.transparent,
          foreground: scheme.onSurface,
          border: scheme.outline,
        );
      case SproutlyButtonVariant.ghost:
        return (background: Colors.transparent, foreground: scheme.onSurface, border: null);
      case SproutlyButtonVariant.destructive:
        return (background: scheme.error, foreground: scheme.onError, border: null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final ({Color background, Color foreground, Color? border}) colors = _colors(theme);
    final bool reduce = SproutlyMotion.reduceMotion(context);

    final Color background = _enabled
        ? (_pressed ? Color.alphaBlend(theme.colorScheme.onSurface.withValues(alpha: 0.08), colors.background) : colors.background)
        : colors.background.withValues(alpha: 0.35);

    return Semantics(
      button: true,
      enabled: _enabled,
      label: widget.semanticLabel ?? widget.label,
      child: AnimatedScale(
        scale: _pressed && !reduce ? 0.96 : 1,
        duration: SproutlyMotion.resolve(context, SproutlyMotion.pressFeedback),
        curve: SproutlyMotion.tactile,
        child: AnimatedContainer(
          duration: SproutlyMotion.resolve(context, SproutlyMotion.effectsFast),
          height: _height,
          width: widget.expand ? double.infinity : null,
          decoration: BoxDecoration(
            color: background,
            borderRadius: SproutlyShapes.radiusPill,
            border: colors.border == null
                ? null
                : Border.all(color: _enabled ? colors.border! : colors.border!.withValues(alpha: 0.4)),
            boxShadow: widget.variant == SproutlyButtonVariant.primary && _enabled && !context.isDark
                ? <BoxShadow>[
                    BoxShadow(
                      color: colors.background.withValues(alpha: 0.28),
                      blurRadius: 18,
                      offset: const Offset(0, 8),
                    ),
                  ]
                : null,
          ),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              borderRadius: SproutlyShapes.radiusPill,
              onTap: _enabled
                  ? () {
                      Haptics.press();
                      widget.onPressed!();
                    }
                  : null,
              onHighlightChanged: (bool value) {
                if (reduce) return;
                setState(() => _pressed = value);
              },
              child: Padding(
                padding: _padding,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  mainAxisSize: widget.expand ? MainAxisSize.max : MainAxisSize.min,
                  children: <Widget>[
                    if (widget.isLoading)
                      SizedBox(
                        height: 18,
                        width: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.2,
                          color: colors.foreground,
                        ),
                      )
                    else if (widget.icon != null) ...<Widget>[
                      Icon(widget.icon, size: 19, color: _enabled ? colors.foreground : colors.foreground.withValues(alpha: 0.5)),
                      const SizedBox(width: 8),
                    ],
                    Flexible(
                      child: Text(
                        widget.label,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: _labelStyle(theme).copyWith(
                          color: _enabled ? colors.foreground : colors.foreground.withValues(alpha: 0.5),
                        ),
                      ),
                    ),
                    if (widget.trailingIcon != null && !widget.isLoading) ...<Widget>[
                      const SizedBox(width: 8),
                      Icon(widget.trailingIcon, size: 19, color: colors.foreground),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Square icon-only action with the same press behaviour.
class SproutlyIconButton extends StatefulWidget {
  const SproutlyIconButton({
    required this.icon,
    required this.onPressed,
    required this.tooltip,
    super.key,
    this.isSelected = false,
    this.size = 44,
  });

  final IconData icon;
  final VoidCallback? onPressed;
  final String tooltip;
  final bool isSelected;
  final double size;

  @override
  State<SproutlyIconButton> createState() => _SproutlyIconButtonState();
}

class _SproutlyIconButtonState extends State<SproutlyIconButton> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    final Color background = widget.isSelected
        ? theme.colorScheme.primaryContainer
        : theme.colorScheme.surfaceContainerHigh.withValues(alpha: 0.6);
    final Color foreground =
        widget.isSelected ? theme.colorScheme.onPrimaryContainer : theme.colorScheme.onSurface;

    return Tooltip(
      message: widget.tooltip,
      child: AnimatedScale(
        scale: _pressed ? 0.92 : 1,
        duration: SproutlyMotion.resolve(context, SproutlyMotion.pressFeedback),
        curve: SproutlyMotion.tactile,
        child: Material(
          color: background,
          shape: const CircleBorder(),
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            onTap: widget.onPressed == null
                ? null
                : () {
                    Haptics.tap();
                    widget.onPressed!();
                  },
            onHighlightChanged: (bool value) => setState(() => _pressed = value),
            child: SizedBox(
              height: widget.size,
              width: widget.size,
              child: Icon(widget.icon, size: widget.size * 0.48, color: foreground),
            ),
          ),
        ),
      ),
    );
  }
}
