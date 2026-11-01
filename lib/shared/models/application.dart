import 'enums.dart';
import 'firestore_codec.dart';

/// A published (or in-progress) application.
///
/// Denormalised developer fields (`ownerName`, `ownerPhotoUrl`) are copied in on
/// write so a feed render never needs a second read. Server-owned counters are
/// read-only here: the client displays them, Cloud Functions maintain them.
class AppListing {
  const AppListing({
    required this.id,
    required this.ownerId,
    required this.name,
    required this.tagline,
    required this.description,
    required this.categoryId,
    required this.createdAt,
    required this.updatedAt,
    this.ownerName = '',
    this.ownerPhotoUrl,
    this.iconUrl,
    this.bannerUrl,
    this.tags = const <String>[],
    this.websiteUrl,
    this.launchUrl,
    this.downloadUrl,
    this.platforms = const <AppPlatform>[],
    this.pricingModel = PricingModel.free,
    this.priceLabel,
    this.isOpenSource = false,
    this.screenshots = const <String>[],
    this.previewImages = const <String>[],
    this.features = const <String>[],
    this.documentationUrl,
    this.supportEmail,
    this.privacyPolicyUrl,
    this.termsUrl,
    this.status = ApplicationStatus.draft,
    this.rejectionReason,
    this.rating = 0,
    this.reviewCount = 0,
    this.viewCount = 0,
    this.uniqueViewerCount = 0,
    this.clickCount = 0,
    this.downloadCount = 0,
    this.bookmarkCount = 0,
    this.trendingScore = 0,
    this.sponsored = false,
    this.featured = false,
    this.publishedAt,
    this.searchKeywords = const <String>[],
    this.moderationNote,
  });

  final String id;
  final String ownerId;
  final String ownerName;
  final String? ownerPhotoUrl;

  final String name;
  final String tagline;
  final String description;
  final String categoryId;
  final List<String> tags;

  final String? iconUrl;
  final String? bannerUrl;
  final List<String> screenshots;
  final List<String> previewImages;

  final String? websiteUrl;
  final String? launchUrl;
  final String? downloadUrl;
  final List<AppPlatform> platforms;
  final PricingModel pricingModel;
  final String? priceLabel;
  final bool isOpenSource;

  final List<String> features;
  final String? documentationUrl;
  final String? supportEmail;
  final String? privacyPolicyUrl;
  final String? termsUrl;

  final ApplicationStatus status;
  final String? rejectionReason;
  final String? moderationNote;

  final double rating;
  final int reviewCount;
  final int viewCount;
  final int uniqueViewerCount;
  final int clickCount;
  final int downloadCount;
  final int bookmarkCount;
  final int trendingScore;
  final bool sponsored;
  final bool featured;

  final List<String> searchKeywords;
  final DateTime createdAt;
  final DateTime updatedAt;
  final DateTime? publishedAt;

  bool get isLive => status.isLive;
  bool get isPending => status == ApplicationStatus.pendingReview;
  bool get hasIcon => iconUrl != null && iconUrl!.isNotEmpty;
  bool get isPaid => pricingModel == PricingModel.paid || pricingModel == PricingModel.subscription;

  double get clickThroughRate => viewCount == 0 ? 0 : clickCount / viewCount;

  /// The single URL the "Get app" button should open, in priority order.
  String? get primaryActionUrl => downloadUrl ?? launchUrl ?? websiteUrl;

