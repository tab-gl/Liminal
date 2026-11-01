import 'firestore_codec.dart';

class DeveloperResponse {
  const DeveloperResponse({required this.body, required this.respondedAt});

  final String body;
  final DateTime respondedAt;

  factory DeveloperResponse.fromMap(Map<String, dynamic> map) => DeveloperResponse(
        body: Codec.str(map['body']),
        respondedAt: Codec.dateOrNow(map['respondedAt']),
      );
}

/// A single rating and its text. One per user per application, enforced by the
/// deterministic document id and by firestore.rules.
class Review {
  const Review({
    required this.id,
    required this.applicationId,
    required this.userId,
    required this.rating,
    required this.body,
    required this.createdAt,
    required this.updatedAt,
    this.userName = 'Sproutly member',
    this.userPhotoUrl,
    this.developerResponse,
    this.helpfulCount = 0,
    this.status = 'published',
  });

  final String id;
  final String applicationId;
  final String userId;
  final String userName;
  final String? userPhotoUrl;
  final int rating;
  final String body;
  final DeveloperResponse? developerResponse;
  final int helpfulCount;
  final String status;
  final DateTime createdAt;
  final DateTime updatedAt;

  factory Review.fromDoc(String id, Map<String, dynamic> map) {
    final Map<String, dynamic> response = Codec.map(map['developerResponse']);
    return Review(
      id: id,
      applicationId: Codec.str(map['applicationId']),
      userId: Codec.str(map['userId']),
      userName: Codec.str(map['userName'], fallback: 'Sproutly member'),
      userPhotoUrl: Codec.strOrNull(map['userPhotoUrl']),
      rating: Codec.integer(map['rating'], fallback: 5),
      body: Codec.str(map['body']),
      developerResponse:
          response.isEmpty ? null : DeveloperResponse.fromMap(response),
      helpfulCount: Codec.integer(map['helpfulCount']),
      status: Codec.str(map['status'], fallback: 'published'),
      createdAt: Codec.dateOrNow(map['createdAt']),
      updatedAt: Codec.dateOrNow(map['updatedAt']),
    );
  }

  Map<String, dynamic> toCreateMap({required String userName, String? userPhotoUrl}) {
    final DateTime now = DateTime.now().toUtc();
    return <String, dynamic>{
      'applicationId': applicationId,
      'userId': userId,
      'userName': userName,
      'userPhotoUrl': userPhotoUrl,
      'rating': rating,
      'body': body.trim(),
      'helpfulCount': 0,
      'status': 'published',
      'createdAt': Codec.stamp(now),
      'updatedAt': Codec.stamp(now),
    };
  }

  Map<String, dynamic> toUpdateMap() => <String, dynamic>{
        'rating': rating,
        'body': body.trim(),
        'updatedAt': Codec.stamp(DateTime.now().toUtc()),
      };
}
