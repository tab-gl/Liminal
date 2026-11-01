import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/callable_client.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/review.dart';
import '../domain/review_repository.dart';

class ReviewRepositoryImpl implements ReviewRepository {
  ReviewRepositoryImpl({
    required FirebaseFirestore firestore,
    required String? Function() uid,
    required CallableClient callables,
  })  : _db = firestore,
        _uid = uid,
        _callables = callables;

  final FirebaseFirestore _db;
  final String? Function() _uid;
  final CallableClient _callables;

  String _requireUid() {
    final String? uid = _uid();
    if (uid == null) throw const AppException.unauthenticated('Sign in to review an app.');
    return uid;
  }

  @override
  Stream<List<Review>> watchForApplication(String applicationId, {int limit = 20}) {
    return FirestorePaths.reviewsRef
        .where('applicationId', isEqualTo: applicationId)
        .where('status', isEqualTo: 'published')
        .orderBy('createdAt', descending: true)
        .limit(limit)
        .snapshots()
        .map((QuerySnapshot<Map<String, dynamic>> snap) => snap.docs
            .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => Review.fromDoc(d.id, d.data()))
            .toList());
  }

  @override
  Future<Review?> mineFor(String applicationId) async {
    final String? uid = _uid();
    if (uid == null) return null;
    return guard(() async {
      final DocumentSnapshot<Map<String, dynamic>> snap =
          await FirestorePaths.reviewsRef.doc(FirestorePaths.reviewId(applicationId, uid)).get();
      final Map<String, dynamic>? data = snap.data();
      if (!snap.exists || data == null) return null;
      return Review.fromDoc(snap.id, data);
    });
  }

  @override
  Future<void> create({
    required String applicationId,
    required int rating,
    required String body,
  }) async {
    final String uid = _requireUid();
    final UserProfileLike me = await _me();
    await guard(() async {
      await FirestorePaths.reviewsRef
          .doc(FirestorePaths.reviewId(applicationId, uid))
          .set(<String, dynamic>{
        'applicationId': applicationId,
        'userId': uid,
        'userName': me.name,
        'userPhotoUrl': me.photoUrl,
        'rating': rating,
        'body': body.trim(),
        'helpfulCount': 0,
        'status': 'published',
        'createdAt': FieldValue.serverTimestamp(),
        'updatedAt': FieldValue.serverTimestamp(),
      });
    });
  }

  @override
  Future<void> update({
    required String applicationId,
    required int rating,
    required String body,
  }) async {
    final String uid = _requireUid();
    await guard(() async {
      await FirestorePaths.reviewsRef
          .doc(FirestorePaths.reviewId(applicationId, uid))
          .update(<String, dynamic>{
        'rating': rating,
        'body': body.trim(),
        'updatedAt': FieldValue.serverTimestamp(),
      });
    });
  }

  @override
  Future<void> delete(String applicationId) async {
    final String uid = _requireUid();
    await guard(() async {
      await FirestorePaths.reviewsRef.doc(FirestorePaths.reviewId(applicationId, uid)).delete();
    });
  }

  @override
  Future<void> respond({required String reviewId, required String body}) async {
    _requireUid();
    await _callables.call('respondToReview', <String, dynamic>{
      'reviewId': reviewId,
      'body': body.trim(),
    });
  }

  Future<UserProfileLike> _me() async {
    final String uid = _requireUid();
    final DocumentSnapshot<Map<String, dynamic>> snap = await FirestorePaths.user(uid).get();
    final Map<String, dynamic> data = snap.data() ?? const <String, dynamic>{};
    return UserProfileLike(
      name: (data['displayName'] as String?) ?? 'Sproutly member',
      photoUrl: data['photoUrl'] as String?,
    );
  }
}

class UserProfileLike {
  const UserProfileLike({required this.name, this.photoUrl});

  final String name;
  final String? photoUrl;
}

final Provider<ReviewRepository> reviewRepositoryProvider = Provider<ReviewRepository>((Ref ref) {
  return ReviewRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    uid: () => ref.read(currentUserProvider)?.uid,
    callables: ref.watch(callableProvider),
  );
});

final applicationReviewsProvider =
    StreamProvider.family<List<Review>, String>((Ref ref, String applicationId) {
  return ref.watch(reviewRepositoryProvider).watchForApplication(applicationId);
});
