import '../../../shared/models/application.dart';

abstract class BookmarkRepository {
  /// Live bookmark state for one application, so the button reflects another
  /// device's change without a manual refresh.
  Stream<bool> watch(String applicationId);

  Future<void> add({required String applicationId, required String appName});

  Future<void> remove(String applicationId);

  /// Returns the new state.
  Future<bool> toggle({required String applicationId, required String appName});

  /// Bookmarked applications, newest bookmark first.
  Future<List<AppListing>> saved();
}
