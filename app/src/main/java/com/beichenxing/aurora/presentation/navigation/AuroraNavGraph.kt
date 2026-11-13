package com.beichenxing.aurora.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.beichenxing.aurora.presentation.character.CharacterListScreen
import com.beichenxing.aurora.presentation.chat.ChatScreen
import com.beichenxing.aurora.presentation.settings.SettingsScreen

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(
        route = NavRoutes.CharacterList.route,
        label = "角色",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    ),
    BottomNavItem(
        route = NavRoutes.Settings.route,
        label = "设置",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

// Routes where the bottom nav should be hidden
private val routesWithoutBottomNav = setOf(
    NavRoutes.Chat.route.substringBefore("{") // "chat/"
)

@Composable
fun AuroraNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: ""

    // Hide bottom nav on chat screen
    val showBottomNav = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon
                                                  else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.CharacterList.route
            // No padding here - each screen handles its own insets
        ) {
            composable(NavRoutes.CharacterList.route) {
                CharacterListScreen(
                    modifier = Modifier.padding(innerPadding),
                    onCharacterClick = { characterId ->
                        navController.navigate(NavRoutes.Chat.route(characterId))
                    }
                )
            }

            composable(NavRoutes.Settings.route) {
                SettingsScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }

            composable(
                route = NavRoutes.Chat.route,
                arguments = listOf(
                    navArgument(NavRoutes.Chat.ARG_CHARACTER_ID) {
                        type = NavType.StringType
                    }
                )
            ) {
                // Chat screen manages its own full-screen layout
                ChatScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}