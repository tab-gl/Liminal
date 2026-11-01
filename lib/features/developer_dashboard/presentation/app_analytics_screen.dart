import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_page.dart';
import '../../../core/widgets/sproutly_sheet.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../../publishing/data/publishing_repository_impl.dart';
import '../../publishing/presentation/publish_controller.dart';
import '../data/dashboard_repository_impl.dart';
import '../domain/dashboard_repository.dart';
import 'widgets/views_chart.dart';

final _listingProvider =
    FutureProvider.family<AppListing?, String>((Ref ref, String id) {
  return ref.watch(publishingRepositoryProvider).fetch(id);
});

class AppAnalyticsScreen extends ConsumerWidget {
  const AppAnalyticsScreen({required this.applicationId, super.key});

  final String applicationId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<AppListing?> async = ref.watch(_listingProvider(applicationId));
    final AsyncValue<List<DailyStat>> series = ref.watch(appSeriesProvider(applicationId));

    return SproutlyPage(
      title: 'Listing',
      showBack: true,
      scrollable: false,
      padding: EdgeInsets.zero,
      child: async.when(
        loading: () => const Center(child: SproutlyLoader()),
        error: (Object error, StackTrace _) => ErrorStateView(
          error: error,
          onRetry: () => ref.invalidate(_listingProvider(applicationId)),
        ),
        data: (AppListing? listing) {
          if (listing == null) {
            return const EmptyView(
              title: 'Listing not found',
              message: 'It may have been deleted.',
              icon: Icons.inventory_2_outlined,
            );
          }
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
            children: <Widget>[
              Row(
                children: <Widget>[
                  Expanded(child: Text(listing.name, style: context.text.headlineSmall)),
                  StatusChip(status: listing.status.wire),
                ],
              ),
              const SizedBox(height: 6),
              Text(listing.tagline, style: context.text.bodySmall),
              if (listing.status == ApplicationStatus.rejected && listing.rejectionReason != null)
                Container(
                  margin: const EdgeInsets.only(top: 12),
                  padding: const EdgeInsets.all(13),
                  decoration: BoxDecoration(
                    color: context.colors.errorContainer,
                    borderRadius: SproutlyShapes.radiusMd,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        'Changes requested',
                        style: context.text.titleSmall!.copyWith(color: context.colors.onErrorContainer),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        listing.rejectionReason!,
                        style: context.text.bodySmall!.copyWith(color: context.colors.onErrorContainer),
                      ),
                    ],
                  ),
                ),
              if (listing.status == ApplicationStatus.pendingReview)
                Padding(
                  padding: const EdgeInsets.only(top: 12),
                  child: Container(
                    padding: const EdgeInsets.all(13),
                    decoration: BoxDecoration(
                      color: context.tokens.warningContainer,
                      borderRadius: SproutlyShapes.radiusMd,
                    ),
                    child: Text(
                      'In the moderation queue. You can withdraw it while it waits.',
                      style: context.text.bodySmall!.copyWith(color: context.tokens.warning),
                    ),
                  ),
                ),
              const SizedBox(height: 18),
              GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisSpacing: 10,
                mainAxisSpacing: 10,
                childAspectRatio: 1.8,
                children: <Widget>[
                  MetricTile(
                    label: 'Total views',
                    value: Formatters.compactCount(listing.viewCount),
                    icon: Icons.visibility_outlined,
                    emphasis: true,
                  ),
                  MetricTile(
                    label: 'Clicks',
                    value: Formatters.compactCount(listing.clickCount),
                    icon: Icons.ads_click_rounded,
                  ),
                  MetricTile(
                    label: 'Bookmarks',
                    value: '${listing.bookmarkCount}',
                    icon: Icons.bookmark_border_rounded,
                  ),
                  MetricTile(
                    label: 'Rating',
                    value: listing.reviewCount == 0
                        ? '-'
                        : '${listing.rating.toStringAsFixed(1)} (${listing.reviewCount})',
                    icon: Icons.star_outline_rounded,
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Text('Last 30 days', style: context.text.titleLarge),
              const SizedBox(height: 10),
              series.when(
                data: (List<DailyStat> days) => ViewsChart(days: days),
                loading: () => const SizedBox(height: 170, child: Center(child: SproutlyLoader())),
                error: (Object error, StackTrace _) => ErrorStateView(error: error),
              ),
              const SizedBox(height: 22),
              SproutlyButton(
                label: 'Edit listing',
                icon: Icons.edit_outlined,
                expand: true,
                onPressed: () {
                  ref.read(publishDraftProvider.notifier).load(listing);
                  context.push('/publish/edit/${listing.id}');
                },
              ),
              const SizedBox(height: 10),
              SproutlyButton(
                label: 'Promote this app',
                icon: Icons.campaign_outlined,
                variant: SproutlyButtonVariant.tonal,
                expand: true,
                onPressed: () => context.push('/advertising?app=${listing.id}'),
              ),
              const SizedBox(height: 10),
              SproutlyButton(
                label: 'View public page',
                icon: Icons.open_in_new_rounded,
                variant: SproutlyButtonVariant.outline,
                expand: true,
                onPressed: () => context.push('/app/${listing.id}'),
              ),
              if (listing.status == ApplicationStatus.pendingReview) ...<Widget>[
                const SizedBox(height: 10),
                SproutlyButton(
                  label: 'Withdraw submission',
                  variant: SproutlyButtonVariant.ghost,
                  expand: true,
                  onPressed: () async {
                    final bool ok = await showConfirmDialog(
                      context,
                      title: 'Withdraw this submission?',
                      message: 'It returns to draft. You can resubmit any time.',
                      confirmLabel: 'Withdraw',
                    );
                    if (!ok) return;
                    await ref.read(publishingRepositoryProvider).withdraw(listing.id);
                    ref.invalidate(_listingProvider(listing.id));
                  },
                ),
              ],
              if (listing.status == ApplicationStatus.draft) ...<Widget>[
                const SizedBox(height: 10),
                SproutlyButton(
                  label: 'Delete draft',
                  variant: SproutlyButtonVariant.destructive,
                  expand: true,
                  onPressed: () async {
                    final bool ok = await showConfirmDialog(
                      context,
                      title: 'Delete this draft?',
                      message: 'This cannot be undone.',
                      confirmLabel: 'Delete',
                      destructive: true,
                    );
                    if (!ok) return;
                    await ref.read(publishingRepositoryProvider).deleteDraft(listing.id);
                    ref.invalidate(myAppsProvider);
                    if (context.mounted) context.pop();
                  },
                ),
              ],
            ],
          );
        },
      ),
    );
  }
}
