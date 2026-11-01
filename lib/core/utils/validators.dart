import '../constants/app_constants.dart';

/// Form validators. Each returns null when valid, matching Flutter's
/// FormFieldValidator contract, so they drop straight into TextFormField.
class Validators {
  const Validators._();

  static final RegExp _email = RegExp(r'^[\w.+\-]+@[\w\-]+\.[\w.\-]+$');
  static final RegExp _https = RegExp(r'^https://[^\s]+$');
  static final RegExp _handle = RegExp(r'^[a-zA-Z0-9._-]{3,32}$');

  static String? email(String? value) {
    final String v = (value ?? '').trim();
    if (v.isEmpty) return 'Email is required';
    if (!_email.hasMatch(v)) return 'Enter a valid email address';
    return null;
  }

  static String? password(String? value) {
    final String v = value ?? '';
    if (v.isEmpty) return 'Password is required';
    if (v.length < 8) return 'Use at least 8 characters';
    if (v.length > 128) return 'Use fewer than 128 characters';
    if (!RegExp(r'[A-Za-z]').hasMatch(v) || !RegExp(r'[0-9]').hasMatch(v)) {
      return 'Mix letters and numbers';
    }
    return null;
  }

  static String? confirmPassword(String? value, String original) {
    if (value != original) return 'Passwords do not match';
    return null;
  }

  static String? required(String? value, {String label = 'This field'}) {
    if ((value ?? '').trim().isEmpty) return '$label is required';
    return null;
  }

  static String? maxLength(String? value, int max, {String label = 'Text'}) {
    if ((value ?? '').length > max) return '$label must be $max characters or fewer';
    return null;
  }

  static String? minLength(String? value, int min, {String label = 'Text'}) {
    if ((value ?? '').trim().length < min) return '$label needs at least $min characters';
    return null;
  }

  static String? httpsUrl(String? value, {bool optional = true}) {
    final String v = (value ?? '').trim();
    if (v.isEmpty) return optional ? null : 'A link is required';
    if (!_https.hasMatch(v)) return 'Use a full https:// link';
    return null;
  }

  static String? appName(String? value) =>
      required(value, label: 'App name') ?? maxLength(value, AppConstants.maxNameLength, label: 'App name');

  static String? tagline(String? value) =>
      required(value, label: 'Tagline') ?? maxLength(value, AppConstants.maxTaglineLength, label: 'Tagline');

  static String? description(String? value) =>
      minLength(value, AppConstants.minDescriptionLength, label: 'Description') ??
      maxLength(value, AppConstants.maxDescriptionLength, label: 'Description');

  static String? reviewBody(String? value) =>
      minLength(value, AppConstants.minReviewLength, label: 'Review') ??
      maxLength(value, AppConstants.maxReviewLength, label: 'Review');

  static String? handle(String? value) {
    final String v = (value ?? '').trim();
    if (v.isEmpty) return null;
    if (!_handle.hasMatch(v)) return 'Use 3-32 letters, numbers, dots, dashes';
    return null;
  }

  /// Budget in whole rupees, validated against the platform floor and ceiling.
  static String? campaignBudget(String? value) {
    final int? paise = Formatters.rupeesToPaise(value);
    if (paise == null) return 'Enter a budget in rupees';
    if (paise < AppConstants.minimumCampaignBudgetPaise) {
      return 'Minimum budget is Rs ${AppConstants.minimumCampaignBudgetPaise ~/ 100}';
    }
    if (paise > AppConstants.maximumCampaignBudgetPaise) {
      return 'Maximum budget is Rs ${AppConstants.maximumCampaignBudgetPaise ~/ 100}';
    }
    return null;
  }
}

/// Parsing and display helpers used by validators and the UI.
class Formatters {
  const Formatters._();

  /// "12,500" -> 1250000 paise. Returns null when the input is not a number.
  static int? rupeesToPaise(String? value) {
    final String cleaned = (value ?? '').replaceAll(RegExp(r'[^0-9.]'), '');
    if (cleaned.isEmpty) return null;
    final double? rupees = double.tryParse(cleaned);
    if (rupees == null || rupees <= 0) return null;
    return (rupees * 100).round();
  }

  /// 1250000 paise -> "12,500". Indian digit grouping.
  static String paiseToRupees(int paise) => _groupIndian((paise ~/ 100).toString());

  /// "Rs 12,500" - the form used on cards and receipts.
  static String inr(int paise) => 'Rs ${paiseToRupees(paise)}';

  static String _groupIndian(String digits) {
    if (digits.length <= 3) return digits;
    final String last3 = digits.substring(digits.length - 3);
    String head = digits.substring(0, digits.length - 3);
    final List<String> parts = <String>[];
    while (head.length > 2) {
      parts.insert(0, head.substring(head.length - 2));
      head = head.substring(0, head.length - 2);
    }
    if (head.isNotEmpty) parts.insert(0, head);
    return '${parts.join(',')},$last3';
  }

  /// 1234 -> "1.2K", 1234567 -> "12.3L", 12345678 -> "1.2Cr".
  static String compactCount(int value) {
    if (value < 1000) return value.toString();
    if (value < 100000) return '${(value / 1000).toStringAsFixed(value < 10000 ? 1 : 0)}K';
    if (value < 10000000) return '${(value / 100000).toStringAsFixed(1)}L';
    return '${(value / 10000000).toStringAsFixed(1)}Cr';
  }

  static String percent(double ratio) => '${(ratio * 100).toStringAsFixed(1)}%';
}
