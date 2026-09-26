package com.michele.eurocoins.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.backup.BackupService
import com.michele.eurocoins.data.backup.GoogleAccountManager
import com.michele.eurocoins.ui.settings.SettingsScreen
import com.michele.eurocoins.ui.settings.SettingsViewModel
import com.michele.eurocoins.ui.settings.UserSettings
import com.michele.eurocoins.ui.backup.BackupViewModel
import com.michele.eurocoins.ui.browse.BrowseScreen
import com.michele.eurocoins.ui.browse.BrowseViewModel
import com.michele.eurocoins.ui.detail.CoinDetailScreen
import com.michele.eurocoins.ui.detail.CoinDetailViewModel
import com.michele.eurocoins.ui.home.HomeScreen
import com.michele.eurocoins.ui.home.HomeViewModel
import com.michele.eurocoins.ui.list.CoinFilter
import com.michele.eurocoins.ui.list.CoinListScreen
import com.michele.eurocoins.ui.list.CoinListViewModel
import com.michele.eurocoins.ui.theme.ThemePreference

private const val ROUTE_HOME = "home"
private const val ROUTE_BROWSE = "browse"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_COINS = "coins/{kind}/{value}"
private const val ROUTE_DETAIL = "detail/{coinId}"
private const val ARG_KIND = "kind"
private const val ARG_VALUE = "value"
private const val ARG_COIN_ID = "coinId"

private const val KIND_YEAR = "year"
private const val KIND_YEAR_COMMON = "year-common"
private const val KIND_COUNTRY = "country"

@Composable
fun EuroCoinsNavHost(
    repository: CoinRepository,
    backupService: BackupService,
    accountManager: GoogleAccountManager,
    themePreference: ThemePreference,
    userSettings: UserSettings,
) {
    val navController = rememberNavController()

    fun openDetail(id: Long) = navController.navigate("detail/$id")

    // Fade-through: la schermata che esce sfuma in fretta, quella nuova entra dopo una breve pausa con
    // una frenata morbida alla fine (è la parte che dà la sensazione "burrosa"). Nella pausa si vede il
    // fondo dell'app, che in MainActivity è `background` (non `surface`, bianco nel tema chiaro).
    val enterFade = fadeIn(tween(durationMillis = 420, delayMillis = 90, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)))
    val exitFade = fadeOut(tween(durationMillis = 120, easing = LinearEasing))

    // Il default di Navigation Compose è una dissolvenza da 700 ms: la nuova schermata
    // resta quasi trasparente per un istante e sembra un ritardo dopo il tocco.
    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        enterTransition = { enterFade },
        exitTransition = { exitFade },
        popEnterTransition = { enterFade },
        popExitTransition = { exitFade },
    ) {
        composable(ROUTE_HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory { initializer { HomeViewModel(repository, userSettings) } },
            )
            HomeScreen(
                viewModel = viewModel,
                onCommemorativeClick = { navController.navigate(ROUTE_BROWSE) },
                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_SETTINGS) {
            val backupViewModel: BackupViewModel = viewModel(
                factory = viewModelFactory { initializer { BackupViewModel(backupService, accountManager) } },
            )
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory { initializer { SettingsViewModel(repository, userSettings, themePreference) } },
            )
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                backupViewModel = backupViewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(ROUTE_BROWSE) {
            val browseViewModel: BrowseViewModel = viewModel(
                factory = viewModelFactory { initializer { BrowseViewModel(repository, userSettings.defaultTab.value) } },
            )
            val allCoinsViewModel: CoinListViewModel = viewModel(
                key = "all",
                factory = viewModelFactory { initializer { CoinListViewModel(repository, CoinFilter.All) } },
            )
            BrowseScreen(
                viewModel = browseViewModel,
                allCoinsViewModel = allCoinsViewModel,
                // Uri.encode: "Città del Vaticano" e "Paesi Bassi" hanno spazi/accenti.
                onYearClick = { year, commonOnly ->
                    val kind = if (commonOnly) KIND_YEAR_COMMON else KIND_YEAR
                    navController.navigate("coins/$kind/$year")
                },
                onCountryClick = { paese -> navController.navigate("coins/$KIND_COUNTRY/${Uri.encode(paese)}") },
                onCoinClick = ::openDetail,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = ROUTE_COINS,
            arguments = listOf(
                navArgument(ARG_KIND) { type = NavType.StringType },
                navArgument(ARG_VALUE) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val kind = backStackEntry.arguments?.getString(ARG_KIND) ?: return@composable
            val value = backStackEntry.arguments?.getString(ARG_VALUE) ?: return@composable
            val filter = when (kind) {
                KIND_YEAR -> CoinFilter.Year(value.toInt())
                KIND_YEAR_COMMON -> CoinFilter.Year(value.toInt(), commonOnly = true)
                else -> CoinFilter.Country(value)
            }
            val viewModel: CoinListViewModel = viewModel(
                key = "coins-$kind-$value",
                factory = viewModelFactory { initializer { CoinListViewModel(repository, filter) } },
            )
            CoinListScreen(
                viewModel = viewModel,
                filter = filter,
                onCoinClick = ::openDetail,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument(ARG_COIN_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getLong(ARG_COIN_ID) ?: return@composable
            val viewModel: CoinDetailViewModel = viewModel(
                factory = viewModelFactory { initializer { CoinDetailViewModel(repository, coinId) } },
            )
            CoinDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
