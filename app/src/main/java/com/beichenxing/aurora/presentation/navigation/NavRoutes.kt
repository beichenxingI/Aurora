package com.beichenxing.aurora.presentation.navigation

sealed class NavRoutes(val route: String) {
    data object CharacterList : NavRoutes("character_list")
    data object Chats : NavRoutes("chats")
    data object Settings : NavRoutes("settings")

    data object Chat : NavRoutes("chat/{characterId}/{chatId}") {
        const val ARG_CHARACTER_ID = "characterId"
        const val ARG_CHAT_ID = "chatId"
        fun route(characterId: String, chatId: String = "new") =
            "chat/$characterId/$chatId"
    }
}