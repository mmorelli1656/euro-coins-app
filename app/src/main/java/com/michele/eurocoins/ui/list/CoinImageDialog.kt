package com.michele.eurocoins.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.displayCountry

/**
 * Ingrandimento della moneta aperto dal tocco sulla miniatura: foto grande,
 * paese · anno, tema e il pulsante "Details" verso la schermata completa.
 * Un tocco fuori dalla scheda (o "Close") lo chiude e lascia la lista dov'era.
 *
 * L'immagine è la stessa già caricata dalla miniatura (stesso URL): di solito
 * è già nella cache su disco di Coil, quindi si apre quasi subito.
 */
@Composable
fun CoinImageDialog(coin: Coin, onDismiss: () -> Unit, onDetails: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Angoli arrotondati: lo sfondo bianco è quello del JPEG BCE e senza ritaglio
                // resta un quadrato spigoloso dentro la scheda dai bordi curvi. Scartato il
                // cerchio: le monete non sono centrate uguale nelle foto e risultavano storte.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SubcomposeAsyncImage(
                        model = coin.urlImmagineFonte,
                        contentDescription = coin.tema,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Il contenuto va sempre composto (come nelle altre schermate): un
                        // ramo che lo salta in Loading lasciava la richiesta senza avviarsi.
                        // Nessuna rotella: la prima apertura non tornava mai a Success e la
                        // rotella girava all'infinito sopra la foto già visibile.
                        if (painter.state.value is AsyncImagePainter.State.Error) {
                            Icon(
                                imageVector = Icons.Filled.BrokenImage,
                                contentDescription = "Couldn't load this image",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp),
                            )
                        } else {
                            SubcomposeAsyncImageContent()
                        }
                    }
                }
                Text(
                    text = "${coin.displayCountry()} · ${coin.anno}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = coin.tema, style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Button(onClick = onDetails, modifier = Modifier.padding(start = 8.dp)) { Text("Details") }
                }
            }
        }
    }
}
