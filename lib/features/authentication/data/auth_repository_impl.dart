import 'dart:async';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/callable_client.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/user_profile.dart';
import '../domain/auth_repository.dart';

class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl({
    required FirebaseAuth auth,
    required FirebaseFirestore firestore,
    required CallableClient callables,
  })  : _auth = auth,
        _firestore = firestore,
        _callables = callables;

  final FirebaseAuth _auth;
  final FirebaseFirestore _firestore;
  final CallableClient _callables;

  @override
  Stream<String?> get uidChanges => _auth.authStateChanges().map((User? u) => u?.uid);

  @override
  String? get currentUid => _auth.currentUser?.uid;

  @override
  bool get isEmailVerified => _auth.currentUser?.emailVerified ?? false;

  @override
  Future<UserProfile> signInWithEmail({
    required String email,
    required String password,
  }) async {
    try {
      final UserCredential credential = await _auth.signInWithEmailAndPassword(
        email: email.trim(),
        password: password,
      );
      return _profileFromCredential(credential);
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  @override
  Future<UserProfile> signUpWithEmail({
    required String email,
    required String password,
    required String displayName,
  }) async {
    try {
      final UserCredential credential = await _auth.createUserWithEmailAndPassword(
        email: email.trim(),
        password: password,
      );
      final User? user = credential.user;
      if (user == null) {
        throw const AppException(AppErrorCode.unknown, 'The account could not be created.');
      }
      await user.updateDisplayName(displayName.trim());
      final UserProfile profile = await _createProfile(user, displayName.trim());
      await user.sendEmailVerification();
      return profile;
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  @override
  Future<UserProfile> signInWithGoogle() async {
    try {
      final UserCredential credential = await _auth.signInWithProvider(GoogleAuthProvider());
      return _profileFromCredential(credential);
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  @override
  Future<void> sendEmailVerification() async {
    final User? user = _auth.currentUser;
    if (user == null || user.emailVerified) return;
    await guard(() => user.sendEmailVerification());
  }

  @override
  Future<void> sendPasswordReset(String email) =>
      guard(() => _auth.sendPasswordResetEmail(email: email.trim()));

  @override
  Future<void> reloadUser() async {
    try {
      await _auth.currentUser?.reload();
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  @override
  Future<void> signOut() => guard(() => _auth.signOut());

  @override
  Future<void> deleteAccount() async {
    final User? user = _auth.currentUser;
    if (user == null) return;
    try {
      // The callable removes the profile, listings and campaigns first; the
      // auth record can only be deleted by the client that owns the session.
      await _callables.call('deleteAccount');
      await user.delete();
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  @override
  Future<UserProfile> ensureProfile() async {
    final User? user = _auth.currentUser;
    if (user == null) throw const AppException.unauthenticated();
    final DocumentReference<Map<String, dynamic>> ref = FirestorePaths.user(user.uid);
    try {
      final DocumentSnapshot<Map<String, dynamic>> snap = await ref.get();
      if (snap.exists) {
        final Map<String, dynamic> data = snap.data()!;
        final String storedName = (data['displayName'] as String?) ?? '';
        if (storedName.isEmpty && (user.displayName ?? '').isNotEmpty) {
          await ref.update(<String, dynamic>{
            'displayName': user.displayName,
            'updatedAt': FieldValue.serverTimestamp(),
          });
          data['displayName'] = user.displayName;
        }
        return UserProfile.fromMap(user.uid, data);
      }
      return _createProfile(user, user.displayName ?? _nameFromEmail(user.email));
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  @override
  Future<void> updateProfile({
    String? displayName,
    String? bio,
    String? websiteUrl,
    String? photoUrl,
    bool? isPublic,
    bool? onboarded,
    Map<String, String>? links,
  }) async {
    final User? user = _auth.currentUser;
    if (user == null) throw const AppException.unauthenticated();
    final Map<String, dynamic> patch = <String, dynamic>{
      if (displayName != null) 'displayName': displayName.trim(),
      if (bio != null) 'bio': bio.trim(),
      if (websiteUrl != null) 'websiteUrl': websiteUrl.trim(),
      if (photoUrl != null) 'photoUrl': photoUrl,
      if (isPublic != null) 'isPublic': isPublic,
      if (onboarded != null) 'onboarded': onboarded,
      if (links != null) 'links': links,
      'updatedAt': FieldValue.serverTimestamp(),
    };
    try {
      await FirestorePaths.user(user.uid).update(patch);
      if (displayName != null) await user.updateDisplayName(displayName.trim());
      if (photoUrl != null) await user.updatePhotoURL(photoUrl);
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  @override
  Future<void> requestDeveloperAccess({required String motivation}) async {
    await _callables.call('requestDeveloperAccess', <String, dynamic>{
      'motivation': motivation,
    });
  }

  Future<UserProfile> _profileFromCredential(UserCredential credential) async {
    final User? user = credential.user;
    if (user == null) throw const AppException(AppErrorCode.unknown, 'Sign-in did not complete.');
    return ensureProfile();
  }

  Future<UserProfile> _createProfile(User user, String displayName) async {
    final DateTime now = DateTime.now().toUtc();
    final UserProfile profile = UserProfile(
      uid: user.uid,
      displayName: displayName.isEmpty ? _nameFromEmail(user.email) : displayName,
      photoUrl: user.photoURL,
      createdAt: now,
      updatedAt: now,
    );
    try {
      await FirestorePaths.user(user.uid).set(profile.toCreateMap(now));
      return profile;
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  static String _nameFromEmail(String? email) {
    if (email == null || !email.contains('@')) return 'Sproutly member';
    return email.split('@').first.replaceAll(RegExp(r'[._-]+'), ' ');
  }
}

final Provider<AuthRepository> authRepositoryProvider = Provider<AuthRepository>((Ref ref) {
  return AuthRepositoryImpl(
    auth: ref.watch(firebaseAuthProvider),
    firestore: ref.watch(firestoreProvider),
    callables: ref.watch(callableProvider),
  );
});
