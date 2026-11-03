package app.tab.store.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import app.tab.store.core.utils.AppErrors
import app.tab.store.domain.model.UserRole
import app.tab.store.feature.profile.AccountSwitcherSheet
import app.tab.store.feature.profile.ProfileViewModel
import app.tab.store.feature.profile.Workspace
import app.tab.store.feature.admin.AdminCategoriesScreen
import app.tab.store.feature.admin.AdminDashboardScreen
import app.tab.store.feature.admin.AdminProductsScreen
import app.tab.store.feature.admin.AdminStoresScreen
import app.tab.store.feature.admin.AdminUsersScreen
import app.tab.store.feature.auth.ForgotPasswordScreen
import app.tab.store.feature.auth.LoginScreen
import app.tab.store.feature.auth.RegisterScreen
import app.tab.store.feature.home.HomeScreen
import app.tab.store.feature.notifications.NotificationsScreen
import app.tab.store.feature.orders.OrderDetailScreen
import app.tab.store.feature.orders.OrderPlacedScreen
import app.tab.store.feature.orders.OrdersScreen
import app.tab.store.feature.product.ProductDetailScreen
import app.tab.store.feature.profile.ProfileEditScreen
import app.tab.store.feature.profile.ProfileScreen
import app.tab.store.feature.profile.SettingsScreen
import app.tab.store.feature.search.SearchScreen
import app.tab.store.feature.seller.SellerAnalyticsScreen
import app.tab.store.feature.seller.SellerDashboardScreen
import app.tab.store.feature.seller.SellerOrdersScreen
import app.tab.store.feature.seller.SellerProductEditScreen
import app.tab.store.feature.seller.SellerProductsScreen
import app.tab.store.feature.seller.SellerSetupScreen
import app.tab.store.feature.seller.SellerStoreEditScreen
import app.tab.store.feature.store.StoreScreen
import app.tab.store.feature.wishlist.WishlistScreen

private val enter: androidx.compose.animation.EnterTransition =
    fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 }

private val exit: androidx.compose.animation.ExitTransition =
    fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { -it / 12 }

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
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    var showSwitcher by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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

    fun goTab(route: String) = navController.navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                SproutlyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = ::goTab,
                )
            }
        },
    ) { innerPadding ->
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
                    else -> Routes.HOME
                }
            },
            modifier = Modifier.padding(innerPadding),
            enterTransition = { enter },
            exitTransition = { exit },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            // ------------------------------------------------------------- auth
            composable(Routes.LOGIN) {
                LoginScreen(
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
                    onSearch = { goTab(Routes.SEARCH) },
                    onCategory = { go(Routes.searchInCategory(it)) },
                    onSeeAll = { goTab(Routes.SEARCH) },
                    onDownloads = { goTab(Routes.ORDERS) },
                    onProfile = { goTab(Routes.PROFILE) },
                    onNotifications = { go(Routes.NOTIFICATIONS) },
                    onBecomeDeveloper = { go(Routes.SELLER_SETUP) },
                    onOpenSwitcher = { showSwitcher = true },
                )
            }
            composable(
                route = "search?categoryId={categoryId}",
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                SearchScreen(
                    initialCategoryId = entry.arguments?.getString("categoryId"),
                    onProduct = { go(Routes.product(it)) },
                )
            }
            composable(
                route = Routes.PRODUCT,
                arguments = listOf(navArgument("productId") { type = NavType.StringType }),
            ) { entry ->
                ProductDetailScreen(
                    productId = entry.arguments?.getString("productId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onStore = { go(Routes.store(it)) },
                    onDownloads = { goTab(Routes.ORDERS) },
                )
            }
            composable(
                route = Routes.STORE,
                arguments = listOf(navArgument("storeId") { type = NavType.StringType }),
            ) { entry ->
                StoreScreen(
                    storeId = entry.arguments?.getString("storeId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onProduct = { go(Routes.product(it)) },
                )
            }

            composable(Routes.WISHLIST) {
                WishlistScreen(
                    onBack = { navController.popBackStack() },
                    onProduct = { go(Routes.product(it)) },
                )
            }

            composable(Routes.ORDERS) {
                OrdersScreen(
                    onOrder = { go(Routes.orderDetail(it)) },
                    onStartShopping = { goTab(Routes.HOME) },
                )
            }
            composable(
                route = Routes.ORDER_DETAIL,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            ) { entry ->
                OrderDetailScreen(
                    orderId = entry.arguments?.getString("orderId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.ORDER_PLACED,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            ) { entry ->
                OrderPlacedScreen(
                    orderId = entry.arguments?.getString("orderId").orEmpty(),
                    onTrackOrder = { orderId ->
                        navController.navigate(Routes.orderDetail(orderId)) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onKeepShopping = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onEditProfile = { go(Routes.PROFILE_EDIT) },
                    onWishlist = { go(Routes.WISHLIST) },
                    onOrders = { goTab(Routes.ORDERS) },
                    onNotifications = { go(Routes.NOTIFICATIONS) },
                    onSellerHub = { go(Routes.SELLER_DASHBOARD) },
                    onBecomeDeveloper = { go(Routes.SELLER_SETUP) },
                    onSwitchWorkspace = { workspace ->
                        when (workspace) {
                            Workspace.SHOPPER -> goTab(Routes.HOME)
                            Workspace.DEVELOPER -> go(Routes.SELLER_DASHBOARD)
                            Workspace.ADMIN -> go(Routes.ADMIN_DASHBOARD)
                        }
                    },
                    onAddAccount = { profileViewModel.signOut() },
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
                            link.startsWith("order/") -> go(Routes.orderDetail(link.removePrefix("order/")))
                            link.startsWith("product/") -> go(Routes.product(link.removePrefix("product/")))
                            link.startsWith("store/") -> go(Routes.store(link.removePrefix("store/")))
                        }
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
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
                    onOrders = { go(Routes.SELLER_ORDERS) },
                    onAnalytics = { go(Routes.SELLER_ANALYTICS) },
                    onStoreEdit = { go(Routes.SELLER_STORE_EDIT) },
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
            composable(Routes.SELLER_ORDERS) {
                SellerOrdersScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SELLER_ANALYTICS) {
                SellerAnalyticsScreen(onBack = { navController.popBackStack() })
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
                )
            }
            composable(Routes.ADMIN_STORES) {
                AdminStoresScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADMIN_USERS) {
                AdminUsersScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADMIN_PRODUCTS) {
                AdminProductsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADMIN_CATEGORIES) {
                AdminCategoriesScreen(onBack = { navController.popBackStack() })
            }
        }

        // One account switcher for the whole app; the store's avatar and the
        // profile header both open this one.
        if (showSwitcher) {
            AccountSwitcherSheet(
                user = profileState.user,
                current = Workspace.SHOPPER,
                onDismiss = { showSwitcher = false },
                onSwitch = { workspace ->
                    showSwitcher = false
                    when (workspace) {
                        Workspace.SHOPPER -> goTab(Routes.HOME)
                        Workspace.DEVELOPER -> go(Routes.SELLER_DASHBOARD)
                        Workspace.ADMIN -> go(Routes.ADMIN_DASHBOARD)
                    }
                },
                onAddAccount = {
                    showSwitcher = false
                    profileViewModel.signOut()
                },
                onSignOut = {
                    showSwitcher = false
                    profileViewModel.signOut()
                },
            )
        }
    }
}
