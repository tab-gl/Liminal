import 'package:material_ui/material_ui.dart';

/// Motion tokens.
///
/// Two families, matching expressive guidance: *spatial* motion moves or
/// resizes a container, *effects* motion fades or scales content. Spatial is
/// slower and springier; effects are short and linear-feeling.
class SproutlyMotion {
  const SproutlyMotion._();

  static const Duration effectsFast = Duration(milliseconds: 120);
  static const Duration effectsDefault = Duration(milliseconds: 200);
  static const Duration spatialFast = Duration(milliseconds: 260);
  static const Duration spatialDefault = Duration(milliseconds: 380);
  static const Duration spatialSlow = Duration(milliseconds: 520);
  static const Duration pressFeedback = Duration(milliseconds: 90);

  static const Curve emphasized = Cubic(0.05, 0.7, 0.1, 1.0);
  static const Curve emphasizedDecelerate = Cubic(0.05, 0.7, 0.1, 1.0);
  static const Curve emphasizedAccelerate = Cubic(0.3, 0.0, 0.8, 0.15);
  static const Curve standard = Cubic(0.2, 0.0, 0.0, 1.0);
  static const Curve tactile = Curves.easeOutBack;

  /// True when the platform asks for reduced motion. Every custom animation in
  /// Sproutly checks this and falls back to a zero-duration swap.
  static bool reduceMotion(BuildContext context) =>
      MediaQuery.maybeDisableAnimationsOf(context) ?? false;

  static Duration resolve(BuildContext context, Duration duration) =>
      reduceMotion(context) ? Duration.zero : duration;
}
