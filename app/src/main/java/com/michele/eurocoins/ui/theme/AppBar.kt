package com.michele.eurocoins.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * Colori di [TopAppBar] uguali allo sfondo della schermata. Di default la barra usa
 * `surface` (quasi bianco nel tema chiaro): sopra al fondo beige compariva una fascia
 * chiara e lo sfondo sembrava cambiare colore a metà schermata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
)
