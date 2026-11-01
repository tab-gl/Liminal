import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/app_notification.dart';
import '../domain/notification_repository.dart';

class NotificationRepositoryImpl implements NotificationRepository {
  NotificationRepositoryImpl({
    required FirebaseFirestore firestore,
    required String? Function() uid,
  })  : _db = firestore,
        _uid = uid;

  final FirebaseFirestore _db;
  final String? Function() _uid;

  @override
  Stream<List<AppNotification>> watchInbox({int limit = 50}) {
    final String? uid = _uid();
    if (uid == null) return Stream<List<AppNotification>>.value(const <AppNotification>[]);
    return FirestorePaths.notificationsRef
        .where('userId', isEqualTo: uid)
        .orderBy('createdAt', descending: true)
        .limit(limit)
        .snapshots()
        .map((QuerySnapshot<Map<String, dynamic>> snap) => snap.docs
            .map((QueryDocumentSnapshot<Map<String, dynamic>> d) =>
                AppNotification.fromDoc(d.id, d.data()))
            .toList());
  }

  @override
  Stream<int> watchUnreadCount() {
    final String? uid = _uid();
    if (uid == null) return Stream<int>.value(0);
    return FirestorePaths.notificationsRef
        .where('userId', isEqualTo: uid)
        .where('readAt', isNull: true)
        .snapshots()
        .map((QuerySnapshot<Map<String, dynamic>> snap) => snap.docs.length);
  }

  @override
  Future<void> markRead(String notificationId) => guard(() async {
        await FirestorePaths.notification(notificationId).update(<String, dynamic>{
          'readAt': FieldValue.serverTimestamp(),
        });
      });

  @override
  Future<void> markAllRead() async {
    final String? uid = _uid();
    if (uid == null) return;
    await guard(() async {
      final QuerySnapshot<Map<String, dynamic>> unread = await FirestorePaths.notificationsRef
          .where('userId', isEqualTo: uid)
          .where('readAt', isNull: true)
          .limit(200)
          .get();
      if (unread.docs.isEmpty) return;
      final WriteBatch batch = _db.batch();
      for (final QueryDocumentSnapshot<Map<String, dynamic>> doc in unread.docs) {
        batch.update(doc.reference, <String, dynamic>{'readAt': FieldValue.serverTimestamp()});
      }
      await batch.commit();
    });
  }
}

final Provider<NotificationRepository> notificationRepositoryProvider =
    Provider<NotificationRepository>((Ref ref) {
  return NotificationRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    uid: () => ref.read(currentUserProvider)?.uid,
  );
});

final StreamProvider<List<AppNotification>> inboxProvider =
    StreamProvider<List<AppNotification>>((Ref ref) {
  return ref.watch(notificationRepositoryProvider).watchInbox();
});

final StreamProvider<int> unreadCountProvider = StreamProvider<int>((Ref ref) {
  return ref.watch(notificationRepositoryProvider).watchUnreadCount();
});
