package com.beichenxing.aurora.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.beichenxing.aurora.presentation.chats.ChatsScreen
import com.beichenxing.aurora.presentation.common.component.AuroraNavBar
import com.beichenxing.aurora.presentation.common.component.AuroraNavItem
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import com.beichenxing.aurora.presentation.settings.SettingsScreen

private val navItems = listOf(
    AuroraNavItem(
        route         = NavRoutes.CharacterList.route,
        label         = "角色",
        selectedIcon   = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline
    ),
    AuroraNavItem(
        route         = NavRoutes.Chats.route,
        label         = "回忆",
        selectedIcon   = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    ),
    AuroraNavItem(
        route         = NavRoutes.Settings.route,
        label         = "设置",
        selectedIcon   = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

// Routes where the bottom nav should be hidden (chat is full-screen)
private val routesWithoutNav = setOf("chat/{characterId}/{chatId}")

@Composable
fun AuroraNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showNav = navItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    // Use Scaffold with transparent bottom bar container so the
    // floating nav bar overlays content edge-to-edge
    Scaffold(
        containerColor = AuroraColors.Transparent,
        bottomBar = {}  // empty — we draw AuroraNavBar manually as overlay
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Page content ──────────────────────────────────────────────────
            NavHost(
                navController    = navController,
                startDestination = NavRoutes.CharacterList.route,
                modifier         = Modifier.fillMaxSize()
            ) {
                composable(NavRoutes.CharacterList.route) {
                    CharacterListScreen(
                        onCharacterClick = { characterId ->
                            navController.navigate(NavRoutes.Chat.route(characterId, "new"))
                        }
                    )
                }

                composable(NavRoutes.Chats.route) {
                    ChatsScreen(
                        onChatClick = { chatId, characterId ->
                            navController.navigate(NavRoutes.Chat.route(characterId, chatId))
                        }
                    )
                }

                composable(NavRoutes.Settings.route) {
                    SettingsScreen()
                }

                composable(
                    route = NavRoutes.Chat.route,
                    arguments = listOf(
                        navArgument(NavRoutes.Chat.ARG_CHARACTER_ID) {
                            type = NavType.StringType
                        },
                        navArgument(NavRoutes.Chat.ARG_CHAT_ID) {
                            type = NavType.StringType
                            defaultValue = "new"
                        }
                    )
                ) {
                    ChatScreen(onNavigateBack = { navController.popBackStack() })
                }
            }

            // ── Floating nav bar overlay ───────────────────────────────────────
            if (showNav) {
                AuroraNavBar(
                    items        = navItems,
                    currentRoute = currentRoute,
                    onItemClick  = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}