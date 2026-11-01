import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/callable_client.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../../../shared/models/report.dart';
import '../domain/application_repository.dart';

class ApplicationRepositoryImpl implements ApplicationRepository {
  ApplicationRepositoryImpl({
    required FirebaseFirestore firestore,
    required String? Function() uid,
    required CallableClient callables,
  })  : _db = firestore,
        _uid = uid,
        _callables = callables;

  final FirebaseFirestore _db;
  final String? Function() _uid;
  final CallableClient _callables;

  @override
  Stream<AppListing?> watch(String applicationId) {
    return FirestorePaths.application(applicationId)
        .snapshots()
        .map((DocumentSnapshot<Map<String, dynamic>> snap) {
      final Map<String, dynamic>? data = snap.data();
      if (!snap.exists || data == null) return null;
      return AppListing.fromDoc(snap.id, data);
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

  @override
  Future<void> recordEvent({
    required String applicationId,
    required AppEventType type,
  }) async {
    final String? uid = _uid();
    if (uid == null) return; // Anonymous views are not tracked.
    try {
      // Ingested by the server, which validates the app and de-duplicates.
      await _callables.call('recordApplicationEvent', <String, dynamic>{
        'applicationId': applicationId,
        'type': type.wire,
        'platform': 'flutter',
      });
    } catch (_) {
      // Telemetry must never surface an error to the reader.
    }
  }

  @override
  Future<void> reportContent({
    required String targetId,
    required String target,
    required String reason,
    String? details,
  }) async {
    final String? uid = _uid();
    if (uid == null) throw const AppException.unauthenticated('Sign in to report content.');
    await guard(() async {
      await FirestorePaths.reportsRef.add(<String, dynamic>{
        'reporterId': uid,
        'target': target,
        'targetId': targetId,
        'reason': reason,
        'details': details,
        'status': 'open',
        'createdAt': FieldValue.serverTimestamp(),
      });
    });
  }

  @override
  Future<List<ContentReport>> myReports() async {
    final String? uid = _uid();
    if (uid == null) return const <ContentReport>[];
    return guard(() async {
      final QuerySnapshot<Map<String, dynamic>> snap = await FirestorePaths.reportsRef
          .where('reporterId', isEqualTo: uid)
          .orderBy('createdAt', descending: true)
          .limit(50)
          .get();
      return snap.docs
          .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => ContentReport.fromDoc(d.id, d.data()))
          .toList();
    });
  }
}

final Provider<ApplicationRepository> applicationRepositoryProvider =
    Provider<ApplicationRepository>((Ref ref) {
  return ApplicationRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    uid: () => ref.read(currentUserProvider)?.uid,
    callables: ref.watch(callableProvider),
  );
});

final applicationProvider =
    StreamProvider.family<AppListing?, String>((Ref ref, String id) {
  return ref.watch(applicationRepositoryProvider).watch(id);
});
