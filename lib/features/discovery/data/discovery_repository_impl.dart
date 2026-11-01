import 'dart:ui' show PlatformDispatcher;

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/foundation.dart' show TargetPlatform, defaultTargetPlatform;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/utils/page.dart';
import '../../../firebase/callable_client.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../../../shared/models/user_profile.dart';
import '../domain/discovery_repository.dart';

class DiscoveryRepositoryImpl implements DiscoveryRepository {
  DiscoveryRepositoryImpl({
    required FirebaseFirestore firestore,
    required CallableClient callables,
  })  : _db = firestore,
        _callables = callables;

  final FirebaseFirestore _db;
  final CallableClient _callables;

  Query<Map<String, dynamic>> _liveQuery() =>
      FirestorePaths.applicationsRef.where('status', isEqualTo: ApplicationStatus.approved.wire);

  Query<Map<String, dynamic>> _sorted(Query<Map<String, dynamic>> query, DiscoverySort sort) {
    switch (sort) {
      case DiscoverySort.newest:
        return query.orderBy('createdAt', descending: true);
      case DiscoverySort.topRated:
        return query.orderBy('rating', descending: true);
      case DiscoverySort.mostViewed:
        return query.orderBy('viewCount', descending: true);
      case DiscoverySort.trending:
        return query.orderBy('trendingScore', descending: true);
    }
  }

  @override
  Future<Paged<AppListing>> feed({
    Object? cursor,
    int limit = 12,
    DiscoverySort sort = DiscoverySort.newest,
    String? categoryId,
    AppPlatform? platform,
  }) async {
    return guard(() async {
      Query<Map<String, dynamic>> query = _liveQuery();
      if (categoryId != null && categoryId.isNotEmpty) {
        query = query.where('categoryId', isEqualTo: categoryId);
      }
      if (platform != null) {
        query = query.where('platforms', arrayContains: platform.name);
      }
      query = _sorted(query, sort).limit(limit);
      if (cursor is DocumentSnapshot<Map<String, dynamic>>) {
        query = query.startAfterDocument(cursor);
      }
      final QuerySnapshot<Map<String, dynamic>> snapshot = await query.get();
      final List<AppListing> items = snapshot.docs
          .map((QueryDocumentSnapshot<Map<String, dynamic>> doc) =>
              AppListing.fromDoc(doc.id, doc.data()))
          .toList();
      return Paged<AppListing>(
        items: items,
        hasMore: snapshot.docs.length == limit,
        cursor: snapshot.docs.isEmpty ? cursor : snapshot.docs.last,
      );
    });
  }

  @override
  Future<List<AppListing>> search(String query, {int limit = 20}) async {
    final String term = query.trim().toLowerCase();
    if (term.isEmpty) return const <AppListing>[];

    return guard(() async {
      final List<Query<Map<String, dynamic>>> queries = <Query<Map<String, dynamic>>>[
        // Title prefix. \uf8ff is the highest code point, so the range covers
        // every string starting with `term`.
        _liveQuery()
            .where('search.title', isGreaterThanOrEqualTo: term)
            .where('search.title', isLessThanOrEqualTo: '$term\uf8ff')
            .orderBy('search.title')
            .limit(limit),
        // Tag / keyword match.
        _liveQuery()
            .where('search.keywords', arrayContains: term)
            .orderBy('createdAt', descending: true)
            .limit(limit),
      ];

      final List<QuerySnapshot<Map<String, dynamic>>> results =
          await Future.wait(queries.map((Query<Map<String, dynamic>> q) => q.get()));

      final Map<String, AppListing> merged = <String, AppListing>{};
      for (final QuerySnapshot<Map<String, dynamic>> snapshot in results) {
        for (final QueryDocumentSnapshot<Map<String, dynamic>> doc in snapshot.docs) {
          merged.putIfAbsent(doc.id, () => AppListing.fromDoc(doc.id, doc.data()));
        }
      }
      final List<AppListing> items = merged.values.toList()
        ..sort((AppListing a, AppListing b) => b.trendingScore.compareTo(a.trendingScore));
      return items;
    });
  }

