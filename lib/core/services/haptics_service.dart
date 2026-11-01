import 'package:flutter/services.dart';

/// Tactile feedback, centrally configured.
///
/// Sproutly uses four intensities and nothing else, so the app feels
/// consistent: [tap] for selection, [press] for primary actions, [success] for
/// completed work, [warn] for destructive confirmation.
class Haptics {
  const Haptics._();

  static bool enabled = true;

  static Future<void> tap() async {
    if (!enabled) return;
    await HapticFeedback.selectionClick();
  }

  static Future<void> press() async {
    if (!enabled) return;
    await HapticFeedback.lightImpact();
  }

  static Future<void> success() async {
    if (!enabled) return;
    await HapticFeedback.mediumImpact();
  }

  static Future<void> warn() async {
    if (!enabled) return;
    await HapticFeedback.heavyImpact();
  }
}
