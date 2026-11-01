package com.michele.eurocoins.ui.insights

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * I simboli disegnati a mano delle medaglie che Material non ha (2026-11-01, mockup approvato): le monete che si impilano
 * (una moneta, una colonna, due, tre: la scala delle soglie, senza numeri) e il cerchio delle dodici stelle europee. Tutto
 * a tratto sottile come le icone Material delle altre medaglie, su una griglia di 34 unità riportata alla misura reale.
 */
internal enum class BadgeArt { COIN, STACK_100, STACK_250, STACK_500, EU_STARS }

private const val GRID = 34f
private const val COIN_H = 3.5f
private const val COIN_STEP = 4.5f
private const val BASE = 30f

internal fun DrawScope.drawBadgeArt(art: BadgeArt, color: Color) {
    val k = size.minDimension / GRID
    val stroke = Stroke(width = 1.5f * k)

    fun coin(x: Float, y: Float, w: Float, h: Float) = drawRoundRect(
        color = color,
        topLeft = Offset(x * k, y * k),
        size = Size(w * k, h * k),
        cornerRadius = CornerRadius(1.6f * k),
        style = stroke,
    )

    // Una colonna di [n] monete che parte dal fondo (BASE) e sale.
    fun column(x: Float, w: Float, n: Int) {
        for (i in 0 until n) coin(x, BASE - (i + 1) * COIN_STEP + 1f, w, COIN_H)
    }

    when (art) {
        // Una moneta di fronte (cerchio e anello interno), non di taglio: una barretta sola si leggeva come un segno "meno".
        BadgeArt.COIN -> {
            drawCircle(color, radius = 10f * k, center = Offset(17f * k, 17f * k), style = stroke)
            drawCircle(color, radius = 5.8f * k, center = Offset(17f * k, 17f * k), style = stroke)
        }
        BadgeArt.STACK_100 -> column(8f, 18f, 4)
        BadgeArt.STACK_250 -> {
            column(3f, 13f, 3)
            column(18f, 13f, 5)
        }
        BadgeArt.STACK_500 -> {
            column(1.5f, 9.5f, 4)
            column(12.25f, 9.5f, 7)
            column(23f, 9.5f, 5)
        }
        BadgeArt.EU_STARS -> {
            // Dodici punti su un cerchio, come le stelle della bandiera (e del bordo della 2 euro).
            for (i in 0 until 12) {
                val angle = i * PI.toFloat() / 6f
                drawCircle(
                    color = color,
                    radius = 2.0f * k,
                    center = Offset((17f + 13f * sin(angle)) * k, (17f - 13f * cos(angle)) * k),
                )
            }
        }
    }
}
