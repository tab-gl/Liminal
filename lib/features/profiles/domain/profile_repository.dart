import '../../../shared/models/application.dart';
import '../../../shared/models/user_profile.dart';

abstract class ProfileRepository {
  Stream<UserProfile?> watch(String uid);

  Future<UserProfile?> fetch(String uid);

  /// Public, approved apps by a developer, newest first.
  Future<List<AppListing>> appsBy(String developerId, {int limit});

  Stream<bool> watchFollowing(String developerId);

  Future<bool> toggleFollow(String developerId);

  Future<int> followerCount(String developerId);
}
