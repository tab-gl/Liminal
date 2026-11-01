import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/app_image.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/rating_stars.dart';
import '../../../core/widgets/section_header.dart';
import '../../../core/widgets/shape_morph_loader.dart';
import '../../../core/widgets/skeleton.dart';
import '../../../core/widgets/sproutly_banner_ad.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/user_profile.dart';
import '../../../shared/widgets/application_card.dart';
import '../domain/discovery_repository.dart';
import 'discovery_providers.dart';

class DiscoverScreen extends ConsumerStatefulWidget {
  const DiscoverScreen({super.key});

  @override
  ConsumerState<DiscoverScreen> createState() => _DiscoverScreenState();
}

class _DiscoverScreenState extends ConsumerState<DiscoverScreen> {
  final ScrollController _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scroll.removeListener(_onScroll);
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scroll.hasClients) return;
    if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 480) {
      ref.read(discoveryFeedProvider.notifier).loadMore();
    }
  }

  @override
  Widget build(BuildContext context) {
    final UserProfile? profile = ref.watch(profileOrNullProvider);
    final AsyncValue<List<AppListing>> feed = ref.watch(discoveryFeedProvider);
    final DiscoverySort sort = ref.watch(feedSortProvider);

    return Scaffold(
      backgroundColor: context.colors.surface,
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          color: context.colors.primary,
          onRefresh: () async {
            ref.invalidate(featuredProvider);
            ref.invalidate(trendingProvider);
            ref.invalidate(recentlyLaunchedProvider);
            ref.invalidate(editorsPicksProvider);
            ref.invalidate(risingStartupsProvider);
            ref.invalidate(popularDevelopersProvider);
            ref.invalidate(sponsoredRailProvider);
            await ref.read(discoveryFeedProvider.notifier).refresh();
          },
          child: ListView(
            controller: _scroll,
            padding: EdgeInsets.only(bottom: 120 + MediaQuery.paddingOf(context).bottom),
            children: <Widget>[
              _Header(profile: profile),
              const SizedBox(height: 14),
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 20),
                child: _SearchEntry(),
              ),
              const _FeaturedRail(),
              _Rails(),
              const SizedBox(height: 8),
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 18, 20, 6),
                child: Row(
                  children: <Widget>[
                    Expanded(
                      child: Text('All apps', style: context.text.titleLarge),
                    ),
                    _SortMenu(
                      current: sort,
                      onSelected: (DiscoverySort value) {
                        ref.read(feedSortProvider.notifier).set(value);
                        ref.read(discoveryFeedProvider.notifier).refresh();
                      },
                    ),
                  ],
                ),
              ),
              feed.when(
                data: (List<AppListing> items) {
                  if (items.isEmpty) {
                    return const EmptyView(
                      title: 'Nothing here yet',
                      message: 'No apps match this view. Try a different sort, or publish the first one.',
                      icon: Icons.spa_outlined,
                    );
                  }
                  return Column(
                    children: <Widget>[
                      for (final AppListing listing in items)
                        Padding(
                          padding: const EdgeInsets.fromLTRB(20, 0, 20, 12),
                          child: ApplicationCard(
                            listing: listing,
                            onTap: () => context.push('/app/${listing.id}'),
                          ),
                        ),
                      if (ref.read(discoveryFeedProvider.notifier).isLoadingMore)
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 20),
                          child: Center(child: ShapeMorphLoader(size: 28)),
                        ),
                      // One banner at the end of the feed: after the person has
                      // scrolled through real content, never above it.
                      const SproutlyBannerAd(placement: 'discover_feed'),
                    ],
                  );
                },
                loading: () => const CardListSkeleton(),
                error: (Object error, StackTrace _) => ErrorStateView(
                  error: error,
                  onRetry: () => ref.read(discoveryFeedProvider.notifier).refresh(),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.profile});

  final UserProfile? profile;

  @override
  Widget build(BuildContext context) {
    final String name = profile?.displayName.split(' ').first ?? 'there';
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 10, 20, 0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  'Hello, $name',
                  style: context.text.bodySmall!.copyWith(color: context.colors.onSurfaceVariant),
                ),
                const SizedBox(height: 2),
                Text(
                  AppConstants.tagline,
                  style: context.text.headlineSmall!.copyWith(letterSpacing: -0.5),
                ),
              ],
            ),
          ),
          ClipOval(
            child: AppImage(
              url: profile?.photoUrl,
              height: 42,
              width: 42,
              radius: BorderRadius.circular(42),
              icon: Icons.person_rounded,
              isIcon: true,
            ),
          ),
        ],
      ),
    );
  }
}

