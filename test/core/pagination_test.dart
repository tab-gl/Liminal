import 'package:flutter_test/flutter_test.dart';
import 'package:sproutly/core/providers/pagination_provider.dart';
import 'package:sproutly/core/utils/page.dart';

void main() {
  test('appends pages, de-duplicates and stops when the server says so', () async {
    int calls = 0;
    final PaginatedList<String> list = PaginatedList<String>(
      limit: 2,
      fetch: ({Object? cursor, required int limit}) async {
        calls++;
        if (cursor == null) {
          return const Paged<String>(items: <String>['a', 'b'], hasMore: true, cursor: 'c1');
        }
        return const Paged<String>(items: <String>['b', 'c'], hasMore: false, cursor: 'c2');
      },
    );

    await list.reset();
    expect(list.items, <String>['a', 'b']);
    expect(list.hasMore, isTrue);

    final List<String> fresh = await list.loadMore();
    expect(fresh, <String>['c'], reason: 'b was already present');
    expect(list.items, <String>['a', 'b', 'c']);
    expect(list.hasMore, isFalse);

    // A further call must not hit the network.
    await list.loadMore();
    expect(calls, 2);
  });

  test('records the error and stops paginating on failure', () async {
    final PaginatedList<String> list = PaginatedList<String>(
      limit: 2,
      fetch: ({Object? cursor, required int limit}) async => throw StateError('offline'),
    );

    await expectLater(list.reset(), throwsA(isA<StateError>()));
    expect(list.error, isA<StateError>());
    expect(list.hasMore, isFalse);
    expect(list.items, isEmpty);
  });
}
