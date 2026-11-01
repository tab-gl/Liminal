import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/constants/categories.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/providers/pagination_provider.dart';
import '../../../core/utils/page.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/widgets/application_card.dart';
import '../../discovery/data/discovery_repository_impl.dart';
import '../../discovery/domain/discovery_repository.dart';

/// One category, paginated.
///
/// Uses a locally owned [PaginatedList] rather than a global provider: the
/// category is a route parameter, and a screen-scoped controller cannot leak
/// state between categories.
class CategoryScreen extends ConsumerStatefulWidget {
  const CategoryScreen({required this.categoryId, super.key});

  final String categoryId;

  @override
  ConsumerState<CategoryScreen> createState() => _CategoryScreenState();
}

class _CategoryScreenState extends ConsumerState<CategoryScreen> {
  final ScrollController _scroll = ScrollController();
  late PaginatedList<AppListing> _page;
  DiscoverySort _sort = DiscoverySort.newest;
  bool _loading = true;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _page = PaginatedList<AppListing>(fetch: _fetch, limit: 12);
    _scroll.addListener(_onScroll);
    _load();
  }

  Future<Paged<AppListing>> _fetch({Object? cursor, required int limit}) {
    return ref.read(discoveryRepositoryProvider).feed(
          cursor: cursor,
          limit: limit,
          sort: _sort,
          categoryId: widget.categoryId,
        );
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scroll.hasClients) return;
    if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 400) _loadMore();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
      _page = PaginatedList<AppListing>(fetch: _fetch, limit: 12);
    });
    try {
      await _page.reset();
    } catch (error) {
      _error = error;
    }
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _loadMore() async {
    if (_page.isLoading || !_page.hasMore) return;
    try {
      await _page.loadMore();
      if (mounted) setState(() {});
    } catch (error) {
      if (mounted) setState(() => _error = error);
    }
  }

  @override
  Widget build(BuildContext context) {
    final AppCategory category = AppCategory.byId(widget.categoryId);
    return Scaffold(
      backgroundColor: context.colors.surface,
      body: SafeArea(
        child: Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.fromLTRB(8, 6, 12, 4),
              child: Row(
                children: <Widget>[
                  SproutlyIconButton(
                    icon: Icons.arrow_back_rounded,
                    tooltip: 'Back',
                    onPressed: () => context.pop(),
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(category.label, style: context.text.headlineSmall),
                        Text(category.blurb, style: context.text.bodySmall),
                      ],
                    ),
                  ),
                  PopupMenuButton<DiscoverySort>(
                    initialValue: _sort,
                    onSelected: (DiscoverySort value) {
                      setState(() => _sort = value);
                      _load();
                    },
                    itemBuilder: (BuildContext context) => <PopupMenuEntry<DiscoverySort>>[
                      for (final DiscoverySort sort in DiscoverySort.values)
                        PopupMenuItem<DiscoverySort>(value: sort, child: Text(sort.label)),
                    ],
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                      decoration: BoxDecoration(
                        color: context.colors.surfaceContainerHigh,
                        borderRadius: SproutlyShapes.radiusPill,
                      ),
                      child: Row(
                        children: <Widget>[
                          Icon(Icons.swap_vert_rounded, size: 14, color: context.colors.onSurfaceVariant),
                          const SizedBox(width: 4),
                          Text(_sort.label, style: context.text.labelSmall),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: _loading
                  ? const CardListSkeleton()
                  : _error != null
                      ? ErrorStateView(error: _error!, onRetry: _load)
                      : _page.items.isEmpty
                          ? EmptyView(
                              title: 'No apps in ${category.label} yet',
                              message: 'Be the first to publish here - it takes about five minutes.',
                              icon: category.icon,
                              actionLabel: 'Publish an app',
                              onAction: () => context.go('/publish'),
                            )
                          : RefreshIndicator(
                              color: context.colors.primary,
                              onRefresh: _load,
                              child: ListView.separated(
                                controller: _scroll,
                                padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
                                itemCount: _page.items.length + (_page.isLoading ? 1 : 0),
                                separatorBuilder: (_, __) => const SizedBox(height: 12),
                                itemBuilder: (BuildContext context, int index) {
                                  if (index >= _page.items.length) {
                                    return const Padding(
                                      padding: EdgeInsets.symmetric(vertical: 18),
                                      child: Center(child: SproutlyLoader()),
                                    );
                                  }
                                  final AppListing listing = _page.items[index];
                                  return ApplicationCard(
                                    listing: listing,
                                    onTap: () => context.push('/app/${listing.id}'),
                                  );
                                },
                              ),
                            ),
            ),
          ],
        ),
      ),
    );
  }
}
