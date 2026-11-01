import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/utils/debouncer.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/widgets/application_card.dart';
import '../data/discovery_repository_impl.dart';

final searchResultsProvider =
    FutureProvider.family<List<AppListing>, String>((Ref ref, String query) {
  if (query.trim().isEmpty) return Future<List<AppListing>>.value(const <AppListing>[]);
  return ref.watch(discoveryRepositoryProvider).search(query, limit: 30);
});

class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key, this.initialQuery});

  final String? initialQuery;

  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends ConsumerState<SearchScreen> {
  late final TextEditingController _controller =
      TextEditingController(text: widget.initialQuery ?? '');
  final Debouncer _debounce = Debouncer(
    duration: const Duration(milliseconds: AppConstants.searchDebounceMs),
  );
  String _query = '';

  @override
  void initState() {
    super.initState();
    _query = widget.initialQuery ?? '';
  }

  @override
  void dispose() {
    _debounce.dispose();
    _controller.dispose();
    super.dispose();
  }

  void _onChanged(String value) {
    _debounce.run(() {
      if (!mounted) return;
      setState(() => _query = value.trim());
    });
  }

  @override
  Widget build(BuildContext context) {
    final AsyncValue<List<AppListing>> results = ref.watch(searchResultsProvider(_query));

    return Scaffold(
      backgroundColor: context.colors.surface,
      body: SafeArea(
        child: Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 10, 16, 6),
              child: Row(
                children: <Widget>[
                  Expanded(
                    child: SproutlySearchField(
                      controller: _controller,
                      autofocus: widget.initialQuery == null,
                      onChanged: _onChanged,
                      onSubmitted: (String value) => setState(() => _query = value.trim()),
                      onClear: () => setState(() => _query = ''),
                    ),
                  ),
                  TextButton(
                    onPressed: () => context.pop(),
                    child: const Text('Cancel'),
                  ),
                ],
              ),
            ),
            Expanded(
              child: _query.isEmpty
                  ? const EmptyView(
                      title: 'Search Sproutly',
                      message: 'Try an app name, a developer, or a tag like "notion" or "ai".',
                      icon: Icons.search_rounded,
                    )
                  : results.when(
                      data: (List<AppListing> items) {
                        if (items.isEmpty) {
                          return EmptyView(
                            title: 'No results for "$_query"',
                            message: 'Check the spelling, or browse categories instead.',
                            icon: Icons.search_off_rounded,
                            actionLabel: 'Browse categories',
                            onAction: () => context.go('/categories'),
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
                      loading: () => const CardListSkeleton(count: 4),
                      error: (Object error, StackTrace _) => ErrorStateView(
                        error: error,
                        onRetry: () => ref.invalidate(searchResultsProvider(_query)),
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
