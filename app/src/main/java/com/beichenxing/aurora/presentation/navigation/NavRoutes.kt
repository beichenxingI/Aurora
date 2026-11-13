package com.beichenxing.aurora.presentation.navigation

sealed class NavRoutes(val route: String) {
    // ─── Top-level destinations (bottom nav) ─────────────────────────────────
    data object CharacterList : NavRoutes("character_list")
    data object Settings : NavRoutes("settings")

    // ─── Chat ─────────────────────────────────────────────────────────────────
    // Route template: "chat/{characterId}"
    data object Chat : NavRoutes("chat/{characterId}") {
        const val ARG_CHARACTER_ID = "characterId"
        fun route(characterId: String) = "chat/$characterId"
    }
}