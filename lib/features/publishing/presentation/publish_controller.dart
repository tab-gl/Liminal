import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/utils/validators.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../data/publishing_repository_impl.dart';

/// The five-step publishing draft, held in memory until the user saves it.
@immutable
class PublishDraft {
  const PublishDraft({
    this.id = '',
    this.name = '',
    this.tagline = '',
    this.description = '',
    this.categoryId = '',
    this.tags = const <String>[],
    this.iconUrl,
    this.bannerUrl,
    this.screenshots = const <String>[],
    this.websiteUrl,
    this.launchUrl,
    this.downloadUrl,
    this.platforms = const <AppPlatform>[],
    this.pricingModel = PricingModel.free,
    this.priceLabel,
    this.isOpenSource = false,
    this.features = const <String>[],
    this.documentationUrl,
    this.supportEmail,
    this.privacyPolicyUrl,
    this.termsUrl,
    this.step = 0,
    this.savedAt,
  });

  final String id;
  final String name;
  final String tagline;
  final String description;
  final String categoryId;
  final List<String> tags;
  final String? iconUrl;
  final String? bannerUrl;
  final List<String> screenshots;
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
  final int step;
  final DateTime? savedAt;

  static const List<String> stepTitles = <String>[
    'Basics',
    'Product',
    'Media',
    'Details',
    'Review',
  ];

  double get completion {
    int done = 0;
    if (name.isNotEmpty && tagline.isNotEmpty && description.isNotEmpty) done++;
    if (categoryId.isNotEmpty) done++;
    if (iconUrl != null) done++;
    if (platforms.isNotEmpty) done++;
    if (primaryLink != null) done++;
    if (screenshots.isNotEmpty) done++;
    return done / 6;
  }

  String? get primaryLink => downloadUrl ?? launchUrl ?? websiteUrl;

  /// Field-level validation for one step. Returns human-readable problems; an
  /// empty list means the step may be left.
  List<String> validateStep(int index) {
    final List<String> problems = <String>[];
    switch (index) {
      case 0:
        // Required: name, category, icon. Everything else on this step is
        // optional - a listing can go live with just a name and a face.
        if (Validators.appName(name) != null) problems.add('App name: 2-80 characters.');
        if (categoryId.isEmpty) problems.add('Pick a category.');
        if (iconUrl == null || iconUrl!.isEmpty) problems.add('Add an app icon.');
        if (tagline.isNotEmpty && tagline.length > AppConstants.maxTaglineLength) {
          problems.add('Tagline: up to ${AppConstants.maxTaglineLength} characters.');
        }
        if (description.isNotEmpty &&
            description.trim().length < AppConstants.minDescriptionLength) {
          problems.add(
            'Description is optional, but if you add one use at least '
            '${AppConstants.minDescriptionLength} characters.',
          );
        }
        if (tags.length > AppConstants.maxTags) problems.add('Use at most ${AppConstants.maxTags} tags.');
      case 1:
        if (platforms.isEmpty) problems.add('Select at least one platform.');
        if (primaryLink == null || primaryLink!.isEmpty) {
          problems.add('Add a website, launch or download link.');
        }
        for (final String? link in <String?>[websiteUrl, launchUrl, downloadUrl, documentationUrl]) {
          if (link != null && link.isNotEmpty && Validators.httpsUrl(link) != null) {
            problems.add('Links must start with https://');
            break;
          }
        }
        if (pricingModel == PricingModel.paid && (priceLabel ?? '').isEmpty) {
          problems.add('Add a price label for a paid app.');
        }
      case 2:
        // Screenshots are optional. Listings with them convert better, which is
        // a recommendation, not a gate.
        if (screenshots.length > AppConstants.maxScreenshots) {
          problems.add('Use at most ${AppConstants.maxScreenshots} screenshots.');
        }
      case 3:
        if (Validators.httpsUrl(privacyPolicyUrl, optional: true) != null) {
          problems.add('The privacy policy link must be a full https:// URL.');
        }
        if (supportEmail != null && supportEmail!.isNotEmpty && Validators.email(supportEmail) != null) {
          problems.add('The support email is not valid.');
        }
      case 4:
        for (int i = 0; i < 4; i++) {
          problems.addAll(validateStep(i));
        }
    }
    return problems;
  }

