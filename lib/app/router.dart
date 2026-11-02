import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../core/providers/app_providers.dart';
import '../features/admin/presentation/admin_screen.dart';
import '../features/advertising/presentation/advertising_screen.dart';
import '../features/advertising/presentation/campaign_detail_screen.dart';
import '../features/applications/presentation/application_detail_screen.dart';
import '../features/authentication/presentation/forgot_password_screen.dart';
import '../features/authentication/presentation/sign_in_screen.dart';
import '../features/authentication/presentation/sign_up_screen.dart';
import '../features/bookmarks/presentation/bookmarks_screen.dart';
import '../features/categories/presentation/categories_screen.dart';
import '../features/categories/presentation/category_screen.dart';
import '../features/developer_dashboard/presentation/app_analytics_screen.dart';
import '../features/developer_dashboard/presentation/dashboard_screen.dart';
import '../features/discovery/presentation/discover_screen.dart';
import '../features/discovery/presentation/search_screen.dart';
import '../features/notifications/presentation/activity_screen.dart';
import '../features/onboarding/presentation/onboarding_screen.dart';
import '../features/profiles/presentation/developer_profile_screen.dart';
import '../features/profiles/presentation/profile_screen.dart';
import '../features/publishing/presentation/publish_flow_screen.dart';
import '../features/publishing/presentation/publish_screen.dart';
import '../features/settings/presentation/edit_profile_screen.dart';
import '../features/settings/presentation/settings_screen.dart';
import '../shared/models/user_profile.dart';
import 'app_shell.dart';

/// Routes that render without a session. Discovery and public listing pages are
/// browsable before sign-in, which is how a marketplace gets its first users.
const Set<String> _publicRoutes = <String>{
  '/discover',
  '/categories',
  '/search',
};

bool _isPublic(String location) {
  if (_publicRoutes.contains(location)) return true;
  return location.startsWith('/app/') ||
      location.startsWith('/developer/') ||
      location.startsWith('/category/');
}

const Set<String> _authRoutes = <String>{
  '/sign-in',
  '/sign-up',
  '/forgot-password',
};

