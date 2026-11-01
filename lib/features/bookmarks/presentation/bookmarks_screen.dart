import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/widgets/application_card.dart';
import '../data/bookmark_repository_impl.dart';

class BookmarksScreen extends ConsumerWidget {
  const BookmarksScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<AppListing>> saved = ref.watch(savedAppsProvider);

    return Scaffold(
      backgroundColor: context.colors.surface,
      body: SafeArea(
        child: Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.fromLTRB(8, 6, 20, 6),
              child: Row(
                children: <Widget>[
                  SproutlyIconButton(
                    icon: Icons.arrow_back_rounded,
                    tooltip: 'Back',
                    onPressed: () => context.pop(),
                  ),
                  const SizedBox(width: 6),
                  Expanded(child: Text('Saved apps', style: context.text.headlineSmall)),
                  IconButton(
                    icon: const Icon(Icons.refresh_rounded),
                    onPressed: () => ref.invalidate(savedAppsProvider),
                    tooltip: 'Refresh',
                  ),
                ],
              ),
            ),
            Expanded(
              child: saved.when(
                data: (List<AppListing> items) {
                  if (items.isEmpty) {
                    return EmptyView(
                      title: 'Nothing saved yet',
                      message: 'Tap the bookmark on any app to keep it here.',
                      icon: Icons.bookmark_border_rounded,
                      actionLabel: 'Browse apps',
                      onAction: () => context.go('/discover'),
                    );
                  }
                  return ListView.separated(
                    padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
                    itemCount: items.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (BuildContext context, int index) => ApplicationCard(
                      listing: items[index],
                      onTap: () => context.push('/app/${items[index].id}'),
                    ),
                  );
                },
                loading: () => const CardListSkeleton(),
                error: (Object error, StackTrace _) => ErrorStateView(
                  error: error,
                  onRetry: () => ref.invalidate(savedAppsProvider),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
