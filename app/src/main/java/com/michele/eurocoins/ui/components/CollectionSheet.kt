package com.michele.eurocoins.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.stableKey

/**
 * Pannello per registrare una moneta: una riga per qualità (Standard / BU /
 * Proof) con spunta e, se spuntata, il prezzo pagato (facoltativo).
 *
 * Lavora su una BOZZA locale e scrive solo con "Save": chiudere il pannello
 * senza salvare non cambia nulla, quindi un tocco sbagliato non cancella un
 * prezzo già inserito. Alla prima apertura (moneta non ancora posseduta)
 * "Standard" è già spuntata, perché è il caso di quasi tutte le monete: un
 * tocco sulla casella + Save.
 *
 * Usato sia dall'elenco (casella accanto a ogni moneta) sia dal dettaglio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionSheet(
    coin: Coin,
    currentItems: List<CollectionItem>,
    onSave: (Map<CoinQuality, Int?>) -> Unit,
    onDismiss: () -> Unit,
) {
    val checked = remember(coin.stableKey) {
        mutableStateMapOf<CoinQuality, Boolean>().apply {
            CoinQuality.entries.forEach { quality ->
                this[quality] = currentItems.any { it.quality == quality } ||
                    (currentItems.isEmpty() && quality == CoinQuality.STANDARD)
            }
        }
    }
    val prices = remember(coin.stableKey) {
        mutableStateMapOf<CoinQuality, String>().apply {
            CoinQuality.entries.forEach { quality ->
                this[quality] = formatPrice(currentItems.firstOrNull { it.quality == quality }?.priceCents)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text("My collection", style = MaterialTheme.typography.titleLarge)
            Text(
                text = coin.tema,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )

            CoinQuality.entries.forEach { quality ->
                val isChecked = checked[quality] == true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    Checkbox(checked = isChecked, onCheckedChange = { checked[quality] = it })
                    Text(
                        text = quality.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.width(76.dp),
                    )
                    if (isChecked) {
                        OutlinedTextField(
                            value = prices[quality].orEmpty(),
                            onValueChange = { prices[quality] = it },
                            label = { Text("Price paid (€)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(
                            CoinQuality.entries
                                .filter { checked[it] == true }
                                .associateWith { parsePriceCents(prices[it].orEmpty()) },
                        )
                    },
                ) { Text("Save") }
            }
        }
    }
}
