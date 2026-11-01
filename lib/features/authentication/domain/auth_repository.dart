import '../../../shared/models/user_profile.dart';

/// Authentication contract. The presentation layer depends on this, never on
/// FirebaseAuth directly, so sign-in can be swapped or faked in tests.
abstract class AuthRepository {
  Stream<String?> get uidChanges;

  String? get currentUid;

  bool get isEmailVerified;

  Future<UserProfile> signInWithEmail({
    required String email,
    required String password,
  });

  Future<UserProfile> signUpWithEmail({
    required String email,
    required String password,
    required String displayName,
  });

  /// Google via the platform OAuth flow (no separate Google SDK dependency).
  Future<UserProfile> signInWithGoogle();

  Future<void> sendEmailVerification();

  Future<void> sendPasswordReset(String email);

  Future<void> reloadUser();

  Future<void> signOut();

  Future<void> deleteAccount();

  /// Creates the Firestore profile document on first sign-in if it is missing,
  /// and keeps the denormalised display name in sync afterwards.
  Future<UserProfile> ensureProfile();

  Future<void> updateProfile({
    String? displayName,
    String? bio,
    String? websiteUrl,
    String? photoUrl,
    bool? isPublic,
    bool? onboarded,
    Map<String, String>? links,
  });

  /// Requests the `developer` role. Approval is an admin action; the client can
  /// only ask, and the flag it may set is `requestedDeveloper`, not `role`.
  Future<void> requestDeveloperAccess({required String motivation});
}
