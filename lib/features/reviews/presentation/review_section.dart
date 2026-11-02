import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';
import 'package:timeago/timeago.dart' as timeago;

import '../../../core/errors/error_mapper.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/app_image.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/rating_stars.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/widgets/sproutly_sheet.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/review.dart';
import '../data/review_repository_impl.dart';
import '../domain/review_repository.dart';

/// Ratings and written reviews for one application.
class ReviewSection extends ConsumerWidget {
  const ReviewSection({required this.listing, super.key});

  final AppListing listing;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Review>> reviews = ref.watch(applicationReviewsProvider(listing.id));
    final String? uid = ref.watch(currentUserProvider)?.uid;
    final bool isOwner = uid == listing.ownerId;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 22, 20, 8),
          child: Row(
            children: <Widget>[
              Text('Reviews', style: context.text.titleLarge),
              const Spacer(),
              if (!isOwner && uid != null)
                TextButton.icon(
                  onPressed: () => _openComposer(context, ref),
                  icon: const Icon(Icons.rate_review_outlined, size: 17),
                  label: const Text('Write a review'),
                ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20),
          child: _RatingSummary(listing: listing),
        ),
        const SizedBox(height: 8),
        reviews.when(
          data: (List<Review> items) {
            if (items.isEmpty) {
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: EmptyView(
                  title: 'No reviews yet',
                  message: isOwner
                      ? 'Reviews appear here once people start using your app.'
                      : 'Be the first to tell the developer what worked.',
                  icon: Icons.chat_bubble_outline_rounded,
                  compact: true,
                ),
              );
            }
            return Column(
              children: <Widget>[
                for (final Review review in items.take(8))
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 6, 20, 6),
                    child: _ReviewTile(
                      review: review,
                      canRespond: isOwner,
                      isMine: review.userId == uid,
                    ),
                  ),
                if (items.length > 8)
                  Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(
                      'Showing the 8 most recent of ${items.length} reviews',
                      style: context.text.labelSmall,
                    ),
                  ),
              ],
            );
          },
          loading: () => const Padding(
            padding: EdgeInsets.symmetric(vertical: 24),
            child: Center(child: SproutlyLoader()),
          ),
          error: (Object error, StackTrace _) => ErrorStateView(error: error),
        ),
      ],
    );
  }

  Future<void> _openComposer(BuildContext context, WidgetRef ref) async {
    final Review? mine = await ref.read(reviewRepositoryProvider).mineFor(listing.id);
    if (!context.mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) => _ReviewComposer(
        applicationId: listing.id,
        existing: mine,
      ),
    );
  }
}

class _RatingSummary extends StatelessWidget {
  const _RatingSummary({required this.listing});

  final AppListing listing;

  @override
  Widget build(BuildContext context) {
    if (listing.reviewCount == 0) {
      return Text(
        'No ratings yet. A first review helps this app get discovered.',
        style: context.text.bodySmall,
      );
    }
    return Row(
      children: <Widget>[
        Text(
          listing.rating.toStringAsFixed(1),
          style: context.text.displaySmall!.copyWith(fontSize: 34),
        ),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            RatingStars(rating: listing.rating, size: 16),
            const SizedBox(height: 3),
            Text(
              '${listing.reviewCount} review${listing.reviewCount == 1 ? '' : 's'}',
              style: context.text.labelSmall,
            ),
          ],
        ),
      ],
    );
  }
}

class _ReviewTile extends ConsumerWidget {
  const _ReviewTile({
    required this.review,
    required this.canRespond,
    required this.isMine,
  });

