import 'dart:async';
import 'dart:ui' show PlatformDispatcher;

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_app_check/firebase_app_check.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart' show WidgetsFlutterBinding, runApp;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/config/app_config.dart';
import '../core/config/firebase_options.dart';
import '../core/providers/ads_provider.dart';
import '../core/providers/app_providers.dart';
import '../core/services/ads_service.dart';
import '../core/services/analytics_service.dart';
import '../core/services/deep_link_service.dart';
import '../core/services/notification_service.dart';
import '../core/services/remote_config_service.dart';
import 'app.dart';

/// Startup, in two phases.
///
/// **Phase 1 (awaited, ~200 ms):** engine binding, Firebase init, crash
/// handlers. This is the only work that can block the first frame, because
/// `Firebase.initializeApp` is a hard prerequisite for the very first Firestore
/// and Auth read - and `FirebaseAuth.currentUser` is synchronously available the
/// moment it returns, which is what lets routing decide immediately instead of
/// showing a splash.
///
/// **Phase 2 (not awaited):** App Check, Remote Config, analytics, push, deep
/// links, AdMob. Each of these is network-bound or platform-channel-bound, and
/// none of them is needed to render Discover. They are started immediately after
/// `runApp` and every consumer tolerates them being cold: Remote Config serves
/// its compiled defaults, the banner retries until the SDK is ready, and the
/// router ignores a deep link that arrives a beat late.
///
/// There is no splash screen. The window between the native launch surface and
/// the first Flutter frame is one frame long.
Future<void> bootstrap() async {
  WidgetsFlutterBinding.ensureInitialized();

  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);

  if (AppConfig.useEmulators) {
    await _connectEmulators();
  }

  FlutterError.onError = (FlutterErrorDetails details) {
    FlutterError.presentError(details);
    if (!kDebugMode) FirebaseCrashlytics.instance.recordFlutterFatalError(details);
  };
  PlatformDispatcher.instance.onError = (Object error, StackTrace stack) {
    if (!kDebugMode) FirebaseCrashlytics.instance.recordError(error, stack, fatal: true);
    return true;
  };

  // Offline-first cache. Cheap and synchronous, so it belongs in phase 1.
  FirebaseFirestore.instance.settings = const Settings(
    persistenceEnabled: true,
    cacheSizeBytes: Settings.CACHE_SIZE_UNLIMITED,
  );

  // Constructed synchronously - constructors touch no network - so the
  // providers can be overridden before the first frame.
  final AnalyticsService analytics = AnalyticsService(posthogEnabled: true);
  final RemoteConfigService remoteConfig = RemoteConfigService();
  final NotificationService notifications = NotificationService();
  final DeepLinkService deepLinks = DeepLinkService();
  final AdsService ads = AdsService();

  runApp(
    ProviderScope(
      overrides: [
        analyticsProvider.overrideWithValue(analytics),
        remoteConfigProvider.overrideWithValue(remoteConfig),
        notificationServiceProvider.overrideWithValue(notifications),
        deepLinkServiceProvider.overrideWithValue(deepLinks),
        adsServiceProvider.overrideWithValue(ads),
      ],
      child: const SproutlyApp(),
    ),
  );

  // Phase 2 - fire and forget. Deliberately unawaited: awaiting any of these
  // would put a network round trip between launch and the first useful frame.
  unawaited(analytics.init());
  unawaited(remoteConfig.init());
  unawaited(notifications.init());
  unawaited(deepLinks.init());
  unawaited(ads.init());
  unawaited(SystemChrome.setPreferredOrientations(<DeviceOrientation>[
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
    DeviceOrientation.landscapeLeft,
    DeviceOrientation.landscapeRight,
  ]));

  // App Check is the one exception: un-attested requests are rejected outright
  // when enforcement is on, so production waits for it. Development does not,
  // because the debug provider needs a console-registered token.
  if (AppConfig.isProd) {
    await _activateAppCheck();
  } else {
    unawaited(_activateAppCheck());
  }
}

Future<void> _activateAppCheck() async {
  try {
    // No provider arguments: `activate()` already defaults to Play Integrity on
    // Android and DeviceCheck on Apple, and the explicit named arguments are not
    // part of this plugin version's signature. For local debugging, register the
    // debug token printed in the console instead of switching providers.
    await FirebaseAppCheck.instance.activate();
  } catch (error) {
    // A missing debug token or an unregistered app id must not stop launch.
    if (kDebugMode) debugPrint('Sproutly: App Check activation failed ($error)');
  }
}

Future<void> _connectEmulators() async {
  const String host = AppConfig.emulatorHost;
  FirebaseFirestore.instance.useFirestoreEmulator(host, 8080);
  await FirebaseAuth.instance.useAuthEmulator(host, 9099);
  FirebaseFunctions.instanceFor(region: 'asia-south1').useFunctionsEmulator(host, 5001);
  await FirebaseStorage.instance.useStorageEmulator(host, 9199);
}
