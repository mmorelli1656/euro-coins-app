package com.michele.eurocoins.ui.navigation

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
import com.michele.eurocoins.ui.detail.CoinDetailScreen
import com.michele.eurocoins.ui.detail.CoinDetailViewModel
import com.michele.eurocoins.ui.list.CoinListScreen
import com.michele.eurocoins.ui.list.CoinListViewModel

private const val ROUTE_LIST = "list"
private const val ROUTE_DETAIL = "detail/{coinId}"
private const val ARG_COIN_ID = "coinId"

@Composable
fun EuroCoinsNavHost(repository: CoinRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_LIST) {
        composable(ROUTE_LIST) {
            val viewModel: CoinListViewModel = viewModel(
                factory = viewModelFactory { initializer { CoinListViewModel(repository) } },
            )
            CoinListScreen(
                viewModel = viewModel,
                onCoinClick = { id -> navController.navigate("detail/$id") },
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
