import 'package:material_ui/material_ui.dart';

/// Corner radii and shape builders.
///
/// Expressive UI varies its radii deliberately: chips and badges are almost
/// circular, cards are softly rounded, sheets are only rounded at the top. The
/// variation is the visual signature - do not flatten it to one radius.
class SproutlyShapes {
  const SproutlyShapes._();

  static const double xs = 10;
  static const double sm = 16;
  static const double md = 20;
  static const double lg = 28;
  static const double xl = 34;
  static const double xxl = 44;
  static const double pill = 999;

  static BorderRadius get radiusXs => BorderRadius.circular(xs);
  static BorderRadius get radiusSm => BorderRadius.circular(sm);
  static BorderRadius get radiusMd => BorderRadius.circular(md);
  static BorderRadius get radiusLg => BorderRadius.circular(lg);
  static BorderRadius get radiusXl => BorderRadius.circular(xl);
  static BorderRadius get radiusPill => BorderRadius.circular(pill);

  /// Cards use a large radius with one tightened corner, which reads as
  /// "organic" without looking hand-drawn.
  static BorderRadius cardRadius({bool tightened = true}) => BorderRadius.only(
        topLeft: const Radius.circular(lg),
        topRight: Radius.circular(tightened ? sm : lg),
        bottomLeft: Radius.circular(tightened ? sm : lg),
        bottomRight: const Radius.circular(lg),
      );

  static BorderRadius sheetRadius() => const BorderRadius.only(
        topLeft: Radius.circular(xl),
        topRight: Radius.circular(xl),
      );

  static const ShapeBorder card = RoundedRectangleBorder(
    borderRadius: BorderRadius.only(
      topLeft: Radius.circular(lg),
      topRight: Radius.circular(sm),
      bottomLeft: Radius.circular(sm),
      bottomRight: Radius.circular(lg),
    ),
  );

  static const ShapeBorder button = StadiumBorder();
}
