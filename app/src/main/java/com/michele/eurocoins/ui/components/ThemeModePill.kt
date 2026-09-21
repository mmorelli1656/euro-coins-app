package com.michele.eurocoins.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.ui.theme.ThemeMode

/**
 * Pillola a tre icone (sole, luna, auto) per scegliere il tema: un tocco applica
 * subito la scelta e l'opzione attiva resta evidenziata. "Auto" = segue il telefono.
 * Sta nella barra della home: la scelta è rara, non serve una schermata.
 */
@Composable
fun ThemeModePill(mode: ThemeMode, onModeChange: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(2.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillOption(ThemeMode.LIGHT, Icons.Filled.LightMode, "Light theme", mode, onModeChange)
        PillOption(ThemeMode.DARK, Icons.Filled.DarkMode, "Dark theme", mode, onModeChange)
        // Auto per ultimo: i due temi sono affiancati e "segui il telefono" (predefinito) è a parte.
        PillOption(ThemeMode.SYSTEM, Icons.Filled.BrightnessAuto, "Follow system theme", mode, onModeChange)
    }
}

@Composable
private fun PillOption(
    option: ThemeMode,
    icon: ImageVector,
    description: String,
    current: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
) {
    val selected = option == current
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onModeChange(option) }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
