import '../../../core/utils/page.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../../../shared/models/user_profile.dart';

enum DiscoverySort {
  newest,
  topRated,
  mostViewed,
  trending;

  String get label => switch (this) {
        DiscoverySort.newest => 'Newest',
        DiscoverySort.topRated => 'Top rated',
        DiscoverySort.mostViewed => 'Most viewed',
        DiscoverySort.trending => 'Trending',
      };
}

abstract class DiscoveryRepository {
  /// The main feed: approved listings, newest first by default, paginated.
  Future<Paged<AppListing>> feed({
    Object? cursor,
    int limit,
    DiscoverySort sort,
    String? categoryId,
    AppPlatform? platform,
  });

  /// Prefix + keyword search. Firestore has no full-text index, so this runs a
  /// title prefix range query and a keyword array-contains query, then merges
  /// them by id, keeping the best rank.
  Future<List<AppListing>> search(String query, {int limit});

  Future<List<AppListing>> featured({int limit});

  Future<List<AppListing>> trending({int limit});

  Future<List<AppListing>> recentlyLaunched({int limit});

  Future<List<AppListing>> editorsPicks({int limit});

  Future<List<AppListing>> risingStartups({int limit});

  Future<List<UserProfile>> popularDevelopers({int limit});

  /// Sponsored placements are chosen and priced on the server; the client only
  /// renders what it is given and reports the impression.
  Future<List<AppListing>> sponsored({required AdPlacement placement, int limit});
}
