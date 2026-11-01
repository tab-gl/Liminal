import '../../../shared/models/application.dart';

abstract class PublishingRepository {
  /// Creates a draft or updates an existing one. Returns the document id.
  Future<String> save(AppListing draft);

  /// Moves a draft into the moderation queue. The client can only perform this
  /// transition; approval belongs to a moderator.
  Future<void> submit(String applicationId);

  /// Pulls a submission back to draft, only while it is still pending review.
  Future<void> withdraw(String applicationId);

  Future<void> deleteDraft(String applicationId);

  Future<List<AppListing>> myApps();

  Future<AppListing?> fetch(String applicationId);
}
