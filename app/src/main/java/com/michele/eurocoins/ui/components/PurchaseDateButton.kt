package com.michele.eurocoins.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.FIRST_PURCHASE_YEAR
import com.michele.eurocoins.data.epochDayToPickerMillis
import com.michele.eurocoins.data.formatPurchaseDate
import com.michele.eurocoins.data.isSelectablePurchaseDay
import com.michele.eurocoins.data.pickerMillisToEpochDay
import java.time.LocalDate

/**
 * Data di acquisto nei pannelli di collezione: un pulsante di SOLO TESTO con l'icona del calendario,
 * "Add date" (verdigris: è azionabile) oppure la data scelta (colore normale) con una piccola ✕ che la
 * toglie. Sta sulla riga che c'è già (il sottotitolo nelle commemorative, la riga dell'anno nelle
 * Regular), così non aggiunge altezza al pannello: una riga o una card in più la appesantivano.
 * Il tocco apre il selettore standard di Material (non si scelgono date future); la data è
 * facoltativa e parte vuota.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDateButton(
    epochDay: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button, onClickLabel = "Choose purchase date") { open = true }
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = null,
                tint = if (epochDay == null) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = epochDay?.let { formatPurchaseDate(it) } ?: "Add date",
                style = MaterialTheme.typography.labelLarge,
                color = if (epochDay == null) colors.primary else colors.onSurface,
                maxLines = 1,
            )
        }
        if (epochDay != null) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear purchase date",
                tint = colors.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onChange(null) }
                    .padding(8.dp),
            )
        }
    }
    if (open) {
        PurchaseDatePickerDialog(
            initial = epochDay,
            onDismiss = { open = false },
            onConfirm = {
                onChange(it)
                open = false
            },
        )
    }
}

/** Selettore di data di Material, con il fondo `surface` come gli altri dialog (il default sarebbe un grigio-viola fuori palette). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDatePickerDialog(initial: Long?, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val today = remember { LocalDate.now() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.let { epochDayToPickerMillis(it) },
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors)
    }
}