class _FeaturedRail extends ConsumerWidget {
  const _FeaturedRail();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<AppListing>> featured = ref.watch(featuredProvider);
    return featured.maybeWhen(
      data: (List<AppListing> items) {
        if (items.isEmpty) return const SizedBox.shrink();
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            const SectionHeader(
              title: 'Featured this week',
              subtitle: 'Hand-picked by the Sproutly editors',
            ),
            SizedBox(
              height: 208,
              child: PageView.builder(
                controller: PageController(viewportFraction: 0.87),
                padEnds: false,
                itemCount: items.length,
                itemBuilder: (BuildContext context, int index) => Padding(
                  padding: EdgeInsets.only(left: index == 0 ? 20 : 0, right: 12),
                  child: _FeaturedCard(listing: items[index]),
                ),
              ),
            ),
          ],
        );
      },
      orElse: () => const Padding(
        padding: EdgeInsets.only(top: 10),
        child: RailSkeleton(count: 2),
      ),
    );
  }
}

class _FeaturedCard extends StatelessWidget {
  const _FeaturedCard({required this.listing});

  final AppListing listing;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: SproutlyShapes.radiusXl,
      onTap: () => context.push('/app/${listing.id}'),
      child: Container(
        decoration: BoxDecoration(
          color: context.tokens.heroSurface,
          borderRadius: SproutlyShapes.radiusXl,
          border: Border.all(color: context.colors.outlineVariant),
        ),
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                AppImage(
                  url: listing.iconUrl,
                  height: 48,
                  width: 48,
                  radius: BorderRadius.circular(14),
                  isIcon: true,
                ),
                const Spacer(),
                if (listing.sponsored)
                  const SponsoredBadge()
                else
                  SproutlyPill(
                    label: 'Featured',
                    dense: true,
                    icon: Icons.auto_awesome_rounded,
                    background: context.colors.surface.withValues(alpha: 0.7),
                    foreground: context.colors.onSurface,
                  ),
              ],
            ),
            const Spacer(),
            Text(
              listing.name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: context.text.titleLarge!.copyWith(
                color: context.colors.onPrimaryContainer,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              listing.tagline,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: context.text.bodySmall!.copyWith(
                color: context.colors.onPrimaryContainer.withValues(alpha: 0.8),
                height: 1.4,
              ),
            ),
            const SizedBox(height: 10),
            Row(
              children: <Widget>[
                if (listing.reviewCount > 0)
                  RatingStars(rating: listing.rating, size: 13, count: listing.reviewCount)
                else
                  Text('Just launched', style: context.text.labelSmall),
                const Spacer(),
                Icon(Icons.arrow_forward_rounded, size: 16, color: context.colors.onPrimaryContainer),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _Rails extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      children: <Widget>[
        _ListingRail(
          title: 'Trending now',
          subtitle: 'What people are opening today',
          async: ref.watch(trendingProvider),
        ),
        _ListingRail(
          title: 'Recently launched',
          subtitle: 'Fresh from independent developers',
          async: ref.watch(recentlyLaunchedProvider),
        ),
        _DeveloperRail(async: ref.watch(popularDevelopersProvider)),
        _ListingRail(
          title: "Editor's picks",
          subtitle: 'Rated 4.0 and above',
          async: ref.watch(editorsPicksProvider),
        ),
        _ListingRail(
          title: 'Rising startups',
          subtitle: 'Launched in the last 60 days',
          async: ref.watch(risingStartupsProvider),
        ),
        _ListingRail(
          title: 'Sponsored',
          subtitle: 'Paid placements, clearly labelled',
          async: ref.watch(sponsoredRailProvider),
          showSponsoredLabel: true,
        ),
      ],
    );
  }
}

class _ListingRail extends StatelessWidget {
  const _ListingRail({
    required this.title,
    required this.subtitle,
    required this.async,
    this.onSeeAll,
    this.showSponsoredLabel = false,
  });

  final String title;
  final String subtitle;
  final AsyncValue<List<AppListing>> async;
  final VoidCallback? onSeeAll;
  final bool showSponsoredLabel;

