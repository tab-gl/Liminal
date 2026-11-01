import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';

/// Emits `true` when the device has a usable network path.
///
/// This is a hint, not a guarantee: Firestore's own offline cache is what keeps
/// the app usable when the network drops mid-session.
class ConnectivityService {
  ConnectivityService({Connectivity? connectivity})
      : _connectivity = connectivity ?? Connectivity();

  final Connectivity _connectivity;

  Stream<bool> get changes => _connectivity.onConnectivityChanged
      .map((List<ConnectivityResult> results) => results.any(
            (ConnectivityResult r) => r != ConnectivityResult.none,
          ));

  Future<bool> get isOnline async {
    final List<ConnectivityResult> results = await _connectivity.checkConnectivity();
    return results.any((ConnectivityResult r) => r != ConnectivityResult.none);
  }
}