  @override
  Future<List<AppListing>> featured({int limit = 10}) => _rail(
        _liveQuery().where('featured', isEqualTo: true),
        field: 'trendingScore',
        limit: limit,
      );

  @override
  Future<List<AppListing>> trending({int limit = 10}) =>
      _rail(_liveQuery(), field: 'trendingScore', limit: limit);

  @override
  Future<List<AppListing>> recentlyLaunched({int limit = 10}) =>
      _rail(_liveQuery(), field: 'createdAt', limit: limit);

  @override
  Future<List<AppListing>> editorsPicks({int limit = 10}) => _rail(
        _liveQuery().where('featured', isEqualTo: true).where('rating', isGreaterThanOrEqualTo: 4),
        field: 'rating',
        limit: limit,
      );

  @override
  Future<List<AppListing>> risingStartups({int limit = 10}) async {
    // Young apps that are already gaining traction: created in the last 60 days
    // and ranked by trending score. Cheap, indexed, and honest - no fabricated
    // "momentum" number.
    final DateTime cutoff = DateTime.now().toUtc().subtract(const Duration(days: 60));
    return _rail(
      _liveQuery().where('createdAt', isGreaterThanOrEqualTo: Timestamp.fromDate(cutoff)),
      field: 'trendingScore',
      limit: limit,
    );
  }

  Future<List<AppListing>> _rail(
    Query<Map<String, dynamic>> base, {
    required String field,
    required int limit,
  }) async {
    return guard(() async {
      final QuerySnapshot<Map<String, dynamic>> snapshot =
          await base.orderBy(field, descending: true).limit(limit).get();
      return snapshot.docs
          .map((QueryDocumentSnapshot<Map<String, dynamic>> doc) =>
              AppListing.fromDoc(doc.id, doc.data()))
          .toList();
    });
  }

  @override
  Future<List<UserProfile>> popularDevelopers({int limit = 10}) async {
    return guard(() async {
      final QuerySnapshot<Map<String, dynamic>> snapshot = await FirestorePaths.usersRef
          // Firestore rules are not filters: prove that each returned profile is public.
          .where('isPublic', isEqualTo: true)
          .where('publishedAppCount', isGreaterThan: 0)
          .orderBy('publishedAppCount', descending: true)
          .limit(limit)
          .get();
      return snapshot.docs
          .map((QueryDocumentSnapshot<Map<String, dynamic>> doc) =>
              UserProfile.fromMap(doc.id, doc.data()))
          .toList();
    });
  }

  @override
  Future<List<AppListing>> sponsored({
    required AdPlacement placement,
    int limit = 3,
    String? categoryId,
  }) async {
    // Server-side selection: the function enforces approval, budget, targeting
    // and pacing, and returns app ids it is willing to serve right now.
    try {
      final Map<String, dynamic> response = await _callables.call('serveAds', <String, dynamic>{
        'placement': placement.name,
        'limit': limit,
        // Delivery context the server matches campaign targeting against.
        'platform': defaultTargetPlatform == TargetPlatform.iOS ? 'ios' : 'android',
        'country': PlatformDispatcher.instance.locale.countryCode,
        if (categoryId != null) 'categoryId': categoryId,
      });
      final List<String> ids = (response['applicationIds'] as List<dynamic>? ?? <dynamic>[])
          .whereType<String>()
          .toList();
      if (ids.isEmpty) return const <AppListing>[];
      final List<DocumentSnapshot<Map<String, dynamic>>> docs =
          await Future.wait(ids.map((String id) => FirestorePaths.application(id).get()));
      return docs
          .where((DocumentSnapshot<Map<String, dynamic>> d) => d.exists && d.data() != null)
          .map((DocumentSnapshot<Map<String, dynamic>> d) => AppListing.fromDoc(d.id, d.data()!))
          .toList();
    } catch (_) {
      // An ad failure must never break discovery.
      return const <AppListing>[];
    }
  }
}

final Provider<DiscoveryRepository> discoveryRepositoryProvider =
    Provider<DiscoveryRepository>((Ref ref) {
  return DiscoveryRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    callables: ref.watch(callableProvider),
  );
});
