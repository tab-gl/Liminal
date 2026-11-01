import 'enums.dart';
import 'firestore_codec.dart';

/// An in-app notification. Rows are created by Cloud Functions and are
/// immutable to the client except for `readAt`.
class AppNotification {
  const AppNotification({
    required this.id,
    required this.userId,
    required this.type,
    required this.title,
    required this.body,
    required this.createdAt,
    this.route,
    this.readAt,
    this.data = const <String, String>{},
  });

  final String id;
  final String userId;
  final NotificationType type;
  final String title;
  final String body;
  final String? route;
  final Map<String, String> data;
  final DateTime createdAt;
  final DateTime? readAt;

  bool get isUnread => readAt == null;

  factory AppNotification.fromDoc(String id, Map<String, dynamic> map) {
    return AppNotification(
      id: id,
      userId: Codec.str(map['userId']),
      type: NotificationType.parse(map['type']),
      title: Codec.str(map['title']),
      body: Codec.str(map['body']),
      route: Codec.strOrNull(map['route']),
      data: Codec.map(map['data'])
          .map((String k, dynamic v) => MapEntry<String, String>(k, v.toString())),
      createdAt: Codec.dateOrNow(map['createdAt']),
      readAt: Codec.date(map['readAt']),
    );
  }
}
