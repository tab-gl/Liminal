import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/providers/pagination_provider.dart';
import '../../../core/utils/page.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../../../shared/models/user_profile.dart';
import '../data/discovery_repository_impl.dart';
import '../domain/discovery_repository.dart';

/// Sort order for the main feed. Riverpod 3 dropped `StateProvider`, so this is
/// a plain notifier with one setter - same ergonomics, no legacy import.
class FeedSortController extends Notifier<DiscoverySort> {
  @override
  DiscoverySort build() => DiscoverySort.newest;

  void set(DiscoverySort value) => state = value;
}

final NotifierProvider<FeedSortController, DiscoverySort> feedSortProvider =
    NotifierProvider<FeedSortController, DiscoverySort>(FeedSortController.new);

class FeedPlatformController extends Notifier<AppPlatform?> {
  @override
  AppPlatform? build() => null;

  void set(AppPlatform? value) => state = value;
}

final NotifierProvider<FeedPlatformController, AppPlatform?> feedPlatformProvider =
    NotifierProvider<FeedPlatformController, AppPlatform?>(FeedPlatformController.new);

/// The infinite main feed.
class DiscoveryFeedController extends FeedController<AppListing> {
  @override
  int get pageSize => AppConstants.feedPageSize;

  @override
  Future<Paged<AppListing>> fetchPage({Object? cursor, required int limit}) {
    return ref.read(discoveryRepositoryProvider).feed(
          cursor: cursor,
          limit: limit,
          sort: ref.read(feedSortProvider),
          platform: ref.read(feedPlatformProvider),
        );
  }
}

final AsyncNotifierProvider<DiscoveryFeedController, List<AppListing>> discoveryFeedProvider =
    AsyncNotifierProvider<DiscoveryFeedController, List<AppListing>>(DiscoveryFeedController.new);

final FutureProvider<List<AppListing>> featuredProvider =
    FutureProvider<List<AppListing>>((Ref ref) {
  return ref.watch(discoveryRepositoryProvider).featured(limit: 6);
});

final FutureProvider<List<AppListing>> trendingProvider =
    FutureProvider<List<AppListing>>((Ref ref) {
  return ref.watch(discoveryRepositoryProvider).trending(limit: AppConstants.railSize);
});

final FutureProvider<List<AppListing>> recentlyLaunchedProvider =
    FutureProvider<List<AppListing>>((Ref ref) {
  return ref.watch(discoveryRepositoryProvider).recentlyLaunched(limit: AppConstants.railSize);
});

final FutureProvider<List<AppListing>> editorsPicksProvider =
    FutureProvider<List<AppListing>>((Ref ref) {
  return ref.watch(discoveryRepositoryProvider).editorsPicks(limit: AppConstants.railSize);
});

final FutureProvider<List<AppListing>> risingStartupsProvider =
    FutureProvider<List<AppListing>>((Ref ref) {
  return ref.watch(discoveryRepositoryProvider).risingStartups(limit: AppConstants.railSize);
});

final FutureProvider<List<UserProfile>> popularDevelopersProvider =
    FutureProvider<List<UserProfile>>((Ref ref) {
  return ref.watch(discoveryRepositoryProvider).popularDevelopers(limit: AppConstants.railSize);
});

final FutureProvider<List<AppListing>> sponsoredRailProvider =
    FutureProvider<List<AppListing>>((Ref ref) {
  return ref.watch(discoveryRepositoryProvider).sponsored(placement: AdPlacement.discoveryRail);
});
