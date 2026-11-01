import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:url_launcher/url_launcher.dart';

/// What happened when someone pressed "Install".
enum InstallOutcome {
  /// The system package installer was launched with a downloaded APK.
  installerLaunched,

  /// A store listing (Play Store / App Store) was opened. This is the only
  /// install path a third-party app is allowed to offer for store apps.
  storeOpened,

  /// Android refused the "install unknown apps" grant.
  permissionDenied,

  /// The platform cannot install this kind of package at all (iOS + APK).
  unsupported,

  /// Download or hand-off failed.
  failed,

  /// The listing has no link to install from.
  noLink,
}

class InstallResult {
  const InstallResult(this.outcome, this.message, {this.canOpenSettings = false});

  final InstallOutcome outcome;
  final String message;

  /// True when the fix is in system settings, so the UI can offer the shortcut.
  final bool canOpenSettings;

  bool get isSuccess =>
      outcome == InstallOutcome.installerLaunched || outcome == InstallOutcome.storeOpened;
}

/// Installs an app from a listing, rather than just opening a browser.
///
/// Three paths, decided by the URL:
///
///  * **Direct `.apk`** (Android): request `REQUEST_INSTALL_PACKAGES`, stream the
///    file to the cache directory with progress, then hand it to the system
///    installer through `open_filex`'s FileProvider. The user still confirms the
///    install in the OS dialog - no app can install silently, by design.
///  * **Play Store / App Store links**: open the native store client
///    (`market://` when the package id is known). Google Play's policy requires
///    store-distributed apps to be installed through Play, so there is
///    deliberately no APK side-load for those.
///  * **Anything else** (a web app): open it in the browser.
///
/// iOS has no side-loading path at all, so an `.apk` link reports
/// [InstallOutcome.unsupported] with an explanation instead of failing silently.
class InstallService {
  const InstallService();

  static const String _apkMime = 'application/vnd.android.package-archive';

  Future<InstallResult> install(String? url, {void Function(double)? onProgress}) async {
    final Uri? uri = url == null ? null : Uri.tryParse(url);
    if (uri == null || !uri.hasScheme) {
      return const InstallResult(InstallOutcome.noLink, 'This listing has no install link yet.');
    }

    final String host = uri.host.toLowerCase();
    final String path = uri.path.toLowerCase();

    if (host.contains('play.google.com')) return _openPlayStore(uri);
    if (host.contains('apps.apple.com') || host.contains('itunes.apple.com')) {
      return _openInBrowser(uri, 'Opening the App Store.');
    }
    if (host == 'market') return _openInBrowser(uri, 'Opening the Play Store.');
    if (path.endsWith('.apk')) return _installApk(uri, onProgress);

    return _openInBrowser(uri, 'Opening ${host.isEmpty ? "the link" : host}.');
  }

  Future<InstallResult> _openPlayStore(Uri uri) async {
    final String? packageId = uri.queryParameters['id'];
    if (packageId != null && Platform.isAndroid) {
      final Uri market = Uri.parse('market://details?id=$packageId');
      if (await launchUrl(market, mode: LaunchMode.externalApplication)) {
        return const InstallResult(InstallOutcome.storeOpened, 'Opening Google Play.');
      }
    }
    return _openInBrowser(uri, 'Opening Google Play in the browser.');
  }

  Future<InstallResult> _openInBrowser(Uri uri, String message) async {
    final bool opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
    return InstallResult(
      opened ? InstallOutcome.storeOpened : InstallOutcome.failed,
      opened ? message : 'Could not open that link.',
    );
  }

  Future<InstallResult> _installApk(Uri uri, void Function(double)? onProgress) async {
    if (!Platform.isAndroid) {
      return const InstallResult(
        InstallOutcome.unsupported,
        'iOS can only install apps from the App Store, so this download cannot be installed here.',
      );
    }
    if (uri.scheme != 'https') {
      return const InstallResult(
        InstallOutcome.failed,
        'Sproutly only installs packages over a secure https link.',
      );
    }

    // Android 8+ requires an explicit grant per app before an install intent is
    // honoured. Ask, do not assume.
    PermissionStatus status = await Permission.requestInstallPackages.status;
    if (!status.isGranted) {
      status = await Permission.requestInstallPackages.request();
    }
    if (!status.isGranted) {
      return InstallResult(
        InstallOutcome.permissionDenied,
        'Android needs permission to install apps from Sproutly. Enable '
        '"Install unknown apps" for Sproutly, then try again.',
        canOpenSettings: status.isPermanentlyDenied || status.isDenied,
      );
    }

    try {
      final File file = await _download(uri, onProgress);
      final OpenResult result = await OpenFilex.open(file.path, type: _apkMime);
      switch (result.type) {
        case ResultType.done:
          return const InstallResult(
            InstallOutcome.installerLaunched,
            'Confirm the install in the Android dialog.',
          );
        case ResultType.permissionDenied:
          return const InstallResult(
            InstallOutcome.permissionDenied,
            'Android blocked the installer. Allow "Install unknown apps" for Sproutly.',
            canOpenSettings: true,
          );
        case ResultType.noAppToOpen:
          return const InstallResult(
            InstallOutcome.failed,
            'This device has no package installer available.',
          );
        case ResultType.fileNotFound:
          return const InstallResult(
            InstallOutcome.failed,
            'The download did not finish. Try again.',
          );
        case ResultType.error:
          return InstallResult(InstallOutcome.failed, result.message);
      }
    } catch (error) {
      if (kDebugMode) debugPrint('Sproutly: install failed ($error)');
      return const InstallResult(
        InstallOutcome.failed,
        'The download failed. Check your connection and try again.',
      );
    }
  }

  /// Streams the APK into the app cache, reporting progress. Written with
  /// `HttpClient` rather than a package so there is no extra dependency and no
  /// in-memory buffer for a file that can be 100 MB.
  Future<File> _download(Uri uri, void Function(double)? onProgress) async {
    final Directory dir = await getTemporaryDirectory();
    final File file = File(
      '${dir.path}/sproutly_${DateTime.now().millisecondsSinceEpoch}.apk',
    );
    final HttpClient client = HttpClient();
    try {
      final HttpClientRequest request = await client.getUrl(uri);
      request.headers.set(HttpHeaders.acceptHeader, 'application/octet-stream');
      final HttpClientResponse response = await request.close();
      if (response.statusCode != HttpStatus.ok) {
        throw HttpException('Download failed with ${response.statusCode}');
      }
      final int total = response.contentLength;
      final IOSink sink = file.openWrite();
      int received = 0;
      try {
        await for (final List<int> chunk in response) {
          sink.add(chunk);
          received += chunk.length;
          if (total > 0) onProgress?.call((received / total).clamp(0, 1));
        }
      } finally {
        await sink.close();
      }
      onProgress?.call(1);
      return file;
    } finally {
      client.close(force: true);
    }
  }
}
