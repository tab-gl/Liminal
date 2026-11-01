/// Compile-time configuration.
///
/// Nothing secret lives here. Every value is injected at build time:
///
/// ```sh
/// flutter run \
///   --dart-define=SPROUTLY_ENV=dev \
///   --dart-define=RAZORPAY_KEY_ID=rzp_test_xxxxxxxx
/// ```
///
/// Secret keys (Razorpay key *secret*, Stripe secret key) are never shipped to
/// the client: they live in Cloud Functions environment config only.
class AppConfig {
  const AppConfig._();

  static const String environment =
      String.fromEnvironment('SPROUTLY_ENV', defaultValue: 'dev');

  /// Public Razorpay key id. Publishable by design; the secret stays server side.
  static const String razorpayKeyId =
      String.fromEnvironment('RAZORPAY_KEY_ID', defaultValue: '');

  /// Public Stripe publishable key.
  static const String stripePublishableKey =
      String.fromEnvironment('STRIPE_PUBLISHABLE_KEY', defaultValue: '');

  /// Optional PostHog project key. Analytics stays disabled when empty.
  static const String posthogApiKey =
      String.fromEnvironment('POSTHOG_API_KEY', defaultValue: '');
  static const String posthogHost =
      String.fromEnvironment('POSTHOG_HOST', defaultValue: 'https://eu.i.posthog.com');

  /// AdMob. The app id belongs in the platform manifests (see
  /// tool/configure_ads_platform.sh); the unit ids are read here.
  ///
  /// These are publishable values - an AdMob app id and unit id ship inside
  /// every binary. Nothing secret is involved, which is why they are defaults
  /// rather than required dart-defines.
  static const String admobAppId =
      String.fromEnvironment('ADMOB_APP_ID', defaultValue: 'ca-app-pub-9808362123414569~3261485863');

  /// Banner unit. Debug builds substitute Google's test unit automatically -
  /// see AdsService.unitId().
  static const String admobBannerUnitId = String.fromEnvironment(
    'ADMOB_BANNER_UNIT_ID',
    defaultValue: 'ca-app-pub-9808362123414569/3733801140',
  );

  /// Interstitial unit. Left empty on purpose: the account currently has one
  /// ad unit, and serving a banner unit as an interstitial fails. Create an
  /// interstitial unit in AdMob, then pass it with
  /// --dart-define=ADMOB_INTERSTITIAL_UNIT_ID=... or set it here.
  static const String admobInterstitialUnitId =
      String.fromEnvironment('ADMOB_INTERSTITIAL_UNIT_ID', defaultValue: '');

  /// Emulator host, e.g. 10.0.2.2 on an Android emulator.
  static const String emulatorHost =
      String.fromEnvironment('FIREBASE_EMULATOR_HOST', defaultValue: '10.0.2.2');
  static const bool useEmulators =
      bool.fromEnvironment('USE_FIREBASE_EMULATORS', defaultValue: false);

  static bool get isProd => environment == 'prod';
  static bool get isDev => environment == 'dev';

  static bool get hasRazorpay => razorpayKeyId.isNotEmpty;
  static bool get hasStripe => stripePublishableKey.isNotEmpty;
  static bool get hasPosthog => posthogApiKey.isNotEmpty;

  static bool get hasInterstitialUnit => admobInterstitialUnitId.isNotEmpty;
}
