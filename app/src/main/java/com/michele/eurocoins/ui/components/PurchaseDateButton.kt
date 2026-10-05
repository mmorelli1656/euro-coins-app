package com.michele.eurocoins.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.FIRST_PURCHASE_YEAR
import com.michele.eurocoins.data.epochDayToPickerMillis
import com.michele.eurocoins.data.formatPurchaseDate
import com.michele.eurocoins.data.isSelectablePurchaseDay
import com.michele.eurocoins.data.pickerMillisToEpochDay
import com.michele.eurocoins.data.purchaseDateLabel
import java.time.LocalDate

/**
 * Pulsante della data nelle card di finitura dei pannelli di collezione: un TONDO da 40 dp con l'icona del
 * calendario. Senza data: bordo e icona (calendario con "+") nel colore primario su un fondo appena
 * tinto; con la data: tondo pieno con il calendario spuntato in bianco. Una data per FINITURA (ottobre
 * 2026: prima era una sola per moneta, e una finitura aggiunta dopo ereditava o sovrascriveva la data
 * dell'altra). Il tocco apre il selettore standard di Material (non si scelgono date future) con, se la
 * data c'è, "Remove" per toglierla; la data è facoltativa e parte vuota.
 *
 * Varianti scartate dopo mockup: una scritta "Add date" da 12-14 sp sotto il nome ("troppo piccola e
 * difficile da cliccare", anche a 32 dp di area), un chip da 32 o 36 dp e una striscia a tutta larghezza
 * (+34 dp per card). Il tondo ha un bersaglio da 40 dp senza far crescere la card da 64 dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDateButton(
    epochDay: Long?,
    onChange: (Long?) -> Unit,
    /** Nome della finitura, per il lettore di schermo ("Standard", "BU"...). */
    finish: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val set = epochDay != null
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (set) colors.primary else colors.primary.copy(alpha = 0.08f))
            .then(if (set) Modifier else Modifier.border(1.5.dp, colors.primary, CircleShape))
            .clickable(role = Role.Button, onClickLabel = if (set) "Change date" else "Add date") { open = true }
            .semantics {
                contentDescription = if (epochDay != null) "$finish date: ${formatPurchaseDate(epochDay)}" else "Add date for $finish"
            },
    ) {
        Icon(
            imageVector = if (set) CalendarIcons.Check else CalendarIcons.Plus,
            contentDescription = null,
            tint = if (set) colors.onPrimary else colors.primary,
            modifier = Modifier.size(22.dp),
        )
    }
    if (open) {
        PurchaseDatePickerDialog(
            initial = epochDay,
            onDismiss = { open = false },
            onConfirm = {
                onChange(it)
                open = false
            },
            onRemove = {
                onChange(null)
                open = false
            },
        )
    }
}

/**
 * Data di una finitura nel dettaglio (sola lettura): icona del calendario + "12 Mar 2026", o "No date" in
 * grigio se manca. Senza "Bought": una moneta può essere stata trovata, ricevuta o ereditata.
 */
@Composable
fun PurchaseDateLine(epochDay: Long?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.CalendarToday,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = purchaseDateLabel(epochDay),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Selettore di data di Material, con il fondo `surface` come gli altri dialog (il default sarebbe un grigio-viola fuori palette). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDatePickerDialog(initial: Long?, onDismiss: () -> Unit, onConfirm: (Long) -> Unit, onRemove: () -> Unit) {
    val today = remember { LocalDate.now() }
    // Senza una data il selettore si apre con OGGI già scelto: "Add date" + OK basta per il caso "l'ho
    // trovata/presa oggi". La data della moneta resta comunque vuota finché non si preme OK (annullando
    // non si salva niente), quindi chi registra una collezione che ha già non si ritrova date inventate.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = epochDayToPickerMillis(initial ?: today.toEpochDay()),
        yearRange = FIRST_PURCHASE_YEAR..today.year,
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = isSelectablePurchaseDay(pickerMillisToEpochDay(utcTimeMillis), today)
                override fun isSelectableYear(year: Int) = year in FIRST_PURCHASE_YEAR..today.year
            }
        },
    )
    val colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(pickerMillisToEpochDay(it)) } ?: onDismiss() },
            ) { Text("OK") }
        },
        dismissButton = {
            // "Remove" solo se c'è già una data: toglierla non ha altro posto ora che il pulsante è un tondo.
            if (initial != null) TextButton(onClick = onRemove) { Text("Remove") }
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors)
    }
}
