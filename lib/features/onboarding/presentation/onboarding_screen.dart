import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/providers/settings_provider.dart';
import '../../../core/services/haptics_service.dart';
import '../../../core/theme/sproutly_motion.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../authentication/presentation/auth_controller.dart';

/// Three quiet steps, no carousel of feature bullets.
///
/// 1. The promise. 2. What the person came for (which decides where they land).
/// 3. Notification permission - asked here, once, with an explicit "Not now".
class OnboardingScreen extends ConsumerStatefulWidget {
  const OnboardingScreen({super.key});

  @override
  ConsumerState<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends ConsumerState<OnboardingScreen> {
  final PageController _pages = PageController();
  int _index = 0;
  String _intent = 'explore';
  bool _busy = false;

  @override
  void dispose() {
    _pages.dispose();
    super.dispose();
  }

  Future<void> _finish({required bool enablePush}) async {
    setState(() => _busy = true);
    if (enablePush) {
      final bool granted = await ref.read(notificationServiceProvider).requestPermission();
      await ref.read(settingsProvider.notifier).setPush(granted);
      final String? uid = ref.read(currentUserProvider)?.uid;
      if (granted && uid != null) {
        await ref.read(notificationServiceProvider).syncToken(uid);
      }
    }
    await ref.read(authControllerProvider.notifier).updateProfile(onboarded: true);
    if (!mounted) return;
    setState(() => _busy = false);
    context.go(_intent == 'publish' ? '/publish' : '/discover');
  }

  void _next() {
    Haptics.tap();
    if (_index < 2) {
      _pages.nextPage(duration: SproutlyMotion.spatialDefault, curve: SproutlyMotion.emphasized);
    } else {
      _finish(enablePush: false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return Scaffold(
      backgroundColor: theme.colorScheme.surface,
      body: SafeArea(
        child: Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 0),
              child: Row(
                children: <Widget>[
                  for (int i = 0; i < 3; i++)
                    AnimatedContainer(
                      duration: SproutlyMotion.spatialFast,
                      margin: const EdgeInsets.only(right: 6),
                      height: 5,
                      width: i == _index ? 26 : 8,
                      decoration: BoxDecoration(
                        color: i <= _index
                            ? theme.colorScheme.primary
                            : theme.colorScheme.outlineVariant,
                        borderRadius: SproutlyShapes.radiusPill,
                      ),
                    ),
                  const Spacer(),
                  TextButton(
                    onPressed: _busy ? null : () => _finish(enablePush: false),
                    child: const Text('Skip'),
                  ),
                ],
              ),
            ),
            Expanded(
              child: PageView(
                controller: _pages,
                onPageChanged: (int i) => setState(() => _index = i),
                children: <Widget>[
                  _WelcomeStep(),
                  _IntentStep(
                    selected: _intent,
                    onSelect: (String value) {
                      Haptics.tap();
                      setState(() => _intent = value);
                    },
                  ),
                  _NotificationStep(),
                ],
              ),
            ),
            Padding(
              padding: EdgeInsets.fromLTRB(24, 8, 24, 16 + MediaQuery.paddingOf(context).bottom),
              child: Column(
                children: <Widget>[
                  SproutlyButton(
                    label: _index == 2 ? 'Start exploring' : 'Continue',
                    expand: true,
                    size: SproutlyButtonSize.large,
                    isLoading: _busy,
                    trailingIcon: Icons.arrow_forward_rounded,
                    onPressed: _next,
                  ),
                  if (_index == 2) ...<Widget>[
                    const SizedBox(height: 10),
                    TextButton(
                      onPressed: _busy ? null : () => _finish(enablePush: true),
                      child: const Text('Turn on notifications'),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _WelcomeStep extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 28),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            height: 92,
            width: 92,
            decoration: BoxDecoration(
              color: context.tokens.heroSurface,
              borderRadius: BorderRadius.only(
                topLeft: const Radius.circular(48),
                topRight: const Radius.circular(24),
                bottomLeft: const Radius.circular(24),
                bottomRight: const Radius.circular(48),
              ),
            ),
            child: Icon(Icons.eco_rounded, size: 46, color: theme.colorScheme.onPrimaryContainer),
          ),
          const SizedBox(height: 34),
          Text(AppConstants.appName, style: theme.textTheme.displaySmall),
          const SizedBox(height: 10),
          Text(
            AppConstants.tagline,
            style: theme.textTheme.titleMedium!.copyWith(color: theme.colorScheme.primary),
          ),
          const SizedBox(height: 18),
          Text(
            'A marketplace built for the people who make apps, not for the '
            'companies that distribute them. Publish for free, find early '
            'audiences, and promote what you build.',
            style: theme.textTheme.bodyLarge!.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
              height: 1.55,
            ),
          ),
        ],
      ),
    );
  }
}

class _IntentStep extends StatelessWidget {
  const _IntentStep({required this.selected, required this.onSelect});

  final String selected;
  final ValueChanged<String> onSelect;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 28),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text('What brings you here?', style: theme.textTheme.headlineMedium),
          const SizedBox(height: 8),
          Text(
            'This only decides where we drop you first. Everything stays open.',
            style: theme.textTheme.bodyMedium!.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
          const SizedBox(height: 26),
          _IntentCard(
            icon: Icons.explore_rounded,
            title: 'Discover new apps',
            body: 'Browse launches, follow developers, save what you like.',
            selected: selected == 'explore',
            onTap: () => onSelect('explore'),
          ),
          const SizedBox(height: 12),
          _IntentCard(
            icon: Icons.rocket_launch_rounded,
            title: 'Publish my app',
            body: 'List your product in minutes and start finding users.',
            selected: selected == 'publish',
            onTap: () => onSelect('publish'),
          ),
        ],
      ),
    );
  }
}

class _IntentCard extends StatelessWidget {
  const _IntentCard({
    required this.icon,
    required this.title,
    required this.body,
    required this.selected,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String body;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return InkWell(
      borderRadius: SproutlyShapes.radiusLg,
      onTap: onTap,
      child: AnimatedContainer(
        duration: SproutlyMotion.spatialFast,
        curve: SproutlyMotion.emphasized,
        padding: const EdgeInsets.all(18),
        decoration: BoxDecoration(
          color: selected ? theme.colorScheme.primaryContainer : theme.colorScheme.surfaceContainerLowest,
          borderRadius: SproutlyShapes.radiusLg,
          border: Border.all(
            color: selected ? theme.colorScheme.primary : theme.colorScheme.outlineVariant,
            width: selected ? 1.6 : 1,
          ),
        ),
        child: Row(
          children: <Widget>[
            Icon(icon, size: 24, color: theme.colorScheme.onPrimaryContainer),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(title, style: theme.textTheme.titleMedium),
                  const SizedBox(height: 2),
                  Text(body, style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            if (selected) Icon(Icons.check_circle_rounded, color: theme.colorScheme.primary),
          ],
        ),
      ),
    );
  }
}

class _NotificationStep extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 28),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Icon(Icons.notifications_active_outlined, size: 40, color: theme.colorScheme.primary),
          const SizedBox(height: 22),
          Text('Stay in the loop', style: theme.textTheme.headlineMedium),
          const SizedBox(height: 10),
          Text(
            'We only send three kinds of push: your app was approved, someone '
            'reviewed it, or a campaign needs attention. Nothing promotional.',
            style: theme.textTheme.bodyMedium!.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
              height: 1.55,
            ),
          ),
        ],
      ),
    );
  }
}
