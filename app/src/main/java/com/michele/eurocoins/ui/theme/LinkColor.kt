package com.michele.eurocoins.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Verdigris scuro per i link sul fondo chiaro: 6:1 sul grigio-verde, contro il 4:1 di `primary` (sotto AA per testo piccolo). */
private val LinkOnLight = Color(0xFF34503F)

/** Colore dei link nel testo piccolo: nel tema chiaro il verdigris scurito, nello scuro il primario (già chiaro). */
@Composable
fun linkColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) LinkOnLight else MaterialTheme.colorScheme.primary
