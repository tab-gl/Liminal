import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../../../shared/models/user_profile.dart';
import '../domain/profile_repository.dart';

class ProfileRepositoryImpl implements ProfileRepository {
  ProfileRepositoryImpl({
    required FirebaseFirestore firestore,
    required String? Function() uid,
  })  : _db = firestore,
        _uid = uid;

  final FirebaseFirestore _db;
  final String? Function() _uid;

  @override
  Stream<UserProfile?> watch(String uid) => FirestorePaths.user(uid)
      .snapshots()
      .map((DocumentSnapshot<Map<String, dynamic>> snap) {
    final Map<String, dynamic>? data = snap.data();
    if (!snap.exists || data == null) return null;
    return UserProfile.fromMap(snap.id, data);
  });

  @override
  Future<UserProfile?> fetch(String uid) => guard(() async {
        final DocumentSnapshot<Map<String, dynamic>> snap = await FirestorePaths.user(uid).get();
        final Map<String, dynamic>? data = snap.data();
        if (!snap.exists || data == null) return null;
        return UserProfile.fromMap(snap.id, data);
      });

  @override
  Future<List<AppListing>> appsBy(String developerId, {int limit = 30}) async {
    return guard(() async {
      final QuerySnapshot<Map<String, dynamic>> snap = await FirestorePaths.applicationsRef
          .where('ownerId', isEqualTo: developerId)
          .where('status', isEqualTo: ApplicationStatus.approved.wire)
          .orderBy('createdAt', descending: true)
          .limit(limit)
          .get();
      return snap.docs
          .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => AppListing.fromDoc(d.id, d.data()))
          .toList();
    });
  }

  @override
  Stream<bool> watchFollowing(String developerId) {
    final String? uid = _uid();
    if (uid == null) return Stream<bool>.value(false);
    return FirestorePaths.followsRef
        .doc(FirestorePaths.followId(uid, developerId))
        .snapshots()
        .map((DocumentSnapshot<Map<String, dynamic>> snap) => snap.exists);
  }

  @override
  Future<bool> toggleFollow(String developerId) async {
    final String? uid = _uid();
    if (uid == null) throw const AppException.unauthenticated('Sign in to follow developers.');
    if (uid == developerId) {
      throw const AppException(AppErrorCode.invalidInput, 'You cannot follow yourself.');
    }
    final DocumentReference<Map<String, dynamic>> ref =
        FirestorePaths.followsRef.doc(FirestorePaths.followId(uid, developerId));
    return guard(() async {
      final DocumentSnapshot<Map<String, dynamic>> snap = await ref.get();
      if (snap.exists) {
        await ref.delete();
        return false;
      }
      await ref.set(<String, dynamic>{
        'followerId': uid,
        'developerId': developerId,
        'createdAt': FieldValue.serverTimestamp(),
      });
      return true;
    });
  }

  @override
  Future<int> followerCount(String developerId) async {
    return guard(() async {
      final snapshot =
          await FirestorePaths.followsRef.where('developerId', isEqualTo: developerId).count().get();
      return snapshot.count ?? 0;
    });
  }
}

final Provider<ProfileRepository> profileRepositoryProvider =
    Provider<ProfileRepository>((Ref ref) {
  return ProfileRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    uid: () => ref.read(currentUserProvider)?.uid,
  );
});

final profileByIdProvider =
    StreamProvider.family<UserProfile?, String>((Ref ref, String uid) {
  return ref.watch(profileRepositoryProvider).watch(uid);
});

final developerAppsProvider =
    FutureProvider.family<List<AppListing>, String>((Ref ref, String uid) {
  return ref.watch(profileRepositoryProvider).appsBy(uid);
});

final followingProvider =
    StreamProvider.family<bool, String>((Ref ref, String developerId) {
  return ref.watch(profileRepositoryProvider).watchFollowing(developerId);
});
