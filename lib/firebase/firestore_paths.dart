import 'package:cloud_firestore/cloud_firestore.dart';

/// Every Firestore path used by the app, in one place.
///
/// Collection names are constants so a typo is a compile error, and document
/// ids that encode two entities are built by the helpers below so the client and
/// the security rules can never disagree about their shape.
class FirestorePaths {
  const FirestorePaths._();

  static const String users = 'users';
  static const String applications = 'applications';
  static const String categories = 'categories';
  static const String reviews = 'reviews';
  static const String bookmarks = 'bookmarks';
  static const String developerFollows = 'developer_follows';
  static const String applicationEvents = 'application_events';
  static const String advertisingCampaigns = 'advertising_campaigns';
  static const String campaignEvents = 'campaign_events';
  static const String notifications = 'notifications';
  static const String reports = 'reports';
  static const String adminAuditLogs = 'admin_audit_logs';
  static const String payments = 'payments';
  static const String config = 'config';

  static FirebaseFirestore get _db => FirebaseFirestore.instance;

  static CollectionReference<Map<String, dynamic>> get usersRef => _db.collection(users);
  static CollectionReference<Map<String, dynamic>> get applicationsRef =>
      _db.collection(applications);
  static CollectionReference<Map<String, dynamic>> get categoriesRef => _db.collection(categories);
  static CollectionReference<Map<String, dynamic>> get reviewsRef => _db.collection(reviews);
  static CollectionReference<Map<String, dynamic>> get bookmarksRef => _db.collection(bookmarks);
  static CollectionReference<Map<String, dynamic>> get followsRef =>
      _db.collection(developerFollows);
  static CollectionReference<Map<String, dynamic>> get eventsRef =>
      _db.collection(applicationEvents);
  static CollectionReference<Map<String, dynamic>> get campaignsRef =>
      _db.collection(advertisingCampaigns);
  static CollectionReference<Map<String, dynamic>> get notificationsRef =>
      _db.collection(notifications);
  static CollectionReference<Map<String, dynamic>> get reportsRef => _db.collection(reports);
  static CollectionReference<Map<String, dynamic>> get auditRef =>
      _db.collection(adminAuditLogs);
  static CollectionReference<Map<String, dynamic>> get paymentsRef => _db.collection(payments);

  static DocumentReference<Map<String, dynamic>> user(String uid) => usersRef.doc(uid);

  static DocumentReference<Map<String, dynamic>> application(String appId) =>
      applicationsRef.doc(appId);

  /// Daily analytics rollup written by the scheduled aggregation function.
  static DocumentReference<Map<String, dynamic>> appAnalytics(String appId, String yyyyMmDd) =>
      application(appId).collection('analytics').doc(yyyyMmDd);

  static DocumentReference<Map<String, dynamic>> campaign(String campaignId) =>
      campaignsRef.doc(campaignId);

  static DocumentReference<Map<String, dynamic>> notification(String id) =>
      notificationsRef.doc(id);

  // Composite ids. These mirror the security rules exactly.
  static String bookmarkId(String uid, String applicationId) => '${uid}_$applicationId';
  static String reviewId(String applicationId, String uid) => '${applicationId}_$uid';
  static String followId(String followerId, String developerId) => '${followerId}_$developerId';
  static String eventId() => eventsRef.doc().id;
}
