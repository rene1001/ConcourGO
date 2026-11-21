package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.authStateFlow
import com.example.data.local.UserPreferences
import com.example.notifications.NotificationHelper
import com.example.ui.AppContainer
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.PathScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.ConcoursGoTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.QuizViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private const val ROUTE_PATH = "path"
private const val ROUTE_LEADERBOARD = "leaderboard"
private const val ROUTE_SHOP = "shop"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_QUIZ = "quiz/{levelId}"

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_DESTINATIONS = listOf(
    TopDestination(
        route = ROUTE_PATH,
        labelRes = R.string.nav_path,
        selectedIcon = Icons.Rounded.Route,
        unselectedIcon = Icons.Outlined.Route,
        testTag = "nav_tab_path"
    ),
    TopDestination(
        route = ROUTE_LEADERBOARD,
        labelRes = R.string.nav_leaderboard,
        selectedIcon = Icons.Rounded.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        testTag = "nav_tab_leaderboard"
    ),
    TopDestination(
        route = ROUTE_SHOP,
        labelRes = R.string.nav_shop,
        selectedIcon = Icons.Rounded.ShoppingBag,
        unselectedIcon = Icons.Outlined.ShoppingBag,
        testTag = "nav_tab_shop"
    ),
    TopDestination(
        route = ROUTE_PROFILE,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_tab_profile"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        val container = AppContainer.getInstance(this)
        setContent {
            ConcoursGoTheme {
                ConcoursGoApp(container = container)
            }
        }
    }
}

@Composable
fun ConcoursGoApp(container: AppContainer) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val prefs by container.preferencesRepository.preferencesFlow
        .collectAsStateWithLifecycle(initialValue = UserPreferences())
    val scope = rememberCoroutineScope()

    val user = currentUser
    if (user == null && !prefs.offlineGuestEntered) {
        AuthScreen(
            onAuthSuccess = {
                scope.launch { container.preferencesRepository.setOfflineGuestEntered(false) }
            },
            onContinueOfflineGuest = {
                scope.launch { container.preferencesRepository.setOfflineGuestEntered(true) }
            }
        )
    } else {
        val sessionKey = user?.uid ?: "offline_guest"
        key(sessionKey) {
            MainFeatureShell(
                container = container,
                currentUserId = user?.uid,
                currentUserDisplayName = user?.displayName
            )
        }
    }
}

@Composable
private fun MainFeatureShell(
    container: AppContainer,
    currentUserId: String?,
    currentUserDisplayName: String?
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isInQuiz = currentDestination?.route == ROUTE_QUIZ

    val sessionKey = currentUserId ?: "offline_guest"
    val mainViewModel: MainViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                MainViewModel(
                    repository = container.concoursRepository,
                    currentUserId = currentUserId,
                    currentUserDisplayName = currentUserDisplayName,
                    paymentRepository = container.bictorysPaymentRepository
                )
            }
        }
    )

    val dashboardState by mainViewModel.dashboardState.collectAsStateWithLifecycle()
    val leaderboardState by mainViewModel.leaderboardState.collectAsStateWithLifecycle()
    val shopState by mainViewModel.shopState.collectAsStateWithLifecycle()
    val statusMessage by mainViewModel.statusMessage.collectAsStateWithLifecycle()
    val isSyncing by mainViewModel.isSyncing.collectAsStateWithLifecycle()
    val isRefreshingLeaderboard by mainViewModel.isRefreshingLeaderboard.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mainViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mainViewModel.refreshStreakOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isInQuiz && !useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        TOP_DESTINATIONS.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isInQuiz && useNavRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TOP_DESTINATIONS.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_PATH,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_PATH) {
                        PathScreen(
                            state = dashboardState,
                            onStartLevel = { levelId ->
                                navController.navigate("quiz/$levelId")
                            },
                            onToggleOffline = mainViewModel::toggleOfflineMode,
                            onRefillHeartsGems = mainViewModel::refillHeartsWithGems,
                            onRefillHeartsFree = mainViewModel::refillHeartsFree
                        )
                    }

                    composable(ROUTE_LEADERBOARD) {
                        LeaderboardScreen(
                            leaderboardState = leaderboardState,
                            isRefreshing = isRefreshingLeaderboard,
                            onRefresh = mainViewModel::refreshLeaderboard,
                            onRequestSignIn = mainViewModel::exitGuestMode
                        )
                    }

                    composable(ROUTE_SHOP) {
                        ShopScreen(
                            dashboardState = dashboardState,
                            shopState = shopState,
                            onSelectPack = mainViewModel::selectShopPack,
                            onSelectMethod = mainViewModel::selectPaymentMethod,
                            onUpdateName = mainViewModel::updateShopCustomerName,
                            onUpdatePhone = mainViewModel::updateShopCustomerPhone,
                            onUpdateEmail = mainViewModel::updateShopCustomerEmail,
                            onSelectCountry = mainViewModel::selectShopCountry,
                            onPayWithBictorys = mainViewModel::startBictorysCheckout,
                            onClearCheckoutUrl = mainViewModel::clearCheckoutUrlToOpen,
                            onVerifyPending = { mainViewModel.verifyPendingPayment(silent = false) },
                            onDismissPending = mainViewModel::dismissPendingPayment,
                            onClearMessages = mainViewModel::clearShopMessages
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            state = dashboardState,
                            isSyncing = isSyncing,
                            statusMessage = statusMessage,
                            onClearStatusMessage = mainViewModel::clearStatusMessage,
                            onSelectAvatar = mainViewModel::selectAvatar,
                            onUpdateDailyGoal = mainViewModel::updateDailyGoal,
                            onToggleOfflineMode = mainViewModel::toggleOfflineMode,
                            onSyncNow = mainViewModel::syncNow,
                            onToggleNotifications = { enabled ->
                                mainViewModel.setNotificationsEnabled(enabled, context)
                            },
                            onUpdateReminderTime = { h, m ->
                                mainViewModel.updateReminderTime(h, m, context)
                            },
                            onTestNotification = {
                                mainViewModel.sendTestNotification(context)
                            },
                            onConnectGoogle = mainViewModel::exitGuestMode,
                            onSignOutComplete = mainViewModel::exitGuestMode
                        )
                    }

                    composable(
                        route = ROUTE_QUIZ,
                        arguments = listOf(navArgument("levelId") { type = NavType.StringType })
                    ) { entry ->
                        val levelId = entry.arguments?.getString("levelId") ?: "u1_l1"
                        val quizViewModel: QuizViewModel = viewModel(
                            key = "${sessionKey}_$levelId",
                            factory = viewModelFactory {
                                initializer {
                                    QuizViewModel(
                                        repository = container.concoursRepository,
                                        levelId = levelId,
                                        currentUserId = currentUserId
                                    )
                                }
                            }
                        )
                        QuizScreen(
                            viewModel = quizViewModel,
                            onExitToPath = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
