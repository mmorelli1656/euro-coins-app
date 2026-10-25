package com.michele.eurocoins.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.ui.components.LocalProAccess
import com.michele.eurocoins.ui.detail.DetailCard
import com.michele.eurocoins.ui.detail.sansTitleMedium
import com.michele.eurocoins.ui.theme.appBarColors

/** Le tre schede di Insights: la prima è libera, le altre due sono del Pro. */
private enum class InsightsTab(val label: String, val proOnly: Boolean) {
    EURO("The euro", false),
    COLLECTION("Collection", true),
    SPENDING("Spending", true),
}

/**
 * Insights (2026-10-25): una pagina con tre schede. "The euro" è per tutti e si apre per prima; "Collection" e
 * "Spending" (statistiche sulla collezione) sono del Pro, con il lucchetto sull'etichetta. Un utente Pro, finché le
 * statistiche non ci sono, trova solo l'avviso "Coming soon".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(onBack: () -> Unit) {
    val pro = LocalProAccess.current
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = InsightsTab.entries[tabIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text("Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState(), enabled = true)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                when (tab) {
                    InsightsTab.EURO -> EuroTab()
                    else -> ProTab(title = tab.label, isPro = pro.isPro, onGoPro = pro.onGoPro)
                }
            }
        }
    }
}

/** Una scheda del Pro: con il Pro, per ora, "Coming soon"; senza, l'invito (il pulsante porta alle Impostazioni). */
@Composable
private fun ProTab(title: String, isPro: Boolean, onGoPro: () -> Unit) {
    DetailCard {
        if (isPro) {
            Text(title, style = sansTitleMedium())
            Text("Coming soon.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        } else {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text("$title is part of Pro", style = sansTitleMedium(), modifier = Modifier.padding(top = 8.dp))
            Text(
                text = "Statistics about your collection, with no ads.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Button(onClick = onGoPro, shape = RoundedCornerShape(50)) { Text("Go Pro") }
        }
    }
}
