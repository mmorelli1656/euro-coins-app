package com.michele.eurocoins.ui.regular

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Metallo di un taglio: dà i colori della moneta disegnata. */
internal enum class CoinMetal(val outer: Color, val inner: Color) {
    COPPER(Color(0xFFC08A6B), Color(0xFFC08A6B)), // 1, 2, 5 cent
    NORDIC_GOLD(Color(0xFFD3B56C), Color(0xFFD3B56C)), // 10, 20, 50 cent
    BIMETAL_GOLD_RING(Color(0xFFD3B56C), Color(0xFFD9DBD9)), // 1 euro: anello oro, centro argento
    BIMETAL_SILVER_RING(Color(0xFFD9DBD9), Color(0xFFD3B56C)), // 2 euro: anello argento, centro oro
}

/** Diametro reale (mm) e metallo di un taglio. */
internal class CoinSpec(val diameterMm: Float, val metal: CoinMetal)

/** Il taglio più grande: la 2 euro, a cui è commisurata la moneta di dimensione piena. */
private const val LARGEST_DIAMETER_MM = 25.75f

/** Null per un taglio non riconosciuto (la card mostra solo lo spazio vuoto). */
internal fun coinSpecFor(taglio: String): CoinSpec? = when (taglio) {
    "1 cent" -> CoinSpec(16.25f, CoinMetal.COPPER)
    "2 cent" -> CoinSpec(18.75f, CoinMetal.COPPER)
    "5 cent" -> CoinSpec(21.25f, CoinMetal.COPPER)
    "10 cent" -> CoinSpec(19.75f, CoinMetal.NORDIC_GOLD)
    "20 cent" -> CoinSpec(22.25f, CoinMetal.NORDIC_GOLD)
    "50 cent" -> CoinSpec(24.25f, CoinMetal.NORDIC_GOLD)
    "1 euro" -> CoinSpec(23.25f, CoinMetal.BIMETAL_GOLD_RING)
    "2 euro" -> CoinSpec(25.75f, CoinMetal.BIMETAL_SILVER_RING)
    else -> null
}

/** Rapporto tra il diametro di [taglio] e quello della 2 euro (1.0 per la più grande). */
internal fun coinScaleFor(taglio: String): Float? = coinSpecFor(taglio)?.let { it.diameterMm / LARGEST_DIAMETER_MM }

/**
 * Moneta disegnata di un taglio, IN SCALA REALE: la 2 euro riempie [maxSize], le altre sono
 * proporzionali ai millimetri veri (1 cent = 63%). Metalli e colori come le monete vere, senza foto:
 * un taglio ha 41 disegni nazionali diversi, e una foto di un solo paese direbbe "la 2 euro è questa".
 * Sta in uno spazio di altezza [maxSize] allineato a sinistra, così i titoli sotto restano allineati
 * tra le card qualunque sia la moneta. Decorativa: nessuna descrizione per l'accessibilità.
 */
@Composable
internal fun DenominationCoin(taglio: String, modifier: Modifier = Modifier, maxSize: Dp = 44.dp) {
    Box(modifier = modifier.height(maxSize), contentAlignment = Alignment.CenterStart) {
        val spec = coinSpecFor(taglio) ?: return@Box
        val size = maxSize * (spec.diameterMm / LARGEST_DIAMETER_MM)
        val ink = Color(0xFF3B4A42)
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(spec.metal.outer)
                .border(size * 0.03f, ink.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Cerchio interno: bordo rilevato delle monete monometalliche, centro delle bimetalliche.
            Box(
                modifier = Modifier
                    .size(size * 0.66f)
                    .clip(CircleShape)
                    .background(spec.metal.inner)
                    .border(size * 0.02f, ink.copy(alpha = 0.3f), CircleShape),
            )
        }
    }
}
