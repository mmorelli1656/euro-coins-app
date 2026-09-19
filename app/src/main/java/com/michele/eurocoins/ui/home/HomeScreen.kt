package com.michele.eurocoins.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.R

/**
 * Schermata d'ingresso: sceglie tra i due cataloghi. Oggi solo le
 * commemorative sono navigabili (l'unico dataset che la pipeline produce);
 * la card divisionale è disabilitata finché quella fonte non esiste — vedi
 * CLAUDE.md § Scopo del progetto nel repo della pipeline dati.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onCommemorativeClick: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CatalogCard(
                icon = Icons.Filled.EmojiEvents,
                title = "Commemorative Coins",
                subtitle = "€2 coins issued to mark a special event, one or more per country per year, 2004–present.",
                enabled = true,
                onClick = onCommemorativeClick,
            )
            CatalogCard(
                icon = Icons.Filled.MonetizationOn,
                title = "Circulation Coins",
                subtitle = "The everyday 1 cent – 2€ series issued by each country. Coming soon.",
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Composable
private fun CatalogCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable(onClick = onClick) else it },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        // surfaceVariant coincide col background del tema (vedi Theme.kt):
        // senza un bordo esplicito la card disabilitata si confonde con lo
        // sfondo e non si vede nemmeno come card, non solo come "spenta".
        border = if (enabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(36.dp),
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = contentColor)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
