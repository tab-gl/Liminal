import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../firebase/firestore_paths.dart';
import '../../../shared/models/application.dart';
import '../../publishing/data/publishing_repository_impl.dart';
import '../../publishing/domain/publishing_repository.dart';
import '../domain/dashboard_repository.dart';

class DashboardRepositoryImpl implements DashboardRepository {
  DashboardRepositoryImpl({
    required FirebaseFirestore firestore,
    required PublishingRepository publishing,
  })  : _db = firestore,
        _publishing = publishing;

  final FirebaseFirestore _db;
  final PublishingRepository _publishing;

  @override
  Future<DashboardSummary> summary({int days = 30}) async {
    return guard(() async {
      final List<AppListing> apps = await _publishing.myApps();
      final Map<String, DailyStat> merged = <String, DailyStat>{};
      for (final AppListing app in apps.where((AppListing a) => a.isLive)) {
        final List<DailyStat> series = await appSeries(app.id, days: days);
        for (final DailyStat stat in series) {
          final String key = _key(stat.date);
          final DailyStat existing = merged[key] ??
              DailyStat(date: stat.date);
          merged[key] = DailyStat(
            date: stat.date,
            views: existing.views + stat.views,
            uniqueViewers: existing.uniqueViewers + stat.uniqueViewers,
            clicks: existing.clicks + stat.clicks,
            downloads: existing.downloads + stat.downloads,
            bookmarks: existing.bookmarks + stat.bookmarks,
            reviews: existing.reviews + stat.reviews,
          );
        }
      }
      final List<DailyStat> ordered = merged.values.toList()
        ..sort((DailyStat a, DailyStat b) => a.date.compareTo(b.date));
      return DashboardSummary(apps: apps, days: ordered);
    });
  }

  @override
  Future<List<DailyStat>> appSeries(String applicationId, {int days = 30}) async {
    return guard(() async {
      final QuerySnapshot<Map<String, dynamic>> snap = await FirestorePaths.application(applicationId)
          .collection('analytics')
          .orderBy(FieldPath.documentId, descending: true)
          .limit(days)
          .get();
      final List<DailyStat> stats = snap.docs.map((QueryDocumentSnapshot<Map<String, dynamic>> doc) {
        final Map<String, dynamic> data = doc.data();
        return DailyStat(
          date: DateTime.tryParse(doc.id) ?? DateTime.now().toUtc(),
          views: (data['views'] as num?)?.toInt() ?? 0,
          uniqueViewers: (data['uniqueViewers'] as num?)?.toInt() ?? 0,
          clicks: (data['clicks'] as num?)?.toInt() ?? 0,
          downloads: (data['downloads'] as num?)?.toInt() ?? 0,
          bookmarks: (data['bookmarks'] as num?)?.toInt() ?? 0,
          reviews: (data['reviews'] as num?)?.toInt() ?? 0,
        );
      }).toList()
        ..sort((DailyStat a, DailyStat b) => a.date.compareTo(b.date));
      return stats;
    });
  }

  String _key(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

}

final Provider<DashboardRepository> dashboardRepositoryProvider =
    Provider<DashboardRepository>((Ref ref) {
  return DashboardRepositoryImpl(
    firestore: ref.watch(firestoreProvider),
    publishing: ref.watch(publishingRepositoryProvider),
  );
});

final FutureProvider<DashboardSummary> dashboardSummaryProvider =
    FutureProvider<DashboardSummary>((Ref ref) {
  return ref.watch(dashboardRepositoryProvider).summary();
});

final appSeriesProvider =
    FutureProvider.family<List<DailyStat>, String>((Ref ref, String applicationId) {
  return ref.watch(dashboardRepositoryProvider).appSeries(applicationId);
});
