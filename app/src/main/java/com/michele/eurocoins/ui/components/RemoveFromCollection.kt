package com.michele.eurocoins.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.YearOption

/**
 * "Remove" dei pannelli della collezione (commemorative e Regular Issues): toglie dalla collezione TUTTO ciò che il
 * pannello vede per quella moneta, con una conferma. Sta in basso a sinistra, in testo rosso (il colore del
 * distruttivo, come "Reset collection"), lontano da Cancel e Save; compare solo se c'è già qualcosa di salvato.
 * Non serve nessuna funzione nuova nel database: è il salvataggio con zero spunte, che già sostituisce tutto.
 * (2026-10-08, mockup approvato.) Nelle Regular vale per il taglio della serie guardata, cioè le annate nella sua
 * finestra: quelle di un'altra serie sono un'altra riga e restano.
 */
@Composable
fun RemoveButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("Remove", color = MaterialTheme.colorScheme.error)
    }
}

/** Finestra di conferma: il testo dice cosa si perde; "Remove" in rosso, "Cancel" a fianco. */
@Composable
fun RemoveConfirmDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { DialogTitle("Remove from collection?") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Remove", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Quante annate elencare nel testo prima di abbreviare con "…" (con 12 annate il testo sarebbe un muro). */
private const val MAX_LISTED_YEARS = 6

/** "Standard", "Standard and BU", "Standard, BU and Proof": le finiture salvate, nell'ordine di [CoinQuality]. */
internal fun joinQualities(qualities: Collection<CoinQuality>): String {
    val labels = CoinQuality.entries.filter { it in qualities }.map { it.label }
    return when (labels.size) {
        0 -> ""
        1 -> labels[0]
        else -> labels.dropLast(1).joinToString(", ") + " and " + labels.last()
    }
}

/** Testo di conferma di una commemorativa: quali finiture si perdono. */
internal fun commemorativeRemovalMessage(title: String, qualities: Collection<CoinQuality>): String =
    "This removes ${joinQualities(qualities)} for $title."

/**
 * Testo di conferma di un taglio Regular: quante annate si perdono e quali (la varietà EFS compare come "2002 EFS").
 * Una sola annata si nomina; da due in su si conta e si elencano le prime [MAX_LISTED_YEARS].
 */
internal fun regularRemovalMessage(denomination: String, where: String, years: List<YearOption>): String {
    val labels = years.distinct().sortedWith(compareBy({ it.year }, { it.variety })).map { if (it.variety.isEmpty()) "${it.year}" else "${it.year} ${it.variety}" }
    return when {
        labels.isEmpty() -> "This removes $denomination for $where."
        labels.size == 1 -> "This removes ${labels[0]} of $denomination for $where."
        labels.size <= MAX_LISTED_YEARS ->
            "This removes all ${labels.size} years of $denomination for $where: " +
                labels.dropLast(1).joinToString(", ") + " and " + labels.last() + "."
        else ->
            "This removes all ${labels.size} years of $denomination for $where: " +
                labels.take(MAX_LISTED_YEARS).joinToString(", ") + "…"
    }
}