  factory AppListing.fromDoc(String id, Map<String, dynamic> map) {
    final Map<String, dynamic> search = Codec.map(map['search']);
    final Map<String, dynamic> moderation = Codec.map(map['moderation']);
    return AppListing(
      id: id,
      ownerId: Codec.str(map['ownerId']),
      ownerName: Codec.str(map['ownerName'], fallback: 'Independent developer'),
      ownerPhotoUrl: Codec.strOrNull(map['ownerPhotoUrl']),
      name: Codec.str(map['name'], fallback: 'Untitled app'),
      tagline: Codec.str(map['tagline']),
      description: Codec.str(map['description']),
      categoryId: Codec.str(map['categoryId'], fallback: 'other'),
      tags: Codec.strings(map['tags']),
      iconUrl: Codec.strOrNull(map['iconUrl']),
      bannerUrl: Codec.strOrNull(map['bannerUrl']),
      screenshots: Codec.strings(map['screenshots']),
      previewImages: Codec.strings(map['previewImages']),
      websiteUrl: Codec.strOrNull(map['websiteUrl']),
      launchUrl: Codec.strOrNull(map['launchUrl']),
      downloadUrl: Codec.strOrNull(map['downloadUrl']),
      platforms: Codec.strings(map['platforms']).map(AppPlatform.parse).toList(),
      pricingModel: PricingModel.parse(map['pricingModel']),
      priceLabel: Codec.strOrNull(map['priceLabel']),
      isOpenSource: Codec.boolean(map['isOpenSource']),
      features: Codec.strings(map['features']),
      documentationUrl: Codec.strOrNull(map['documentationUrl']),
      supportEmail: Codec.strOrNull(map['supportEmail']),
      privacyPolicyUrl: Codec.strOrNull(map['privacyPolicyUrl']),
      termsUrl: Codec.strOrNull(map['termsUrl']),
      status: ApplicationStatus.parse(map['status']),
      rejectionReason: Codec.strOrNull(map['rejectionReason']),
      moderationNote: Codec.strOrNull(moderation['note']),
      rating: Codec.decimal(map['rating']),
      reviewCount: Codec.integer(map['reviewCount']),
      viewCount: Codec.integer(map['viewCount']),
      uniqueViewerCount: Codec.integer(map['uniqueViewerCount']),
      clickCount: Codec.integer(map['clickCount']),
      downloadCount: Codec.integer(map['downloadCount']),
      bookmarkCount: Codec.integer(map['bookmarkCount']),
      trendingScore: Codec.integer(map['trendingScore']),
      sponsored: Codec.boolean(map['sponsored']),
      featured: Codec.boolean(map['featured']),
      searchKeywords: Codec.strings(search['keywords']),
      createdAt: Codec.dateOrNow(map['createdAt']),
      updatedAt: Codec.dateOrNow(map['updatedAt']),
      publishedAt: Codec.date(map['publishedAt']),
    );
  }

  /// The full create payload. Counters start at zero because the security rule
  /// asserts they are zero; a client can never seed its own social proof.
  Map<String, dynamic> toCreateMap({required String ownerName, String? ownerPhotoUrl}) {
    final DateTime now = DateTime.now().toUtc();
    return <String, dynamic>{
      'ownerId': ownerId,
      'ownerName': ownerName,
      'ownerPhotoUrl': ownerPhotoUrl,
      'name': name.trim(),
      'tagline': tagline.trim(),
      'description': description.trim(),
      'categoryId': categoryId,
      'tags': tags,
      'iconUrl': iconUrl,
      'bannerUrl': bannerUrl,
      'screenshots': screenshots,
      'previewImages': previewImages,
      'websiteUrl': websiteUrl,
      'launchUrl': launchUrl,
      'downloadUrl': downloadUrl,
      'platforms': platforms.map((AppPlatform p) => p.name).toList(),
      'pricingModel': pricingModel.name,
      'priceLabel': priceLabel,
      'isOpenSource': isOpenSource,
      'features': features,
      'documentationUrl': documentationUrl,
      'supportEmail': supportEmail,
      'privacyPolicyUrl': privacyPolicyUrl,
      'termsUrl': termsUrl,
      'status': status.wire,
      'rating': 0,
      'reviewCount': 0,
      'viewCount': 0,
      'uniqueViewerCount': 0,
      'clickCount': 0,
      'downloadCount': 0,
      'bookmarkCount': 0,
      'trendingScore': 0,
      'sponsored': false,
      'featured': false,
      'search': <String, dynamic>{
        'title': name.trim().toLowerCase(),
        'keywords': _keywords(),
      },
      'createdAt': Codec.stamp(now),
      'updatedAt': Codec.stamp(now),
    };
  }

