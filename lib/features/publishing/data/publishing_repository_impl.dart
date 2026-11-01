import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/callable_client.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../domain/publishing_repository.dart';

class PublishingRepositoryImpl implements PublishingRepository {
  PublishingRepositoryImpl({
    required FirebaseFirestore firestore,
    required String? Function() uid,
    required String? Function() displayName,
    required String? Function() photoUrl,
    required CallableClient callables,
  })  : _db = firestore,
        _uid = uid,
        _displayName = displayName,
        _photoUrl = photoUrl,
        _callables = callables;

  final FirebaseFirestore _db;
  final String? Function() _uid;
  final String? Function() _displayName;
  final String? Function() _photoUrl;
  final CallableClient _callables;

  String _requireUid() {
    final String? uid = _uid();
    if (uid == null) throw const AppException.unauthenticated('Sign in to publish an app.');
    return uid;
  }

  @override
  Future<String> save(AppListing draft) async {
    final String uid = _requireUid();
    return guard(() async {
      if (draft.id.isEmpty) {
        final DocumentReference<Map<String, dynamic>> ref =
            FirestorePaths.applicationsRef.doc();
        final AppListing withOwner = draft.copyWithStatusOwner(uid);
        await ref.set(withOwner.toCreateMap(
          ownerName: _displayName() ?? 'Independent developer',
          ownerPhotoUrl: _photoUrl(),
        ));
        return ref.id;
      }
      await FirestorePaths.application(draft.id).update(draft.toContentUpdate());
      return draft.id;
    });
  }

  @override
  Future<void> submit(String applicationId) => guard(() async {
        // The function re-validates the listing (links, media, duplicate names),
        // flips the status and queues moderator notifications. Doing it server
        // side keeps a tampered client from skipping the queue.
        await _callables.call('submitApplication', <String, dynamic>{
          'applicationId': applicationId,
        });
      });

  @override
  Future<void> withdraw(String applicationId) => guard(() async {
        await FirestorePaths.application(applicationId).update(<String, dynamic>{
          'status': ApplicationStatus.draft.wire,
          'updatedAt': FieldValue.serverTimestamp(),
        });
      });

  @override
  Future<void> deleteDraft(String applicationId) => guard(() async {
        await FirestorePaths.application(applicationId).delete();
      });

  @override
  Future<List<AppListing>> myApps() async {
    final String uid = _requireUid();
    return guard(() async {
      final QuerySnapshot<Map<String, dynamic>> snap = await FirestorePaths.applicationsRef
          .where('ownerId', isEqualTo: uid)
          .orderBy('updatedAt', descending: true)
          .limit(60)
          .get();
      return snap.docs
          .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => AppListing.fromDoc(d.id, d.data()))
          .toList();
    });
  }

  @override
  Future<AppListing?> fetch(String applicationId) => guard(() async {
        final DocumentSnapshot<Map<String, dynamic>> snap =
            await FirestorePaths.application(applicationId).get();
        final Map<String, dynamic>? data = snap.data();
        if (!snap.exists || data == null) return null;
        return AppListing.fromDoc(snap.id, data);
      });
}

extension on AppListing {
  /// Ensures the draft carries the signed-in owner before its first write.
  AppListing copyWithStatusOwner(String uid) => AppListing(
        id: id,
        ownerId: uid,
        ownerName: ownerName,
        name: name,
        tagline: tagline,
        description: description,
        categoryId: categoryId,
        createdAt: createdAt,
        updatedAt: updatedAt,
        tags: tags,
        iconUrl: iconUrl,
        bannerUrl: bannerUrl,
        screenshots: screenshots,
        previewImages: previewImages,
        websiteUrl: websiteUrl,
        launchUrl: launchUrl,
        downloadUrl: downloadUrl,
        platforms: platforms,
        pricingModel: pricingModel,
        priceLabel: priceLabel,
        isOpenSource: isOpenSource,
        features: features,
        documentationUrl: documentationUrl,
        supportEmail: supportEmail,
        privacyPolicyUrl: privacyPolicyUrl,
        termsUrl: termsUrl,
        status: status,
      );
}

final Provider<PublishingRepository> publishingRepositoryProvider =
    Provider<PublishingRepository>((Ref ref) {
  return PublishingRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    uid: () => ref.read(currentUserProvider)?.uid,
    displayName: () => ref.read(profileOrNullProvider)?.displayName,
    photoUrl: () => ref.read(profileOrNullProvider)?.photoUrl,
    callables: ref.watch(callableProvider),
  );
});
