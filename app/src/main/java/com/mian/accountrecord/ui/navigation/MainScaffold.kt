package com.mian.accountrecord.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mian.accountrecord.domain.model.AuthState
import com.mian.accountrecord.domain.usecase.CheckAuthStateUseCase

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

private val bottomNavItems = listOf(
    BottomNavItem("首页", Icons.Filled.Home, Screen.Home),
    BottomNavItem("报表", Icons.Filled.Assessment, Screen.Report),
    BottomNavItem("预算", Icons.Filled.AccountBalanceWallet, Screen.Budget),
    BottomNavItem("我的", Icons.Filled.Person, Screen.Profile)
)

/** Routes where bottom nav bar and FAB should be hidden */
private val authRoutes = setOf(Screen.Login.route, Screen.Welcome.route)

@Composable
fun MainScaffold(checkAuthStateUseCase: CheckAuthStateUseCase) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Shared state so the FAB can trigger QuickEntryPanel inside HomeScreen
    var quickEntryVisible by remember { mutableStateOf(false) }

    val isOnHomeScreen = currentDestination?.hierarchy?.any {
        it.route == Screen.Home.route
    } == true

    // Hide global FAB on pages that have their own FAB
    val isOnBudgetScreen = currentDestination?.hierarchy?.any {
        it.route == Screen.Budget.route
    } == true

    // Determine if current route is an auth route (Login or Welcome)
    val currentRoute = currentDestination?.route
    val isAuthRoute = currentRoute != null && authRoutes.any { authRoute ->
        currentRoute == authRoute || currentRoute.startsWith(authRoute.substringBefore("{"))
    }

    // Determine startDestination based on auth state
    val startDestination = remember {
        when (checkAuthStateUseCase()) {
            AuthState.AUTHENTICATED -> Screen.Home.route
            else -> Screen.Login.route
        }
    }

    Scaffold(
        bottomBar = {
            if (!isAuthRoute) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                val isAlreadySelected = currentDestination?.hierarchy?.any {
                                    it.route == item.screen.route
                                } == true
                                if (!isAlreadySelected) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Hide FAB on auth routes and when QuickEntryPanel is open
            if (isOnHomeScreen && !quickEntryVisible) {
                androidx.compose.material3.LargeFloatingActionButton(
                    onClick = {
                        if (!isOnHomeScreen) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        quickEntryVisible = true
                    },
                    shape = CircleShape,
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "记账",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        com.mian.accountrecord.ui.components.AdaptiveContainer(
            modifier = Modifier.padding(innerPadding)
        ) {
            AppNavGraph(
                navController = navController,
                modifier = Modifier,
                startDestination = startDestination,
                quickEntryVisible = quickEntryVisible,
                onQuickEntryVisibilityChange = { quickEntryVisible = it }
            )
        }
    }
}
