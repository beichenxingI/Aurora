package com.beichenxing.aurora.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.beichenxing.aurora.presentation.common.component.AuroraBackground
import com.beichenxing.aurora.presentation.common.component.AuroraNavBar
import com.beichenxing.aurora.presentation.common.component.AuroraNavItem
import com.beichenxing.aurora.presentation.settings.SettingsScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

private val navItems = listOf(
    AuroraNavItem(
        route          = NavRoutes.CharacterList.route,
        label          = "角色",
        selectedIcon   = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline
    ),
    AuroraNavItem(
        route          = NavRoutes.Chats.route,
        label          = "回忆",
        selectedIcon   = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    ),
    AuroraNavItem(
        route          = NavRoutes.Settings.route,
        label          = "设置",
        selectedIcon   = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

private val routesWithoutNav = setOf(NavRoutes.Chat.route)

@Composable
fun AuroraNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showNav = navItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    // HazeState shared between source (NavHost layer) and child (NavBar)
    val hazeState = remember { HazeState() }

    AuroraBackground {
        // ── Outer Box: full screen overlay stack ─────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {

            // ── LAYER 1: Page content (Haze SOURCE) ───────────────────────────
            // NavBar is NOT inside this Box — this is critical.
            // Haze 1.0.2 forbids hazeChild being a descendant of haze source.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState)         // source only contains page content
            ) {
                NavHost(
                    navController    = navController,
                    startDestination = NavRoutes.CharacterList.route,
                    modifier         = Modifier.fillMaxSize()
                ) {
                    composable(NavRoutes.CharacterList.route) {
                        CharacterListScreen(
                            onCharacterClick = { characterId ->
                                navController.navigate(
                                    NavRoutes.Chat.route(characterId, "new")
                                )
                            }
                        )
                    }

                    composable(NavRoutes.Chats.route) {
                        ChatsScreen(
                            onChatClick = { chatId, characterId ->
                                navController.navigate(
                                    NavRoutes.Chat.route(characterId, chatId)
                                )
                            }
                        )
                    }

                    composable(NavRoutes.Settings.route) {
                        SettingsScreen()
                    }

                    composable(
                        route     = NavRoutes.Chat.route,
                        arguments = listOf(
                            navArgument(NavRoutes.Chat.ARG_CHARACTER_ID) {
                                type = NavType.StringType
                            },
                            navArgument(NavRoutes.Chat.ARG_CHAT_ID) {
                                type         = NavType.StringType
                                defaultValue = "new"
                            }
                        )
                    ) {
                        ChatScreen(onNavigateBack = { navController.popBackStack() })
                    }
                }
            }

            // ── LAYER 2: NavBar overlay (Haze CHILD — sibling of source Box) ──
            // Lives outside the .haze(hazeState) Box: no ancestor/descendant conflict.
            if (showNav) {
                AuroraNavBar(
                    items        = navItems,
                    currentRoute = currentRoute,
                    hazeState    = hazeState,
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