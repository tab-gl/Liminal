import 'dart:async';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';

import '../../firebase/firestore_paths.dart';

/// Entry point for messages delivered while the app is terminated or in the
/// background. Must stay top level and annotated so the engine can find it.
@pragma('vm:entry-point')
Future<void> sproutlyBackgroundHandler(RemoteMessage message) async {
  // Data-only payloads are rendered by the system tray notification configured
  // by the sender. Nothing to do here beyond logging in debug builds.
  if (kDebugMode) {
    debugPrint('Sproutly: background message ${message.messageId}');
  }
}

/// Push notifications and permission handling.
///
/// Permissions are requested contextually - only when the user first reaches a
/// surface that benefits from them (following a developer, publishing, or
/// opening Activity) - never on first launch.
class NotificationService {
  NotificationService({FirebaseMessaging? messaging})
      : _messaging = messaging ?? FirebaseMessaging.instance;

  final FirebaseMessaging _messaging;
  final StreamController<RemoteMessage> _opened = StreamController<RemoteMessage>.broadcast();

  Stream<RemoteMessage> get onNotificationOpened => _opened.stream;

  /// Foreground messages: surfaced in-app by the Activity screen.
  Stream<RemoteMessage> get onForeground => FirebaseMessaging.onMessage;

  Future<void> init() async {
    FirebaseMessaging.onBackgroundMessage(sproutlyBackgroundHandler);
    await _messaging.setForegroundNotificationPresentationOptions(
      alert: false,
      badge: true,
      sound: false,
    );
    FirebaseMessaging.onMessageOpenedApp.listen(_opened.add);
    final RemoteMessage? initial = await _messaging.getInitialMessage();
    if (initial != null) _opened.add(initial);
  }

  /// Returns true when the user granted (or already had) permission.
  Future<bool> requestPermission() async {
    final NotificationSettings settings = await _messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
      provisional: false,
    );
    return settings.authorizationStatus == AuthorizationStatus.authorized ||
        settings.authorizationStatus == AuthorizationStatus.provisional;
  }

  Future<NotificationSettings> status() => _messaging.getNotificationSettings();

  /// Persists the device token on the user's private profile document. The
  /// Cloud Function that sends pushes reads it from there.
  Future<void> syncToken(String uid) async {
    try {
      final String? token = await _messaging.getToken();
      if (token == null) return;
      await FirestorePaths.user(uid).collection('private').doc('devices').set(
        <String, dynamic>{
          'fcmToken': token,
          'platform': defaultTargetPlatform.name,
          'updatedAt': DateTime.now().toUtc(),
        },
        SetOptions(merge: true),
      );
      _messaging.onTokenRefresh.listen((String fresh) async {
        await FirestorePaths.user(uid).collection('private').doc('devices').set(
          <String, dynamic>{'fcmToken': fresh, 'updatedAt': DateTime.now().toUtc()},
          SetOptions(merge: true),
        );
      });
    } catch (error) {
      // A missing token (no Play Services, permission denied) is not fatal.
      if (kDebugMode) debugPrint('Sproutly: token sync skipped ($error)');
    }
  }

  /// Deep-link payload of a notification tap, e.g. "/app/abc123".
  static String? routeOf(RemoteMessage message) {
    final Object? route = message.data['route'];
    return route is String && route.startsWith('/') ? route : null;
  }

  Future<void> clearToken() async {
    try {
      await _messaging.deleteToken();
    } catch (_) {
      // ignored
    }
  }
}
