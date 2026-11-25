package com.roldanmatias.rickverse.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.roldanmatias.rickverse.R
import com.roldanmatias.rickverse.feature.characters.ui.CastListRoute
import kotlin.reflect.KClass

private enum class Tab(
    val graph: Route,
    val graphClass: KClass<out Route>,
    val root: Route,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Characters(
        Route.CharactersGraph,
        Route.CharactersGraph::class,
        Route.CastList,
        R.string.nav_characters,
        Icons.Default.Person,
    ),
    Episodes(
        Route.EpisodesGraph,
        Route.EpisodesGraph::class,
        Route.EpisodeList,
        R.string.nav_episodes,
        Icons.Default.PlayArrow,
    ),
    Settings(
        Route.SettingsGraph,
        Route.SettingsGraph::class,
        Route.Settings,
        R.string.nav_settings,
        Icons.Default.Settings,
    ),
}

/**
 * Bottom navigation (a rail on wide screens, via the adaptive suite) with **three independent back stacks**:
 * each tab is a nested graph, and `saveState`/`restoreState` keep every stack alive while another tab is shown.
 */
@Composable
internal fun RootNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            Tab.entries.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.graphClass) } == true
                item(
                    selected = selected,
                    onClick = { navController.onTabClick(tab, selected) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = Route.CharactersGraph) {
            navigation<Route.CharactersGraph>(startDestination = Route.CastList) {
                composable<Route.CastList> {
                    // Navigation to detail arrives with the detail screen (Phase 5).
                    CastListRoute(onCastClick = { })
                }
            }
            navigation<Route.EpisodesGraph>(startDestination = Route.EpisodeList) {
                composable<Route.EpisodeList> { PlaceholderScreen() }
            }
            navigation<Route.SettingsGraph>(startDestination = Route.Settings) {
                composable<Route.Settings> { PlaceholderScreen() }
            }
        }
    }
}

private fun NavHostController.onTabClick(tab: Tab, alreadySelected: Boolean) {
    if (alreadySelected) {
        // Re-selecting the active tab pops back to that tab's root.
        popBackStack(tab.root, inclusive = false)
        return
    }
    navigate(tab.graph) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun PlaceholderScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.placeholder_coming_soon))
    }
}
