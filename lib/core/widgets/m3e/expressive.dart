// Material 3 Expressive integration - the ONLY file in Sproutly that imports
// package:material_3_expressive.
//
// Why one file: Flutter 3.47 does not ship Material 3 Expressive in the SDK
// (see m3.material.io/develop/flutter - "M3 Expressive is not available on
// Flutter"). It arrives through the community package
// material_3_expressive, which implements the M3E component set on top of the
// decoupled material_ui package. Isolating the dependency here means:
//
//   * an upgrade to the package touches this file and nothing else,
//   * every call site below uses a signature documented in the package's API
//     reference for 1.2.0 (no invented widget names), and
//   * if the package has to be dropped, three widgets need a replacement and
//     the rest of the design system is unaffected.
//
// Verified signatures used here (material_3_expressive 1.2.0):
//   M3EButtonGroup(type:, selectedIndex:, onSelectedIndexChanged:, actions:)
//   M3EButtonGroupAction(label:)
//   M3EFab(icon:, size:, onPressed:)
import 'package:material_3_expressive/material_3_expressive.dart';
import 'package:material_ui/material_ui.dart' hide CornerRounding, RoundedPolygon;

/// Segmented period selector (Day / Week / Month) built from M3E buttons.
/// The connected variant morphs its corners into one continuous capsule, which
/// is the expressive detail a stock SegmentedButton does not have.
class ExpressivePeriodSelector extends StatelessWidget {
  const ExpressivePeriodSelector({
    required this.labels,
    required this.selectedIndex,
    required this.onChanged,
    super.key,
  });

  final List<String> labels;
  final int selectedIndex;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return M3EButtonGroup(
      type: M3EButtonGroupType.connected,
      selectedIndex: selectedIndex,
      onSelectedIndexChanged: (int? index) {
        if (index == null || index == selectedIndex) return;
        onChanged(index);
      },
      actions: <M3EButtonGroupAction>[
        for (final String label in labels) M3EButtonGroupAction(label: Text(label)),
      ],
    );
  }
}

/// The primary publish action as an M3E floating action button.
class ExpressiveActionFab extends StatelessWidget {
  const ExpressiveActionFab({
    required this.icon,
    required this.onPressed,
    super.key,
    this.semanticLabel,
  });

  final IconData icon;
  final VoidCallback onPressed;
  final String? semanticLabel;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: semanticLabel,
      button: true,
      child: M3EFab(
        icon: Icon(icon),
        size: M3EFabSize.regular,
        onPressed: onPressed,
      ),
    );
  }
}
