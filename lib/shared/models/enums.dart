/// Domain enums and their wire representations.
///
/// Firestore stores the string `name`; unknown values fall back to a safe
/// default instead of throwing, so an older client survives a backend that
/// added a status.
enum UserRole {
  user,
  developer,
  admin;

  static UserRole parse(Object? raw) => UserRole.values.firstWhere(
        (UserRole r) => r.name == raw,
        orElse: () => UserRole.user,
      );

  bool get canPublish => this == UserRole.developer || this == UserRole.admin;
  bool get isAdmin => this == UserRole.admin;
}

enum ApplicationStatus {
  draft,
  pendingReview,
  approved,
  rejected,
  suspended;

  String get wire => switch (this) {
        ApplicationStatus.pendingReview => 'pending_review',
        _ => name,
      };

  static ApplicationStatus parse(Object? raw) => switch (raw) {
        'pending_review' => ApplicationStatus.pendingReview,
        'approved' => ApplicationStatus.approved,
        'rejected' => ApplicationStatus.rejected,
        'suspended' => ApplicationStatus.suspended,
        _ => ApplicationStatus.draft,
      };

  bool get isLive => this == ApplicationStatus.approved;
  bool get isEditable => this != ApplicationStatus.suspended;
  bool get isPublic => this == ApplicationStatus.approved;
}

enum PricingModel {
  free,
  freemium,
  paid,
  subscription;

  static PricingModel parse(Object? raw) => PricingModel.values.firstWhere(
        (PricingModel p) => p.name == raw,
        orElse: () => PricingModel.free,
      );

  String get label => switch (this) {
        PricingModel.free => 'Free',
        PricingModel.freemium => 'Freemium',
        PricingModel.paid => 'Paid',
        PricingModel.subscription => 'Subscription',
      };

  bool get isFree => this == PricingModel.free;
}

enum AppPlatform {
  android,
  ios,
  web,
  desktop,
  other;

  static AppPlatform parse(Object? raw) => AppPlatform.values.firstWhere(
        (AppPlatform p) => p.name == raw,
        orElse: () => AppPlatform.other,
      );

  String get label => switch (this) {
        AppPlatform.android => 'Android',
        AppPlatform.ios => 'iOS',
        AppPlatform.web => 'Web',
        AppPlatform.desktop => 'Desktop',
        AppPlatform.other => 'Other',
      };
}

enum CampaignObjective {
  awareness,
  installs,
  clicks,
  launches;

  static CampaignObjective parse(Object? raw) => CampaignObjective.values.firstWhere(
        (CampaignObjective o) => o.name == raw,
        orElse: () => CampaignObjective.awareness,
      );

  String get label => switch (this) {
        CampaignObjective.awareness => 'Awareness',
        CampaignObjective.installs => 'Installs',
        CampaignObjective.clicks => 'Clicks',
        CampaignObjective.launches => 'Launch reach',
      };
}

enum CampaignStatus {
  draft,
  pendingApproval,
  active,
  paused,
  completed,
  exhausted,
  rejected;

  String get wire => switch (this) {
        CampaignStatus.pendingApproval => 'pending_approval',
        _ => name,
      };

  static CampaignStatus parse(Object? raw) => switch (raw) {
        'pending_approval' => CampaignStatus.pendingApproval,
        'active' => CampaignStatus.active,
        'paused' => CampaignStatus.paused,
        'completed' => CampaignStatus.completed,
        'exhausted' => CampaignStatus.exhausted,
        'rejected' => CampaignStatus.rejected,
        _ => CampaignStatus.draft,
      };

  bool get isDelivering => this == CampaignStatus.active;
  bool get isBillable => this == CampaignStatus.active || this == CampaignStatus.exhausted;
}

/// Where a sponsored card may appear. The Cloud Function picks placements from
/// this list and refuses to serve a campaign outside it.
enum AdPlacement {
  sponsoredCard,
  featuredHome,
  discoveryRail,
  categoryPromotion,
  featuredLaunch;

  static AdPlacement parse(Object? raw) => AdPlacement.values.firstWhere(
        (AdPlacement p) => p.name == raw,
        orElse: () => AdPlacement.sponsoredCard,
      );

  String get label => switch (this) {
        AdPlacement.sponsoredCard => 'Sponsored card in Discover',
        AdPlacement.featuredHome => 'Featured on home',
        AdPlacement.discoveryRail => 'Sponsored rail',
        AdPlacement.categoryPromotion => 'Category promotion',
        AdPlacement.featuredLaunch => 'Featured launch',
      };
}

enum NotificationType {
  reviewReceived,
  applicationApproved,
  applicationRejected,
  applicationSuspended,
  newFollower,
  campaignApproved,
  campaignRejected,
  campaignBudgetLow,
  campaignExhausted,
  system;

  static NotificationType parse(Object? raw) => NotificationType.values.firstWhere(
        (NotificationType t) => t.name == raw,
        orElse: () => NotificationType.system,
      );
}

enum ReportTarget { application, review, user, campaign }

enum ReportStatus {
  open,
  resolved,
  dismissed;

  static ReportStatus parse(Object? raw) => ReportStatus.values.firstWhere(
        (ReportStatus s) => s.name == raw,
        orElse: () => ReportStatus.open,
      );
}

enum AppEventType {
  view,
  uniqueView,
  click,
  download,
  share;

  /// Firestore stores the snake_case form used in security rules.
  String get wire => switch (this) {
        AppEventType.uniqueView => 'unique_view',
        _ => name,
      };

  static AppEventType parse(Object? raw) => switch (raw) {
        'unique_view' => AppEventType.uniqueView,
        'click' => AppEventType.click,
        'download' => AppEventType.download,
        'share' => AppEventType.share,
        _ => AppEventType.view,
      };
}
