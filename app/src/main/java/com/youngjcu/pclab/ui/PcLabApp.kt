package com.youngjcu.pclab.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.youngjcu.pclab.data.repository.ThemePreference
import com.youngjcu.pclab.ui.screens.BuilderScreen
import com.youngjcu.pclab.ui.screens.HomeScreen
import com.youngjcu.pclab.ui.screens.LoadingScreen
import com.youngjcu.pclab.ui.screens.ResultScreen
import com.youngjcu.pclab.ui.screens.SettingsScreen
import com.youngjcu.pclab.ui.screens.StatisticsScreen
import com.youngjcu.pclab.ui.theme.PcLabTheme

private object Route {
    const val HOME = "home"
    const val BUILDER = "builder"
    const val RESULT = "result"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
}

@Composable
fun PcLabApp(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val density = LocalDensity.current
    val darkTheme = when (state.settings.theme) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalDensity provides Density(density.density, state.settings.fontScale)) {
        PcLabTheme(darkTheme = darkTheme, colourBlindMode = state.settings.colourBlindMode) {
            val navEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navEntry?.destination?.route
            Scaffold(
                bottomBar = {
                    if (currentRoute in setOf(Route.HOME, Route.STATISTICS, Route.SETTINGS)) {
                        AppNavigationBar(currentRoute.orEmpty()) { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            ) { padding ->
                if (state.isLoading || state.errorMessage != null) {
                    LoadingScreen(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        isLoading = state.isLoading,
                        error = state.errorMessage,
                        onRetry = viewModel::loadCatalogue
                    )
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = Route.HOME,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    ) {
                        composable(Route.HOME) {
                            HomeScreen(
                                catalogue = state.catalogue,
                                statistics = state.statistics,
                                onStartMission = { id ->
                                    viewModel.startMission(id)
                                    navController.navigate(Route.BUILDER)
                                },
                                onStatistics = { navController.navigate(Route.STATISTICS) },
                                onSettings = { navController.navigate(Route.SETTINGS) }
                            )
                        }
                        composable(Route.BUILDER) {
                            val builderViewModel: BuilderViewModel = hiltViewModel()
                            val builderState by builderViewModel.state.collectAsStateWithLifecycle()
                            BuilderScreen(
                                mission = state.selectedMission,
                                catalogue = builderState.catalogue ?: state.catalogue,
                                draft = state.draft,
                                onSelectPart = viewModel::selectPart,
                                onSubmit = {
                                    viewModel.submitBuild()
                                    navController.navigate(Route.RESULT)
                                },
                                onBack = { navController.popBackStack() },
                                apiLoading = builderState.isLoading,
                                apiError = builderState.errorMessage,
                                onRetryApi = builderViewModel::refreshCatalogue
                            )
                        }
                        composable(Route.RESULT) {
                            ResultScreen(
                                mission = state.selectedMission,
                                evaluation = state.evaluation,
                                onSaveFavourite = viewModel::saveFavourite,
                                onBackHome = {
                                    navController.navigate(Route.HOME) {
                                        popUpTo(Route.HOME) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Route.STATISTICS) { StatisticsScreen(state.statistics, state.catalogue?.missions.orEmpty()) }
                        composable(Route.SETTINGS) {
                            SettingsScreen(
                                settings = state.settings,
                                onThemeChange = viewModel::updateTheme,
                                onFontScaleChange = viewModel::updateFontScale,
                                onColourBlindChange = viewModel::updateColourBlindMode,
                                onResetProgress = viewModel::resetProgress
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigationBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple(Route.HOME, "Home", Icons.Default.Home),
        Triple(Route.STATISTICS, "Statistics", Icons.Default.BarChart),
        Triple(Route.SETTINGS, "Settings", Icons.Default.Settings)
    )
    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = route == currentRoute,
                onClick = { onNavigate(route) },
                icon = { androidx.compose.material3.Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors()
            )
        }
    }
}
