import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_page.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import 'publish_controller.dart';

/// The Publish tab: start a new listing, or pick up a draft.
class PublishScreen extends ConsumerWidget {
  const PublishScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<AppListing>> apps = ref.watch(myAppsProvider);

    return SproutlyPage(
      title: 'Publish',
      subtitle: 'Free, forever. No listing fee, no revenue share.',
      scrollable: false,
      padding: EdgeInsets.zero,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
        children: <Widget>[
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: context.tokens.heroSurface,
              borderRadius: SproutlyShapes.radiusXl,
              border: Border.all(color: context.colors.outlineVariant),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Icon(Icons.rocket_launch_rounded, size: 30, color: context.colors.onPrimaryContainer),
                const SizedBox(height: 14),
                Text(
                  'Put your app in front of\npeople who care',
                  style: context.text.headlineSmall!.copyWith(
                    color: context.colors.onPrimaryContainer,
                    height: 1.25,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Five short steps. A human moderator reviews every listing, '
                  'usually within a day.',
                  style: context.text.bodySmall!.copyWith(
                    color: context.colors.onPrimaryContainer.withValues(alpha: 0.85),
                    height: 1.5,
                  ),
                ),
                const SizedBox(height: 16),
                SproutlyButton(
                  label: 'Start a new listing',
                  icon: Icons.add_rounded,
                  onPressed: () {
                    ref.read(publishDraftProvider.notifier).reset();
                    context.push('/publish/new');
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 22),
          Row(
            children: <Widget>[
              Text('Your listings', style: context.text.titleLarge),
              const Spacer(),
              TextButton(
                onPressed: () => context.push('/dashboard'),
                child: const Text('Dashboard'),
              ),
            ],
          ),
          const SizedBox(height: 6),
          apps.when(
            data: (List<AppListing> items) {
              if (items.isEmpty) {
                return EmptyView(
                  title: 'No listings yet',
                  message: 'Your drafts and published apps will appear here.',
                  icon: Icons.inventory_2_outlined,
                  compact: true,
                );
              }
              return Column(
                children: <Widget>[
                  for (final AppListing listing in items)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _ListingRow(listing: listing),
                    ),
                ],
              );
            },
            loading: () => const CardListSkeleton(count: 3),
            error: (Object error, StackTrace _) => ErrorStateView(
              error: error,
              onRetry: () => ref.invalidate(myAppsProvider),
            ),
          ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: context.colors.surfaceContainerLowest,
              borderRadius: SproutlyShapes.radiusLg,
              border: Border.all(color: context.colors.outlineVariant),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text('What makes a listing work', style: context.text.titleSmall),
                const SizedBox(height: 8),
                for (final String tip in const <String>[
                  'A tagline that says what the app does, not what it aspires to.',
                  'Three or more screenshots - listings with them get far more taps.',
                  'A working link. Broken links are the top reason listings are rejected.',
                  'An honest pricing label. "Free" with a paywall gets reported.',
                ])
                  Padding(
                    padding: const EdgeInsets.only(bottom: 6),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Padding(
                          padding: const EdgeInsets.only(top: 2),
                          child: Icon(Icons.tips_and_updates_outlined,
                              size: 14, color: context.colors.primary),
                        ),
                        const SizedBox(width: 8),
                        Expanded(child: Text(tip, style: context.text.labelSmall!.copyWith(height: 1.45))),
                      ],
                    ),
                  ),
                Text(
                  'Limits: ${AppConstants.maxNameLength}-character name, '
                  '${AppConstants.maxScreenshots} screenshots, '
                  '${AppConstants.maxTags} tags.',
                  style: context.text.labelSmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ListingRow extends StatelessWidget {
  const _ListingRow({required this.listing});

  final AppListing listing;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: SproutlyShapes.radiusLg,
      onTap: () => context.push('/dashboard/${listing.id}'),
      child: Container(
        padding: const EdgeInsets.all(12),
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
                  const SizedBox(height: 3),
                  Row(
                    children: <Widget>[
                      StatusChip(status: listing.status.wire),
                      const SizedBox(width: 8),
                      if (listing.status == ApplicationStatus.approved)
                        Text(
                          '${listing.viewCount} views',
                          style: context.text.labelSmall,
                        ),
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
