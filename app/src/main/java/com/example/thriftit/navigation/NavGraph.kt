package com.example.thriftit.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.thriftit.ui.screens.auth.AuthScreen
import com.example.thriftit.ui.screens.buy.BuyScreen
import com.example.thriftit.ui.screens.notification.NotificationScreen
import com.example.thriftit.ui.screens.profile.ProfileSetupScreen
import com.example.thriftit.ui.screens.sell.SellScreen
import com.example.thriftit.ui.screens.settings.SettingsScreen

@Composable
fun ThriftItNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = AuthRoute,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<AuthRoute> {
            AuthScreen(
                onNavigateToProfile = {
                    navController.navigate(ProfileSetupRoute) {
                        popUpTo<AuthRoute> { inclusive = true }
                    }
                },
            )
        }

        // Profile Setup Screen
        composable<ProfileSetupRoute> {
            ProfileSetupScreen(
                onNavigateToMain = {
                    navController.navigate(MainRoute) {
                        popUpTo<ProfileSetupRoute> { inclusive = true }
                    }
                },
            )
        }

        // Main Screen with Bottom Navigation
        composable<MainRoute> {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val mainNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = mainNavController)
        },
    ) { paddingValues ->
        NavHost(
            navController = mainNavController,
            startDestination = BuyRoute,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable<BuyRoute> {
                BuyScreen()
            }

            composable<SellRoute> {
                SellScreen()
            }

            composable<SettingsRoute> {
                SettingsScreen()
            }

            composable<NotificationRoute> {
                NotificationScreen()
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val items =
        listOf(
            BottomNavItem(
                title = "Buy",
                icon = Icons.Default.Home,
                route = BuyRoute,
            ),
            BottomNavItem(
                title = "Sell",
                icon = Icons.Default.ShoppingCart,
                route = SellRoute,
            ),
            BottomNavItem(
                title = "Settings",
                icon = Icons.Default.Settings,
                route = SettingsRoute,
            ),
        )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.hasRoute(item.route::class) == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                    )
                },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: Any,
)
