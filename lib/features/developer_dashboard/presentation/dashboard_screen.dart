import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/m3e/expressive.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_page.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../data/dashboard_repository_impl.dart';
import '../domain/dashboard_repository.dart';
import 'widgets/views_chart.dart';

class DashboardScreen extends ConsumerStatefulWidget {
  const DashboardScreen({super.key});

  @override
  ConsumerState<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends ConsumerState<DashboardScreen> {
  int _period = 0; // 0 = 30 days, 1 = 7 days, 2 = today
  String _metric = 'views';

  List<DailyStat> _slice(List<DailyStat> all) {
    final int take = switch (_period) { 0 => 30, 1 => 7, _ => 1 };
    if (all.length <= take) return all;
    return all.sublist(all.length - take);
  }

  @override
  Widget build(BuildContext context) {
    final AsyncValue<DashboardSummary> async = ref.watch(dashboardSummaryProvider);

    return SproutlyPage(
      title: 'Dashboard',
      subtitle: 'Real numbers from your listings',
      scrollable: false,
      padding: EdgeInsets.zero,
      showBack: true,
      actions: <Widget>[
        IconButton(
          tooltip: 'Refresh',
          icon: const Icon(Icons.refresh_rounded),
          onPressed: () => ref.invalidate(dashboardSummaryProvider),
        ),
      ],
      child: async.when(
        loading: () => const Center(child: SproutlyLoader()),
        error: (Object error, StackTrace _) => ErrorStateView(
          error: error,
          onRetry: () => ref.invalidate(dashboardSummaryProvider),
        ),
        data: (DashboardSummary summary) {
          final List<DailyStat> series = _slice(summary.days);
          final int totalViews = series.fold(0, (int s, DailyStat d) => s + d.views);
          final int totalClicks = series.fold(0, (int s, DailyStat d) => s + d.clicks);
          final int totalBookmarks = series.fold(0, (int s, DailyStat d) => s + d.bookmarks);
          final double ctr = totalViews == 0 ? 0 : totalClicks / totalViews;

          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
            children: <Widget>[
              Center(
                child: ExpressivePeriodSelector(
                  labels: const <String>['30 days', '7 days', 'Today'],
                  selectedIndex: _period,
                  onChanged: (int index) => setState(() => _period = index),
                ),
              ),
              const SizedBox(height: 16),
              GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisSpacing: 10,
                mainAxisSpacing: 10,
                childAspectRatio: 1.7,
                children: <Widget>[
                  MetricTile(
                    label: 'Views',
                    value: Formatters.compactCount(totalViews),
                    icon: Icons.visibility_outlined,
                    emphasis: true,
                  ),
                  MetricTile(
                    label: 'Clicks through',
                    value: Formatters.compactCount(totalClicks),
                    icon: Icons.ads_click_rounded,
                  ),
                  MetricTile(
                    label: 'Click rate',
                    value: Formatters.percent(ctr),
                    icon: Icons.trending_up_rounded,
                  ),
                  MetricTile(
                    label: 'Bookmarks',
                    value: Formatters.compactCount(totalBookmarks),
                    icon: Icons.bookmark_border_rounded,
                  ),
                  MetricTile(
                    label: 'Reviews',
                    value: '${summary.totalReviews}',
                    icon: Icons.rate_review_outlined,
                  ),
                  MetricTile(
                    label: 'Live apps',
                    value: '${summary.liveApps}',
                    icon: Icons.apps_rounded,
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Row(
                children: <Widget>[
                  Text('Activity', style: context.text.titleLarge),
                  const Spacer(),
                  PopupMenuButton<String>(
                    initialValue: _metric,
                    onSelected: (String value) => setState(() => _metric = value),
                    itemBuilder: (BuildContext context) => const <PopupMenuEntry<String>>[
                      PopupMenuItem<String>(value: 'views', child: Text('Views')),
                      PopupMenuItem<String>(value: 'clicks', child: Text('Clicks')),
                      PopupMenuItem<String>(value: 'bookmarks', child: Text('Bookmarks')),
                    ],
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 6),
                      decoration: BoxDecoration(
                        color: context.colors.surfaceContainerHigh,
                        borderRadius: SproutlyShapes.radiusPill,
                      ),
                      child: Row(
                        children: <Widget>[
                          Text(_metric, style: context.text.labelSmall),
                          const Icon(Icons.arrow_drop_down_rounded, size: 16),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              ViewsChart(days: series, metric: _metric),
              const SizedBox(height: 24),
              Text('Your listings', style: context.text.titleLarge),
              const SizedBox(height: 10),
              if (summary.apps.isEmpty)
                EmptyView(
                  title: 'No listings yet',
                  message: 'Publish an app to start collecting real numbers.',
                  icon: Icons.rocket_launch_outlined,
                  compact: true,
                  actionLabel: 'Publish an app',
                  onAction: () => context.go('/publish'),
                )
              else
                for (final AppListing listing in summary.apps)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _AppRow(listing: listing),
                  ),
            ],
          );
        },
      ),
    );
  }
}

class _AppRow extends StatelessWidget {
  const _AppRow({required this.listing});

  final AppListing listing;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: SproutlyShapes.radiusLg,
      onTap: () => context.push('/dashboard/${listing.id}'),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: context.colors.surfaceContainerLowest,
          borderRadius: SproutlyShapes.radiusLg,
          border: Border.all(color: context.colors.outlineVariant),
        ),
        child: Row(
          children: <Widget>[
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(listing.name, style: context.text.titleSmall, maxLines: 1),
                  const SizedBox(height: 4),
                  Wrap(
                    spacing: 6,
                    children: <Widget>[
                      StatusChip(status: listing.status.wire),
                      if (listing.reviewCount > 0)
                        SproutlyPill(
                          label: '${listing.rating.toStringAsFixed(1)} ★ (${listing.reviewCount})',
                          dense: true,
                        ),
                      SproutlyPill(label: '${listing.viewCount} views', dense: true),
                    ],
                  ),
                ],
              ),
            ),
            Icon(Icons.chevron_right_rounded, size: 18, color: context.colors.onSurfaceVariant),
          ],
        ),
      ),
    );
  }
}
