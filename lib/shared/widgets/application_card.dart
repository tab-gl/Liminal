import 'package:material_ui/material_ui.dart';

import '../../core/constants/categories.dart';
import '../../core/theme/theme_context.dart';
import '../../core/widgets/app_image.dart';
import '../../core/widgets/badges.dart';
import '../../core/widgets/rating_stars.dart';
import '../../core/widgets/sproutly_card.dart';
import '../models/application.dart';

enum AppCardDensity { list, compact, grid }

/// The application card.
///
/// Every card carries the full decision set in one glance: icon, name,
/// one-line pitch, category, developer, rating and review count, pricing, and -
/// when the placement is paid - an unmissable Sponsored label.
class ApplicationCard extends StatelessWidget {
  const ApplicationCard({
    required this.listing,
    required this.onTap,
    super.key,
    this.density = AppCardDensity.list,
    this.onBookmark,
    this.isBookmarked = false,
    this.showDeveloper = true,
    this.trailing,
  });

  final AppListing listing;
  final VoidCallback onTap;
  final AppCardDensity density;
  final VoidCallback? onBookmark;
  final bool isBookmarked;
  final bool showDeveloper;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    switch (density) {
      case AppCardDensity.grid:
        return _GridCard(
          listing: listing,
          onTap: onTap,
          isBookmarked: isBookmarked,
          onBookmark: onBookmark,
        );
      case AppCardDensity.compact:
        return _CompactCard(listing: listing, onTap: onTap);
      case AppCardDensity.list:
        return SproutlyCard(
          onTap: onTap,
          padding: const EdgeInsets.all(14),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              AppImage(
                url: listing.iconUrl,
                height: 56,
                width: 56,
                radius: BorderRadius.circular(16),
                isIcon: true,
                icon: Icons.apps_rounded,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        Expanded(
                          child: Text(
                            listing.name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: context.text.titleMedium,
                          ),
                        ),
                        if (listing.sponsored) const SponsoredBadge(),
                      ],
                    ),
                    const SizedBox(height: 2),
                    Text(
                      listing.tagline,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: context.text.bodySmall,
                    ),
                    const SizedBox(height: 8),
                    _MetaLine(listing: listing, showDeveloper: showDeveloper),
                  ],
                ),
              ),
              if (trailing != null) ...<Widget>[const SizedBox(width: 6), trailing!],
            ],
          ),
        );
    }
  }
}

class _MetaLine extends StatelessWidget {
  const _MetaLine({required this.listing, required this.showDeveloper});

  final AppListing listing;
  final bool showDeveloper;

  @override
  Widget build(BuildContext context) {
    final AppCategory category = AppCategory.byId(listing.categoryId);
    return Wrap(
      spacing: 6,
      runSpacing: 6,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: <Widget>[
        if (listing.reviewCount > 0)
          Row(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              RatingStars(rating: listing.rating, size: 12),
              const SizedBox(width: 4),
              Text(
                listing.rating.toStringAsFixed(1),
                style: context.text.labelSmall!.copyWith(
                  color: context.colors.onSurface,
                  fontWeight: FontWeight.w700,
                ),
              ),
              Text(
                ' (${listing.reviewCount})',
                style: context.text.labelSmall,
              ),
            ],
          )
        else
          Text('New', style: context.text.labelSmall!.copyWith(color: context.tokens.success)),
        SproutlyPill(label: category.label, dense: true, icon: category.icon),
        PricingBadge(label: listing.priceLabel ?? listing.pricingModel.label, isFree: listing.pricingModel.isFree),
        if (listing.isOpenSource)
          const SproutlyPill(label: 'Open source', dense: true, icon: Icons.code_rounded),
        if (showDeveloper && listing.ownerName.isNotEmpty)
          SproutlyPill(label: listing.ownerName, dense: true, icon: Icons.person_outline_rounded),
      ],
    );
  }
}

class _CompactCard extends StatelessWidget {
  const _CompactCard({required this.listing, required this.onTap});

  final AppListing listing;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 156,
      child: SproutlyCard(
        onTap: onTap,
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Row(
              children: <Widget>[
                AppImage(
                  url: listing.iconUrl,
                  height: 42,
                  width: 42,
                  radius: BorderRadius.circular(13),
                  isIcon: true,
                ),
                const Spacer(),
                if (listing.sponsored)
                  const Icon(Icons.campaign_outlined, size: 14)
                else if (listing.featured)
                  Icon(Icons.auto_awesome_rounded, size: 14, color: context.tokens.olive),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              listing.name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: context.text.titleSmall,
            ),
            const SizedBox(height: 2),
            Text(
              listing.tagline,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: context.text.labelSmall!.copyWith(height: 1.35),
            ),
            const SizedBox(height: 8),
            Row(
              children: <Widget>[
                if (listing.reviewCount > 0) ...<Widget>[
                  Icon(Icons.star_rounded, size: 13, color: context.tokens.olive),
                  const SizedBox(width: 2),
                  Text(
                    listing.rating.toStringAsFixed(1),
                    style: context.text.labelSmall!.copyWith(fontWeight: FontWeight.w700),
                  ),
                ] else
                  Text('New', style: context.text.labelSmall),
                const Spacer(),
                Text(
                  listing.pricingModel.isFree ? 'Free' : listing.pricingModel.label,
                  style: context.text.labelSmall,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _GridCard extends StatelessWidget {
  const _GridCard({
    required this.listing,
    required this.onTap,
    required this.isBookmarked,
    required this.onBookmark,
  });

  final AppListing listing;
  final VoidCallback onTap;
  final bool isBookmarked;
  final VoidCallback? onBookmark;

  @override
  Widget build(BuildContext context) {
    return SproutlyCard(
      onTap: onTap,
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              AppImage(
                url: listing.iconUrl,
                height: 52,
                width: 52,
                radius: BorderRadius.circular(15),
                isIcon: true,
              ),
              const Spacer(),
              if (listing.sponsored)
                const SponsoredBadge()
              else if (onBookmark != null)
                GestureDetector(
                  onTap: onBookmark,
                  behavior: HitTestBehavior.opaque,
                  child: Icon(
                    isBookmarked ? Icons.bookmark_rounded : Icons.bookmark_border_rounded,
                    size: 20,
                    color: isBookmarked ? context.colors.primary : context.colors.onSurfaceVariant,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            listing.name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: context.text.titleSmall,
          ),
          const SizedBox(height: 3),
          Expanded(
            child: Text(
              listing.tagline,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: context.text.labelSmall!.copyWith(height: 1.4),
            ),
          ),
          const SizedBox(height: 8),
          _MetaLine(listing: listing, showDeveloper: false),
        ],
      ),
    );
  }
}

/// Horizontal developer identity used on cards, detail pages and profiles.
class DeveloperRow extends StatelessWidget {
  const DeveloperRow({
    required this.name,
    required this.onTap,
    super.key,
    this.photoUrl,
    this.subtitle,
    this.size = 34,
  });

  final String name;
  final String? photoUrl;
  final String? subtitle;
  final VoidCallback onTap;
  final double size;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(999),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            ClipOval(
              child: AppImage(
                url: photoUrl,
                height: size,
                width: size,
                radius: BorderRadius.circular(size),
                icon: Icons.person_rounded,
                isIcon: true,
              ),
            ),
            const SizedBox(width: 9),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Text(name, style: context.text.titleSmall),
                if (subtitle != null)
                  Text(subtitle!, style: context.text.labelSmall),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