  final Review review;
  final bool canRespond;
  final bool isMine;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: context.colors.surfaceContainerLowest,
        borderRadius: SproutlyShapes.radiusLg,
        border: Border.all(color: context.colors.outlineVariant),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              ClipOval(
                child: AppImage(
                  url: review.userPhotoUrl,
                  height: 30,
                  width: 30,
                  radius: BorderRadius.circular(30),
                  icon: Icons.person_rounded,
                  isIcon: true,
                ),
              ),
              const SizedBox(width: 9),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(review.userName, style: context.text.titleSmall),
                    Row(
                      children: <Widget>[
                        RatingStars(rating: review.rating.toDouble(), size: 11),
                        const SizedBox(width: 6),
                        Text(timeago.format(review.createdAt), style: context.text.labelSmall),
                      ],
                    ),
                  ],
                ),
              ),
              if (isMine)
                SproutlyPill(label: 'You', dense: true, background: context.colors.primaryContainer),
            ],
          ),
          const SizedBox(height: 9),
          Text(review.body, style: context.text.bodyMedium!.copyWith(height: 1.5)),
          if (review.developerResponse != null)
            Container(
              margin: const EdgeInsets.only(top: 10),
              padding: const EdgeInsets.all(11),
              decoration: BoxDecoration(
                color: context.colors.primaryContainer.withValues(alpha: 0.5),
                borderRadius: SproutlyShapes.radiusMd,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Icon(Icons.storefront_outlined, size: 13, color: context.colors.onPrimaryContainer),
                      const SizedBox(width: 5),
                      Text(
                        'Developer response',
                        style: context.text.labelSmall!.copyWith(
                          color: context.colors.onPrimaryContainer,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    review.developerResponse!.body,
                    style: context.text.bodySmall!.copyWith(color: context.colors.onPrimaryContainer),
                  ),
                ],
              ),
            ),
          if (canRespond && review.developerResponse == null)
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton(
                onPressed: () => _respond(context, ref),
                child: const Text('Reply as developer'),
              ),
            ),
        ],
      ),
    );
  }

  Future<void> _respond(BuildContext context, WidgetRef ref) async {
    final TextEditingController controller = TextEditingController();
    final String? body = await showDialog<String>(
      context: context,
      builder: (BuildContext context) => AlertDialog(
        title: const Text('Reply publicly'),
        content: SproutlyField(
          label: 'Your reply',
          controller: controller,
          maxLines: 4,
          hint: 'Thanks for trying it - the fix ships next week.',
          maxLength: 600,
        ),
        actions: <Widget>[
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          SproutlyButton(
            label: 'Post reply',
            size: SproutlyButtonSize.small,
            onPressed: () => Navigator.pop(context, controller.text.trim()),
          ),
        ],
      ),
    );
    controller.dispose();
    if (body == null || body.isEmpty) return;
    try {
      await ref.read(reviewRepositoryProvider).respond(reviewId: review.id, body: body);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Reply posted')),
        );
      }
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(error))));
      }
    }
  }
}

class _ReviewComposer extends ConsumerStatefulWidget {
  const _ReviewComposer({required this.applicationId, this.existing});

  final String applicationId;
  final Review? existing;

  @override
  ConsumerState<_ReviewComposer> createState() => _ReviewComposerState();
}

class _ReviewComposerState extends ConsumerState<_ReviewComposer> {
  late final TextEditingController _body =
      TextEditingController(text: widget.existing?.body ?? '');
  late int _rating = widget.existing?.rating ?? 5;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _body.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_body.text.trim().length < 12) {
      setState(() => _error = 'Please write at least 12 characters.');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    final ReviewRepository repo = ref.read(reviewRepositoryProvider);
    try {
      if (widget.existing == null) {
        await repo.create(applicationId: widget.applicationId, rating: _rating, body: _body.text);
      } else {
        await repo.update(applicationId: widget.applicationId, rating: _rating, body: _body.text);
      }
      if (!mounted) return;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(widget.existing == null ? 'Review published' : 'Review updated')),
      );
    } catch (error) {
      if (mounted) {
        setState(() {
          _busy = false;
          _error = error.toString();
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      child: SproutlySheet(
        title: widget.existing == null ? 'Rate this app' : 'Edit your review',
        subtitle: 'Reviews are public and tied to your profile.',
        actions: <Widget>[
          SproutlyButton(
            label: widget.existing == null ? 'Publish review' : 'Save changes',
            onPressed: _busy ? null : _save,
            isLoading: _busy,
            expand: true,
          ),
        ],
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Center(child: RatingStars(rating: _rating.toDouble(), size: 26, onChanged: (int v) => setState(() => _rating = v))),
            const SizedBox(height: 18),
            SproutlyField(
              label: 'Your review',
              controller: _body,
              maxLines: 6,
              maxLength: 2000,
              showCounter: true,
              hint: 'What does this app do well? What is missing?',
              errorText: _error,
            ),
          ],
        ),
      ),
    );
  }
}
