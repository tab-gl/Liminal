import 'enums.dart';
import 'firestore_codec.dart';

/// Audience targeting. Empty lists mean "no restriction on this axis", which the
/// delivery function treats as the whole platform.
class CampaignTargeting {
  const CampaignTargeting({
    this.categoryIds = const <String>[],
    this.platforms = const <AppPlatform>[],
    this.countries = const <String>[],
  });

  final List<String> categoryIds;
  final List<AppPlatform> platforms;

  /// ISO 3166-1 alpha-2. Empty means worldwide.
  final List<String> countries;

  factory CampaignTargeting.fromMap(Map<String, dynamic> map) => CampaignTargeting(
        categoryIds: Codec.strings(map['categoryIds']),
        platforms: Codec.strings(map['platforms']).map(AppPlatform.parse).toList(),
        countries: Codec.strings(map['countries']),
      );

  Map<String, dynamic> toMap() => <String, dynamic>{
        'categoryIds': categoryIds,
        'platforms': platforms.map((AppPlatform p) => p.name).toList(),
        'countries': countries,
      };

  bool get isBroad => categoryIds.isEmpty && platforms.isEmpty && countries.isEmpty;
}

class CampaignCreative {
  const CampaignCreative({required this.headline, this.body, this.imageUrl});

  final String headline;
  final String? body;
  final String? imageUrl;

  factory CampaignCreative.fromMap(Map<String, dynamic> map) => CampaignCreative(
        headline: Codec.str(map['headline']),
        body: Codec.strOrNull(map['body']),
        imageUrl: Codec.strOrNull(map['imageUrl']),
      );

  Map<String, dynamic> toMap() => <String, dynamic>{
        'headline': headline.trim(),
        'body': body?.trim(),
        'imageUrl': imageUrl,
      };
}

/// A self-service advertising campaign.
///
/// Money and delivery counters are server-owned: `budget` is set once at
/// creation, `spend`, `impressions` and `clicks` are only ever written by Cloud
/// Functions. The client can read them and can pause or resume.
class Campaign {
  const Campaign({
    required this.id,
    required this.ownerId,
    required this.applicationId,
    required this.name,
    required this.objective,
    required this.budgetPaise,
    required this.startDate,
    required this.endDate,
    required this.createdAt,
    required this.updatedAt,
    this.applicationName = '',
    this.applicationIconUrl,
    this.status = CampaignStatus.draft,
    this.targeting = const CampaignTargeting(),
    this.placements = const <AdPlacement>[],
    this.creative = const CampaignCreative(headline: ''),
    this.impressions = 0,
    this.clicks = 0,
    this.spendPaise = 0,
    this.currency = 'INR',
    this.rejectionReason,
    this.approvedAt,
  });

  final String id;
  final String ownerId;
  final String applicationId;
  final String applicationName;
  final String? applicationIconUrl;
  final String name;
  final CampaignObjective objective;
  final int budgetPaise;
  final int spendPaise;
  final DateTime startDate;
  final DateTime endDate;
  final CampaignTargeting targeting;
  final List<AdPlacement> placements;
  final CampaignCreative creative;
  final CampaignStatus status;
  final int impressions;
  final int clicks;
  final String currency;
  final String? rejectionReason;
  final DateTime? approvedAt;
  final DateTime createdAt;
  final DateTime updatedAt;

  int get remainingPaise => (budgetPaise - spendPaise).clamp(0, budgetPaise);
  double get ctr => impressions == 0 ? 0 : clicks / impressions;
  double get spendRatio => budgetPaise == 0 ? 0 : (spendPaise / budgetPaise).clamp(0, 1);
  double get cpcPaise => clicks == 0 ? 0 : spendPaise / clicks;
  bool get isLive => status.isDelivering;

  factory Campaign.fromDoc(String id, Map<String, dynamic> map) {
    return Campaign(
      id: id,
      ownerId: Codec.str(map['ownerId']),
      applicationId: Codec.str(map['applicationId']),
      applicationName: Codec.str(map['applicationName']),
      applicationIconUrl: Codec.strOrNull(map['applicationIconUrl']),
      name: Codec.str(map['name'], fallback: 'Untitled campaign'),
      objective: CampaignObjective.parse(map['objective']),
      budgetPaise: Codec.integer(map['budget']),
      spendPaise: Codec.integer(map['spend']),
      startDate: Codec.dateOrNow(map['startDate']),
      endDate: Codec.dateOrNow(map['endDate']),
      targeting: CampaignTargeting.fromMap(Codec.map(map['targeting'])),
      placements: Codec.strings(map['placements']).map(AdPlacement.parse).toList(),
      creative: CampaignCreative.fromMap(Codec.map(map['creative'])),
      status: CampaignStatus.parse(map['status']),
      impressions: Codec.integer(map['impressions']),
      clicks: Codec.integer(map['clicks']),
      currency: Codec.str(map['currency'], fallback: 'INR'),
      rejectionReason: Codec.strOrNull(map['rejectionReason']),
      approvedAt: Codec.date(map['approvedAt']),
      createdAt: Codec.dateOrNow(map['createdAt']),
      updatedAt: Codec.dateOrNow(map['updatedAt']),
    );
  }

  /// New campaigns are always submitted for approval with zero delivery.
  Map<String, dynamic> toCreateMap() {
    final DateTime now = DateTime.now().toUtc();
    return <String, dynamic>{
      'ownerId': ownerId,
      'applicationId': applicationId,
      'applicationName': applicationName,
      'applicationIconUrl': applicationIconUrl,
      'name': name.trim(),
      'objective': objective.name,
      'budget': budgetPaise,
      'spend': 0,
      'impressions': 0,
      'clicks': 0,
      'currency': currency,
      'startDate': Codec.stamp(startDate),
      'endDate': Codec.stamp(endDate),
      'targeting': targeting.toMap(),
      'placements': placements.map((AdPlacement p) => p.name).toList(),
      'creative': creative.toMap(),
      'status': CampaignStatus.pendingApproval.wire,
      'createdAt': Codec.stamp(now),
      'updatedAt': Codec.stamp(now),
    };
  }
}
