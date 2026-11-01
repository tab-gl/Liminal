package app.tab.store.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.tab.store.di.ServiceGraph
import app.tab.store.feature.activity.ActivityScreen
import app.tab.store.feature.admin.AdminScreen
import app.tab.store.feature.ads.AdvertisingScreen
import app.tab.store.feature.auth.SignInScreen
import app.tab.store.feature.bookmarks.BookmarksScreen
import app.tab.store.feature.categories.CategoriesScreen
import app.tab.store.feature.dashboard.DashboardScreen
import app.tab.store.feature.detail.DetailScreen
import app.tab.store.feature.discover.DiscoverScreen
import app.tab.store.feature.onboarding.OnboardingScreen
import app.tab.store.feature.profile.ProfileScreen
import app.tab.store.feature.publish.PublishScreen
import app.tab.store.feature.settings.SettingsScreen
import app.tab.store.push.PushManager
import app.tab.store.ui.components.FloatingNavBar
import kotlinx.coroutines.launch

object Routes {
    const val DISCOVER = "discover"
    const val CATEGORIES = "categories"
    const val PUBLISH = "publish"
    const val ACTIVITY = "activity"
    const val PROFILE = "profile"
    const val SIGN_IN = "sign-in"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val ADVERTISING = "advertising"
    const val ADMIN = "admin"
    const val BOOKMARKS = "bookmarks"
    const val SETTINGS = "settings"
    const val DETAIL = "app/{appId}"
    fun detail(appId: String) = "app/$appId"

    /** Incoming https://sproutly.app/... links, mapped onto the graph. */
    fun fromDeepLink(uri: android.net.Uri?): String? {
        if (uri == null) return null
        val segments = uri.pathSegments
        if (segments.size < 2) return null
        return when (segments[0]) {
            "app" -> detail(segments[1])
            "bookmarks" -> BOOKMARKS
            "dashboard" -> DASHBOARD
            else -> null
        }
    }
}

@Composable
fun SproutlyNavHost(initialRoute: String? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val uid = ServiceGraph.auth.uid
    val unread by ServiceGraph.notifications
        .unreadCount(uid)
        .collectAsState(initial = 0)

    // First-run gate and push registration. Both are side effects of having a
    // session, so they belong here rather than in each screen.
    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        launch { runCatching { PushManager.syncToken(uid) } }
        runCatching { ServiceGraph.auth.ensureProfile() }.getOrNull()?.let { profile ->
            if (!profile.onboarded && currentRoute != Routes.ONBOARDING) {
                navController.navigate(Routes.ONBOARDING) { launchSingleTop = true }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = when {
                uid == null -> Routes.SIGN_IN
                initialRoute != null -> initialRoute
                else -> Routes.DISCOVER
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.SIGN_IN) {
                SignInScreen(
                    onSignedIn = {
                        navController.navigate(Routes.DISCOVER) {
                            popUpTo(Routes.SIGN_IN) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onDone = {
                        navController.navigate(Routes.DISCOVER) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    onOpenApp = { id -> navController.navigate(Routes.detail(id)) },
                    bottomPadding = 120.dp(),
                )
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    onOpenCategory = { id -> navController.navigate(Routes.detail(id)) },
                    bottomPadding = 120.dp(),
                )
            }
            composable(Routes.PUBLISH) {
                PublishScreen(
                    onSubmitted = { navController.navigate(Routes.PROFILE) },
                    bottomPadding = 120.dp(),
                )
            }
            composable(Routes.ACTIVITY) { ActivityScreen(bottomPadding = 120.dp()) }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onSignOut = {
                        navController.navigate(Routes.SIGN_IN) { popUpTo(0) { inclusive = true } }
                    },
                    onOpenDashboard = { navController.navigate(Routes.DASHBOARD) },
                    onOpenAdvertising = { navController.navigate(Routes.ADVERTISING) },
                    onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                    bottomPadding = 120.dp(),
                )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(onBack = { navController.popBackStack() }, bottomPadding = 24.dp())
            }
            composable(Routes.ADVERTISING) {
                AdvertisingScreen(onBack = { navController.popBackStack() }, bottomPadding = 24.dp())
            }
            composable(Routes.ADMIN) {
                AdminScreen(onBack = { navController.popBackStack() }, bottomPadding = 24.dp())
            }
            composable(Routes.BOOKMARKS) {
                BookmarksScreen(
                    onOpenApp = { id -> navController.navigate(Routes.detail(id)) },
                    onBack = { navController.popBackStack() },
                    bottomPadding = 24.dp(),
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onSignOut = {
                        navController.navigate(Routes.SIGN_IN) { popUpTo(0) { inclusive = true } }
                    },
                    bottomPadding = 24.dp(),
                )
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("appId") { type = NavType.StringType }),
            ) { entry ->
                DetailScreen(
                    appId = entry.arguments?.getString("appId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }

        val chromeRoutes = listOf(
            Routes.DISCOVER, Routes.CATEGORIES, Routes.PUBLISH, Routes.ACTIVITY, Routes.PROFILE,
        )
        if (currentRoute in chromeRoutes) {
            FloatingNavBar(
                currentRoute = currentRoute.orEmpty(),
                unreadCount = unread,
                onSelect = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Routes.DISCOVER) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
