package com.michele.eurocoins.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
 * Contrasto leggero delle miniature (tentativo, ottobre 2026): `out = c · (in − PIVOT) + PIVOT` su
 * ogni canale. Il perno è sui chiari (170/255, non 128) perché le monete sono chiare (luminanza
 * media 187-213) su sfondo bianco: con il perno al grigio medio si schiarirebbero verso il bianco
 * dello sfondo e perderebbero il bordo, mentre così i chiari restano dove sono, le ombre del rilievo
 * si scuriscono e il bianco puro resta bianco. [THUMBNAIL_CONTRAST] = 1 spegne l'effetto; 1.2 è
 * "leggero", oltre ~1.4 i toni dell'argento iniziano a bruciare. Solo miniature degli elenchi, non
 * dettaglio né Home.
 */
const val THUMBNAIL_CONTRAST = 1.2f
private const val THUMBNAIL_CONTRAST_PIVOT = 170f

val ThumbnailColorFilter: ColorFilter = THUMBNAIL_CONTRAST.let { c ->
    val offset = THUMBNAIL_CONTRAST_PIVOT * (1f - c)
    ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, offset,
                0f, c, 0f, 0f, offset,
                0f, 0f, c, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

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
