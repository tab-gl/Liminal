# AdMob

Sproutly runs two kinds of advertising, and they are deliberately different
things:

1. **Sproutly's own ad inventory** (this document) - Google AdMob banners that
   pay for hosting, so publishing stays free.
2. **Campaigns sold to developers** (`docs/ADVERTISING.md`) - server-side
   placements inside the marketplace itself.

A Sponsored card sold to a developer always outranks a house ad; AdMob is the
floor of revenue, not the ceiling.

## Credentials

| Value | Where it lives |
| --- | --- |
| App id `ca-app-pub-9808362123414569~3261485863` | Native manifests (Android `AndroidManifest.xml`, iOS `Info.plist`) and `AppConfig.admobAppId` |
| Banner unit `ca-app-pub-9808362123414569/3733801140` | `AppConfig.admobBannerUnitId`, overridable with `--dart-define=ADMOB_BANNER_UNIT_ID` |
| Interstitial unit | **Not configured yet** - see below |

Neither value is a secret. An AdMob app id and unit id ship inside every binary
by design; the account is protected by the AdMob console, not by hiding them.

### Interstitials

The account currently has one ad unit, which is a **banner** unit. Loading a
banner unit into an interstitial slot fails, so
`AppConfig.admobInterstitialUnitId` is intentionally empty and
`AdsService.interstitialUnitId` returns null in release builds - the app simply
does not attempt interstitials. In debug it uses Google's test interstitial, so
the code path stays exercised.

To turn them on: create an interstitial unit in AdMob, then either edit the
default in `lib/core/config/app_config.dart` or build with
`--dart-define=ADMOB_INTERSTITIAL_UNIT_ID=ca-app-pub-.../...`.

## Platform setup

The AdMob SDK reads the app id from the native manifest **before Dart starts**,
so it cannot come from `--dart-define` alone. This repository ships no `android/`
or `ios/` folders (they are generated), so run this once after
`flutter create .`:

```bash
bash tool/configure_platform.sh
```

It is idempotent, and it adds:

- **Android** - `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID">`
  inside `<application>`, plus an explicit `INTERNET` permission.
- **iOS** - `GADApplicationIdentifier`, an `NSUserTrackingUsageDescription` for
  App Tracking Transparency, and a starter `SKAdNetworkItems` array.

Verify after running: `git diff android/app/src/main/AndroidManifest.xml`.

## Code

| File | Role |
| --- | --- |
| `lib/core/services/ads_service.dart` | Initialisation, test-unit substitution, request building, interstitial pre-load/show |
| `lib/core/widgets/sproutly_banner_ad.dart` | The banner placement widget |
| `lib/core/providers/ads_provider.dart` | Riverpod access to the service |
| `lib/app/bootstrap.dart` | Initialises the SDK once, before `runApp` |

### Where ads appear

| Placement | Screen | Position |
| --- | --- | --- |
| `discover_feed` | Discover | End of the paginated feed, after real content |
| `app_detail` | Listing page | Below the reviews |

Both are 320×50 banners inside the scroll view. There is no ad on first paint, no
interstitial on app launch, and no ad on the publishing flow or the dashboard -
a developer doing work should never be interrupted by an ad.

### The four rules the service enforces

1. **Never serve a production ad in a debug build.** `bannerUnitId` returns
   Google's test unit when `kDebugMode` is true. This is not a nicety: clicking
   your own live ads is the fastest way to get an AdMob account suspended.
2. **One initialisation.** `MobileAds.instance.initialize()` is awaited in
   bootstrap and the initialised instance is injected through
   `adsServiceProvider.overrideWithValue`.
3. **Consent is a request parameter, not a comment.** `buildRequest()` returns
   `AdRequest(nonPersonalizedAds: !personalised)`, so there is exactly one place
   where the consent decision is applied and no caller can forget it.
4. **A failed ad is invisible.** `SproutlyBannerAd` collapses to zero height on
   failure, retries once after 30 seconds, then gives up for that screen. It
   disposes the `BannerAd` in `dispose()`, which is required - a leaked banner
   keeps a platform view alive.

## Consent

Personalised ads are on by default and can be switched off in **Settings → Ads →
Personalised ads**. The preference persists in the secure store and is applied to
the live service immediately, so the next request is non-personalised.

For users in the EEA/UK, GDPR requires a **Google-certified CMP** before serving
personalised ads. Google's UMP SDK is native-only; the clean options are:

- integrate UMP natively in `MainActivity`/`AppDelegate` and expose the result
  over a method channel, then call `AdsService.setPersonalised(...)`; or
- use a community Flutter UMP plugin and call the same method.

Until that is wired, the correct launch posture for EEA traffic is to default
`adsPersonalised` to `false` for those locales - `AdsService.setPersonalised`
is the single switch to drive from the consent result.

## Testing

```bash
flutter run            # test unit ids, real layout
flutter run --release --dart-define=SPROUTLY_ENV=prod   # real unit ids
```

In debug, the log prints a test-device hash on first run; add it to
`AdsService.testDeviceIds` so your own device receives test ads even in a
release build.

Checklist before shipping:

1. `bash tool/configure_platform.sh` has been run and the diff looks right.
2. The app id in AdMob matches the package id / bundle id exactly.
3. Payments profile and tax information are complete, otherwise earnings are
   held.
4. `ads.txt` is not needed for apps - only for web.
5. No ad sits above the fold on first launch, and no ad appears on a screen a
   developer uses to do work.
