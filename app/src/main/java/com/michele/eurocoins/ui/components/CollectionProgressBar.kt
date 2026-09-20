package com.michele.eurocoins.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.Progress

/** Barra "x / y possedute" usata su home, card anno e card paese. */
@Composable
fun CollectionProgressBar(
    progress: Progress,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outline,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = trackColor,
        )
        Text(
            text = "${progress.owned} / ${progress.total} collected",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
