package com.roldanmatias.rickverse.navigation

import kotlinx.serialization.Serializable

/** Type-safe routes: no string paths, arguments are checked at compile time. */
internal sealed interface Route {
    // One graph per tab, each with its own back stack.
    @Serializable
    data object CharactersGraph : Route

    @Serializable
    data object EpisodesGraph : Route

    @Serializable
    data object SettingsGraph : Route

    @Serializable
    data object CastList : Route

    @Serializable
    data class CastDetail(val id: Int) : Route

    @Serializable
    data object EpisodeList : Route

    @Serializable
    data class EpisodeDetail(val id: Int) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object NetworkLog : Route
}
