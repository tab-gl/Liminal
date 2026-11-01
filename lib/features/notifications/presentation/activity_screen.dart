import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';
import 'package:timeago/timeago.dart' as timeago;

import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_page.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/app_notification.dart';
import '../../../shared/models/enums.dart';
import '../data/notification_repository_impl.dart';

class ActivityScreen extends ConsumerWidget {
  const ActivityScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<AppNotification>> inbox = ref.watch(inboxProvider);

    return SproutlyPage(
      title: 'Activity',
      subtitle: 'Approvals, reviews and campaign alerts',
      scrollable: false,
      padding: EdgeInsets.zero,
      actions: <Widget>[
        IconButton(
          tooltip: 'Mark all as read',
          icon: const Icon(Icons.done_all_rounded, size: 20),
          onPressed: () => ref.read(notificationRepositoryProvider).markAllRead(),
        ),
      ],
      child: inbox.when(
        data: (List<AppNotification> items) {
          if (items.isEmpty) {
            return const EmptyView(
              title: 'Nothing yet',
              message: 'When your app is approved, reviewed, or a campaign needs attention, '
                  'it shows up here.',
              icon: Icons.notifications_none_rounded,
            );
          }
          return ListView.separated(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
            itemCount: items.length,
            separatorBuilder: (_, __) => const SizedBox(height: 10),
            itemBuilder: (BuildContext context, int index) => _NotificationTile(item: items[index]),
          );
        },
        loading: () => const CardListSkeleton(count: 4),
        error: (Object error, StackTrace _) => ErrorStateView(
          error: error,
          onRetry: () => ref.invalidate(inboxProvider),
        ),
      ),
    );
  }
}

class _NotificationTile extends ConsumerWidget {
  const _NotificationTile({required this.item});

  final AppNotification item;

  static IconData _icon(NotificationType type) => switch (type) {
        NotificationType.applicationApproved => Icons.check_circle_outline_rounded,
        NotificationType.applicationRejected => Icons.edit_note_rounded,
        NotificationType.applicationSuspended => Icons.pause_circle_outline_rounded,
        NotificationType.reviewReceived => Icons.chat_bubble_outline_rounded,
        NotificationType.newFollower => Icons.person_add_alt_rounded,
        NotificationType.campaignApproved => Icons.campaign_outlined,
        NotificationType.campaignRejected => Icons.report_gmailerrorred_rounded,
        NotificationType.campaignBudgetLow => Icons.speed_rounded,
        NotificationType.campaignExhausted => Icons.money_off_rounded,
        NotificationType.system => Icons.info_outline_rounded,
      };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return InkWell(
      borderRadius: SproutlyShapes.radiusLg,
      onTap: () async {
        if (item.isUnread) {
          await ref.read(notificationRepositoryProvider).markRead(item.id);
        }
        if (item.route != null && context.mounted) context.push(item.route!);
      },
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: item.isUnread
              ? context.colors.primaryContainer.withValues(alpha: 0.45)
              : context.colors.surfaceContainerLowest,
          borderRadius: SproutlyShapes.radiusLg,
          border: Border.all(color: context.colors.outlineVariant),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Container(
              height: 34,
              width: 34,
              decoration: BoxDecoration(
                color: context.colors.surface,
                borderRadius: SproutlyShapes.radiusSm,
              ),
              child: Icon(_icon(item.type), size: 17, color: context.colors.primary),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(item.title, style: context.text.titleSmall),
                  const SizedBox(height: 2),
                  Text(item.body, style: context.text.bodySmall!.copyWith(height: 1.45)),
                  const SizedBox(height: 5),
                  Text(timeago.format(item.createdAt), style: context.text.labelSmall),
                ],
              ),
            ),
            if (item.isUnread)
              Container(
                height: 8,
                width: 8,
                margin: const EdgeInsets.only(top: 5, left: 6),
                decoration: BoxDecoration(
                  color: context.colors.primary,
                  shape: BoxShape.circle,
                ),
              ),
          ],
        ),
      ),
    );
  }
}
