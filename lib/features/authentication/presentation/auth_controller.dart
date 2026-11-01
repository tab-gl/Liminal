import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../shared/models/user_profile.dart';
import '../data/auth_repository_impl.dart';
import '../domain/auth_repository.dart';

/// Drives the authentication screens.
///
/// State is the *last error*, not the loading flag: each screen owns its own
/// button spinner, and errors need to survive long enough to be shown in a
/// snackbar or an inline banner.
class AuthController extends Notifier<AppException?> {
  @override
  AppException? build() => null;

  AuthRepository get _repo => ref.read(authRepositoryProvider);

  Future<bool> signIn({required String email, required String password}) =>
      _run(() => _repo.signInWithEmail(email: email, password: password));

  Future<bool> signUp({
    required String email,
    required String password,
    required String displayName,
  }) =>
      _run(() => _repo.signUpWithEmail(
            email: email,
            password: password,
            displayName: displayName,
          ));

  Future<bool> signInWithGoogle() => _run(_repo.signInWithGoogle);

  Future<bool> sendPasswordReset(String email) => _run(() async {
        await _repo.sendPasswordReset(email);
        return null;
      });

  Future<bool> sendVerification() => _run(() async {
        await _repo.sendEmailVerification();
        return null;
      });

  Future<bool> signOut() => _run(() async {
        await _repo.signOut();
        return null;
      });

  Future<bool> deleteAccount() => _run(() async {
        await _repo.deleteAccount();
        return null;
      });

  Future<bool> updateProfile({
    String? displayName,
    String? bio,
    String? websiteUrl,
    String? photoUrl,
    bool? isPublic,
    bool? onboarded,
    Map<String, String>? links,
  }) =>
      _run(() async {
        await _repo.updateProfile(
          displayName: displayName,
          bio: bio,
          websiteUrl: websiteUrl,
          photoUrl: photoUrl,
          isPublic: isPublic,
          onboarded: onboarded,
          links: links,
        );
        return null;
      });

  Future<bool> requestDeveloperAccess(String motivation) => _run(() async {
        await _repo.requestDeveloperAccess(motivation: motivation);
        return null;
      });

  /// Runs an auth action, records any failure and reports success.
  Future<bool> _run(Future<UserProfile?> Function() action) async {
    state = null;
    try {
      await action();
      // Keep the analytics identity in step with the session.
      await ref.read(analyticsProvider).setUser(_repo.currentUid);
      return true;
    } catch (error, stack) {
      state = mapError(error, stack);
      return false;
    }
  }

  void clearError() => state = null;
}

final NotifierProvider<AuthController, AppException?> authControllerProvider =
    NotifierProvider<AuthController, AppException?>(AuthController.new);
