import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../firebase/callable_client.dart';
import '../../firebase/firestore_paths.dart';
import '../../shared/models/user_profile.dart';
import '../services/analytics_service.dart';
import '../services/connectivity_service.dart';
import '../services/deep_link_service.dart';
import '../services/install_service.dart';
import '../services/media_service.dart';
import '../services/notification_service.dart';
import '../services/payment_service.dart';
import '../services/remote_config_service.dart';
import '../services/secure_store.dart';

/// Service locator, Riverpod style.
///
/// Anything that needs async initialisation (analytics, remote config, push,
/// deep links) is created in bootstrap.dart and injected through ProviderScope
/// overrides, so no provider secretly performs I/O on first read.
final Provider<FirebaseAuth> firebaseAuthProvider =
    Provider<FirebaseAuth>((Ref ref) => FirebaseAuth.instance);

final Provider<FirebaseFirestore> firestoreProvider =
    Provider<FirebaseFirestore>((Ref ref) => FirebaseFirestore.instance);

final Provider<CallableClient> callableProvider =
    Provider<CallableClient>((Ref ref) => CallableClient());

final Provider<AnalyticsService> analyticsProvider =
    Provider<AnalyticsService>((Ref ref) => AnalyticsService(posthogEnabled: true));

final Provider<MediaService> mediaProvider =
    Provider<MediaService>((Ref ref) => MediaService());

final Provider<SecureStore> secureStoreProvider =
    Provider<SecureStore>((Ref ref) => SecureStore());

final Provider<ConnectivityService> connectivityProvider =
    Provider<ConnectivityService>((Ref ref) => ConnectivityService());

/// In-app install (APK hand-off + store links).
final Provider<InstallService> installServiceProvider =
    Provider<InstallService>((Ref ref) => const InstallService());

final Provider<PaymentService> paymentProvider = Provider<PaymentService>(
  (Ref ref) => PaymentService(callables: ref.watch(callableProvider)),
);

// Overridden in bootstrap with already-initialised instances.
final Provider<RemoteConfigService> remoteConfigProvider =
    Provider<RemoteConfigService>((Ref ref) => RemoteConfigService());
final Provider<NotificationService> notificationServiceProvider =
    Provider<NotificationService>((Ref ref) => NotificationService());
final Provider<DeepLinkService> deepLinkServiceProvider =
    Provider<DeepLinkService>((Ref ref) => DeepLinkService());

/// Raw auth stream. Fires on sign-in, sign-out and token refresh.
final StreamProvider<User?> authStateProvider = StreamProvider<User?>(
  (Ref ref) => ref.watch(firebaseAuthProvider).authStateChanges(),
);

/// Synchronous snapshot of the persisted session.
///
/// `FirebaseAuth.currentUser` is populated the instant `initializeApp` returns,
/// before any stream emits. Routing reads this so the first frame can already
/// decide where to go - which is what removes the need for a splash screen.
final Provider<User?> initialUserProvider =
    Provider<User?>((Ref ref) => ref.watch(firebaseAuthProvider).currentUser);

/// The signed-in Firebase user, or null. A null during startup means
/// "not resolved yet", which the router treats as loading.
final Provider<User?> currentUserProvider = Provider<User?>((Ref ref) {
  return ref.watch(authStateProvider).value;
});

/// The user's Firestore profile, kept live so a role change (an admin promoting
/// someone to developer) unlocks new surfaces without a restart.
final StreamProvider<UserProfile?> userProfileProvider =
    StreamProvider<UserProfile?>((Ref ref) {
  final User? user = ref.watch(currentUserProvider);
  if (user == null) return Stream<UserProfile?>.value(null);
  return FirestorePaths.user(user.uid)
      .snapshots()
      .map((DocumentSnapshot<Map<String, dynamic>> snap) {
    final Map<String, dynamic>? data = snap.data();
    if (!snap.exists || data == null) return null;
    return UserProfile.fromMap(snap.id, data);
  });
});

final Provider<UserProfile?> profileOrNullProvider = Provider<UserProfile?>((Ref ref) {
  return ref.watch(userProfileProvider).value;
});

final Provider<bool> isAdminProvider = Provider<bool>((Ref ref) {
  return ref.watch(profileOrNullProvider)?.isAdmin ?? false;
});

/// Resolves the trusted Firebase Auth role claim for privileged UI surfaces.
/// Force-refresh once per provider evaluation so newly granted/revoked roles
/// are reflected without trusting a mutable profile field.
final FutureProvider<bool> adminClaimProvider = FutureProvider<bool>((Ref ref) async {
  final User? user = ref.watch(currentUserProvider) ?? ref.watch(initialUserProvider);
  if (user == null) return false;
  final IdTokenResult token = await user.getIdTokenResult(true);
  return token.claims?['role'] == 'admin';
});

final Provider<bool> canPublishProvider = Provider<bool>((Ref ref) {
  return ref.watch(profileOrNullProvider)?.canPublish ?? false;
});
