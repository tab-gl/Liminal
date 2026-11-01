import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Keychain / Keystore wrapper for the handful of values that must survive a
/// restart and must never sit in plain shared preferences.
///
/// Firebase Auth manages its own tokens; this store holds only local
/// preferences that are sensitive-ish (last payment method label, draft ids).
class SecureStore {
  SecureStore({FlutterSecureStorage? storage})
      : _storage = storage ??
            const FlutterSecureStorage(
              // v11 encrypts with the platform keystore by default; the old
              // `encryptedSharedPreferences` flag is gone.
              iOptions: IOSOptions(accessibility: KeychainAccessibility.first_unlock),
            );

  final FlutterSecureStorage _storage;

  static const String keyDraftId = 'sproutly.draft.application';
  static const String keyLastCampaign = 'sproutly.last.campaign';
  static const String keyOnboarded = 'sproutly.onboarded';

  Future<String?> read(String key) => _storage.read(key: key);

  Future<void> write(String key, String value) => _storage.write(key: key, value: value);

  Future<void> delete(String key) => _storage.delete(key: key);

  Future<void> clear() => _storage.deleteAll();
}
