package com.michele.eurocoins.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.ui.theme.BronzeLight
import com.michele.eurocoins.ui.theme.InkLight
import com.michele.eurocoins.ui.theme.LilacLight

/** Quota della larghezza della foto occupata dal tondo (variante B del mockup, ottobre 2026). */
private const val PLACEHOLDER_CIRCLE_FRACTION = 0.76f

/** Quota del tondo occupata dal simbolo dell'euro. */
private const val PLACEHOLDER_ICON_FRACTION = 0.45f

/**
 * Segnaposto della foto nei dettagli (commemorative e Regular Issues), dove la foto non c'è o non
 * si carica: lo stesso tondo lilla con l'euro dell'elenco, in scala grande, e sotto il messaggio.
 *
 * Colori FISSI, non del tema: la Hero card è bianca anche nel tema scuro (le foto BCE hanno sfondo
 * bianco puro), e il lilla scuro del tema scuro starebbe male su bianco. Il tondo non arriva a
 * riempire la card (76%, non il 96% di una foto vera): un disco lilla così largo sarebbe un
 * blocco pieno e pesante, e il messaggio ha bisogno di posto sotto. Scelto dopo mockup.
 */
@Composable
fun HeroPlaceholder(message: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth(PLACEHOLDER_CIRCLE_FRACTION)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(LilacLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.EuroSymbol,
                contentDescription = null,
                tint = BronzeLight,
                modifier = Modifier.fillMaxSize(PLACEHOLDER_ICON_FRACTION),
            )
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = InkLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}
