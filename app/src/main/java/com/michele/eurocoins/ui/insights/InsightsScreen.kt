package com.michele.eurocoins.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.BadgeTarget
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.SpentTarget
import com.michele.eurocoins.ui.components.LocalProAccess
import com.michele.eurocoins.ui.theme.appBarColors

/** Le tre schede di Insights: le prime due sono libere, "Statistics" è del Pro (lucchetto sull'etichetta da Free). */
private enum class InsightsTab(val label: String, val proOnly: Boolean) {
    HISTORY("History", false),
    COINS("Coins", false),
    STATISTICS("Statistics", true),
}

/**
 * Insights (2026-10-25): una pagina con tre schede a segmenti, come Years / Countries / All. **History** (si apre per
 * prima): la storia dell'euro, la mappa del lato comune al 2007, gli stati per accordo. **Coins**: di che metalli sono
 * fatte le monete. **Statistics** (Pro): completamento, paesi, traguardi, anni, finiture e spesa (vedi `StatisticsTab`),
 * in un'unica scheda e non Collection e Spending separate, per non avere quattro segmenti; da Free un'anteprima sfocata
 * con l'invito al Pro. Divisione scelta con il proprietario il 2026-10-25:
 * la pagina era arrivata a più di quattro schermate con due argomenti scollegati, la storia (da leggere) e i materiali
 * (da toccare). Ogni scheda ha le sue fonti. Il selettore sta fermo in alto mentre il contenuto scorre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    /** Apre la lista commemorative di un paese (già filtrata su "Missing"): [paese] è il valore stabile `Coin.paese`. */
    onCommemorativeCountryClick: (String) -> Unit,
    /** Apre la schermata Regular Issues di un paese. */
    onRegularCountryClick: (String) -> Unit,
    /** Apre la schermata di un paese Regular già sulla serie indicata (posizione 1-based). */
    onRegularSeriesClick: (String, Int) -> Unit,
    /** Apre la lista commemorative di un anno già su "Missing"; [joint] = la lista delle sole emissioni comuni di quell'anno. */
    onYearClick: (year: Int, joint: Boolean) -> Unit,
    /** Apre l'elenco di un taglio Regular ("2 euro") in tutti i paesi, già su "Missing". */
    onValueClick: (String) -> Unit,
    /** Apre le commemorative possedute in una finitura (già su Owned e sulla qualità). */
    onFinishClick: (CoinQuality) -> Unit,
    /** Apre il dettaglio di una voce della classifica "Most expensive". */
    onSpentItemClick: (SpentTarget) -> Unit,
    /** Apre ciò che si possiede nelle commemorative (scheda All su Owned). */
    onCommemorativeOwnedClick: () -> Unit,
    /** Apre ciò che si possiede nelle Regular Issues (scheda All su Owned). */
    onRegularOwnedClick: () -> Unit,
    /** Il tasto di una medaglia non ancora conquistata: la destinazione la risolve la navigazione. */
    onBadgeClick: (BadgeTarget) -> Unit,
    onBack: () -> Unit,
) {
    val pro = LocalProAccess.current
    val stats by viewModel.stats.collectAsState()
    val layout by viewModel.layout.collectAsState()
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var customizing by rememberSaveable { mutableStateOf(false) }
    val tab = InsightsTab.entries[tabIndex]
    val canCustomize = tab == InsightsTab.STATISTICS && pro.isPro

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text("Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                // Customize: solo sulla scheda Statistics e solo da Pro (da Free la pagina sfocata mostra sempre tutto). Nella barra e non
                // accanto al selettore: lì l'icona comparsa e sparita cambierebbe la larghezza dei segmenti passando da una scheda all'altra.
                actions = {
                    if (canCustomize) {
                        IconButton(onClick = { customizing = true }) { Icon(Icons.Outlined.Tune, contentDescription = "Customize") }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                InsightsTab.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = InsightsTab.entries.size),
                        // Nessuna spunta: sposterebbe l'etichetta (come negli altri selettori dell'app).
                        icon = {},
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (entry.proOnly && !pro.isPro) Icon(Icons.Filled.Lock, contentDescription = "Pro", modifier = Modifier.size(14.dp))
                                Text(entry.label, maxLines = 1)
                            }
                        },
                    )
                }
            }
            // Una scheda diversa = un'altra lista: `key` fa ripartire lo scorrimento dall'alto.
            androidx.compose.runtime.key(tab) {
                val lockedStats = stats
                if (tab == InsightsTab.STATISTICS && !pro.isPro) {
                    // Da Free: la pagina vera, sfocata, che scorre sotto la card dell'invito ferma al centro (ha il suo scorrimento).
                    if (lockedStats != null) {
                        StatisticsLocked(lockedStats, onGoPro = pro.onGoPro, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        when (tab) {
                            InsightsTab.HISTORY -> HistoryTab()
                            InsightsTab.COINS -> CoinsTab()
                            InsightsTab.STATISTICS -> StatisticsTab(
                                stats = stats,
                                onCommemorativeCountry = onCommemorativeCountryClick,
                                onRegularCountry = onRegularCountryClick,
                                onRegularSeries = onRegularSeriesClick,
                                onYear = onYearClick,
                                onValue = onValueClick,
                                onFinish = onFinishClick,
                                onSpentItem = onSpentItemClick,
                                onCommemorativeOwned = onCommemorativeOwnedClick,
                                onRegularOwned = onRegularOwnedClick,
                                onBadgeGo = onBadgeClick,
                                layout = layout,
                            )
                        }
                    }
                }
            }
        }
    }
    if (customizing && canCustomize) {
        CustomizeSheet(layout = layout, onChange = viewModel::setLayout, onDismiss = { customizing = false })
    }
}
