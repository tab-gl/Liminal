/// A single error type the whole app throws and renders.
///
/// Everything crossing the data layer is normalised into an [AppException] so
/// the UI never has to know about FirebaseException, Razorpay codes or
/// PlatformException.
enum AppErrorCode {
  network,
  unauthenticated,
  permissionDenied,
  notFound,
  alreadyExists,
  invalidInput,
  quotaExceeded,
  paymentFailed,
  paymentCancelled,
  moderation,
  unknown,
}

class AppException implements Exception {
  const AppException(
    this.code,
    this.message, {
    this.cause,
    this.retryable = false,
  });

  final AppErrorCode code;

  /// Human-readable, safe to show to a user. Never contains stack traces.
  final String message;
  final Object? cause;
  final bool retryable;

  const AppException.network([String? message])
      : this(
          AppErrorCode.network,
          message ?? 'You appear to be offline. Check your connection.',
          retryable: true,
        );

  const AppException.unauthenticated([String? message])
      : this(
          AppErrorCode.unauthenticated,
          message ?? 'Please sign in to continue.',
        );

  const AppException.permissionDenied([String? message])
      : this(
          AppErrorCode.permissionDenied,
          message ?? 'You do not have access to this.',
        );

  const AppException.notFound([String? message])
      : this(
          AppErrorCode.notFound,
          message ?? 'We could not find that.',
        );

  const AppException.invalidInput(String message)
      : this(AppErrorCode.invalidInput, message);

  const AppException.moderation([String? message])
      : this(
          AppErrorCode.moderation,
          message ?? 'That content did not pass review.',
        );

  const AppException.unknown([String? message])
      : this(
          AppErrorCode.unknown,
          message ?? 'Something went wrong. Please try again.',
          retryable: true,
        );

  @override
  String toString() => 'AppException(${code.name}): $message';
}
