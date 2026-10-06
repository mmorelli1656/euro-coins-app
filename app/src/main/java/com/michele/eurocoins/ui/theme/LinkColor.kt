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

/**
 * Verdigris ancora più scuro per le ICONE di link dei crediti (`SourceCredits`), che stanno sul fondo
 * grigio-verde e non dentro una card bianca: 7:1 contro il ~5:1 di [LinkOnLight]. Separato da
 * [linkColor] per non scurire anche le etichette di sezione e i pulsanti testuali, che stanno su card bianche.
 */
private val CreditIconOnLight = Color(0xFF253A2D)

/** Colore delle icone di link dei crediti: nel tema chiaro il verdigris molto scuro, nello scuro il primario (già chiaro). */
@Composable
fun creditIconColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) CreditIconOnLight else MaterialTheme.colorScheme.primary