  @override
  Widget build(BuildContext context) {
    return async.maybeWhen(
      data: (List<AppListing> items) {
        if (items.isEmpty) return const SizedBox.shrink();
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            SectionHeader(
              title: title,
              subtitle: subtitle,
              actionLabel: onSeeAll == null ? null : 'See all',
              onAction: onSeeAll,
            ),
            SizedBox(
              height: 178,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 20),
                itemCount: items.length,
                separatorBuilder: (_, __) => const SizedBox(width: 12),
                itemBuilder: (BuildContext context, int index) => ApplicationCard(
                  listing: items[index],
                  density: AppCardDensity.compact,
                  onTap: () => context.push('/app/${items[index].id}'),
                ),
              ),
            ),
          ],
        );
      },
      orElse: () => RailSkeleton(count: 3),
    );
  }
}

class _DeveloperRail extends StatelessWidget {
  const _DeveloperRail({required this.async});

  final AsyncValue<List<UserProfile>> async;

  @override
  Widget build(BuildContext context) {
    return async.maybeWhen(
      data: (List<UserProfile> items) {
        if (items.isEmpty) return const SizedBox.shrink();
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            const SectionHeader(title: 'Popular developers', subtitle: 'People shipping in public'),
            SizedBox(
              height: 92,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 20),
                itemCount: items.length,
                separatorBuilder: (_, __) => const SizedBox(width: 10),
                itemBuilder: (BuildContext context, int index) {
                  final UserProfile developer = items[index];
                  return InkWell(
                    borderRadius: SproutlyShapes.radiusLg,
                    onTap: () => context.push('/developer/${developer.uid}'),
                    child: Container(
                      width: 148,
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: context.colors.surfaceContainerLowest,
                        borderRadius: SproutlyShapes.radiusLg,
                        border: Border.all(color: context.colors.outlineVariant),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: <Widget>[
                          Row(
                            children: <Widget>[
                              ClipOval(
                                child: AppImage(
                                  url: developer.photoUrl,
                                  height: 28,
                                  width: 28,
                                  radius: BorderRadius.circular(28),
                                  icon: Icons.person_rounded,
                                  isIcon: true,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  developer.displayName,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: context.text.titleSmall,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Text(
                            '${developer.publishedAppCount} app${developer.publishedAppCount == 1 ? '' : 's'} · ${developer.followerCount} followers',
                            style: context.text.labelSmall,
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        );
      },
      orElse: () => RailSkeleton(count: 2),
    );
  }
}

class _SortMenu extends StatelessWidget {
  const _SortMenu({required this.current, required this.onSelected});

  final DiscoverySort current;
  final ValueChanged<DiscoverySort> onSelected;

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<DiscoverySort>(
      initialValue: current,
      onSelected: onSelected,
      shape: RoundedRectangleBorder(borderRadius: SproutlyShapes.radiusMd),
      itemBuilder: (BuildContext context) => <PopupMenuEntry<DiscoverySort>>[
        for (final DiscoverySort sort in DiscoverySort.values)
          PopupMenuItem<DiscoverySort>(value: sort, child: Text(sort.label)),
      ],
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
        decoration: BoxDecoration(
          color: context.colors.surfaceContainerHigh,
          borderRadius: SproutlyShapes.radiusPill,
        ),
        child: Row(
          children: <Widget>[
            Icon(Icons.swap_vert_rounded, size: 15, color: context.colors.onSurfaceVariant),
            const SizedBox(width: 5),
            Text(current.label, style: context.text.labelSmall),
          ],
        ),
      ),
    );
  }
}

/// A tappable search affordance. The real input lives on its own screen so the
/// keyboard never covers the feed.
class _SearchEntry extends StatelessWidget {
  const _SearchEntry();

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: SproutlyShapes.radiusPill,
      onTap: () => context.push('/search'),
      child: Container(
        height: 46,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        decoration: BoxDecoration(
          color: context.colors.surfaceContainerHigh,
          borderRadius: SproutlyShapes.radiusPill,
        ),
        child: Row(
          children: <Widget>[
            Icon(Icons.search_rounded, size: 20, color: context.colors.onSurfaceVariant),
            const SizedBox(width: 10),
            Text(
              'Search apps, developers, tags',
              style: context.text.bodyMedium!.copyWith(color: context.colors.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}