  AppListing toListing() => AppListing(
        id: id,
        ownerId: '',
        name: name,
        tagline: tagline,
        description: description,
        categoryId: categoryId,
        createdAt: DateTime.now().toUtc(),
        updatedAt: DateTime.now().toUtc(),
        tags: tags,
        iconUrl: iconUrl,
        bannerUrl: bannerUrl,
        screenshots: screenshots,
        websiteUrl: websiteUrl,
        launchUrl: launchUrl,
        downloadUrl: downloadUrl,
        platforms: platforms,
        pricingModel: pricingModel,
        priceLabel: priceLabel,
        isOpenSource: isOpenSource,
        features: features,
        documentationUrl: documentationUrl,
        supportEmail: supportEmail,
        privacyPolicyUrl: privacyPolicyUrl,
        termsUrl: termsUrl,
        status: ApplicationStatus.draft,
      );

  PublishDraft copyWith({
    String? id,
    String? name,
    String? tagline,
    String? description,
    String? categoryId,
    List<String>? tags,
    String? iconUrl,
    String? bannerUrl,
    List<String>? screenshots,
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
    int? step,
    DateTime? savedAt,
  }) {
    return PublishDraft(
      id: id ?? this.id,
      name: name ?? this.name,
      tagline: tagline ?? this.tagline,
      description: description ?? this.description,
      categoryId: categoryId ?? this.categoryId,
      tags: tags ?? this.tags,
      iconUrl: iconUrl ?? this.iconUrl,
      bannerUrl: bannerUrl ?? this.bannerUrl,
      screenshots: screenshots ?? this.screenshots,
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
      step: step ?? this.step,
      savedAt: savedAt ?? this.savedAt,
    );
  }

  factory PublishDraft.fromListing(AppListing listing) => PublishDraft(
        id: listing.id,
        name: listing.name,
        tagline: listing.tagline,
        description: listing.description,
        categoryId: listing.categoryId,
        tags: listing.tags,
        iconUrl: listing.iconUrl,
        bannerUrl: listing.bannerUrl,
        screenshots: listing.screenshots,
        websiteUrl: listing.websiteUrl,
        launchUrl: listing.launchUrl,
        downloadUrl: listing.downloadUrl,
        platforms: listing.platforms,
        pricingModel: listing.pricingModel,
        priceLabel: listing.priceLabel,
        isOpenSource: listing.isOpenSource,
        features: listing.features,
        documentationUrl: listing.documentationUrl,
        supportEmail: listing.supportEmail,
        privacyPolicyUrl: listing.privacyPolicyUrl,
        termsUrl: listing.termsUrl,
      );
}

class PublishController extends Notifier<PublishDraft> {
  @override
  PublishDraft build() => const PublishDraft();

  void update(PublishDraft draft) => state = draft;

  void patch(PublishDraft Function(PublishDraft current) transform) => state = transform(state);

  void goToStep(int step) => state = state.copyWith(step: step.clamp(0, 4));

  void load(AppListing listing) => state = PublishDraft.fromListing(listing);

  void reset() => state = const PublishDraft();

  void setSaved(String id) => state = state.copyWith(id: id, savedAt: DateTime.now().toUtc());
}

final NotifierProvider<PublishController, PublishDraft> publishDraftProvider =
    NotifierProvider<PublishController, PublishDraft>(PublishController.new);

final FutureProvider<List<AppListing>> myAppsProvider =
    FutureProvider<List<AppListing>>((Ref ref) {
  return ref.watch(publishingRepositoryProvider).myApps();
});
