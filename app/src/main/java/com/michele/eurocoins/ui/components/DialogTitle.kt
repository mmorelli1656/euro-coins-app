package com.michele.eurocoins.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Titolo dei dialog: Bold 20 sp come i titoli del resto dell'app. Il default di Material (24 sp
 * a peso normale) risultava sottile e fuori gerarchia accanto ai titoli in grassetto.
 */
@Composable
fun DialogTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp))
}
