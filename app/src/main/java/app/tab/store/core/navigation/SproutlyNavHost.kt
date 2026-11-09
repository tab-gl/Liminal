package app.tab.store.core.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.activity.compose.BackHandler
import app.tab.store.core.ui.Tokens
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import app.tab.store.core.ui.Scaffold
import app.tab.store.core.ui.SnackbarHost
import app.tab.store.core.ui.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import app.tab.store.core.utils.AppErrors
import app.tab.store.domain.model.UserRole
import app.tab.store.feature.auth.rememberGoogleSignIn
import app.tab.store.feature.profile.AccountSwitcherSheet
import app.tab.store.feature.profile.AccountSwitcherViewModel
import app.tab.store.feature.profile.ProfileViewModel
import app.tab.store.feature.profile.Workspace
import app.tab.store.feature.seller.SellerVerificationScreen
import app.tab.store.feature.admin.AdminCategoriesScreen
import app.tab.store.feature.admin.AdminTagsScreen
import app.tab.store.feature.admin.AdminDashboardScreen
import app.tab.store.feature.admin.AdminProductsScreen
import app.tab.store.feature.admin.AdminStoresScreen
import app.tab.store.feature.admin.AdminUsersScreen
import app.tab.store.feature.auth.ForgotPasswordScreen
import app.tab.store.feature.auth.LoginScreen
import app.tab.store.feature.auth.RegisterScreen
import app.tab.store.feature.home.HomeScreen
import app.tab.store.feature.notifications.NotificationsScreen
import app.tab.store.feature.games.GamesScreen
import app.tab.store.feature.legal.LegalScreen
import app.tab.store.feature.onboarding.OnboardingScreen
import app.tab.store.feature.offers.OffersScreen
import app.tab.store.feature.rankings.RankingsScreen
import app.tab.store.feature.product.ProductDetailScreen
import app.tab.store.feature.profile.ProfileEditScreen
import app.tab.store.feature.profile.ProfileScreen
import app.tab.store.feature.profile.SettingsScreen
import app.tab.store.feature.search.SearchScreen
import app.tab.store.feature.seller.SellerAnalyticsScreen
import app.tab.store.feature.seller.SellerDashboardScreen
import app.tab.store.feature.library.MyAppsScreen
import app.tab.store.feature.seller.SellerProductEditScreen
import app.tab.store.feature.seller.SellerProductsScreen
import app.tab.store.feature.seller.SellerSetupScreen
import app.tab.store.feature.seller.SellerStoreEditScreen
import app.tab.store.feature.store.StoreScreen
import app.tab.store.feature.wishlist.WishlistScreen

// Destinations cross-fade and drift a little, so a tab change reads as movement
// rather than a swap. The drift is small on purpose: a big slide on every tap
// feels slow by the third one.
private val enter: androidx.compose.animation.EnterTransition =
    fadeIn(tween(180)) +
        slideInHorizontally(spring(dampingRatio = 0.92f, stiffness = 900f)) { it / 18 } +
        scaleIn(tween(180), initialScale = 0.985f)

private val exit: androidx.compose.animation.ExitTransition =
    fadeOut(tween(120)) +
        slideOutHorizontally(tween(140)) { -it / 24 } +
        scaleOut(tween(140), targetScale = 0.992f)

@Composable
fun SproutlyNavHost(
    isSignedIn: Boolean,
    role: UserRole?,
) {
    // The whole graph is rebuilt when the session changes, so signing in or out
    // always lands on the correct root with a clean back stack.
    // Keyed on the session only. Keying on role as well rebuilt the whole graph
    // the moment a shopper became a developer, which raced the navigation that
    // the onboarding screen performs and dropped them on the wrong screen.
    key(isSignedIn) {
        SproutlyGraph(isSignedIn = isSignedIn, role = role)
    }
}

