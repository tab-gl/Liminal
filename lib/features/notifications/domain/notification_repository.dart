import '../../../shared/models/app_notification.dart';

abstract class NotificationRepository {
  Stream<List<AppNotification>> watchInbox({int limit});

  Stream<int> watchUnreadCount();

  Future<void> markRead(String notificationId);

  Future<void> markAllRead();
}