final Provider<GoRouter> routerProvider = Provider<GoRouter>((Ref ref) {
  // A tiny notifier that re-runs redirects whenever the session or the profile
  // document changes, without rebuilding the router (which would reset the
  // navigation stack).
  final ValueNotifier<int> refresh = ValueNotifier<int>(0);
  ref.listen<AsyncValue<User?>>(authStateProvider, (_, __) => refresh.value++);
  ref.listen<AsyncValue<UserProfile?>>(userProfileProvider, (_, __) => refresh.value++);
  ref.onDispose(refresh.dispose);

  final GoRouter router = GoRouter(
    initialLocation: '/discover',
    refreshListenable: refresh,
    // Screen tracking lives here: MaterialApp.router in the decoupled
    // material_ui package does not accept navigatorObservers.
    observers: <NavigatorObserver>[ref.read(analyticsProvider).observer],
    debugLogDiagnostics: kDebugMode,
    redirect: (BuildContext context, GoRouterState state) async {
      final String location = state.matchedLocation;
      final bool isAuthRoute = _authRoutes.contains(location);
      final AsyncValue<User?> auth = ref.read(authStateProvider);

      // Resolve the session synchronously: `currentUser` is already set after
      // initializeApp, so the very first redirect is decided without waiting for
      // the auth stream. No splash, no loading route.
      final User? user = auth.value ?? ref.read(initialUserProvider);

      if (user == null) {
        if (isAuthRoute) return null;
        if (_isPublic(location)) return null;
        return '/sign-in?from=${Uri.encodeComponent(state.uri.toString())}';
      }

      // The profile loads in parallel. While it is in flight we do not block -
      // the app renders, and when the document lands this listener re-runs the
      // redirect if onboarding is still outstanding.
      final UserProfile? profile = ref.read(userProfileProvider).value;
      if (profile != null && !profile.onboarded && location != '/onboarding') {
        return '/onboarding';
      }

      if (isAuthRoute || location == '/onboarding') {
        return '/discover';
      }

      // Never authorize the admin console from the Firestore profile field.
      // Custom claims are issued only by trusted server-side role management.
      if (location == '/admin') {
        try {
          final IdTokenResult token = await user.getIdTokenResult(true);
          if (token.claims?['role'] != 'admin') return '/discover';
        } catch (_) {
          // Authorization must fail closed if the trusted token cannot be read.
          return '/discover';
        }
      }
      return null;
    },
    routes: <RouteBase>[
      GoRoute(
        path: '/sign-in',
        builder: (BuildContext context, GoRouterState state) =>
            SignInScreen(redirectTo: state.uri.queryParameters['from']),
      ),
      GoRoute(path: '/sign-up', builder: (_, __) => const SignUpScreen()),
      GoRoute(path: '/forgot-password', builder: (_, __) => const ForgotPasswordScreen()),
      GoRoute(path: '/onboarding', builder: (_, __) => const OnboardingScreen()),

      // ---------------------------------------------------------------- shell
      StatefulShellRoute.indexedStack(
        builder: (BuildContext context, GoRouterState state, StatefulNavigationShell shell) =>
            AppShell(shell: shell),
        branches: <StatefulShellBranch>[
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(path: '/discover', builder: (_, __) => const DiscoverScreen()),
            ],
          ),
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(path: '/categories', builder: (_, __) => const CategoriesScreen()),
            ],
          ),
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(path: '/publish', builder: (_, __) => const PublishScreen()),
            ],
          ),
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(path: '/activity', builder: (_, __) => const ActivityScreen()),
            ],
          ),
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(path: '/profile', builder: (_, __) => const ProfileScreen()),
            ],
          ),
        ],
      ),

      // ------------------------------------------------------- full-screen
      GoRoute(
        path: '/search',
        builder: (BuildContext context, GoRouterState state) =>
            SearchScreen(initialQuery: state.uri.queryParameters['q']),
      ),
      GoRoute(
        path: '/app/:id',
        builder: (BuildContext context, GoRouterState state) =>
            ApplicationDetailScreen(applicationId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/developer/:uid',
        builder: (BuildContext context, GoRouterState state) =>
            DeveloperProfileScreen(developerId: state.pathParameters['uid']!),
      ),
      GoRoute(
        path: '/category/:id',
        builder: (BuildContext context, GoRouterState state) =>
            CategoryScreen(categoryId: state.pathParameters['id']!),
      ),
      GoRoute(path: '/bookmarks', builder: (_, __) => const BookmarksScreen()),
      GoRoute(path: '/dashboard', builder: (_, __) => const DashboardScreen()),
      GoRoute(
        path: '/dashboard/:id',
        builder: (BuildContext context, GoRouterState state) =>
            AppAnalyticsScreen(applicationId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/advertising',
        builder: (BuildContext context, GoRouterState state) =>
            AdvertisingScreen(preselectAppId: state.uri.queryParameters['app']),
      ),
      GoRoute(
        path: '/campaign/:id',
        builder: (BuildContext context, GoRouterState state) =>
            CampaignDetailScreen(campaignId: state.pathParameters['id']!),
      ),
      GoRoute(path: '/publish/new', builder: (_, __) => const PublishFlowScreen()),
      GoRoute(
        path: '/publish/edit/:id',
        builder: (BuildContext context, GoRouterState state) =>
            PublishFlowScreen(applicationId: state.pathParameters['id']),
      ),
      GoRoute(path: '/settings', builder: (_, __) => const SettingsScreen()),
      GoRoute(path: '/settings/edit-profile', builder: (_, __) => const EditProfileScreen()),
      GoRoute(path: '/admin', builder: (_, __) => const AdminScreen()),
    ],
    errorBuilder: (BuildContext context, GoRouterState state) => Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                const Icon(Icons.explore_off_outlined, size: 40),
                const SizedBox(height: 12),
                Text('We could not find that page',
                    style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 6),
                Text(state.uri.toString(), style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: () => context.go('/discover'),
                  child: const Text('Back to Discover'),
                ),
              ],
            ),
          ),
        ),
      ),
    ),
  );
  return router;
});