@Composable
private fun SproutlyGraph(isSignedIn: Boolean, role: UserRole?) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Held at graph scope so "Add another account" can sign out from anywhere.
    val firstRun: app.tab.store.core.account.FirstRunStore =
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            androidx.compose.ui.platform.LocalContext.current.applicationContext,
            app.tab.store.core.account.FirstRunEntryPoint::class.java,
        ).firstRun()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    var showSwitcher by remember { mutableStateOf(false) }
    val switcherViewModel: AccountSwitcherViewModel = hiltViewModel()
    val switcherState by switcherViewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val launchGoogle = rememberGoogleSignIn(
        onToken = { token -> switcherViewModel.switchWithGoogle(token) },
        onError = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        },
    )

    // Background stream failures surface here with the actionable message.
    LaunchedEffect(Unit) {
        AppErrors.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // Search lives on an optional-argument route ("search?categoryId=…"), so
    // compare against the base route rather than the raw route pattern.
    val baseRoute = currentRoute?.substringBefore("?")
    val showBottomBar = isSignedIn && mainTabs.any { it.route == baseRoute }

    fun go(route: String) = navController.navigate(route) {
        launchSingleTop = true
    }

    val tabRoutes = (primaryTabs + utilityTabs).map { it.route }

    fun goTab(route: String) = navController.navigate(route) {
        firstRun.rememberTab(route)
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    // Back pops to the screen you came from. Without this, some routes fell through
    // to the activity and landed on the storefront, which felt like being thrown
    // out of the app.
    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.popBackStack()
    }

    Scaffold(
        containerColor = Tokens.colorScheme.background,
        // Insets are handled explicitly below (content clears the status bar, the
        // floating bar clears the gesture area). Letting Scaffold add its own
        // padding on top doubled it and still let content slide under the clock.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Swipe between the browsing tabs: drag from either edge and the
                // neighbouring tab comes in from that side. Edge-only, so it never
                // fights a horizontally scrolling row or the screenshot pager.
                .pointerInput(currentRoute) {
                    val edge = 56.dp.toPx()
                    val threshold = 64.dp.toPx()
                    var total = 0f
                    var active = false
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            active = offset.x < edge || offset.x > size.width - edge
                            total = 0f
                        },
                        onDragEnd = {
                            if (active && abs(total) > threshold) {
                                val order = listOf(Routes.HOME, Routes.GAMES, Routes.OFFERS)
                                val index = order.indexOf(currentRoute)
                                if (index >= 0) {
                                    val next = if (total < 0) index + 1 else index - 1
                                    order.getOrNull(next)?.let { goTab(it) }
                                }
                            }
                            active = false
                            total = 0f
                        },
                        onDragCancel = { active = false; total = 0f },
                        onHorizontalDrag = { change, amount ->
                            if (active) {
                                total += amount
                                change.consume()
                            }
                        },
                    )
                },
        ) {
        NavHost(
            navController = navController,
            // Resolved once per session, so a later role change does not move
            // the start destination underneath the user.
            startDestination = remember {
                when {
                    !isSignedIn -> Routes.LOGIN
                    // Admins land in the store like everyone else — admin status
                    // must never cost them the catalogue. The console is one tap
                    // away from Profile or the account switcher.
                    // First run after signing in: the two opening screens.
                    !firstRun.isDone() -> Routes.ONBOARDING
                    // Back where they were, not always the storefront.
                    else -> firstRun.lastTab()?.takeIf { it in tabRoutes } ?: Routes.HOME
                }
            },
            // Edge-to-edge is on, so the content has to clear the status bar
            // itself; without this the wordmark sat under the clock.
            modifier = Modifier.statusBarsPadding(),
            enterTransition = { enter },
            exitTransition = { exit },
            popEnterTransition = { fadeIn(tween(140)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            // ------------------------------------------------------------- auth
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLegal = { terms -> go(Routes.legal(terms)) },
                    onRegister = { go(Routes.REGISTER) },
                    onForgotPassword = { go(Routes.FORGOT_PASSWORD) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }

            // ----------------------------------------------------------- customer
            composable(Routes.HOME) {
                HomeScreen(
                    onProduct = { go(Routes.product(it)) },
                    onStore = { go(Routes.store(it)) },
                    onSearch = { go(Routes.search()) },
                    // Spoken words become the search query.
                    onSearchQuery = { words -> go(Routes.search(words)) },
                    onCategory = { go(Routes.searchInCategory(it)) },
                    onSeeAll = { goTab(Routes.SEARCH) },
                    onDownloads = { goTab(Routes.LIBRARY) },
                    onProfile = { goTab(Routes.PROFILE) },
                    onNotifications = { go(Routes.NOTIFICATIONS) },
                    onBecomeDeveloper = { go(Routes.SELLER_SETUP) },
                    onOpenSwitcher = { showSwitcher = true },
                    onRankings = { go(Routes.RANKINGS) },
                    onOffers = { go(Routes.OFFERS) },
                )
            }
            composable(
                route = "search?categoryId={categoryId}&query={query}",
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("query") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                SearchScreen(
                    initialCategoryId = entry.arguments?.getString("categoryId"),
                    initialQuery = entry.arguments?.getString("query").orEmpty(),
                    onProduct = { go(Routes.product(it)) },
                    onStore = { go(Routes.store(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.PRODUCT,
                arguments = listOf(navArgument("productId") { type = NavType.StringType }),
                // A shared link resolves here, so the app opens on the app.
                deepLinks = listOf(navDeepLink { uriPattern = Routes.productLink }),
            ) { entry ->
                ProductDetailScreen(
                    productId = entry.arguments?.getString("productId").orEmpty(),
                    isAdmin = role == UserRole.ADMIN,
                    onBack = { navController.popBackStack() },
                    onStore = { go(Routes.store(it)) },
                    onDownloads = { goTab(Routes.LIBRARY) },
                    onProduct = { go(Routes.product(it)) },
                    onSearchTag = { tag -> go(Routes.search(tag)) },
                )
            }
            composable(
                route = Routes.STORE,
                arguments = listOf(navArgument("storeId") { type = NavType.StringType }),
            ) { entry ->
                StoreScreen(
                    storeId = entry.arguments?.getString("storeId").orEmpty(),
                    isAdmin = role == UserRole.ADMIN,
                    onBack = { navController.popBackStack() },
                    onProduct = { go(Routes.product(it)) },
                )
            }

            composable(Routes.GAMES) {
                GamesScreen(
                    onProduct = { go(Routes.product(it)) },
                    onSearch = { go(Routes.search()) },
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onDone = {
                        firstRun.markDone()
                        goTab(Routes.HOME)
                    },
                )
            }
            composable(
                route = Routes.LEGAL,
                arguments = listOf(
                    navArgument("terms") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                LegalScreen(
                    startOnTerms = entry.arguments?.getBoolean("terms") == true,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.RANKINGS) {
                RankingsScreen(onProduct = { go(Routes.product(it)) })
            }
            composable(Routes.OFFERS) {
                OffersScreen(onProduct = { go(Routes.product(it)) })
            }
            composable(Routes.WISHLIST) {
                WishlistScreen(
                    onBack = { navController.popBackStack() },
                    onProduct = { go(Routes.product(it)) },
                )
            }

            composable(Routes.LIBRARY) {
                MyAppsScreen(
                    onProduct = { go(Routes.product(it)) },
                    onBrowse = { goTab(Routes.HOME) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onEditProfile = { go(Routes.PROFILE_EDIT) },
                    onWishlist = { go(Routes.WISHLIST) },
                    onOrders = { goTab(Routes.LIBRARY) },
                    onNotifications = { go(Routes.NOTIFICATIONS) },
                    onSellerHub = { go(Routes.SELLER_DASHBOARD) },
                    onBecomeDeveloper = { go(Routes.SELLER_SETUP) },
                    onOpenSwitcher = { showSwitcher = true },
                    onLegal = { terms -> go(Routes.legal(terms)) },
                    onAdmin = { go(Routes.ADMIN_DASHBOARD) },
                    onSettings = { go(Routes.SETTINGS) },
                )
            }
            composable(Routes.PROFILE_EDIT) {
                ProfileEditScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpen = { link ->
                        when {
                            // Orders no longer exist as a concept; an old link to one
                            // lands on the library instead of nowhere.
                            link.startsWith("order/") -> goTab(Routes.LIBRARY)
                            link.startsWith("product/") -> go(Routes.product(link.removePrefix("product/")))
                            link.startsWith("store/") -> go(Routes.store(link.removePrefix("store/")))
                        }
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLegal = { terms -> go(Routes.legal(terms)) },
                )
            }

            // ------------------------------------------------------------- seller
            composable(Routes.SELLER_SETUP) {
                SellerSetupScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = {
                        // They came here to publish, so land on the publish form.
                        navController.navigate(Routes.sellerProductEdit(null)) {
                            popUpTo(Routes.SELLER_SETUP) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.SELLER_DASHBOARD) {
                SellerDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onProducts = { go(Routes.SELLER_PRODUCTS) },
                    onOrders = { goTab(Routes.LIBRARY) },
                    onAnalytics = { go(Routes.SELLER_ANALYTICS) },
                    onStoreEdit = { go(Routes.SELLER_STORE_EDIT) },
                    onVerification = { go(Routes.SELLER_VERIFICATION) },
                    onSetupStore = { go(Routes.SELLER_SETUP) },
                )
            }
            composable(Routes.SELLER_PRODUCTS) {
                SellerProductsScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { go(Routes.sellerProductEdit(it)) },
                )
            }
            composable(
                route = Routes.SELLER_PRODUCT_EDIT,
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                SellerProductEditScreen(
                    productId = entry.arguments?.getString("productId")?.takeIf { it.isNotBlank() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SELLER_ANALYTICS) {
                SellerAnalyticsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SELLER_VERIFICATION) {
                SellerVerificationScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SELLER_STORE_EDIT) {
                SellerStoreEditScreen(onBack = { navController.popBackStack() })
            }

            // -------------------------------------------------------------- admin
            composable(Routes.ADMIN_DASHBOARD) {
                AdminDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onStores = { go(Routes.ADMIN_STORES) },
                    onUsers = { go(Routes.ADMIN_USERS) },
                    onProducts = { go(Routes.ADMIN_PRODUCTS) },
                    onCategories = { go(Routes.ADMIN_CATEGORIES) },
                    onTags = { go(Routes.ADMIN_TAGS) },
                )
            }
            composable(Routes.ADMIN_STORES) {
                AdminStoresScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADMIN_USERS) {
                AdminUsersScreen(
                    onBack = { navController.popBackStack() },
                    onOpenStore = { go(Routes.store(it)) },
                )
            }
            composable(Routes.ADMIN_PRODUCTS) {
                AdminProductsScreen(
                    onBack = { navController.popBackStack() },
                    // An admin can manage any listing from here, not only the
                    // ones that were reported.
                    onOpenProduct = { go(Routes.product(it)) },
                )
            }
            composable(Routes.ADMIN_CATEGORIES) {
                AdminCategoriesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADMIN_TAGS) {
                AdminTagsScreen(onBack = { navController.popBackStack() })
            }
        }

            if (showBottomBar) {
                SproutlyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        // Studio is the developer's home; someone who has not
                        // opened a developer profile goes to the setup instead
                        // of an empty dashboard.
                        if (route == Routes.SELLER_DASHBOARD && profileState.user?.isSeller != true) {
                            go(Routes.SELLER_SETUP)
                        } else {
                            goTab(route)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // One account switcher for the whole app; the store's avatar and the
        // profile header both open this one.
        if (showSwitcher) {
            AccountSwitcherSheet(
                accounts = switcherState.accounts,
                currentUid = switcherState.currentUid,
                currentWorkspace = Workspace.SHOPPER,
                isSeller = profileState.user?.isSeller == true,
                isAdmin = profileState.user?.isAdmin == true,
                onDismiss = { showSwitcher = false },
                onSelectAccount = { uid ->
                    showSwitcher = false
                    val account = switcherState.accounts.firstOrNull { it.uid == uid }
                    // A Google account can be switched with the provider's own
                    // picker — no password, and Firebase swaps the session
                    // outright. Anything else has to go through sign-in.
                    if (account?.provider?.contains("google", ignoreCase = true) == true) {
                        launchGoogle()
                    } else {
                        switcherViewModel.selectAccount(uid)
                    }
                },
                onForgetAccount = switcherViewModel::forgetAccount,
                onSwitchWorkspace = { workspace ->
                    showSwitcher = false
                    when (workspace) {
                        Workspace.SHOPPER -> goTab(Routes.HOME)
                        Workspace.DEVELOPER -> go(Routes.SELLER_DASHBOARD)
                        Workspace.ADMIN -> go(Routes.ADMIN_DASHBOARD)
                    }
                },
                onAddAccount = {
                    showSwitcher = false
                    switcherViewModel.addAccount()
                },
                onSignOut = {
                    showSwitcher = false
                    profileViewModel.signOut()
                },
            )
        }
    }
}
