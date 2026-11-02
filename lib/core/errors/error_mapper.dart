import 'dart:async';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';

import 'app_exception.dart';

/// Default copy per error code, so a mapper branch only has to supply a cause.
String defaultMessageFor(AppErrorCode code) => switch (code) {
      AppErrorCode.network => 'You appear to be offline. Check your connection.',
      AppErrorCode.unauthenticated => 'Please sign in to continue.',
      AppErrorCode.permissionDenied => 'You do not have access to this.',
      AppErrorCode.notFound => 'We could not find that.',
      AppErrorCode.alreadyExists => 'That already exists.',
      AppErrorCode.invalidInput => 'That request could not be processed.',
      AppErrorCode.quotaExceeded => 'Slow down a moment and try again.',
      AppErrorCode.paymentFailed => 'The payment did not go through.',
      AppErrorCode.paymentCancelled => 'Payment cancelled.',
      AppErrorCode.moderation => 'That content did not pass review.',
      AppErrorCode.unknown => 'Something went wrong. Please try again.',
    };

/// Builds an [AppException] from a code and an underlying error.
AppException _wrap(
  AppErrorCode code,
  Object cause, {
  String? message,
  bool? retryable,
}) =>
    AppException(
      code,
      message ?? defaultMessageFor(code),
      cause: cause,
      retryable: retryable ??
          (code == AppErrorCode.network || code == AppErrorCode.unknown),
    );

/// Turns platform exceptions into [AppException]s with copy a person can read.
AppException mapError(Object error, [StackTrace? stack]) {
  if (error is AppException) return error;

  if (error is FirebaseAuthException) {
    switch (error.code) {
      case 'invalid-email':
        return AppException(AppErrorCode.invalidInput, 'That email address looks incomplete.', cause: error);
      case 'user-disabled':
        return AppException(AppErrorCode.permissionDenied, 'This account has been disabled.', cause: error);
      case 'user-not-found':
      case 'wrong-password':
      case 'invalid-credential':
        return AppException(AppErrorCode.unauthenticated, 'Email or password is incorrect.', cause: error);
      case 'email-already-in-use':
        return AppException(AppErrorCode.alreadyExists, 'An account already uses this email.', cause: error);
      case 'weak-password':
        return AppException(AppErrorCode.invalidInput, 'Choose a password of at least 8 characters.', cause: error);
      case 'too-many-requests':
        return AppException(AppErrorCode.quotaExceeded, 'Too many attempts. Try again in a few minutes.', cause: error);
      case 'requires-recent-login':
        return AppException(AppErrorCode.permissionDenied, 'Please sign in again to finish this change.', cause: error);
      case 'network-request-failed':
        return AppException.network('Sign-in failed because the network dropped.');
      case 'account-exists-with-different-credential':
        return AppException(
          AppErrorCode.alreadyExists,
          'That email already signs in with a different method. Use it, then link Google from Settings.',
          cause: error,
        );
      default:
        return AppException(AppErrorCode.unknown, error.message ?? 'Sign-in failed.', cause: error);
    }
  }

  if (error is FirebaseFunctionsException) {
    switch (error.code) {
      case 'unauthenticated':
        return _wrap(AppErrorCode.unauthenticated, error);
      case 'permission-denied':
        return _wrap(AppErrorCode.permissionDenied, error);
      case 'not-found':
        return _wrap(AppErrorCode.notFound, error);
      case 'invalid-argument':
      case 'failed-precondition':
        return AppException(
          AppErrorCode.invalidInput,
          error.message ?? 'That request could not be processed.',
          cause: error,
        );
      case 'resource-exhausted':
        return AppException(AppErrorCode.quotaExceeded, 'Slow down a moment and try again.', cause: error);
      case 'unavailable':
      case 'deadline-exceeded':
        return AppException.network('The server took too long to answer. Try again.');
      default:
        return AppException(
          AppErrorCode.unknown,
          error.message ?? 'The request failed.',
          cause: error,
          retryable: true,
        );
    }
  }

  if (error is FirebaseException) {
    switch (error.code) {
      case 'permission-denied':
        return _wrap(AppErrorCode.permissionDenied, error);
      case 'unavailable':
      case 'deadline-exceeded':
      case 'aborted':
        return _wrap(AppErrorCode.network, error);
      case 'not-found':
        return _wrap(AppErrorCode.notFound, error);
      case 'resource-exhausted':
        return AppException(AppErrorCode.quotaExceeded, 'Sproutly is busy right now. Try again shortly.', cause: error);
      case 'unauthenticated':
        return _wrap(AppErrorCode.unauthenticated, error);
      default:
        return _wrap(AppErrorCode.unknown, error, retryable: true);
    }
  }

  if (error is TimeoutException) {
    return AppException.network('That took too long. Check your connection and retry.');
  }

  return _wrap(AppErrorCode.unknown, error, retryable: true);
}

/// Runs [body], converting any thrown error into an [AppException].
Future<T> guard<T>(Future<T> Function() body) async {
  try {
    return await body();
  } catch (error, stack) {
    throw mapError(error, stack);
  }
}


/// OAuth cancellation is an expected user action, not a sign-in failure.
bool isAuthCancellation(Object error) {
  final Object source = error is AppException ? (error.cause ?? error) : error;
  if (source is! FirebaseAuthException) return false;
  final String code = source.code.toLowerCase();
  return code.contains('cancel') || code == 'popup-closed-by-user';
}
