import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/constants/categories.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/services/install_service.dart';
import '../../../core/services/haptics_service.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/app_image.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/rating_stars.dart';
import '../../../core/widgets/sproutly_action_bar.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_banner_ad.dart';
import '../../../core/widgets/sproutly_card.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/widgets/sproutly_sheet.dart';
import '../../../core/widgets/skeleton.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../../../shared/widgets/application_card.dart';
import '../../bookmarks/data/bookmark_repository_impl.dart';
import '../../reviews/presentation/review_section.dart';
import '../data/application_repository_impl.dart';

class ApplicationDetailScreen extends ConsumerStatefulWidget {
  const ApplicationDetailScreen({required this.applicationId, super.key});

  final String applicationId;

  @override
  ConsumerState<ApplicationDetailScreen> createState() => _ApplicationDetailScreenState();
}

class _ApplicationDetailScreenState extends ConsumerState<ApplicationDetailScreen> {
  bool _viewRecorded = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _recordView());
  }

  Future<void> _recordView() async {
    if (_viewRecorded) return;
    _viewRecorded = true;
    await ref.read(applicationRepositoryProvider).recordEvent(
          applicationId: widget.applicationId,
          type: AppEventType.view,
        );
    await ref.read(analyticsProvider).appViewed(widget.applicationId, source: 'detail');
  }

  @override
  Widget build(BuildContext context) {
    final AsyncValue<AppListing?> async = ref.watch(applicationProvider(widget.applicationId));

    return async.when(
      loading: () => Scaffold(
        backgroundColor: context.colors.surface,
        body: const SingleChildScrollView(child: DetailSkeleton()),
      ),
      error: (Object error, StackTrace _) => Scaffold(
        backgroundColor: context.colors.surface,
        body: SafeArea(
          child: Column(
            children: <Widget>[
              Align(
                alignment: Alignment.centerLeft,
                child: SproutlyIconButton(
                  icon: Icons.arrow_back_rounded,
                  tooltip: 'Back',
                  onPressed: () => context.pop(),
                ),
              ),
              Expanded(
                child: ErrorStateView(
                  error: error,
                  onRetry: () => ref.invalidate(applicationProvider(widget.applicationId)),
                ),
              ),
            ],
          ),
        ),
      ),
      data: (AppListing? listing) {
        if (listing == null) {
          return Scaffold(
            backgroundColor: context.colors.surface,
            body: SafeArea(
              child: Column(
                children: <Widget>[
                  Align(
                    alignment: Alignment.centerLeft,
                    child: SproutlyIconButton(
                      icon: Icons.arrow_back_rounded,
                      tooltip: 'Back',
                      onPressed: () => context.pop(),
                    ),
                  ),
                  const Expanded(
                    child: EmptyView(
                      title: 'This app is not available',
                      message: 'It may have been removed by its developer, or the link is wrong.',
                      icon: Icons.link_off_rounded,
                    ),
                  ),
                ],
              ),
            ),
          );
        }
        return _DetailBody(listing: listing);
      },
    );
  }
}

class _DetailBody extends ConsumerWidget {
  const _DetailBody({required this.listing});

  final AppListing listing;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AppCategory category = AppCategory.byId(listing.categoryId);
    final bool bookmarked = ref.watch(bookmarkStatusProvider(listing.id)).value ?? false;
    final String? uid = ref.watch(currentUserProvider)?.uid;
    final bool isOwner = uid == listing.ownerId;

