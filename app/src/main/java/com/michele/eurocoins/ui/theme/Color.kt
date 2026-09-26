package com.michele.eurocoins.ui.theme

import androidx.compose.ui.graphics.Color

// Palette "moneta": verdigris (patina del bronzo) come colore primario,
// bronzo/oro come accento secondario per tirature/dati numerici.
val VerdigrisLight = Color(0xFF4F6B58)
val VerdigrisDark = Color(0xFF8BAB92)
val BronzeLight = Color(0xFF9C6A34)
val BronzeDark = Color(0xFFD0A05E)
val InkLight = Color(0xFF1F2620)
val InkDark = Color(0xFFEBE7D8)
val BackgroundLight = Color(0xFFBEC8BB)
val BackgroundDark = Color(0xFF0F110B)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF20231A)
val OutlineLight = Color(0xFF8A968A)
val OutlineDark = Color(0xFF4A4F3A)

// Piccolo accento viola del tema chiaro: selettore attivo, chip selezionati e cerchi delle
// monete senza foto (tutto il resto è grigio-verde). Il crema/beige di prima è stato tolto:
// tutto quel giallo rendeva le schermate "troppo crema" e con poco contrasto tra le card.
val LilacLight = Color(0xFFE2D9F3)

// Viola del campo prezzo nel pannello "My collection" (bordo a riposo, e "€" + bordo in focus):
// più scuro del lilla delle card, contrasto ~3.4:1 sul lilla (minimo WCAG per componenti UI: 3:1).
// Le varianti scure sono chiare, perché sul fondo scuro serve il contrario.
val PurpleFieldLight = Color(0xFF7A62B5)
val PurpleFieldFocusLight = Color(0xFF4B3391)
val PurpleFieldDark = Color(0xFFA995DB)
val PurpleFieldFocusDark = Color(0xFFCBBCF2)

// Tile Commemorative della Home nel tema scuro: verde profondo con testo crema, al posto del
// salvia chiaro (primary) che era la cosa più luminosa dell'app. Nel chiaro resta primary.
val TileDark = Color(0xFF2F4A38)
