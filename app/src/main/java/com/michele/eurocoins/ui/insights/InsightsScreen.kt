package com.michele.eurocoins.ui.insights

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.ui.theme.appBarColors

/**
 * Insights (2026-10-25): per ora una sola pagina, "The euro", libera per tutti (storia, materiali, lato comune).
 * Le schede Collection e Spending (statistiche sulla collezione, Pro) erano segnaposto e sono state tolte il
 * 2026-10-25 su richiesta, finché non si decide cosa metterci: il mockup è nella chat del 2026-10-24 (completamento per
 * paese e catalogo, finiture, spesa con la riga "basato su N monete con un prezzo"). Quando tornano, il selettore a
 * segmenti torna sopra il contenuto (The euro / Collection / Spending, lucchetto sulle due Pro da Free).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(onBack: () -> Unit) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            EuroTab()
        }
    }
}
