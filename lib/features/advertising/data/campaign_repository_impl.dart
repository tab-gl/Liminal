import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/callable_client.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/campaign.dart';
import '../domain/campaign_repository.dart';

class CampaignRepositoryImpl implements CampaignRepository {
  CampaignRepositoryImpl({
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
    if (uid == null) throw const AppException.unauthenticated('Sign in to advertise.');
    return uid;
  }

  @override
  Future<List<Campaign>> mine() async {
    final String uid = _requireUid();
    return guard(() async {
      final QuerySnapshot<Map<String, dynamic>> snap = await FirestorePaths.campaignsRef
          .where('ownerId', isEqualTo: uid)
          .orderBy('createdAt', descending: true)
          .limit(50)
          .get();
      return snap.docs
          .map((QueryDocumentSnapshot<Map<String, dynamic>> d) => Campaign.fromDoc(d.id, d.data()))
          .toList();
    });
  }

  @override
  Stream<Campaign?> watch(String campaignId) {
    return FirestorePaths.campaign(campaignId)
        .snapshots()
        .map((DocumentSnapshot<Map<String, dynamic>> snap) {
      final Map<String, dynamic>? data = snap.data();
      if (!snap.exists || data == null) return null;
      return Campaign.fromDoc(snap.id, data);
    });
  }

  @override
  Future<String> create(Campaign campaign) async {
    final String uid = _requireUid();
    return guard(() async {
      final DocumentReference<Map<String, dynamic>> ref = FirestorePaths.campaignsRef.doc();
      final Campaign owned = Campaign(
        id: ref.id,
        ownerId: uid,
        applicationId: campaign.applicationId,
        applicationName: campaign.applicationName,
        applicationIconUrl: campaign.applicationIconUrl,
        name: campaign.name,
        objective: campaign.objective,
        budgetPaise: campaign.budgetPaise,
        startDate: campaign.startDate,
        endDate: campaign.endDate,
        createdAt: DateTime.now().toUtc(),
        updatedAt: DateTime.now().toUtc(),
        targeting: campaign.targeting,
        placements: campaign.placements,
        creative: campaign.creative,
      );
      await ref.set(owned.toCreateMap());
      return ref.id;
    });
  }

  @override
  Future<void> pause(String campaignId) => guard(() async {
        await FirestorePaths.campaign(campaignId).update(<String, dynamic>{
          'status': 'paused',
          'updatedAt': FieldValue.serverTimestamp(),
        });
      });

  @override
  Future<void> resume(String campaignId) => guard(() async {
        await FirestorePaths.campaign(campaignId).update(<String, dynamic>{
          'status': 'active',
          'updatedAt': FieldValue.serverTimestamp(),
        });
      });

  @override
  Future<Map<String, dynamic>> track({
    required String campaignId,
    required String kind,
  }) =>
      _callables.call('trackCampaignEvent', <String, dynamic>{
        'campaignId': campaignId,
        'kind': kind,
      });
}

final Provider<CampaignRepository> campaignRepositoryProvider =
    Provider<CampaignRepository>((Ref ref) {
  return CampaignRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    uid: () => ref.read(currentUserProvider)?.uid,
    callables: ref.watch(callableProvider),
  );
});

final FutureProvider<List<Campaign>> myCampaignsProvider =
    FutureProvider<List<Campaign>>((Ref ref) {
  return ref.watch(campaignRepositoryProvider).mine();
});

final campaignProvider =
    StreamProvider.family<Campaign?, String>((Ref ref, String id) {
  return ref.watch(campaignRepositoryProvider).watch(id);
});