  /// Content-only update. Counters, moderation state and sponsorship are
  /// deliberately absent - the rule rejects them and the function owns them.
  Map<String, dynamic> toContentUpdate() {
    final DateTime now = DateTime.now().toUtc();
    return <String, dynamic>{
      'name': name.trim(),
      'tagline': tagline.trim(),
      'description': description.trim(),
      'categoryId': categoryId,
      'tags': tags,
      'iconUrl': iconUrl,
      'bannerUrl': bannerUrl,
      'screenshots': screenshots,
      'previewImages': previewImages,
      'websiteUrl': websiteUrl,
      'launchUrl': launchUrl,
      'downloadUrl': downloadUrl,
      'platforms': platforms.map((AppPlatform p) => p.name).toList(),
      'pricingModel': pricingModel.name,
      'priceLabel': priceLabel,
      'isOpenSource': isOpenSource,
      'features': features,
      'documentationUrl': documentationUrl,
      'supportEmail': supportEmail,
      'privacyPolicyUrl': privacyPolicyUrl,
      'termsUrl': termsUrl,
      'search': <String, dynamic>{
        'title': name.trim().toLowerCase(),
        'keywords': _keywords(),
      },
      'updatedAt': Codec.stamp(now),
    };
  }

  List<String> _keywords() {
    final Set<String> words = <String>{
      ...name.toLowerCase().split(RegExp(r'[^a-z0-9]+')),
      ...tagline.toLowerCase().split(RegExp(r'[^a-z0-9]+')),
      ...tags.map((String t) => t.toLowerCase()),
    };
    return words.where((String w) => w.length > 2).take(40).toList();
  }

  AppListing copyWith({
    String? name,
    String? tagline,
    String? description,
    String? categoryId,
    List<String>? tags,
    String? iconUrl,
    String? bannerUrl,
    List<String>? screenshots,
    List<String>? previewImages,
    String? websiteUrl,
    String? launchUrl,
    String? downloadUrl,
    List<AppPlatform>? platforms,
    PricingModel? pricingModel,
    String? priceLabel,
    bool? isOpenSource,
    List<String>? features,
    String? documentationUrl,
    String? supportEmail,
    String? privacyPolicyUrl,
    String? termsUrl,
    ApplicationStatus? status,
    String? rejectionReason,
  }) {
    return AppListing(
      id: id,
      ownerId: ownerId,
      ownerName: ownerName,
      ownerPhotoUrl: ownerPhotoUrl,
      name: name ?? this.name,
      tagline: tagline ?? this.tagline,
      description: description ?? this.description,
      categoryId: categoryId ?? this.categoryId,
      tags: tags ?? this.tags,
      iconUrl: iconUrl ?? this.iconUrl,
      bannerUrl: bannerUrl ?? this.bannerUrl,
      screenshots: screenshots ?? this.screenshots,
      previewImages: previewImages ?? this.previewImages,
      websiteUrl: websiteUrl ?? this.websiteUrl,
      launchUrl: launchUrl ?? this.launchUrl,
      downloadUrl: downloadUrl ?? this.downloadUrl,
      platforms: platforms ?? this.platforms,
      pricingModel: pricingModel ?? this.pricingModel,
      priceLabel: priceLabel ?? this.priceLabel,
      isOpenSource: isOpenSource ?? this.isOpenSource,
      features: features ?? this.features,
      documentationUrl: documentationUrl ?? this.documentationUrl,
      supportEmail: supportEmail ?? this.supportEmail,
      privacyPolicyUrl: privacyPolicyUrl ?? this.privacyPolicyUrl,
      termsUrl: termsUrl ?? this.termsUrl,
      status: status ?? this.status,
      rejectionReason: rejectionReason ?? this.rejectionReason,
      moderationNote: moderationNote,
      rating: rating,
      reviewCount: reviewCount,
      viewCount: viewCount,
      uniqueViewerCount: uniqueViewerCount,
      clickCount: clickCount,
      downloadCount: downloadCount,
      bookmarkCount: bookmarkCount,
      trendingScore: trendingScore,
      sponsored: sponsored,
      featured: featured,
      searchKeywords: searchKeywords,
      createdAt: createdAt,
      updatedAt: updatedAt,
      publishedAt: publishedAt,
    );
  }
}
