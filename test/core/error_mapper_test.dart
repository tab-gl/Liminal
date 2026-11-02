import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sproutly/core/errors/app_exception.dart';
import 'package:sproutly/core/errors/error_mapper.dart';

void main() {
  group('isAuthCancellation', () {
    test('recognizes Firebase OAuth cancellation codes', () {
      expect(
        isAuthCancellation(FirebaseAuthException(code: 'web-context-cancelled')),
        isTrue,
      );
      expect(
        isAuthCancellation(FirebaseAuthException(code: 'sign_in_canceled')),
        isTrue,
      );
    });

    test('recognizes a normalized cancellation exception', () {
      final FirebaseAuthException cause =
          FirebaseAuthException(code: 'popup-closed-by-user');
      expect(
        isAuthCancellation(AppException(AppErrorCode.unknown, 'Cancelled', cause: cause)),
        isTrue,
      );
    });

    test('does not suppress real authentication failures', () {
      expect(
        isAuthCancellation(FirebaseAuthException(code: 'invalid-credential')),
        isFalse,
      );
      expect(isAuthCancellation(StateError('unexpected')), isFalse);
    });
  });
}
