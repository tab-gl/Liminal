/// A cursor-paginated result.
///
/// Named `Paged` rather than `Page` because `Page` is exported by Flutter's
/// navigator and the two collide in any file that imports Material.
///
/// Firestore's `startAfterDocument` is the cursor; [hasMore] tells the UI
/// whether to keep listening for scroll-to-end. No offsets, no total counts -
/// counting a collection is a full scan and Sproutly never does it in the client.
class Paged<T> {
  const Paged({
    required this.items,
    required this.hasMore,
    this.cursor,
  });

  const Paged.empty()
      : items = const <Never>[],
        hasMore = false,
        cursor = null;

  final List<T> items;
  final bool hasMore;
  final Object? cursor;

  bool get isEmpty => items.isEmpty;
  bool get isNotEmpty => items.isNotEmpty;

  int get length => items.length;

  Paged<T> append(Paged<T> next) => Paged<T>(
        items: <T>[...items, ...next.items],
        hasMore: next.hasMore,
        cursor: next.cursor ?? cursor,
      );

  static Paged<T> single<T>(List<T> items) =>
      Paged<T>(items: items, hasMore: false);
}
