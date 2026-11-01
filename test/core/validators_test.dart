import 'package:flutter_test/flutter_test.dart';
import 'package:sproutly/core/utils/validators.dart';

void main() {
  group('email validation', () {
    test('accepts a normal address', () {
      expect(Validators.email('dev@sproutly.app'), isNull);
    });

    test('rejects a missing domain', () {
      expect(Validators.email('dev@'), isNotNull);
      expect(Validators.email(''), isNotNull);
      expect(Validators.email(null), isNotNull);
    });
  });

  group('password validation', () {
    test('requires length, a letter and a number', () {
      expect(Validators.password('sprout123'), isNull);
      expect(Validators.password('short1'), isNotNull);
      expect(Validators.password('lettersonly'), isNotNull);
      expect(Validators.password('12345678'), isNotNull);
    });
  });

  group('https link validation', () {
    test('only allows https', () {
      expect(Validators.httpsUrl('https://sproutly.app'), isNull);
      expect(Validators.httpsUrl('http://sproutly.app'), isNotNull);
      expect(Validators.httpsUrl(''), isNull, reason: 'optional by default');
      expect(Validators.httpsUrl('', optional: false), isNotNull);
    });
  });

  group('campaign budget', () {
    test('enforces the platform floor and ceiling', () {
      expect(Validators.campaignBudget('500'), isNull);
      expect(Validators.campaignBudget('499'), isNotNull);
      expect(Validators.campaignBudget('500001'), isNotNull);
      expect(Validators.campaignBudget('abc'), isNotNull);
    });
  });

  group('currency formatting', () {
    test('groups digits the Indian way', () {
      expect(Formatters.paiseToRupees(50000), '500');
      expect(Formatters.paiseToRupees(1250000), '12,500');
      expect(Formatters.paiseToRupees(125000000), '12,50,000');
    });

    test('parses rupees into paise', () {
      expect(Formatters.rupeesToPaise('1,250'), 125000);
      expect(Formatters.rupeesToPaise('0'), isNull);
      expect(Formatters.rupeesToPaise(''), isNull);
    });

    test('compacts large counts', () {
      expect(Formatters.compactCount(999), '999');
      expect(Formatters.compactCount(1500), '1.5K');
      expect(Formatters.compactCount(250000), '2.5L');
    });
  });

  group('description validation', () {
    test('requires a minimum length', () {
      expect(Validators.description('Too short'), isNotNull);
      expect(
        Validators.description('A' * 60),
        isNull,
      );
    });
  });
}
