import 'enums.dart';
import 'firestore_codec.dart';

/// A Sproutly person. The same document backs a consumer profile and a
/// developer profile; `isDeveloper` decides which surfaces unlock.
class UserProfile {
  const UserProfile({
    required this.uid,
    required this.displayName,
    required this.createdAt,
    required this.updatedAt,
    this.photoUrl,
    this.bio,
    this.websiteUrl,
    this.links = const <String, String>{},
    this.role = UserRole.user,
    this.isDeveloper = false,
    this.isPublic = true,
    this.followerCount = 0,
    this.publishedAppCount = 0,
    this.reviewCount = 0,
    this.onboarded = false,
  });

  final String uid;
  final String displayName;
  final String? photoUrl;
  final String? bio;
  final String? websiteUrl;
  final Map<String, String> links;
  final UserRole role;
  final bool isDeveloper;
  final bool isPublic;
  final int followerCount;
  final int publishedAppCount;
  final int reviewCount;
  final bool onboarded;
  final DateTime createdAt;
  final DateTime updatedAt;

  bool get canPublish => isDeveloper || role.canPublish;
  bool get isAdmin => role.isAdmin;
  String get initials {
    final List<String> parts = displayName.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty || parts.first.isEmpty) return 'S';
    if (parts.length == 1) return parts.first.characters1;
    return '${parts.first.characters1}${parts.last.characters1}';
  }

  factory UserProfile.fromMap(String uid, Map<String, dynamic> map) {
    return UserProfile(
      uid: uid,
      displayName: Codec.str(map['displayName'], fallback: 'Sproutly member'),
      photoUrl: Codec.strOrNull(map['photoUrl']),
      bio: Codec.strOrNull(map['bio']),
      websiteUrl: Codec.strOrNull(map['websiteUrl']),
      links: Codec.map(map['links']).map(
        (String key, dynamic value) => MapEntry<String, String>(key, value.toString()),
      ),
      role: UserRole.parse(map['role']),
      isDeveloper: Codec.boolean(map['isDeveloper']),
      isPublic: Codec.boolean(map['isPublic'], fallback: true),
      followerCount: Codec.integer(map['followerCount']),
      publishedAppCount: Codec.integer(map['publishedAppCount']),
      reviewCount: Codec.integer(map['reviewCount']),
      onboarded: Codec.boolean(map['onboarded']),
      createdAt: Codec.dateOrNow(map['createdAt']),
      updatedAt: Codec.dateOrNow(map['updatedAt']),
    );
  }

  /// The document a new user owns. Server-owned fields are present with safe
  /// defaults so the security rule can assert them.
  Map<String, dynamic> toCreateMap(DateTime now) => <String, dynamic>{
        'uid': uid,
        'displayName': displayName,
        'photoUrl': photoUrl,
        'bio': bio,
        'websiteUrl': websiteUrl,
        'links': links,
        'role': UserRole.user.name,
        'isDeveloper': false,
        'isPublic': true,
        'followerCount': 0,
        'publishedAppCount': 0,
        'reviewCount': 0,
        'onboarded': false,
        'createdAt': Codec.stamp(now),
        'updatedAt': Codec.stamp(now),
      };

  /// Only fields the owner may change, matching firestore.rules.
  Map<String, dynamic> toProfileUpdate(DateTime now) => <String, dynamic>{
        'displayName': displayName,
        'photoUrl': photoUrl,
        'bio': bio,
        'websiteUrl': websiteUrl,
        'links': links,
        'isPublic': isPublic,
        'onboarded': onboarded,
        'updatedAt': Codec.stamp(now),
      };

  UserProfile copyWith({
    String? displayName,
    String? photoUrl,
    String? bio,
    String? websiteUrl,
    Map<String, String>? links,
    bool? isPublic,
    bool? onboarded,
    bool? isDeveloper,
    int? followerCount,
    int? publishedAppCount,
  }) {
    return UserProfile(
      uid: uid,
      displayName: displayName ?? this.displayName,
      photoUrl: photoUrl ?? this.photoUrl,
      bio: bio ?? this.bio,
      websiteUrl: websiteUrl ?? this.websiteUrl,
      links: links ?? this.links,
      role: role,
      isDeveloper: isDeveloper ?? this.isDeveloper,
      isPublic: isPublic ?? this.isPublic,
      followerCount: followerCount ?? this.followerCount,
      publishedAppCount: publishedAppCount ?? this.publishedAppCount,
      reviewCount: reviewCount,
      onboarded: onboarded ?? this.onboarded,
      createdAt: createdAt,
      updatedAt: updatedAt,
    );
  }
}

extension on String {
  String get characters1 => isEmpty ? '' : substring(0, 1).toUpperCase();
}
