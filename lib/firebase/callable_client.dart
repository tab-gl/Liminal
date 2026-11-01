import 'package:cloud_functions/cloud_functions.dart';

import '../core/errors/error_mapper.dart';

/// Thin typed wrapper over Firebase callable functions.
///
/// Every privileged operation in Sproutly (moderation, campaign approval,
/// payment verification, campaign tracking) goes through here so error
/// normalisation and region selection happen in exactly one place.
class CallableClient {
  CallableClient({FirebaseFunctions? functions})
      : _functions = functions ?? FirebaseFunctions.instanceFor(region: _region);

  static const String _region = 'asia-south1';

  final FirebaseFunctions _functions;

  Future<Map<String, dynamic>> call(
    String name, [
    Map<String, dynamic> payload = const <String, dynamic>{},
  ]) async {
    try {
      final HttpsCallableResult<Map<String, dynamic>> result =
          await _functions.httpsCallable(name).call<Map<String, dynamic>>(payload);
      return result.data;
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  /// Fire-and-forget variant for telemetry where a failure must never block the
  /// user (impression and click beacons).
  void beacon(String name, Map<String, dynamic> payload) {
    _functions.httpsCallable(name).call<Map<String, dynamic>>(payload).ignore();
  }
}
