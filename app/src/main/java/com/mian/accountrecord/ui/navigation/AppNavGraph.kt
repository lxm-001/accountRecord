package com.mian.accountrecord.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mian.accountrecord.ui.auth.LoginScreen
import com.mian.accountrecord.ui.auth.WelcomeScreen
import com.mian.accountrecord.ui.budget.BudgetScreen
import com.mian.accountrecord.ui.category.CategoryManageScreen
import com.mian.accountrecord.ui.detail.TransactionDetailScreen
import com.mian.accountrecord.ui.home.HomeScreen
import com.mian.accountrecord.ui.billimport.BillImportScreen
import com.mian.accountrecord.ui.ledger.LedgerManageScreen
import com.mian.accountrecord.ui.profile.ProfileScreen
import com.mian.accountrecord.ui.report.ReportScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    quickEntryVisible: Boolean = false,
    onQuickEntryVisibilityChange: (Boolean) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {
        // Login route
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { isFirstLogin, nickname, avatarUrl ->
                    if (isFirstLogin) {
                        navController.navigate(Screen.Welcome.createRoute(nickname, avatarUrl)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToAgreement = {
                    navController.navigate(Screen.Agreement.route)
                },
                onNavigateToPrivacy = {
                    navController.navigate(Screen.Privacy.route)
                }
            )
        }

        // Welcome route
        composable(
            route = Screen.Welcome.route,
            arguments = listOf(
                navArgument("nickname") { type = NavType.StringType },
                navArgument("avatarUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val nickname = backStackEntry.arguments?.getString("nickname") ?: ""
            val avatarUrl = backStackEntry.arguments?.getString("avatarUrl")?.let {
                if (it == "none") null else it
            }
            WelcomeScreen(
                nickname = nickname,
                avatarUrl = avatarUrl,
                onTimeout = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToReport = {
                    navController.navigate(Screen.Report.route)
                },
                onNavigateToBudget = {
                    navController.navigate(Screen.Budget.route)
                },
                onNavigateToLedgerManage = {
                    navController.navigate(Screen.LedgerManage.route)
                },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                },
                externalQuickEntryVisible = quickEntryVisible,
                onQuickEntryVisibilityChange = onQuickEntryVisibilityChange
            )
        }

        composable(Screen.Report.route) {
            ReportScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToCategoryManage = {
                    navController.navigate(Screen.CategoryManage.route)
                },
                onNavigateToLedgerManage = {
                    navController.navigate(Screen.LedgerManage.route)
                },
                onNavigateToBillImport = {
                    navController.navigate(Screen.BillImport.route)
                },
                onNavigateToUserSettings = {
                    navController.navigate(Screen.UserSettings.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.UserSettings.route) {
            com.mian.accountrecord.ui.profile.UserSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CategoryManage.route) {
            CategoryManageScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LedgerManage.route) {
            LedgerManageScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType }
            )
        ) {
            TransactionDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BillImport.route) {
            BillImportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Agreement.route) {
            com.mian.accountrecord.ui.auth.WebViewScreen(
                title = "用户协议",
                assetFileName = "agreement.html",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Privacy.route) {
            com.mian.accountrecord.ui.auth.WebViewScreen(
                title = "隐私政策",
                assetFileName = "privacy.html",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
