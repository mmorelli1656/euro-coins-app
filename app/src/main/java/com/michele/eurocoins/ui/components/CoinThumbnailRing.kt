package com.michele.eurocoins.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp

/**
 * Qualità di ridimensionamento delle miniature negli elenchi. Coil non riduce la foto da 270 px
 * alla misura del riquadro (precisione inesatta: al massimo salta per potenze di due), quindi la
 * riduzione la fa Compose, e con la qualità predefinita (bassa, bilineare) la moneta risultava
 * morbida. `Medium` usa i mipmap: stesso aspetto, bordi e rilievi più nitidi.
 */
val ThumbnailFilterQuality = FilterQuality.Medium

/**
 * Anello sottile sopra la foto di una moneta nell'elenco: le monete (argento, oro nordico) sono
 * chiare e la card è bianca, quindi senza un bordo il contorno si perdeva e la moneta sembrava più
 * piccola di quanto è. Va disegnato SOPRA l'immagine (ultimo figlio del riquadro), solo a foto
 * caricata: sul cerchio lilla del segnaposto non serve.
 */
@Composable
fun CoinThumbnailRing() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), CircleShape),
    )
}
