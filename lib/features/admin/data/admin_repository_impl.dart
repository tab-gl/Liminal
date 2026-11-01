import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/callable_client.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/campaign.dart';
import '../../../shared/models/enums.dart';
import '../../../shared/models/report.dart';
import '../../../shared/models/user_profile.dart';
import '../domain/admin_repository.dart';

class AdminRepositoryImpl implements AdminRepository {
  AdminRepositoryImpl({
    required FirebaseFirestore firestore,
    required CallableClient callables,
  })  : _db = firestore,
        _callables = callables;

  final FirebaseFirestore _db;
  final CallableClient _callables;

  @override
  Future<List<AppListing>> pendingApplications() => guard(() async {
        final QuerySnapshot<Map<String, dynamic>> snap = await FirestorePaths.applicationsRef
            .where('status', isEqualTo: ApplicationStatus.pendingReview.wire)
            .orderBy('updatedAt')
            .limit(50)
            .get();
        return snap.docs
            .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => AppListing.fromDoc(d.id, d.data()))
            .toList();
      });

  @override
  Future<void> moderateApplication({
    required String applicationId,
    required bool approve,
    String? reason,
  }) =>
      _callables.call('moderateApplication', <String, dynamic>{
        'applicationId': applicationId,
        'approve': approve,
        'reason': reason,
      });

  @override
  Future<List<Campaign>> pendingCampaigns() => guard(() async {
        final QuerySnapshot<Map<String, dynamic>> snap = await FirestorePaths.campaignsRef
            .where('status', isEqualTo: CampaignStatus.pendingApproval.wire)
            .orderBy('createdAt')
            .limit(50)
            .get();
        return snap.docs
            .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => Campaign.fromDoc(d.id, d.data()))
            .toList();
      });

  @override
  Future<void> moderateCampaign({
    required String campaignId,
    required bool approve,
    String? reason,
  }) =>
      _callables.call('moderateCampaign', <String, dynamic>{
        'campaignId': campaignId,
        'approve': approve,
        'reason': reason,
      });

  @override
  Future<List<ContentReport>> openReports() => guard(() async {
        final QuerySnapshot<Map<String, dynamic>> snap = await FirestorePaths.reportsRef
            .where('status', isEqualTo: 'open')
            .orderBy('createdAt', descending: true)
            .limit(50)
            .get();
        return snap.docs
            .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => ContentReport.fromDoc(d.id, d.data()))
            .toList();
      });

  @override
  Future<void> resolveReport({
    required String reportId,
    required bool uphold,
    String? note,
  }) =>
      _callables.call('resolveReport', <String, dynamic>{
        'reportId': reportId,
        'uphold': uphold,
        'note': note,
      });

  @override
  Future<List<UserProfile>> users({String? query}) => guard(() async {
        Query<Map<String, dynamic>> ref = FirestorePaths.usersRef;
        final String term = (query ?? '').trim().toLowerCase();
        if (term.isNotEmpty) {
          ref = ref
              .where('displayName', isGreaterThanOrEqualTo: query!.trim())
              .where('displayName', isLessThanOrEqualTo: '${query.trim()}\uf8ff');
        }
        final QuerySnapshot<Map<String, dynamic>> snap = await ref.limit(50).get();
        return snap.docs
            .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => UserProfile.fromMap(d.id, d.data()))
            .toList();
      });

  @override
  Future<void> setRole({required String uid, required String role}) =>
      _callables.call('setUserRole', <String, dynamic>{'uid': uid, 'role': role});

  @override
  Future<Map<String, dynamic>> platformStats() => _callables.call('platformStats');
}

final Provider<AdminRepository> adminRepositoryProvider = Provider<AdminRepository>((Ref ref) {
  return AdminRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    callables: ref.watch(callableProvider),
  );
});

final FutureProvider<List<AppListing>> pendingApplicationsProvider =
    FutureProvider<List<AppListing>>((Ref ref) {
  return ref.watch(adminRepositoryProvider).pendingApplications();
});

final FutureProvider<List<Campaign>> pendingCampaignsProvider =
    FutureProvider<List<Campaign>>((Ref ref) {
  return ref.watch(adminRepositoryProvider).pendingCampaigns();
});

final FutureProvider<List<ContentReport>> openReportsProvider =
    FutureProvider<List<ContentReport>>((Ref ref) {
  return ref.watch(adminRepositoryProvider).openReports();
});

final adminUsersProvider =
    FutureProvider.family<List<UserProfile>, String>((Ref ref, String query) {
  return ref.watch(adminRepositoryProvider).users(query: query);
});
