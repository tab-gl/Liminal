import 'package:firebase_remote_config/firebase_remote_config.dart';

import '../config/app_config.dart';

/// Remote flags and copy.
///
/// Startup no longer waits for this: `init()` runs after the first frame, and
/// every getter falls back to the compiled defaults until the fetch lands. That
/// is what keeps the app's first paint independent of a network round trip -
/// and it is why the defaults below are the real values, not placeholders.
class RemoteConfigService {
  RemoteConfigService({FirebaseRemoteConfig? remoteConfig})
      : _remote = remoteConfig ?? FirebaseRemoteConfig.instance;

  final FirebaseRemoteConfig _remote;
  bool _ready = false;

  static const Map<String, Object> _defaults = <String, Object>{
    'publishing_enabled': true,
    'advertising_enabled': true,
    'min_supported_version_android': '1.0.0',
    'min_supported_version_ios': '1.0.0',
    'feed_rail_size': 10,
    'moderation_auto_approve': false,
    'payments_enabled': false,
    'support_email': 'support@sproutly.app',
  };

  bool _flag(String key) {
    if (!_ready) return _defaults[key]! as bool;
    try {
      return _remote.getBool(key);
    } catch (_) {
      return _defaults[key]! as bool;
    }
  }

  int _int(String key) {
    if (!_ready) return _defaults[key]! as int;
    try {
      return _remote.getInt(key);
    } catch (_) {
      return _defaults[key]! as int;
    }
  }

  String _string(String key) {
    if (!_ready) return _defaults[key]! as String;
    try {
      return _remote.getString(key);
    } catch (_) {
      return _defaults[key]! as String;
    }
  }

  bool get publishingEnabled => _flag('publishing_enabled');
  bool get advertisingEnabled => _flag('advertising_enabled');
  bool get paymentsEnabled => _flag('payments_enabled');
  bool get autoApprove => _flag('moderation_auto_approve');
  int get railSize => _int('feed_rail_size');
  String get supportEmail => _string('support_email');
  bool get isReady => _ready;

  /// Called after the first frame. Never awaited by bootstrap.
  Future<void> init() async {
    try {
      await _remote.setDefaults(_defaults);
      await _remote.setConfigSettings(
        RemoteConfigSettings(
          fetchTimeout: const Duration(seconds: 8),
          minimumFetchInterval:
              AppConfig.isProd ? const Duration(hours: 1) : Duration.zero,
        ),
      );
      _ready = true;
      await _remote.fetchAndActivate();
    } catch (_) {
      // Offline, or a project without Remote Config: defaults stand in and the
      // app is fully usable.
      _ready = true;
    }
  }
}
