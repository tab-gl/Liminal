import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';
import 'package:timeago/timeago.dart' as timeago;

import '../../../core/providers/app_providers.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/widgets/sproutly_sheet.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/campaign.dart';
import '../../../shared/models/report.dart';
import '../../../shared/models/user_profile.dart';
import '../data/admin_repository_impl.dart';

/// Protected admin console.
///
/// The router blocks non-admins, the screen re-checks the profile, and every
/// mutation is a callable function that re-verifies the `admin` claim. Three
/// independent gates, none of them decorative.
class AdminScreen extends ConsumerWidget {
  const AdminScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final bool isAdmin = ref.watch(isAdminProvider);

    if (!isAdmin) {
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
                  title: 'Administrators only',
                  message: 'This area requires the admin role. Ask another administrator '
                      'to grant it from the users tab.',
                  icon: Icons.lock_outline_rounded,
                ),
              ),
            ],
          ),
        ),
      );
    }

    return DefaultTabController(
      length: 4,
      child: Scaffold(
        backgroundColor: context.colors.surface,
        body: SafeArea(
          child: Column(
            children: <Widget>[
              Padding(
                padding: const EdgeInsets.fromLTRB(8, 6, 20, 0),
                child: Row(
                  children: <Widget>[
                    SproutlyIconButton(
                      icon: Icons.arrow_back_rounded,
                      tooltip: 'Back',
                      onPressed: () => context.pop(),
                    ),
                    const SizedBox(width: 6),
                    Expanded(child: Text('Admin console', style: context.text.headlineSmall)),
                  ],
                ),
              ),
              const TabBar(
                isScrollable: true,
                tabAlignment: TabAlignment.start,
                tabs: <Widget>[
                  Tab(text: 'Queue'),
                  Tab(text: 'Campaigns'),
                  Tab(text: 'Reports'),
                  Tab(text: 'Users'),
                ],
              ),
              const Expanded(
                child: TabBarView(
                  children: <Widget>[
                    _ModerationTab(),
                    _CampaignModerationTab(),
                    _ReportsTab(),
                    _UsersTab(),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ModerationTab extends ConsumerWidget {
  const _ModerationTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<AppListing>> async = ref.watch(pendingApplicationsProvider);
    return async.when(
      loading: () => const Center(child: SproutlyLoader()),
      error: (Object error, StackTrace _) => ErrorStateView(
        error: error,
        onRetry: () => ref.invalidate(pendingApplicationsProvider),
      ),
      data: (List<AppListing> items) {
        if (items.isEmpty) {
          return const EmptyView(
            title: 'Queue is clear',
            message: 'No listings are waiting for review.',
            icon: Icons.done_all_rounded,
          );
        }
        return ListView.separated(
          padding: const EdgeInsets.all(20),
          itemCount: items.length,
          separatorBuilder: (_, __) => const SizedBox(height: 12),
          itemBuilder: (BuildContext context, int index) => _QueueCard(listing: items[index]),
        );
      },
    );
  }
}

class _QueueCard extends ConsumerStatefulWidget {
  const _QueueCard({required this.listing});

  final AppListing listing;

  @override
  ConsumerState<_QueueCard> createState() => _QueueCardState();
}

class _QueueCardState extends ConsumerState<_QueueCard> {
  bool _busy = false;

  Future<void> _decide({required bool approve}) async {
    String? reason;
    if (!approve) {
      final TextEditingController controller = TextEditingController();
      final String? value = await showDialog<String>(
        context: context,
        builder: (BuildContext context) => AlertDialog(
          title: const Text('Send back to the developer'),
          content: SproutlyField(
            label: 'What needs to change?',
            controller: controller,
            maxLines: 3,
            hint: 'The download link returns a 404.',
          ),
          actions: <Widget>[
            TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
            SproutlyButton(
              label: 'Request changes',
              size: SproutlyButtonSize.small,
              onPressed: () => Navigator.pop(context, controller.text.trim()),
            ),
          ],
        ),
      );
      controller.dispose();
      if (value == null || value.isEmpty) return;
      reason = value;
    }
    setState(() => _busy = true);
    try {
      await ref.read(adminRepositoryProvider).moderateApplication(
            applicationId: widget.listing.id,
            approve: approve,
            reason: reason,
          );
      ref.invalidate(pendingApplicationsProvider);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final AppListing listing = widget.listing;
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
              Expanded(child: Text(listing.name, style: context.text.titleSmall)),
              StatusChip(status: listing.status.wire),
            ],
          ),
          const SizedBox(height: 3),
          Text(listing.tagline, style: context.text.bodySmall, maxLines: 2),
          const SizedBox(height: 6),
          Text(
            '${listing.ownerName} · ${listing.categoryId} · updated ${timeago.format(listing.updatedAt)}',
            style: context.text.labelSmall,
          ),
          const SizedBox(height: 10),
          Row(
            children: <Widget>[
              Expanded(
                child: SproutlyButton(
                  label: 'Approve',
                  size: SproutlyButtonSize.small,
                  isLoading: _busy,
                  onPressed: () => _decide(approve: true),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: SproutlyButton(
                  label: 'Request changes',
                  size: SproutlyButtonSize.small,
                  variant: SproutlyButtonVariant.outline,
                  onPressed: _busy ? null : () => _decide(approve: false),
                ),
              ),
              IconButton(
                tooltip: 'Open listing',
                icon: const Icon(Icons.open_in_new_rounded, size: 17),
                onPressed: () => context.push('/app/${listing.id}'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _CampaignModerationTab extends ConsumerWidget {
  const _CampaignModerationTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Campaign>> async = ref.watch(pendingCampaignsProvider);
    return async.when(
      loading: () => const Center(child: SproutlyLoader()),
      error: (Object error, StackTrace _) => ErrorStateView(
        error: error,
        onRetry: () => ref.invalidate(pendingCampaignsProvider),
      ),
      data: (List<Campaign> items) {
        if (items.isEmpty) {
          return const EmptyView(
            title: 'No campaigns waiting',
            message: 'New campaigns appear here for approval.',
            icon: Icons.campaign_outlined,
          );
        }
        return ListView.separated(
          padding: const EdgeInsets.all(20),
          itemCount: items.length,
          separatorBuilder: (_, __) => const SizedBox(height: 12),
          itemBuilder: (BuildContext context, int index) {
            final Campaign campaign = items[index];
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
                  Text(campaign.name, style: context.text.titleSmall),
                  const SizedBox(height: 3),
                  Text(
                    '${campaign.applicationName} · requested ${campaign.requestedBudgetPaise ~/ 100} INR, funded ${campaign.budgetPaise ~/ 100} INR · '
                    '${campaign.placements.length} placements',
                    style: context.text.labelSmall,
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: <Widget>[
                      Expanded(
                        child: SproutlyButton(
                          label: 'Approve',
                          size: SproutlyButtonSize.small,
                          onPressed: () async {
                            await ref.read(adminRepositoryProvider).moderateCampaign(
                                  campaignId: campaign.id,
                                  approve: true,
                                );
                            ref.invalidate(pendingCampaignsProvider);
                          },
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: SproutlyButton(
                          label: 'Reject',
                          size: SproutlyButtonSize.small,
                          variant: SproutlyButtonVariant.outline,
                          onPressed: () async {
                            await ref.read(adminRepositoryProvider).moderateCampaign(
                                  campaignId: campaign.id,
                                  approve: false,
                                  reason: 'Did not meet the creative policy.',
                                );
                            ref.invalidate(pendingCampaignsProvider);
                          },
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }
}

class _ReportsTab extends ConsumerWidget {
  const _ReportsTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<ContentReport>> async = ref.watch(openReportsProvider);
    return async.when(
      loading: () => const Center(child: SproutlyLoader()),
      error: (Object error, StackTrace _) => ErrorStateView(
        error: error,
        onRetry: () => ref.invalidate(openReportsProvider),
      ),
      data: (List<ContentReport> items) {
        if (items.isEmpty) {
          return const EmptyView(
            title: 'No open reports',
            message: 'User reports land here.',
            icon: Icons.flag_outlined,
          );
        }
        return ListView.separated(
          padding: const EdgeInsets.all(20),
          itemCount: items.length,
          separatorBuilder: (_, __) => const SizedBox(height: 12),
          itemBuilder: (BuildContext context, int index) {
            final ContentReport report = items[index];
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
                  Text(report.reason, style: context.text.titleSmall),
                  const SizedBox(height: 3),
                  Text(
                    '${report.target} · ${report.targetId} · ${timeago.format(report.createdAt)}',
                    style: context.text.labelSmall,
                  ),
                  if (report.details != null) ...<Widget>[
                    const SizedBox(height: 6),
                    Text(report.details!, style: context.text.bodySmall),
                  ],
                  const SizedBox(height: 10),
                  Row(
                    children: <Widget>[
                      Expanded(
                        child: SproutlyButton(
                          label: 'Uphold',
                          size: SproutlyButtonSize.small,
                          onPressed: () async {
                            await ref.read(adminRepositoryProvider).resolveReport(
                                  reportId: report.id,
                                  uphold: true,
                                );
                            ref.invalidate(openReportsProvider);
                          },
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: SproutlyButton(
                          label: 'Dismiss',
                          size: SproutlyButtonSize.small,
                          variant: SproutlyButtonVariant.outline,
                          onPressed: () async {
                            await ref.read(adminRepositoryProvider).resolveReport(
                                  reportId: report.id,
                                  uphold: false,
                                );
                            ref.invalidate(openReportsProvider);
                          },
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }
}

class _UsersTab extends ConsumerStatefulWidget {
  const _UsersTab();

  @override
  ConsumerState<_UsersTab> createState() => _UsersTabState();
}

class _UsersTabState extends ConsumerState<_UsersTab> {
  final TextEditingController _query = TextEditingController();
  String _submitted = '';

  @override
  void dispose() {
    _query.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final AsyncValue<List<UserProfile>> async = ref.watch(adminUsersProvider(_submitted));
    return Column(
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
          child: SproutlyField(
            label: 'Search by display name',
            controller: _query,
            hint: 'Start typing a name',
            textInputAction: TextInputAction.search,
            onSubmitted: (String value) => setState(() => _submitted = value.trim()),
          ),
        ),
        Expanded(
          child: async.when(
            loading: () => const Center(child: SproutlyLoader()),
            error: (Object error, StackTrace _) => ErrorStateView(error: error),
            data: (List<UserProfile> items) => ListView.separated(
              padding: const EdgeInsets.fromLTRB(20, 4, 20, 40),
              itemCount: items.length,
              separatorBuilder: (_, __) => const SizedBox(height: 10),
              itemBuilder: (BuildContext context, int index) {
                final UserProfile user = items[index];
                return Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: context.colors.surfaceContainerLowest,
                    borderRadius: SproutlyShapes.radiusLg,
                    border: Border.all(color: context.colors.outlineVariant),
                  ),
                  child: Row(
                    children: <Widget>[
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(user.displayName, style: context.text.titleSmall),
                            const SizedBox(height: 2),
                            Text(
                              '${user.role.name} · ${user.publishedAppCount} apps',
                              style: context.text.labelSmall,
                            ),
                          ],
                        ),
                      ),
                      PopupMenuButton<String>(
                        icon: const Icon(Icons.more_horiz_rounded, size: 18),
                        onSelected: (String role) async {
                          await ref.read(adminRepositoryProvider).setRole(uid: user.uid, role: role);
                          ref.invalidate(adminUsersProvider(_submitted));
                        },
                        itemBuilder: (BuildContext context) => const <PopupMenuEntry<String>>[
                          PopupMenuItem<String>(value: 'user', child: Text('Set as user')),
                          PopupMenuItem<String>(value: 'developer', child: Text('Verify as developer')),
                          PopupMenuItem<String>(value: 'admin', child: Text('Make administrator')),
                        ],
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        ),
      ],
    );
  }
}
