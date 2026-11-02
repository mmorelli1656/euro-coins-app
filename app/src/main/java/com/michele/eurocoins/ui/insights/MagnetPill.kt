package com.michele.eurocoins.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pillola "Strongly magnetic" / "Not magnetic" / "Centre slightly magnetic" accanto al titolo del materiale nella scheda
 * Coins (2026-10-26, rimessa su richiesta del proprietario: stava nei mockup e nella prima versione, poi era diventata
 * una riga "Magnet" della tabella). Pillola = etichetta, come nel resto dell'app. Tre tinte: neutra se non è
 * magnetica, verde del primario se lo è, lilla per il solo centro del 1 e 2 euro. Il significato non sta nel solo colore:
 * cambia l'icona (calamita, barrata se non è magnetica) e il testo.
 */
@Composable
internal fun MagnetPill(magnetism: Magnetism, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (background, content) = when (magnetism) {
        Magnetism.NONE -> scheme.outline.copy(alpha = 0.28f) to scheme.onSurface
        Magnetism.STRONG -> scheme.primary.copy(alpha = 0.40f) to scheme.onSurface
        Magnetism.CENTRE -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (magnetism == Magnetism.NONE) MagnetIcons.Off else MagnetIcons.On,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = magnetism.label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            color = content,
            maxLines = 1,
        )
    }
}

/**
 * Calamita e calamita barrata: disegno a tratto da 2 px su 24, come le altre icone Tabler dell'app (la calamita è
 * "magnet" di Tabler Icons, MIT, © Paweł Kuna, tabler.io/icons; la barratura è una diagonale aggiunta). Material non ha una
 * calamita. Si colorano con `tint` come ogni `Icon`.
 */
internal object MagnetIcons {
    private const val MAGNET = "M4 13v-8a2 2 0 0 1 2 -2h1a2 2 0 0 1 2 2v8a2 2 0 0 0 6 0v-8a2 2 0 0 1 2 -2h1a2 2 0 0 1 2 2v8a8 8 0 0 1 -16 0 " +
        "M4 8l5 0 M15 8l4 0"

    val On: ImageVector by lazy { stroked("Magnet", MAGNET) }

    val Off: ImageVector by lazy { stroked("MagnetOff", "$MAGNET M3 3l18 18") }

    private fun stroked(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
}
