/// Platform-wide constants. Keep business limits here so client validation and
/// Cloud Function validation can quote the same numbers.
class AppConstants {
  const AppConstants._();

  static const String appName = 'Sproutly';
  static const String tagline = 'Build it. Publish it. Grow it.';
  static const String promise = 'Every great app deserves a chance to grow.';

  // Pagination
  static const int feedPageSize = 12;
  static const int listPageSize = 20;
  static const int searchDebounceMs = 320;

  // Publishing limits
  static const int maxNameLength = 80;
  static const int maxTaglineLength = 140;
  static const int minDescriptionLength = 60;
  static const int maxDescriptionLength = 4000;
  static const int maxTags = 8;
  static const int maxScreenshots = 8;
  static const int maxFeatures = 12;
  static const int maxReviewsPerUserPerApp = 1;

  // Advertising
  static const int minimumCampaignBudgetPaise = 50000; // INR 500
  static const int maximumCampaignBudgetPaise = 50000000; // INR 5,00,000
  static const int maxCampaignNameLength = 80;

  // Reviews
  static const int minReviewLength = 12;
  static const int maxReviewLength = 2000;

  // Discovery rails
  static const int railSize = 10;

  // Cache
  static const Duration firestoreCacheTtl = Duration(minutes: 10);
  static const Duration remoteConfigTtl = Duration(hours: 1);
}
