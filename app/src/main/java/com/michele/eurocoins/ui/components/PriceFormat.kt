package com.michele.eurocoins.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

/** 1250 -> "12.50"; null -> "" (prezzo non indicato). */
fun formatPrice(cents: Int?): String =
    cents?.let { BigDecimal(it).movePointLeft(2).setScale(2).toPlainString() }.orEmpty()

/** "12,50" o "12.50" -> 1250; vuoto, non numerico o negativo -> null (prezzo non indicato). */
fun parsePriceCents(input: String): Int? {
    val value = input.trim().replace(',', '.')
    if (value.isEmpty()) return null
    return try {
        BigDecimal(value).takeIf { it.signum() >= 0 }?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.toInt()
    } catch (_: NumberFormatException) {
        null
    }
}
