package com.beichenxing.aurora.presentation.navigation

sealed class NavRoutes(val route: String) {
    // Top-level destinations
    data object CharacterList : NavRoutes("character_list")
    data object Settings : NavRoutes("settings")

    // Chat
    data class Chat(val chatId: String = "{chatId}") : NavRoutes("chat/{chatId}") {
        companion object {
            fun route(chatId: String) = "chat/$chatId"
        }
    }

    // Character
    data class CharacterDetail(val characterId: String = "{characterId}") :
        NavRoutes("character/{characterId}") {
        companion object {
            fun route(characterId: String) = "character/$characterId"
        }
    }
}