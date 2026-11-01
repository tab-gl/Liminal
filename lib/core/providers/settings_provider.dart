import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../services/haptics_service.dart';
import 'ads_provider.dart';
import 'app_providers.dart';

@immutable
class AppSettings {
  const AppSettings({
    this.themeMode = ThemeMode.system,
    this.hapticsEnabled = true,
    this.pushEnabled = false,
    this.productAnalyticsEnabled = true,
    this.adsPersonalised = true,
  });

  final ThemeMode themeMode;
  final bool hapticsEnabled;
  final bool pushEnabled;
  final bool productAnalyticsEnabled;

  /// Ad personalisation. Off means every AdMob request is built with
  /// nonPersonalizedAds: true - the ad still shows, it just is not targeted.
  final bool adsPersonalised;

  AppSettings copyWith({
    ThemeMode? themeMode,
    bool? hapticsEnabled,
    bool? pushEnabled,
    bool? productAnalyticsEnabled,
    bool? adsPersonalised,
  }) {
    return AppSettings(
      themeMode: themeMode ?? this.themeMode,
      hapticsEnabled: hapticsEnabled ?? this.hapticsEnabled,
      pushEnabled: pushEnabled ?? this.pushEnabled,
      productAnalyticsEnabled: productAnalyticsEnabled ?? this.productAnalyticsEnabled,
      adsPersonalised: adsPersonalised ?? this.adsPersonalised,
    );
  }
}

/// On-device preferences, persisted in the secure store. Everything else about
/// a person lives in their Firestore profile.
class SettingsController extends Notifier<AppSettings> {
  static const String _themeKey = 'sproutly.settings.theme';
  static const String _hapticsKey = 'sproutly.settings.haptics';
  static const String _pushKey = 'sproutly.settings.push';
  static const String _adsKey = 'sproutly.settings.ads.personalised';

  @override
  AppSettings build() {
    Future<void>.microtask(_restore);
    return const AppSettings();
  }

  Future<void> _restore() async {
    final store = ref.read(secureStoreProvider);
    final String? theme = await store.read(_themeKey);
    final String? haptics = await store.read(_hapticsKey);
    final String? push = await store.read(_pushKey);
    final String? ads = await store.read(_adsKey);
    if (theme == null && haptics == null && push == null && ads == null) return;
    state = state.copyWith(
      themeMode: switch (theme) {
        'light' => ThemeMode.light,
        'dark' => ThemeMode.dark,
        _ => ThemeMode.system,
      },
      hapticsEnabled: haptics != 'false',
      pushEnabled: push == 'true',
      adsPersonalised: ads != 'false',
    );
    Haptics.enabled = state.hapticsEnabled;
    ref.read(adsServiceProvider).personalised = state.adsPersonalised;
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    state = state.copyWith(themeMode: mode);
    await ref.read(secureStoreProvider).write(_themeKey, mode.name);
  }

  Future<void> setHaptics(bool enabled) async {
    state = state.copyWith(hapticsEnabled: enabled);
    Haptics.enabled = enabled;
    await ref.read(secureStoreProvider).write(_hapticsKey, enabled.toString());
  }

  /// Consent for personalised advertising. Applied to the live service so the
  /// next request reflects it immediately.
  Future<void> setAdsPersonalised(bool enabled) async {
    state = state.copyWith(adsPersonalised: enabled);
    await ref.read(adsServiceProvider).setPersonalised(enabled);
    await ref.read(secureStoreProvider).write(_adsKey, enabled.toString());
  }

  Future<void> setPush(bool enabled) async {
    state = state.copyWith(pushEnabled: enabled);
    await ref.read(secureStoreProvider).write(_pushKey, enabled.toString());
  }
}

final NotifierProvider<SettingsController, AppSettings> settingsProvider =
    NotifierProvider<SettingsController, AppSettings>(SettingsController.new);

final Provider<ThemeMode> themeModeProvider =
    Provider<ThemeMode>((Ref ref) => ref.watch(settingsProvider).themeMode);
