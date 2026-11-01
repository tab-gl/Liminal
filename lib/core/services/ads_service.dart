import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';

import '../config/app_config.dart';

/// AdMob.
///
/// Rules this service enforces, so no screen has to think about them:
///
///  * **Never serve a production ad in debug.** A debug build uses Google's
///    official test unit ids, which cannot be accidentally clicked for revenue
///    and which will not get the account flagged for invalid traffic.
///  * **One initialisation.** `MobileAds.instance.initialize()` is called once,
///    from bootstrap, and every request waits on [ready].
///  * **Consent is respected.** When the person has not consented to
///    personalisation, every request is built with
///    `nonPersonalizedAds: true`. See docs/ADS.md for the UMP flow.
///  * **Failures are silent.** An ad that cannot load returns null; the layout
///    collapses and the app carries on.
class AdsService {
  AdsService({bool? personalised}) : personalised = personalised ?? true;

  static const String testBannerUnitId = 'ca-app-pub-3940256099942544/6300978111';
  static const String testInterstitialUnitId = 'ca-app-pub-3940256099942544/1033173712';
  static const String testRewardedUnitId = 'ca-app-pub-3940256099942544/5224354917';

  /// Whether personalised advertising is allowed for this person.
  bool personalised;

  bool _initialised = false;
  InterstitialAd? _interstitial;
  bool _loadingInterstitial = false;

  bool get isReady => _initialised;

  /// Banner unit id, swapped for a test unit in debug builds.
  String get bannerUnitId =>
      kDebugMode ? testBannerUnitId : AppConfig.admobBannerUnitId;

  /// Interstitial unit id. Falls back to Google's test unit in debug, and to
  /// null when no interstitial unit is configured for release - serving a
  /// banner unit in a full-screen slot simply fails, so we do not try.
  String? get interstitialUnitId {
    if (kDebugMode) return testInterstitialUnitId;
    return AppConfig.hasInterstitialUnit ? AppConfig.admobInterstitialUnitId : null;
  }

  /// Test devices receive test ads even in a release build, which is what you
  /// want on your own phone while reviewing a store build.
  static const List<String> testDeviceIds = <String>[];

  Future<void> init() async {
    if (_initialised) return;
    try {
      await MobileAds.instance.updateRequestConfiguration(
        RequestConfiguration(
          testDeviceIds: testDeviceIds,
          // Sproutly is a developer marketplace, not a children's app.
          tagForChildDirectedTreatment: TagForChildDirectedTreatment.unspecified,
          tagForUnderAgeOfConsent: TagForUnderAgeOfConsent.unspecified,
          maxAdContentRating: MaxAdContentRating.g,
        ),
      );
      await MobileAds.instance.initialize();
      _initialised = true;
    } catch (error) {
      // A device without Play Services, or a missing app id in the manifest,
      // must not stop the app from starting.
      if (kDebugMode) debugPrint('Sproutly: AdMob init failed ($error)');
    }
  }

  /// The request used everywhere, so the consent flag cannot be forgotten.
  AdRequest buildRequest() => AdRequest(nonPersonalizedAds: !personalised);

  Future<void> setPersonalised(bool value) async {
    personalised = value;
    // Drop anything already loaded so the next ad reflects the new choice.
    _interstitial?.dispose();
    _interstitial = null;
  }

  // ------------------------------------------------------------- interstitial

  /// Pre-loads an interstitial. Call this on a screen the person is already
  /// reading so the ad is ready by the time it would be shown.
  Future<void> preloadInterstitial() async {
    final String? unitId = interstitialUnitId;
    if (!_initialised || unitId == null || _loadingInterstitial || _interstitial != null) {
      return;
    }
    _loadingInterstitial = true;
    try {
      await InterstitialAd.load(
        adUnitId: unitId,
        request: buildRequest(),
        adLoadCallback: InterstitialAdLoadCallback(
          onAdLoaded: (InterstitialAd ad) {
            _interstitial = ad;
            _loadingInterstitial = false;
            ad.fullScreenContentCallback = FullScreenContentCallback(
              onAdDismissedFullScreenContent: (InterstitialAd ad) {
                ad.dispose();
                _interstitial = null;
              },
              onAdFailedToShowFullScreenContent: (InterstitialAd ad, AdError error) {
                ad.dispose();
                _interstitial = null;
              },
            );
          },
          onAdFailedToLoad: (LoadAdError error) {
            _loadingInterstitial = false;
            if (kDebugMode) debugPrint('Sproutly: interstitial failed (${error.code})');
          },
        ),
      );
    } catch (error) {
      _loadingInterstitial = false;
      if (kDebugMode) debugPrint('Sproutly: interstitial threw ($error)');
    }
  }

  /// Shows a pre-loaded interstitial. Returns false when nothing was ready -
  /// the caller should never block a user action waiting for an ad.
  bool showInterstitial() {
    final InterstitialAd? ad = _interstitial;
    if (ad == null) return false;
    _interstitial = null;
    unawaited(ad.show());
    return true;
  }

  bool get hasInterstitial => _interstitial != null;

  void dispose() {
    _interstitial?.dispose();
    _interstitial = null;
  }
}
