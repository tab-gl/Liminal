import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/providers/app_providers.dart';
import '../../../core/services/haptics_service.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/app_image.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/user_profile.dart';
import '../../../shared/widgets/application_card.dart';
import '../data/profile_repository_impl.dart';

class DeveloperProfileScreen extends ConsumerWidget {
  const DeveloperProfileScreen({required this.developerId, super.key});

  final String developerId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<UserProfile?> async = ref.watch(profileByIdProvider(developerId));
    final AsyncValue<List<AppListing>> apps = ref.watch(developerAppsProvider(developerId));
    final bool following = ref.watch(followingProvider(developerId)).value ?? false;
    final String? myUid = ref.watch(currentUserProvider)?.uid;
    final bool isMe = myUid == developerId;

    return Scaffold(
      backgroundColor: context.colors.surface,
      body: SafeArea(
        child: async.when(
          loading: () => const Center(child: SproutlyLoader()),
          error: (Object error, StackTrace _) => ErrorStateView(
            error: error,
            onRetry: () => ref.invalidate(profileByIdProvider(developerId)),
          ),
          data: (UserProfile? profile) {
            if (profile == null) {
              return const EmptyView(
                title: 'Developer not found',
                message: 'This profile may have been removed.',
                icon: Icons.person_off_outlined,
              );
            }
            return ListView(
              padding: const EdgeInsets.fromLTRB(20, 6, 20, 120),
              children: <Widget>[
                Row(
                  children: <Widget>[
                    SproutlyIconButton(
                      icon: Icons.arrow_back_rounded,
                      tooltip: 'Back',
                      onPressed: () => context.pop(),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: <Widget>[
                    ClipOval(
                      child: AppImage(
                        url: profile.photoUrl,
                        height: 74,
                        width: 74,
                        radius: BorderRadius.circular(74),
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
                                  style: context.text.headlineSmall,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              if (profile.isDeveloper) ...<Widget>[
                                const SizedBox(width: 6),
                                const SproutlyPill(
                                  label: 'Verified',
                                  dense: true,
                                  icon: Icons.verified_rounded,
                                ),
                              ],
                            ],
                          ),
                          const SizedBox(height: 3),
                          Text(
                            '${profile.publishedAppCount} apps · ${profile.followerCount} followers',
                            style: context.text.labelMedium,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                if (profile.bio != null && profile.bio!.isNotEmpty)
                  Text(profile.bio!, style: context.text.bodyMedium!.copyWith(height: 1.55)),
                if (profile.websiteUrl != null) ...<Widget>[
                  const SizedBox(height: 8),
                  InkWell(
                    onTap: () => launchUrl(
                      Uri.parse(profile.websiteUrl!),
                      mode: LaunchMode.externalApplication,
                    ),
                    child: Row(
                      children: <Widget>[
                        Icon(Icons.link_rounded, size: 15, color: context.colors.primary),
                        const SizedBox(width: 5),
                        Flexible(
                          child: Text(
                            profile.websiteUrl!,
                            style: context.text.labelMedium!.copyWith(color: context.colors.primary),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 18),
                if (!isMe)
                  SproutlyButton(
                    label: following ? 'Following' : 'Follow',
                    icon: following ? Icons.check_rounded : Icons.add_rounded,
                    variant: following ? SproutlyButtonVariant.tonal : SproutlyButtonVariant.primary,
                    expand: true,
                    onPressed: () async {
                      Haptics.press();
                      try {
                        await ref.read(profileRepositoryProvider).toggleFollow(developerId);
                      } catch (error) {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context)
                              .showSnackBar(SnackBar(content: Text(error.toString())));
                        }
                      }
                    },
                  ),
                const SizedBox(height: 24),
                Text('Apps by ${profile.displayName}', style: context.text.titleLarge),
                const SizedBox(height: 10),
                apps.when(
                  data: (List<AppListing> items) {
                    if (items.isEmpty) {
                      return const EmptyView(
                        title: 'No public apps yet',
                        message: 'This developer has not published a live listing.',
                        icon: Icons.inventory_2_outlined,
                        compact: true,
                      );
                    }
                    return Column(
                      children: <Widget>[
                        for (final AppListing listing in items)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 12),
                            child: ApplicationCard(
                              listing: listing,
                              showDeveloper: false,
                              onTap: () => context.push('/app/${listing.id}'),
                            ),
                          ),
                      ],
                    );
                  },
                  loading: () => const CardListSkeleton(count: 2),
                  error: (Object error, StackTrace _) => ErrorStateView(error: error),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
