import 'package:material_ui/material_ui.dart';

import 'sproutly_shapes.dart';

/// Rounded shapes for the stock Material widgets Sproutly still uses.
///
/// Everything here is a *shape or edge* decision only - colour, type and motion
/// stay with the colour scheme and the Sproutly widgets. Radii come from
/// [SproutlyShapes] so the whole app moves together when a token changes.
class SproutlyComponentThemes {
  const SproutlyComponentThemes._();

  static const StadiumBorder _pill = StadiumBorder();

  static ThemeData apply(ThemeData base, ColorScheme scheme) {
    final RoundedRectangleBorder soft = RoundedRectangleBorder(borderRadius: SproutlyShapes.radiusLg);
    final RoundedRectangleBorder softer = RoundedRectangleBorder(borderRadius: SproutlyShapes.radiusXl);
    final WidgetStateProperty<OutlinedBorder> pillShape = WidgetStatePropertyAll<OutlinedBorder>(_pill);

    return base.copyWith(
      // Floating, rounded snackbars instead of a full-width flat strip.
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: SproutlyShapes.radiusLg),
        insetPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        backgroundColor: scheme.inverseSurface,
        contentTextStyle: base.textTheme.bodyMedium?.copyWith(color: scheme.onInverseSurface),
        actionTextColor: scheme.primaryContainer,
      ),
      dialogTheme: DialogThemeData(
        shape: softer,
        backgroundColor: scheme.surfaceContainerLow,
        surfaceTintColor: const Color(0x00000000),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        shape: RoundedRectangleBorder(borderRadius: SproutlyShapes.sheetRadius()),
        clipBehavior: Clip.antiAlias,
        backgroundColor: scheme.surfaceContainerLow,
        surfaceTintColor: const Color(0x00000000),
      ),
      cardTheme: CardThemeData(
        shape: SproutlyShapes.card,
        clipBehavior: Clip.antiAlias,
        elevation: 0,
        margin: EdgeInsets.zero,
        surfaceTintColor: const Color(0x00000000),
      ),
      chipTheme: base.chipTheme.copyWith(
        shape: _pill,
        side: BorderSide(color: scheme.outlineVariant),
      ),
      popupMenuTheme: PopupMenuThemeData(
        shape: soft,
        surfaceTintColor: const Color(0x00000000),
        color: scheme.surfaceContainerLow,
      ),
      menuTheme: MenuThemeData(
        style: MenuStyle(shape: WidgetStatePropertyAll<OutlinedBorder>(soft)),
      ),
      dropdownMenuTheme: DropdownMenuThemeData(
        menuStyle: MenuStyle(shape: WidgetStatePropertyAll<OutlinedBorder>(soft)),
      ),
      listTileTheme: ListTileThemeData(shape: soft),
      tooltipTheme: TooltipThemeData(
        decoration: BoxDecoration(
          color: scheme.inverseSurface,
          borderRadius: SproutlyShapes.radiusSm,
        ),
        textStyle: base.textTheme.labelMedium?.copyWith(color: scheme.onInverseSurface),
      ),
      textButtonTheme: TextButtonThemeData(style: ButtonStyle(shape: pillShape)),
      filledButtonTheme: FilledButtonThemeData(style: ButtonStyle(shape: pillShape)),
      outlinedButtonTheme: OutlinedButtonThemeData(style: ButtonStyle(shape: pillShape)),
      elevatedButtonTheme: ElevatedButtonThemeData(style: ButtonStyle(shape: pillShape)),
      iconButtonTheme: IconButtonThemeData(style: ButtonStyle(shape: pillShape)),
      segmentedButtonTheme: SegmentedButtonThemeData(style: ButtonStyle(shape: pillShape)),
      // Round line ends on every determinate / indeterminate progress bar.
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        strokeCap: StrokeCap.round,
        linearMinHeight: 6,
        borderRadius: BorderRadius.all(Radius.circular(SproutlyShapes.pill)),
      ),
      tabBarTheme: TabBarThemeData(
        indicatorSize: TabBarIndicatorSize.tab,
        dividerColor: const Color(0x00000000),
        indicator: ShapeDecoration(color: scheme.primaryContainer, shape: _pill),
        labelColor: scheme.onPrimaryContainer,
        unselectedLabelColor: scheme.onSurfaceVariant,
      ),
    );
  }
}
