import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/application.dart';
import '../domain/bookmark_repository.dart';

class BookmarkRepositoryImpl implements BookmarkRepository {
  BookmarkRepositoryImpl({
    required FirebaseFirestore firestore,
    required String? Function() uid,
  })  : _db = firestore,
        _uid = uid;

  final FirebaseFirestore _db;
  final String? Function() _uid;

  String _requireUid() {
    final String? uid = _uid();
    if (uid == null) throw const AppException.unauthenticated('Sign in to save apps.');
    return uid;
  }

  DocumentReference<Map<String, dynamic>> _doc(String applicationId) =>
      FirestorePaths.bookmarksRef.doc(FirestorePaths.bookmarkId(_requireUid(), applicationId));

  @override
  Stream<bool> watch(String applicationId) {
    final String? uid = _uid();
    if (uid == null) return Stream<bool>.value(false);
    return FirestorePaths.bookmarksRef
        .doc(FirestorePaths.bookmarkId(uid, applicationId))
        .snapshots()
        .map((DocumentSnapshot<Map<String, dynamic>> snap) => snap.exists);
  }

  @override
  Future<void> add({required String applicationId, required String appName}) async {
    final String uid = _requireUid();
    await guard(() async {
      await _doc(applicationId).set(<String, dynamic>{
        'userId': uid,
        'applicationId': applicationId,
        'applicationName': appName,
        'createdAt': FieldValue.serverTimestamp(),
      });
      // The counter on the application is maintained by a Cloud Function
      // trigger on this collection, never by the client.
    });
  }

  @override
  Future<void> remove(String applicationId) async {
    _requireUid();
    await guard(() => _doc(applicationId).delete());
  }

  @override
  Future<bool> toggle({required String applicationId, required String appName}) async {
    final bool exists = await guard(() async {
      final DocumentSnapshot<Map<String, dynamic>> snap = await _doc(applicationId).get();
      return snap.exists;
    });
    if (exists) {
      await remove(applicationId);
      return false;
    }
    await add(applicationId: applicationId, appName: appName);
    return true;
  }

  @override
  Future<List<AppListing>> saved() async {
    final String uid = _requireUid();
    return guard(() async {
      final QuerySnapshot<Map<String, dynamic>> bookmarks = await FirestorePaths.bookmarksRef
          .where('userId', isEqualTo: uid)
          .orderBy('createdAt', descending: true)
          .limit(100)
          .get();
      final List<String> ids = bookmarks.docs
          .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => d.data()['applicationId'] as String? ?? '')
          .where((String id) => id.isNotEmpty)
          .toList();
      if (ids.isEmpty) return const <AppListing>[];

      // Firestore has no join: fetch in chunks of 10 (the `whereIn` limit).
      final List<AppListing> listings = <AppListing>[];
      for (int i = 0; i < ids.length; i += 10) {
        final List<String> chunk = ids.sublist(i, i + 10 > ids.length ? ids.length : i + 10);
        final QuerySnapshot<Map<String, dynamic>> apps = await FirestorePaths.applicationsRef
            .where(FieldPath.documentId, whereIn: chunk)
            .get();
        for (final QueryDocumentSnapshot<Map<String, dynamic>> doc in apps.docs) {
          listings.add(AppListing.fromDoc(doc.id, doc.data()));
        }
      }
      // Preserve bookmark order rather than Firestore's document order.
      final Map<String, AppListing> byId = <String, AppListing>{
        for (final AppListing listing in listings) listing.id: listing,
      };
      return ids.map((String id) => byId[id]).whereType<AppListing>().toList();
    });
  }
}

final Provider<BookmarkRepository> bookmarkRepositoryProvider =
    Provider<BookmarkRepository>((Ref ref) {
  return BookmarkRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    uid: () => ref.read(currentUserProvider)?.uid,
  );
});

final bookmarkStatusProvider =
    StreamProvider.family<bool, String>((Ref ref, String applicationId) {
  return ref.watch(bookmarkRepositoryProvider).watch(applicationId);
});

final FutureProvider<List<AppListing>> savedAppsProvider =
    FutureProvider<List<AppListing>>((Ref ref) {
  return ref.watch(bookmarkRepositoryProvider).saved();
});
