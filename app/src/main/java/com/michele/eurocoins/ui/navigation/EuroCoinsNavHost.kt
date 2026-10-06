package com.michele.eurocoins.ui.navigation

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.backup.AutoBackup
import com.michele.eurocoins.data.backup.BackupService
import com.michele.eurocoins.data.backup.GoogleAccountManager
import com.michele.eurocoins.data.pro.Monetization
import com.michele.eurocoins.ui.settings.SettingsScreen
import com.michele.eurocoins.ui.settings.SettingsViewModel
import com.michele.eurocoins.ui.settings.UserSettings
import com.michele.eurocoins.ui.backup.BackupViewModel
import com.michele.eurocoins.ui.browse.BrowseScreen
import com.michele.eurocoins.ui.browse.BrowseViewModel
import com.michele.eurocoins.ui.detail.CoinDetailScreen
import com.michele.eurocoins.ui.detail.CoinDetailViewModel
import com.michele.eurocoins.ui.detail.CoinPagerSession
import com.michele.eurocoins.ui.home.HomeScreen
import com.michele.eurocoins.ui.home.HomeViewModel
import com.michele.eurocoins.ui.list.CoinFilter
import com.michele.eurocoins.ui.list.CoinListScreen
import com.michele.eurocoins.ui.list.CoinListViewModel
import com.michele.eurocoins.ui.regular.RegularDenominationDetailScreen
import com.michele.eurocoins.ui.regular.RegularDenominationDetailViewModel
import com.michele.eurocoins.ui.regular.RegularDenominationListScreen
import com.michele.eurocoins.ui.regular.RegularDenominationListViewModel
import com.michele.eurocoins.ui.regular.RegularPageKey
import com.michele.eurocoins.ui.regular.RegularPagerSession
import com.michele.eurocoins.ui.regular.RegularIssueCountryScreen
import com.michele.eurocoins.ui.regular.RegularIssueCountryViewModel
import com.michele.eurocoins.ui.regular.RegularIssuesScreen
import com.michele.eurocoins.ui.regular.RegularIssuesViewModel
import com.michele.eurocoins.ui.theme.ThemePreference

private const val ROUTE_HOME = "home"
private const val ROUTE_BROWSE = "browse"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_COINS = "coins/{kind}/{value}"
private const val ROUTE_DETAIL = "detail/{coinId}"
private const val ROUTE_REGULAR_ISSUES = "regular-issues"
private const val ROUTE_REGULAR_ISSUE_COUNTRY = "regular-issues/{paese}"
private const val ROUTE_REGULAR_DENOMINATION = "regular-issues/{paese}/{ordine}/{taglio}"
// Un taglio in tutti i paesi (elenco), non il dettaglio di un taglio di una serie.
private const val ROUTE_REGULAR_DENOMINATION_LIST = "regular-denominations/{taglio}"
private const val ARG_KIND = "kind"
private const val ARG_VALUE = "value"
private const val ARG_COIN_ID = "coinId"
private const val ARG_PAESE = "paese"
private const val ARG_ORDINE = "ordine"
private const val ARG_TAGLIO = "taglio"

private const val KIND_YEAR = "year"
private const val KIND_YEAR_COMMON = "year-common"
private const val KIND_COUNTRY = "country"

