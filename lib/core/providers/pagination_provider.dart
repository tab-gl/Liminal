import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../utils/page.dart';

/// Loads one page of a cursor-paginated query.
typedef PageFetcher<T> = Future<Paged<T>> Function({Object? cursor, required int limit});

/// Pagination state for a single list.
///
/// Encodes the "load more" contract once so every infinite list behaves the
/// same: one in-flight request, no duplicate rows, and a hard stop when the
/// server says there is nothing left.
class PaginatedList<T> {
  PaginatedList({required this.fetch, required this.limit});

  final PageFetcher<T> fetch;
  final int limit;

  final List<T> items = <T>[];
  Object? _cursor;
  bool _hasMore = true;
  bool _loading = false;
  Object? _error;

  bool get hasMore => _hasMore;
  bool get isLoading => _loading;
  bool get isEmpty => items.isEmpty;
  Object? get error => _error;

  Future<void> reset() async {
    items.clear();
    _cursor = null;
    _hasMore = true;
    _error = null;
    await loadMore();
  }

  Future<List<T>> loadMore() async {
    if (_loading || !_hasMore) return const <Never>[];
    _loading = true;
    _error = null;
    try {
      final Paged<T> page = await fetch(cursor: _cursor, limit: limit);
      final Set<String> seen = items.map((T item) => item.toString()).toSet();
      final List<T> fresh = page.items.where((T item) => !seen.contains(item.toString())).toList();
      items.addAll(fresh);
      _cursor = page.cursor ?? _cursor;
      _hasMore = page.hasMore;
      return fresh;
    } catch (error) {
      _error = error;
      _hasMore = false;
      rethrow;
    } finally {
      _loading = false;
    }
  }
}

/// Shared notifier shape for infinite feeds. Subclasses supply [fetchPage].
abstract class FeedController<T> extends AsyncNotifier<List<T>> {
  int get pageSize;
  Future<Paged<T>> fetchPage({Object? cursor, required int limit});

  late PaginatedList<T> _page = PaginatedList<T>(fetch: fetchPage, limit: pageSize);

  bool get hasMore => _page.hasMore;
  bool get isLoadingMore => _page.isLoading;

  @override
  Future<List<T>> build() async {
    _page = PaginatedList<T>(fetch: fetchPage, limit: pageSize);
    await _page.reset();
    return List<T>.unmodifiable(_page.items);
  }

  Future<void> loadMore() async {
    if (_page.isLoading || !_page.hasMore) return;
    final List<T> before = state.value ?? <T>[];
    state = AsyncData<List<T>>(before);
    await _page.loadMore();
    state = AsyncData<List<T>>(List<T>.unmodifiable(_page.items));
  }

  Future<void> refresh() async {
    state = AsyncLoading<List<T>>();
    state = await AsyncValue.guard(() async {
      await _page.reset();
      return List<T>.unmodifiable(_page.items);
    });
  }
}