    return Scaffold(
      backgroundColor: context.colors.surface,
      body: Stack(
        children: <Widget>[
          SingleChildScrollView(
            padding: EdgeInsets.only(bottom: 120 + MediaQuery.paddingOf(context).bottom),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                _Header(listing: listing, category: category),
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(listing.name, style: context.text.headlineMedium),
                      const SizedBox(height: 4),
                      Text(
                        listing.tagline,
                        style: context.text.bodyLarge!.copyWith(color: context.colors.onSurfaceVariant),
                      ),
                      const SizedBox(height: 14),
                      Wrap(
                        spacing: 7,
                        runSpacing: 7,
                        children: <Widget>[
                          SproutlyPill(label: category.label, icon: category.icon, dense: true),
                          PricingBadge(
                            label: listing.priceLabel ?? listing.pricingModel.label,
                            isFree: listing.pricingModel.isFree,
                          ),
                          for (final AppPlatform platform in listing.platforms)
                            SproutlyPill(label: platform.label, dense: true),
                          if (listing.isOpenSource)
                            const SproutlyPill(label: 'Open source', dense: true, icon: Icons.code_rounded),
                          if (listing.sponsored) const SponsoredBadge(),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Row(
                        children: <Widget>[
                          if (listing.reviewCount > 0) ...<Widget>[
                            RatingStars(rating: listing.rating, size: 15),
                            const SizedBox(width: 6),
                            Text(
                              '${listing.rating.toStringAsFixed(1)} · ${listing.reviewCount} reviews',
                              style: context.text.labelMedium,
                            ),
                          ] else
                            Text('No ratings yet', style: context.text.labelMedium),
                          const Spacer(),
                          Text(
                            '${_compact(listing.viewCount)} views',
                            style: context.text.labelSmall,
                          ),
                        ],
                      ),
                      const SizedBox(height: 18),
                      _PrimaryActions(
                        listing: listing,
                        isOwner: isOwner,
                        onOpen: () => _install(context, ref, listing),
                        onManage: () => context.push('/dashboard/${listing.id}'),
                      ),
                      if (listing.status != ApplicationStatus.approved)
                        Padding(
                          padding: const EdgeInsets.only(top: 14),
                          child: Container(
                            width: double.infinity,
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: context.tokens.warningContainer,
                              borderRadius: SproutlyShapes.radiusMd,
                            ),
                            child: Row(
                              children: <Widget>[
                                Icon(Icons.visibility_off_outlined, size: 16, color: context.tokens.warning),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    listing.status == ApplicationStatus.pendingReview
                                        ? 'In review. Only you and moderators can see this page.'
                                        : 'This listing is not public (${listing.status.wire.replaceAll('_', ' ')}).',
                                    style: context.text.labelSmall!.copyWith(color: context.tokens.warning),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      const SizedBox(height: 22),
                      DeveloperRow(
                        name: listing.ownerName,
                        photoUrl: listing.ownerPhotoUrl,
                        subtitle: 'View all apps by this developer',
                        onTap: () => context.push('/developer/${listing.ownerId}'),
                      ),
                      const SizedBox(height: 22),
                      Text('About this app', style: context.text.titleLarge),
                      const SizedBox(height: 8),
                      Text(
                        listing.description,
                        style: context.text.bodyMedium!.copyWith(height: 1.6),
                      ),
                      if (listing.features.isNotEmpty) ...<Widget>[
                        const SizedBox(height: 16),
                        for (final String feature in listing.features)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 7),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: <Widget>[
                                Padding(
                                  padding: const EdgeInsets.only(top: 2),
                                  child: Icon(Icons.check_circle_rounded, size: 15, color: context.tokens.success),
                                ),
                                const SizedBox(width: 9),
                                Expanded(
                                  child: Text(feature, style: context.text.bodyMedium),
                                ),
                              ],
                            ),
                          ),
                      ],
                      if (listing.screenshots.isNotEmpty) ...<Widget>[
                        const SizedBox(height: 22),
                        Text('Screenshots', style: context.text.titleLarge),
                        const SizedBox(height: 10),
                        SizedBox(
                          height: 320,
                          child: ListView.separated(
                            scrollDirection: Axis.horizontal,
                            itemCount: listing.screenshots.length,
                            separatorBuilder: (_, __) => const SizedBox(width: 10),
                            itemBuilder: (BuildContext context, int index) => AppImage(
                              url: listing.screenshots[index],
                              width: 170,
                              fit: BoxFit.cover,
                              radius: SproutlyShapes.radiusLg,
                              icon: Icons.image_outlined,
                            ),
                          ),
                        ),
                      ],
                      if (_links(listing).isNotEmpty) ...<Widget>[
                        const SizedBox(height: 22),
                        Text('Links', style: context.text.titleLarge),
                        const SizedBox(height: 8),
                        for (final MapEntry<String, String> link in _links(listing).entries)
                          ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: Icon(Icons.link_rounded, size: 18, color: context.colors.primary),
                            title: Text(link.key, style: context.text.bodyMedium),
                            trailing: const Icon(Icons.open_in_new_rounded, size: 15),
                            onTap: () => _open(context, link.value),
                          ),
                      ],
                      const SizedBox(height: 12),
                      Align(
                        alignment: Alignment.centerLeft,
                        child: TextButton.icon(
                          onPressed: () => _report(context, ref),
                          icon: const Icon(Icons.flag_outlined, size: 16),
                          label: const Text('Report this listing'),
                          style: TextButton.styleFrom(foregroundColor: context.colors.onSurfaceVariant),
                        ),
                      ),
                    ],
                  ),
                ),
                ReviewSection(listing: listing),
                const SproutlyBannerAd(
                  placement: 'app_detail',
                  margin: EdgeInsets.fromLTRB(20, 20, 20, 8),
                ),
              ],
            ),
          ),
          Positioned(
            top: MediaQuery.paddingOf(context).top + 6,
            left: 10,
            child: SproutlyIconButton(
              icon: Icons.arrow_back_rounded,
              tooltip: 'Back',
              onPressed: () => context.pop(),
            ),
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        top: false,
        child: SproutlyActionBar(
          actions: <SproutlyAction>[
            SproutlyAction(
              icon: bookmarked ? Icons.bookmark_rounded : Icons.bookmark_border_rounded,
              label: bookmarked ? 'Saved' : 'Save',
              onPressed: () => _toggleBookmark(context, ref, bookmarked),
            ),
            SproutlyAction(
              icon: Icons.ios_share_rounded,
              label: 'Share',
              onPressed: () => _share(listing),
            ),
            SproutlyAction(
              icon: Icons.download_rounded,
              label: 'Install',
              emphasized: true,
              onPressed: listing.primaryActionUrl == null
                  ? () {}
                  : () => _install(context, ref, listing),
            ),
          ],
        ),
      ),
    );
  }

  static Map<String, String> _links(AppListing listing) {
    return <String, String>{
      if (listing.websiteUrl != null) 'Website': listing.websiteUrl!,
      if (listing.documentationUrl != null) 'Documentation': listing.documentationUrl!,
      if (listing.privacyPolicyUrl != null) 'Privacy policy': listing.privacyPolicyUrl!,
      if (listing.termsUrl != null) 'Terms of service': listing.termsUrl!,
    };
  }

  static String _compact(int value) {
    if (value < 1000) return '$value';
    if (value < 100000) return '${(value / 1000).toStringAsFixed(1)}K';
    if (value < 10000000) return '${(value / 100000).toStringAsFixed(1)}L';
    return '${(value / 10000000).toStringAsFixed(1)}Cr';
  }

  Future<void> _open(BuildContext context, String url) async {
    final Uri? uri = Uri.tryParse(url);
    if (uri == null) return;
    await launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  /// Installs rather than redirects: an `.apk` link downloads and hands the file
  /// to the system package installer, a store link opens the native store client,
  /// and a web app opens in the browser. Progress and the outcome are shown in a
  /// sheet, because a 40 MB download with no feedback feels broken.
  Future<void> _install(BuildContext context, WidgetRef ref, AppListing listing) async {
    final String? url = listing.primaryActionUrl;
    if (url == null) return;
    Haptics.press();
    await ref.read(applicationRepositoryProvider).recordEvent(
          applicationId: listing.id,
          type: AppEventType.click,
        );
    await ref.read(analyticsProvider).appClicked(listing.id, target: url);
    if (!context.mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      isDismissible: false,
      enableDrag: false,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) => _InstallSheet(listing: listing),
    );
  }

  Future<void> _share(AppListing listing) async {
    Haptics.tap();
    await SharePlus.instance.share(
      ShareParams(
        text: '${listing.name} - ${listing.tagline}\nhttps://sproutly.app/app/${listing.id}',
        subject: listing.name,
      ),
    );
  }

  Future<void> _toggleBookmark(BuildContext context, WidgetRef ref, bool current) async {
    Haptics.press();
    try {
      final bool next = await ref.read(bookmarkRepositoryProvider).toggle(
            applicationId: listing.id,
            appName: listing.name,
          );
      await ref.read(analyticsProvider).bookmarked(listing.id, on: next);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next ? 'Saved to your bookmarks' : 'Removed from bookmarks')),
        );
      }
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    }
  }

  Future<void> _report(BuildContext context, WidgetRef ref) async {
    const List<String> reasons = <String>[
      'Misleading description',
      'Broken or malicious link',
      'Copied or stolen app',
      'Spam or duplicate listing',
      'Adult or unsafe content',
      'Something else',
    ];
    final TextEditingController details = TextEditingController();
    String reason = reasons.first;

    final bool? submitted = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) => StatefulBuilder(
        builder: (BuildContext context, void Function(void Function()) setState) {
          return SproutlySheet(
            title: 'Report this listing',
            subtitle: 'Reports go to a moderator. Abusive reports are themselves reportable.',
            actions: <Widget>[
              SproutlyButton(
                label: 'Submit report',
                expand: true,
                onPressed: () => Navigator.of(context).pop(true),
              ),
            ],
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                for (final String option in reasons)
                  InkWell(
                    borderRadius: SproutlyShapes.radiusSm,
                    onTap: () => setState(() => reason = option),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 9, horizontal: 4),
                      child: Row(
                        children: <Widget>[
                          Icon(
                            reason == option
                                ? Icons.radio_button_checked_rounded
                                : Icons.radio_button_unchecked_rounded,
                            size: 18,
                            color: reason == option
                                ? context.colors.primary
                                : context.colors.onSurfaceVariant,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(option, style: context.text.bodyMedium),
                          ),
                        ],
                      ),
                    ),
                  ),
                const SizedBox(height: 10),
                SproutlyField(
                  label: 'Anything else? (optional)',
                  controller: details,
                  maxLines: 3,
                  maxLength: 500,
                ),
              ],
            ),
          );
        },
      ),
    );

    final String extra = details.text.trim();
    details.dispose();
    if (submitted != true) return;
    try {
      await ref.read(applicationRepositoryProvider).reportContent(
            targetId: listing.id,
            target: ReportTarget.application.name,
            reason: reason,
            details: extra.isEmpty ? null : extra,
          );
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Thanks - a moderator will review this.')),
        );
      }
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    }
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.listing, required this.category});

  final AppListing listing;
  final AppCategory category;

  @override
  Widget build(BuildContext context) {
    final double top = MediaQuery.paddingOf(context).top;
    return SizedBox(
      height: 220 + top,
      child: Stack(
        children: <Widget>[
          Positioned.fill(
            child: listing.bannerUrl == null
                ? Container(color: context.tokens.heroSurface)
                : AppImage(
                    url: listing.bannerUrl,
                    fit: BoxFit.cover,
                    radius: BorderRadius.zero,
                  ),
          ),
          // A flat scrim rather than a fade: the icon needs a surface to sit on
          // and the title needs contrast, but the brief is no gradients.
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: context.colors.surface.withValues(alpha: 0.32),
              ),
            ),
          ),
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            height: 56,
            child: ColoredBox(color: context.colors.surface),
          ),
          Positioned(
            left: 20,
            bottom: 0,
            child: Container(
              padding: const EdgeInsets.all(5),
              decoration: BoxDecoration(
                color: context.colors.surface,
                borderRadius: BorderRadius.circular(22),
              ),
              child: AppImage(
                url: listing.iconUrl,
                height: 72,
                width: 72,
                radius: BorderRadius.circular(18),
                isIcon: true,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PrimaryActions extends StatelessWidget {
  const _PrimaryActions({
    required this.listing,
    required this.isOwner,
    required this.onOpen,
    required this.onManage,
  });

  final AppListing listing;
  final bool isOwner;
  final VoidCallback onOpen;
  final VoidCallback onManage;

  /// The button says what will actually happen: a side-loadable package gets
  /// "Install", a store link gets "Get", a web app gets "Open".
  static String _installLabel(AppListing listing) {
    final String url = (listing.primaryActionUrl ?? '').toLowerCase();
    if (url.endsWith('.apk')) return 'Install app';
    if (url.contains('play.google.com') || url.contains('apps.apple.com')) return 'Get the app';
    if (url.isEmpty) return 'Open app';
    return 'Open app';
  }

  @override
  Widget build(BuildContext context) {
    if (isOwner) {
      return Row(
        children: <Widget>[
          Expanded(
            child: SproutlyButton(
              label: 'Manage listing',
              icon: Icons.tune_rounded,
              expand: true,
              size: SproutlyButtonSize.large,
              onPressed: onManage,
            ),
          ),
        ],
      );
    }
    if (listing.primaryActionUrl == null) {
      return Text(
        'This developer has not published a link yet.',
        style: context.text.bodySmall,
      );
    }
    return Row(
      children: <Widget>[
        Expanded(
          child: SproutlyButton(
            label: _PrimaryActions._installLabel(listing),
            icon: Icons.open_in_new_rounded,
            expand: true,
            size: SproutlyButtonSize.large,
            onPressed: onOpen,
          ),
        ),
      ],
    );
  }
}

/// Progress and outcome of an install attempt.
///
/// The sheet cannot be dismissed while a download is running (a cancelled sheet
/// would leave an orphaned HTTP stream), and on a permission failure it offers
/// the system settings shortcut instead of just reporting the error.
class _InstallSheet extends ConsumerStatefulWidget {
  const _InstallSheet({required this.listing});

  final AppListing listing;

  @override
  ConsumerState<_InstallSheet> createState() => _InstallSheetState();
}

class _InstallSheetState extends ConsumerState<_InstallSheet> {
  double? _progress;
  String _status = 'Preparing…';
  InstallResult? _result;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _run());
  }

  Future<void> _run() async {
    final InstallResult result = await ref.read(installServiceProvider).install(
      widget.listing.primaryActionUrl,
      onProgress: (double value) {
        if (!mounted) return;
        setState(() {
          _progress = value;
          _status = 'Downloading… ${(value * 100).round()}%';
        });
      },
    );
    if (!mounted) return;
    setState(() {
      _result = result;
      _status = result.message;
    });
    if (result.isSuccess) Haptics.success();
  }

  @override
  Widget build(BuildContext context) {
    final InstallResult? result = _result;
    final bool busy = result == null;

    return SproutlySheet(
      title: 'Install ${widget.listing.name}',
      subtitle: _status,
      scrollable: false,
      actions: <Widget>[
        if (result?.canOpenSettings ?? false)
          SproutlyButton(
            label: 'Open settings',
            variant: SproutlyButtonVariant.tonal,
            onPressed: () => openAppSettings(),
          ),
        SproutlyButton(
          label: busy ? 'Cancel' : 'Done',
          onPressed: () => Navigator.of(context).maybePop(),
        ),
      ],
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          const SizedBox(height: 4),
          ClipRRect(
            borderRadius: SproutlyShapes.radiusPill,
            child: LinearProgressIndicator(
              value: busy ? _progress : (result!.isSuccess ? 1 : 0),
              minHeight: 8,
              backgroundColor: context.colors.surfaceContainerHigh,
              color: result != null && !result.isSuccess
                  ? context.colors.error
                  : context.colors.primary,
            ),
          ),
          const SizedBox(height: 14),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Icon(
                busy
                    ? Icons.downloading_rounded
                    : result!.isSuccess
                        ? Icons.check_circle_outline_rounded
                        : Icons.info_outline_rounded,
                size: 16,
                color: busy
                    ? context.colors.onSurfaceVariant
                    : result.isSuccess
                        ? context.tokens.success
                        : context.colors.error,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  result == null
                      ? 'Sproutly verifies the link, then hands the package to Android. '
                          'You will see the system install dialog next.'
                      : result.message,
                  style: context.text.labelSmall!.copyWith(height: 1.5),
                ),
              ),
            ],
          ),
          if (!busy && result.outcome == InstallOutcome.unsupported) ...<Widget>[
            const SizedBox(height: 12),
            Text(
              'Listings that ship a Play Store or App Store link install through the '
              'store instead.',
              style: context.text.labelSmall!.copyWith(height: 1.5),
            ),
          ],
        ],
      ),
    );
  }
}
