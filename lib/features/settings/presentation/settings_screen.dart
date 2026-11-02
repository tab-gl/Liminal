import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../../../core/config/app_config.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/providers/settings_provider.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/widgets/sproutly_page.dart';
import '../../../core/widgets/sproutly_sheet.dart';
import '../../../shared/models/user_profile.dart';
import '../../authentication/presentation/auth_controller.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AppSettings settings = ref.watch(settingsProvider);
    final UserProfile? profile = ref.watch(profileOrNullProvider);

    return SproutlyPage(
      title: 'Settings',
      showBack: true,
      scrollable: false,
      padding: EdgeInsets.zero,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
        children: <Widget>[
          _Section(title: 'Appearance'),
          SegmentedButton<ThemeMode>(
            segments: const <ButtonSegment<ThemeMode>>[
              ButtonSegment<ThemeMode>(value: ThemeMode.system, label: Text('System'), icon: Icon(Icons.brightness_auto_rounded, size: 16)),
              ButtonSegment<ThemeMode>(value: ThemeMode.light, label: Text('Light'), icon: Icon(Icons.light_mode_rounded, size: 16)),
              ButtonSegment<ThemeMode>(value: ThemeMode.dark, label: Text('Dark'), icon: Icon(Icons.dark_mode_rounded, size: 16)),
            ],
            selected: <ThemeMode>{settings.themeMode},
            onSelectionChanged: (Set<ThemeMode> value) =>
                ref.read(settingsProvider.notifier).setThemeMode(value.first),
          ),
          const SizedBox(height: 20),
          _Section(title: 'Feel'),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: Text('Haptics', style: context.text.bodyMedium),
            subtitle: Text('Light feedback on taps and actions', style: context.text.labelSmall),
            value: settings.hapticsEnabled,
            onChanged: (bool value) => ref.read(settingsProvider.notifier).setHaptics(value),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: Text('Push notifications', style: context.text.bodyMedium),
            subtitle: Text('Approvals, reviews and campaign alerts only', style: context.text.labelSmall),
            value: settings.pushEnabled,
            onChanged: (bool value) async {
              if (value) {
                final bool granted =
                    await ref.read(notificationServiceProvider).requestPermission();
                await ref.read(settingsProvider.notifier).setPush(granted);
                final String? uid = ref.read(currentUserProvider)?.uid;
                if (granted && uid != null) {
                  await ref.read(notificationServiceProvider).syncToken(uid);
                }
                if (!granted && context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Notifications are blocked in system settings.')),
                  );
                }
              } else {
                await ref.read(settingsProvider.notifier).setPush(false);
                await ref.read(notificationServiceProvider).clearToken();
              }
            },
          ),
          const SizedBox(height: 20),
          _Section(title: 'Ads'),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: Text('Personalised ads', style: context.text.bodyMedium),
            subtitle: Text(
              'Off still shows ads - they are just not tailored. Sproutly keeps '
              'ads small and never puts one above the fold.',
              style: context.text.labelSmall,
            ),
            value: settings.adsPersonalised,
            onChanged: (bool value) =>
                ref.read(settingsProvider.notifier).setAdsPersonalised(value),
          ),
          const SizedBox(height: 20),
          _Section(title: 'Developer'),
          if (profile != null && !profile.isDeveloper)
            _Tile(
              icon: Icons.verified_outlined,
              title: 'Request a verified developer badge',
              subtitle: 'Tells people the listings are from a real team',
              onTap: () => _requestVerification(context, ref),
            ),
          _Tile(
            icon: Icons.insights_rounded,
            title: 'Developer dashboard',
            onTap: () => context.push('/dashboard'),
          ),
          _Tile(
            icon: Icons.campaign_outlined,
            title: 'Advertising',
            onTap: () => context.push('/advertising'),
          ),
          if (profile?.isAdmin ?? false)
            _Tile(
              icon: Icons.admin_panel_settings_outlined,
              title: 'Admin console',
              onTap: () => context.push('/admin'),
            ),
          const SizedBox(height: 20),
          _Section(title: 'Account'),
          _Tile(
            icon: Icons.edit_outlined,
            title: 'Edit profile',
            onTap: () => context.push('/settings/edit-profile'),
          ),
          _Tile(
            icon: Icons.bookmark_border_rounded,
            title: 'Saved apps',
            onTap: () => context.push('/bookmarks'),
          ),
          const SizedBox(height: 14),
          SproutlyButton(
            label: 'Sign out',
            variant: SproutlyButtonVariant.outline,
            expand: true,
            onPressed: () async {
              final bool ok = await showConfirmDialog(
                context,
                title: 'Sign out?',
                message: 'Your saved apps and drafts stay on your account.',
                confirmLabel: 'Sign out',
              );
              if (!ok) return;
              await ref.read(authControllerProvider.notifier).signOut();
              if (context.mounted) context.go('/sign-in');
            },
          ),
          const SizedBox(height: 10),
          SproutlyButton(
            label: 'Delete account',
            variant: SproutlyButtonVariant.destructive,
            expand: true,
            onPressed: () => _deleteAccount(context, ref),
          ),
          const SizedBox(height: 24),
          FutureBuilder<PackageInfo>(
            future: PackageInfo.fromPlatform(),
            builder: (BuildContext context, AsyncSnapshot<PackageInfo> snapshot) {
              final String version = snapshot.hasData
                  ? '${snapshot.data!.version} (${snapshot.data!.buildNumber})'
                  : '-';
              return Column(
                children: <Widget>[
                  Text(
                    '${AppConstants.appName} $version · ${AppConfig.environment}',
                    style: context.text.labelSmall,
                  ),
                  const SizedBox(height: 4),
                  Text(AppConstants.tagline, style: context.text.labelSmall),
                ],
              );
            },
          ),
        ],
      ),
    );
  }

  Future<void> _requestVerification(BuildContext context, WidgetRef ref) async {
    final TextEditingController controller = TextEditingController();
    final String? motivation = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) => Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
        child: SproutlySheet(
          title: 'Request verification',
          subtitle: 'Tell us who you are and what you have shipped.',
          actions: <Widget>[
            SproutlyButton(
              label: 'Send request',
              expand: true,
              onPressed: () => Navigator.pop(context, controller.text.trim()),
            ),
          ],
          child: SproutlyField(
            label: 'Why should this account be verified?',
            controller: controller,
            maxLines: 4,
            maxLength: 500,
          ),
        ),
      ),
    );
    controller.dispose();
    if (motivation == null || motivation.isEmpty) return;
    final bool ok = await ref.read(authControllerProvider.notifier).requestDeveloperAccess(motivation);
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok ? 'Request sent to the Sproutly team.' : 'The request could not be sent.')),
    );
  }

  Future<void> _deleteAccount(BuildContext context, WidgetRef ref) async {
    final bool ok = await showConfirmDialog(
      context,
      title: 'Delete your account?',
      message: 'Your listings, reviews and campaigns are removed. This cannot be undone.',
      confirmLabel: 'Delete everything',
      destructive: true,
    );
    if (!ok) return;
    final bool deleted = await ref.read(authControllerProvider.notifier).deleteAccount();
    if (!context.mounted) return;
    if (deleted) {
      context.go('/sign-in');
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Sign in again, then retry - deleting requires a fresh login.')),
      );
    }
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Text(
        title.toUpperCase(),
        style: context.text.labelSmall!.copyWith(letterSpacing: 1.1, fontWeight: FontWeight.w700),
      ),
    );
  }
}

class _Tile extends StatelessWidget {
  const _Tile({required this.icon, required this.title, this.subtitle, required this.onTap});

  final IconData icon;
  final String title;
  final String? subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: InkWell(
        borderRadius: SproutlyShapes.radiusLg,
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
          decoration: BoxDecoration(
            color: context.colors.surfaceContainerLowest,
            borderRadius: SproutlyShapes.radiusLg,
            border: Border.all(color: context.colors.outlineVariant),
          ),
          child: Row(
            children: <Widget>[
              Icon(icon, size: 19, color: context.colors.primary),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(title, style: context.text.titleSmall),
                    if (subtitle != null)
                      Text(subtitle!, style: context.text.labelSmall),
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
