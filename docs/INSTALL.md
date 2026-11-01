# Install flow

"Install" installs. It does not quietly open a browser and hope.

`lib/core/services/install_service.dart` picks one of three paths from the
listing's primary link, and the UI (`_InstallSheet` on the listing page) shows
progress and the outcome rather than navigating away.

## 1. Direct `.apk` (Android)

The real install path:

1. **Permission.** `Permission.requestInstallPackages.request()` triggers
   Android's "install unknown apps" grant for Sproutly specifically. If the
   person refuses, the sheet offers a shortcut straight to the system settings
   screen (`openAppSettings()`) instead of dead-ending on an error.
2. **Download.** The APK streams over `HttpClient` into the app cache
   (`getTemporaryDirectory()`) chunk by chunk, reporting progress to the sheet.
   Streaming, not buffering - a 100 MB package never sits in memory.
3. **Hand-off.** `OpenFilex.open(path, type: 'application/vnd.android.package-archive')`
   passes the file to the system package installer through the plugin's own
   FileProvider. Android then shows its own confirmation dialog.

**Android still asks for confirmation.** No app can install silently, and
Sproutly does not try: `REQUEST_INSTALL_PACKAGES` only lets us *ask* the
installer to run. Only `https://` links are accepted, so a listing cannot push a
plaintext package.

Outcomes map to `ResultType`: `done` → "confirm the install in the Android
dialog", `permissionDenied` → settings shortcut, `fileNotFound` → retry,
`noAppToOpen` → the device has no installer.

## 2. Play Store / App Store links

`play.google.com/store/apps/details?id=<package>` is rewritten to
`market://details?id=<package>` so the native Play Store client opens on the app
page rather than a web page. If the market scheme is unavailable, it falls back
to the https URL.

Google Play policy requires Play-distributed apps to be installed through Play,
so there is deliberately **no** APK side-load for a store listing. The same
applies to `apps.apple.com`.

## 3. Anything else

A web app, a TestFlight page, a landing site - opened in the external browser,
which is the only sensible action. The button label reflects this: "Install app"
for an APK, "Get the app" for a store link, "Open app" otherwise.

## iOS

There is no side-loading on iOS. An `.apk` link returns
`InstallOutcome.unsupported` with an explanation and a pointer to the store link,
rather than failing silently or pretending to download.

## Native setup

`REQUEST_INSTALL_PACKAGES` must be in the manifest - run once after
`flutter create .`:

```bash
./tool/configure_platform.sh
```

It also injects the AdMob app id and `INTERNET`. The script is idempotent and
tested against sample manifest/plist files.

## Why not a package that "installs APKs" wholesale

Two candidate plugins exist (`install_plugin`, `flutter_install_plugin`); both
wrap the same platform session API and both add a dependency for something
`open_filex` + `permission_handler` already do. Sproutly downloads the file
itself, which means the download can be cancelled, retried, size-checked and
reported honestly - a black-box "install this URL" call cannot.

## What is deliberately not done

- **No silent install, no device-admin tricks, no accessibility-service abuse.**
  Those are the techniques malware uses to install without consent.
- **No auto-install on tap.** The sheet is a deliberate step: the person sees
  what is downloading and confirms in the OS dialog.
- **No MD5-only verification.** Integrity would need a publisher signature or a
  hash published by the developer; a self-reported hash is theatre. If you want
  real verification, add a `packageHash` field to the listing and check it after
  download - the hook is `_download()` in `install_service.dart`.
