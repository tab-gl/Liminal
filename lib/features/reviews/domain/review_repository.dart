import '../../../shared/models/review.dart';

abstract class ReviewRepository {
  /// Newest-first reviews for an application.
  Stream<List<Review>> watchForApplication(String applicationId, {int limit});

  /// The signed-in user's own review, if any. Used to switch the composer
  /// between "write" and "edit".
  Future<Review?> mineFor(String applicationId);

  Future<void> create({
    required String applicationId,
    required int rating,
    required String body,
  });

  Future<void> update({
    required String applicationId,
    required int rating,
    required String body,
  });

  Future<void> delete(String applicationId);

  /// Developer response. Only the application owner can write it; the security
  /// rule and a Cloud Function both check ownership.
  Future<void> respond({required String reviewId, required String body});
}
