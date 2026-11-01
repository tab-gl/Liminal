import '../../../shared/models/application.dart';

/// One day of an application's activity, as aggregated by Cloud Functions.
class DailyStat {
  const DailyStat({
    required this.date,
    this.views = 0,
    this.uniqueViewers = 0,
    this.clicks = 0,
    this.downloads = 0,
    this.bookmarks = 0,
    this.reviews = 0,
  });

  final DateTime date;
  final int views;
  final int uniqueViewers;
  final int clicks;
  final int downloads;
  final int bookmarks;
  final int reviews;
}

class DashboardSummary {
  const DashboardSummary({
    required this.apps,
    required this.days,
  });

  final List<AppListing> apps;
  final List<DailyStat> days;

  int get totalViews => days.fold(0, (int sum, DailyStat d) => sum + d.views);
  int get totalClicks => days.fold(0, (int sum, DailyStat d) => sum + d.clicks);
  int get totalDownloads => days.fold(0, (int sum, DailyStat d) => sum + d.downloads);
  int get totalBookmarks => days.fold(0, (int sum, DailyStat d) => sum + d.bookmarks);
  int get totalReviews => days.fold(0, (int sum, DailyStat d) => sum + d.reviews);
  double get ctr => totalViews == 0 ? 0 : totalClicks / totalViews;
  int get liveApps => apps.where((AppListing a) => a.isLive).length;
}

abstract class DashboardRepository {
  Future<DashboardSummary> summary({int days = 30});

  Future<List<DailyStat>> appSeries(String applicationId, {int days = 30});
}
