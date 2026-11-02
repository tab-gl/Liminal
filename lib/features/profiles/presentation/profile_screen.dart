import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/app_image.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_page.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/user_profile.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<UserProfile?> async = ref.watch(userProfileProvider);
    final AsyncValue<bool> adminAccess = ref.watch(adminClaimProvider);

    return SproutlyPage(
      title: 'Profile',
      scrollable: false,
      padding: EdgeInsets.zero,
      actions: <Widget>[
        IconButton(
          tooltip: 'Settings',
          icon: const Icon(Icons.settings_outlined),
          onPressed: () => context.push('/settings'),
        ),
      ],
      child: async.when(
        loading: () => const Center(child: SproutlyLoader()),
        error: (Object error, StackTrace _) => ErrorStateView(error: error),
        data: (UserProfile? profile) {
          if (profile == null) {
            return const EmptyView(
              title: 'Profile unavailable',
              message: 'We could not load your profile. Pull to refresh or sign in again.',
              icon: Icons.person_off_outlined,
            );
          }
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
            children: <Widget>[
              Row(
                children: <Widget>[
                  ClipOval(
                    child: AppImage(
                      url: profile.photoUrl,
                      height: 68,
                      width: 68,
                      radius: BorderRadius.circular(68),
                      icon: Icons.person_rounded,
                      isIcon: true,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Row(
                          children: <Widget>[
                            Flexible(
                              child: Text(
                                profile.displayName,
                                style: context.text.titleLarge,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            if (profile.isDeveloper) ...<Widget>[
                              const SizedBox(width: 6),
                              const SproutlyPill(label: 'Verified', dense: true, icon: Icons.verified_rounded),
                            ],
                          ],
                        ),
                        const SizedBox(height: 2),
                        Text(
                          profile.bio?.isNotEmpty == true ? profile.bio! : 'Add a short bio in settings',
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: context.text.bodySmall,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              Row(
                children: <Widget>[
                  Expanded(
                    child: MetricTile(
                      label: 'Published apps',
                      value: '${profile.publishedAppCount}',
                      icon: Icons.apps_rounded,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: MetricTile(
                      label: 'Followers',
                      value: '${profile.followerCount}',
                      icon: Icons.people_alt_outlined,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: MetricTile(
                      label: 'Reviews written',
                      value: '${profile.reviewCount}',
                      icon: Icons.rate_review_outlined,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              _ActionTile(
                icon: Icons.bookmark_border_rounded,
                title: 'Saved apps',
                subtitle: 'Everything you bookmarked',
                onTap: () => context.push('/bookmarks'),
              ),
              _ActionTile(
                icon: Icons.insights_rounded,
                title: 'Developer dashboard',
                subtitle: 'Views, clicks and reviews across your apps',
                onTap: () => context.push('/dashboard'),
              ),
              _ActionTile(
                icon: Icons.campaign_outlined,
                title: 'Advertising',
                subtitle: 'Create and manage campaigns',
                onTap: () => context.push('/advertising'),
              ),
              if (adminAccess.value == true)
                _ActionTile(
                  icon: Icons.admin_panel_settings_outlined,
                  title: 'Admin console',
                  subtitle: 'Moderation, campaigns, reports, users',
                  onTap: () => context.push('/admin'),
                ),
              _ActionTile(
                icon: Icons.edit_outlined,
                title: 'Edit profile',
                subtitle: 'Name, bio, links, visibility',
                onTap: () => context.push('/settings/edit-profile'),
              ),
              const SizedBox(height: 22),
              Center(
                child: Text(
                  '${AppConstants.appName} · ${AppConstants.tagline}',
                  textAlign: TextAlign.center,
                  style: context.text.labelSmall,
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _ActionTile extends StatelessWidget {
  const _ActionTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: SproutlyShapes.radiusLg,
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
          decoration: BoxDecoration(
            color: context.colors.surfaceContainerLowest,
            borderRadius: SproutlyShapes.radiusLg,
            border: Border.all(color: context.colors.outlineVariant),
          ),
          child: Row(
            children: <Widget>[
              Icon(icon, size: 20, color: context.colors.primary),
              const SizedBox(width: 13),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(title, style: context.text.titleSmall),
                    const SizedBox(height: 1),
                    Text(subtitle, style: context.text.labelSmall),
                  ],
                ),
              ),
              Icon(Icons.chevron_right_rounded, size: 18, color: context.colors.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }
}
