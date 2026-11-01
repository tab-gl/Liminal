import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../core/widgets/sproutly_nav_bar.dart';
import '../features/notifications/data/notification_repository_impl.dart';

/// The five-destination shell.
///
/// Uses GoRouter's indexed stack so each tab keeps its scroll position and its
/// own navigation history - the behaviour people expect from a native app.
class AppShell extends ConsumerWidget {
  const AppShell({required this.shell, super.key});

  final StatefulNavigationShell shell;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final int unread = ref.watch(unreadCountProvider).value ?? 0;

    return Scaffold(
      extendBody: true,
      body: shell,
      bottomNavigationBar: SproutlyNavBar(
        currentIndex: shell.currentIndex,
        badges: <int, int>{3: unread},
        onSelected: (int index) => shell.goBranch(
          index,
          // Re-tapping the active tab pops it back to its root.
          initialLocation: index == shell.currentIndex,
        ),
      ),
    );
  }
}
