package com.michele.eurocoins.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.michele.eurocoins.ui.backup.BackupScreen
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
private const val ROUTE_BACKUP = "backup"
private const val ROUTE_COINS = "coins/{kind}/{value}"
private const val ROUTE_DETAIL = "detail/{coinId}"
private const val ARG_KIND = "kind"
private const val ARG_VALUE = "value"
private const val ARG_COIN_ID = "coinId"

private const val KIND_YEAR = "year"
private const val KIND_COUNTRY = "country"

@Composable
fun EuroCoinsNavHost(
    repository: CoinRepository,
    backupService: BackupService,
    accountManager: GoogleAccountManager,
    themePreference: ThemePreference,
) {
    val navController = rememberNavController()

    fun openDetail(id: Long) = navController.navigate("detail/$id")

    // Il default di Navigation Compose è una dissolvenza da 700 ms: la nuova schermata
    // resta quasi trasparente per un istante e sembra un ritardo dopo il tocco.
    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        enterTransition = { fadeIn(tween(280)) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(280)) },
        popExitTransition = { fadeOut(tween(120)) },
    ) {
        composable(ROUTE_HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory { initializer { HomeViewModel(repository) } },
            )
            // Riletto a ogni rientro nella home: dopo login/logout dalla schermata Backup l'icona si aggiorna.
            val account = accountManager.currentAccount()
            val themeMode by themePreference.mode.collectAsState()
            HomeScreen(
                viewModel = viewModel,
                accountInitial = account?.let { (it.displayName ?: it.email).firstOrNull()?.uppercase() },
                themeMode = themeMode,
                onThemeModeChange = themePreference::set,
                onCommemorativeClick = { navController.navigate(ROUTE_BROWSE) },
                onProfileClick = { navController.navigate(ROUTE_BACKUP) },
            )
        }
        composable(ROUTE_BACKUP) {
            val viewModel: BackupViewModel = viewModel(
                factory = viewModelFactory { initializer { BackupViewModel(backupService, accountManager) } },
            )
            BackupScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(ROUTE_BROWSE) {
            val browseViewModel: BrowseViewModel = viewModel(
                factory = viewModelFactory { initializer { BrowseViewModel(repository) } },
            )
            val allCoinsViewModel: CoinListViewModel = viewModel(
                key = "all",
                factory = viewModelFactory { initializer { CoinListViewModel(repository, CoinFilter.All) } },
            )
            BrowseScreen(
                viewModel = browseViewModel,
                allCoinsViewModel = allCoinsViewModel,
                // Uri.encode: "Città del Vaticano" e "Paesi Bassi" hanno spazi/accenti.
                onYearClick = { year -> navController.navigate("coins/$KIND_YEAR/$year") },
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