@Composable
fun EuroCoinsNavHost(
    repository: CoinRepository,
    regularIssueRepository: RegularIssueRepository,
    backupService: BackupService,
    accountManager: GoogleAccountManager,
    autoBackup: AutoBackup,
    themePreference: ThemePreference,
    userSettings: UserSettings,
    monetization: Monetization,
) {
    val navController = rememberNavController()
    // Elenco ordinato della lista da cui si apre un dettaglio (per scorrere tra le monete): vedi CoinPagerSession.
    val pagerSession: CoinPagerSession = viewModel()
    // Lo stesso per Regular Issues: le righe (taglio di una serie) della lista da cui si apre il dettaglio.
    val regularPagerSession: RegularPagerSession = viewModel()

    // Annuncio a tutto schermo: si conta ogni moneta guardata (si entra nel dettaglio) e, quando si esce dal
    // dettaglio, se è il momento (abbastanza monete e abbastanza tempo) compare sopra la lista. Un solo
    // punto per tutte le uscite (freccia, gesto indietro): le schermate non sanno niente di pubblicità.
    val activity = LocalContext.current as Activity
    DisposableEffect(navController) {
        var previousRoute: String? = null
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val route = destination.route
            val wasDetail = previousRoute.isDetailRoute()
            val isDetail = route.isDetailRoute()
            if (!wasDetail && isDetail) monetization.interstitial.onCoinViewed()
            if (wasDetail && !isDetail) monetization.interstitial.showIfDue(activity)
            previousRoute = route
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

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
            val adsEnabled by monetization.adsEnabled.collectAsState()
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory { initializer { HomeViewModel(repository, regularIssueRepository, userSettings) } },
            )
            HomeScreen(
                viewModel = viewModel,
                onCommemorativeClick = { navController.navigate(ROUTE_BROWSE) },
                onRegularIssuesClick = { navController.navigate(ROUTE_REGULAR_ISSUES) },
                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                showAds = adsEnabled,
            )
        }
        composable(ROUTE_SETTINGS) {
            val backupViewModel: BackupViewModel = viewModel(
                factory = viewModelFactory { initializer { BackupViewModel(backupService, accountManager, autoBackup) } },
            )
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory { initializer { SettingsViewModel(repository, regularIssueRepository, userSettings, themePreference) } },
            )
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                backupViewModel = backupViewModel,
                monetization = monetization,
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
                onCoinClick = { id ->
                    pagerSession.open(allCoinsViewModel)
                    openDetail(id)
                },
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
                onCoinClick = { id ->
                    pagerSession.open(viewModel)
                    openDetail(id)
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument(ARG_COIN_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getLong(ARG_COIN_ID) ?: return@composable
            CoinDetailScreen(
                coinIds = pagerSession.idsFor(coinId),
                initialCoinId = coinId,
                // Stessa chiave = stessa istanza: la pagina e il titolo della barra condividono il ViewModel.
                viewModelFor = { id ->
                    viewModel(
                        key = "detail-$id",
                        factory = viewModelFactory { initializer { CoinDetailViewModel(repository, id) } },
                    )
                },
                onPageShown = { id ->
                    pagerSession.onPageShown(id)
                    monetization.interstitial.onCoinViewed()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(ROUTE_REGULAR_ISSUES) {
            val viewModel: RegularIssuesViewModel = viewModel(
                factory = viewModelFactory { initializer { RegularIssuesViewModel(regularIssueRepository, userSettings.defaultRegularTab.value) } },
            )
            RegularIssuesScreen(
                viewModel = viewModel,
                // Uri.encode: stesso motivo delle commemorative, "Città del Vaticano" e "Paesi Bassi" hanno spazi/accenti.
                onCountryClick = { paese -> navController.navigate("regular-issues/${Uri.encode(paese)}") },
                onDenominationClick = { taglio -> navController.navigate("regular-denominations/${Uri.encode(taglio)}") },
                onRowClick = { series, taglio ->
                    regularPagerSession.open(
                        pages = viewModel.uiState.value.all.map {
                            RegularPageKey(it.paese, it.viewedSeries.ordineCronologico, it.denomination.image.taglio)
                        },
                        onShown = viewModel::requestScrollTo,
                    )
                    navController.navigate(
                        "regular-issues/${Uri.encode(series.paese)}/${series.ordineCronologico}/${Uri.encode(taglio)}",
                    )
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = ROUTE_REGULAR_DENOMINATION_LIST,
            arguments = listOf(navArgument(ARG_TAGLIO) { type = NavType.StringType }),
        ) { backStackEntry ->
            val taglio = backStackEntry.arguments?.getString(ARG_TAGLIO) ?: return@composable
            val viewModel: RegularDenominationListViewModel = viewModel(
                key = "regular-denomination-list-$taglio",
                factory = viewModelFactory { initializer { RegularDenominationListViewModel(regularIssueRepository, taglio) } },
            )
            RegularDenominationListScreen(
                taglio = taglio,
                viewModel = viewModel,
                onRowClick = { series ->
                    regularPagerSession.open(
                        pages = viewModel.uiState.value.rows.map {
                            RegularPageKey(it.paese, it.viewedSeries.ordineCronologico, taglio)
                        },
                        onShown = viewModel::requestScrollTo,
                    )
                    navController.navigate(
                        "regular-issues/${Uri.encode(series.paese)}/${series.ordineCronologico}/${Uri.encode(taglio)}",
                    )
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = ROUTE_REGULAR_ISSUE_COUNTRY,
            arguments = listOf(navArgument(ARG_PAESE) { type = NavType.StringType }),
        ) { backStackEntry ->
            val paese = backStackEntry.arguments?.getString(ARG_PAESE) ?: return@composable
            val viewModel: RegularIssueCountryViewModel = viewModel(
                key = "regular-issue-country-$paese",
                factory = viewModelFactory { initializer { RegularIssueCountryViewModel(regularIssueRepository, paese) } },
            )
            RegularIssueCountryScreen(
                viewModel = viewModel,
                onDenominationClick = { series, taglio ->
                    // Gli 8 tagli della serie guardata, nell'ordine della schermata. Niente scorrimento al ritorno: la
                    // schermata del paese non è una lista lazy e mantiene da sola la posizione.
                    regularPagerSession.open(
                        pages = viewModel.uiState.value.denominations.map {
                            RegularPageKey(paese, series.ordineCronologico, it.image.taglio)
                        },
                        onShown = {},
                    )
                    navController.navigate(
                        "regular-issues/${Uri.encode(paese)}/${series.ordineCronologico}/${Uri.encode(taglio)}",
                    )
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = ROUTE_REGULAR_DENOMINATION,
            arguments = listOf(
                navArgument(ARG_PAESE) { type = NavType.StringType },
                navArgument(ARG_ORDINE) { type = NavType.IntType },
                navArgument(ARG_TAGLIO) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val paese = backStackEntry.arguments?.getString(ARG_PAESE) ?: return@composable
            val ordine = backStackEntry.arguments?.getInt(ARG_ORDINE) ?: return@composable
            val taglio = backStackEntry.arguments?.getString(ARG_TAGLIO) ?: return@composable
            val initial = RegularPageKey(paese, ordine, taglio)
            RegularDenominationDetailScreen(
                pages = regularPagerSession.pagesFor(initial),
                initialPage = initial,
                // Stessa chiave = stessa istanza: la pagina e il titolo della barra condividono il ViewModel.
                viewModelFor = { page ->
                    viewModel(
                        key = "regular-denomination-${page.paese}-${page.ordine}-${page.taglio}",
                        factory = viewModelFactory {
                            initializer { RegularDenominationDetailViewModel(regularIssueRepository, page.paese, page.ordine, page.taglio) }
                        },
                    )
                },
                onPageShown = { page ->
                    regularPagerSession.onPageShown(page)
                    monetization.interstitial.onCoinViewed()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/** Le due schermate di dettaglio: una moneta commemorativa e il taglio di una serie Regular Issues. */
private fun String?.isDetailRoute(): Boolean = this == ROUTE_DETAIL || this == ROUTE_REGULAR_DENOMINATION
