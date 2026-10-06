package com.michele.eurocoins.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michele.eurocoins.ui.theme.creditIconColor

/** Una fonte nei crediti: "etichetta: valore", con un link opzionale (icona dopo il valore). */
data class SourceItem(val label: String, val value: String, val url: String? = null)

/**
 * Crediti dei dettagli (commemorative e Regular Issues), un solo formato per tutte le fonti:
 * una riga centrata di "Etichetta: Fonte" distanziate (niente "·": andando a capo restava a inizio
 * riga), con l'icona di link subito dopo ogni
 * fonte che ne ha uno (ECB e Numista identiche), e sotto la licenza dell'immagine, che è lunga e
 * va tenuta intera per l'attribuzione. Prima erano tre stili diversi: una frase con lo slug
 * grezzo (`EC_NATIONAL_SIDES`), un'icona sola a fondo riga e la riga Numista a parte.
 * `FlowRow` perché con tutte e tre le fonti e un N# lungo la riga può non stare su uno schermo
 * stretto o con il font ingrandito: va a capo invece di tagliare.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourceCredits(sources: List<SourceItem>, licenseLine: String?) {
    val context = LocalContext.current
    Column(
        // Niente margine orizzontale sulla colonna: la riga della licenza ("License: Copyright of the issuing
        // mint (editorial use)", ~330 dp a 13 sp) con 8 dp per lato non stava in 343 dp e andava a capo.
        // Il margine resta sulla riga delle fonti.
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.Center,
        ) {
            sources.forEach { source ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 32.dp)) {
                    val url = source.url
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (url != null) {
                            Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        } else {
                            Modifier
                        },
                    ) {
                        Text(
                            text = "${source.label}: ${source.value}",
                            // 13 sp Medium (era 12 sp normale): i crediti stanno sul fondo grigio-verde, non in una
                            // card bianca, e nel tema chiaro il testo sottile scuro sul mezzo tono "affondava"
                            // anche con 9:1 di contrasto (nello scuro, su fondo quasi nero, no).
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (url != null) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open ${source.value}",
                                tint = creditIconColor(),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        licenseLine?.let { FooterLine(it) }
    }
}
