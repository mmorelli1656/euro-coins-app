package com.michele.eurocoins.ui.components

import android.graphics.Bitmap
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.transform.Transformation
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Intensità della nitidezza delle miniature (0 = nessuna; sopra ~1.2 compare un alone sui bordi). */
const val THUMBNAIL_SHARPEN_AMOUNT = 0.9f

/**
 * Nitidezza locale (unsharp mask) delle miniature negli elenchi: a 64 dp le scritte e i rilievi di una
 * moneta sono a pochi pixel, e il contrasto globale ([ThumbnailColorFilter]) sposta solo i toni,
 * non aiuta i bordi. Prima riduce la foto alla misura richiesta (168 px sul telefono dell'utente)
 * e solo dopo rende nitido: fatto a misura finale l'effetto è quello visto nei confronti, a 270 o
 * 540 px verrebbe sfocato dal ridimensionamento successivo di Compose. Non ingrandisce mai. Scelto
 * dopo un confronto su 14 monete (originale / contrasto 1.2 / solo nitidezza / nitidezza + 1.2),
 * vedi CLAUDE.md § Decisioni di prodotto, "Visibilità delle miniature". Il risultato sta nella cache
 * in memoria di Coil (la chiave contiene la trasformazione), quindi si paga una volta per foto.
 */
object ThumbnailSharpen : Transformation() {
    override val cacheKey: String = "ThumbnailSharpen-$THUMBNAIL_SHARPEN_AMOUNT"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val targetW = size.width.pxOrElse { input.width }
        val targetH = size.height.pxOrElse { input.height }
        val scale = min(1f, min(targetW.toFloat() / input.width, targetH.toFloat() / input.height))
        val w = max(1, (input.width * scale).roundToInt())
        val h = max(1, (input.height * scale).roundToInt())
        val scaled = if (w != input.width || h != input.height) {
            Bitmap.createScaledBitmap(input, w, h, true)
        } else {
            input
        }
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)
        val sharp = unsharpMask(pixels, w, h, THUMBNAIL_SHARPEN_AMOUNT)
        return Bitmap.createBitmap(sharp, w, h, Bitmap.Config.ARGB_8888)
    }
}

/** Intensità della nitidezza della foto grande del dettaglio: più bassa di quella delle miniature (vedi [DetailSharpen]). */
const val DETAIL_SHARPEN_AMOUNT = 0.45f

/**
 * Nitidezza leggera della foto grande dei dettagli (commemorative e Regular Issues). Qui il difetto non
 * è la perdita di dettaglio per la riduzione, come nelle miniature, ma la morbidezza di un
 * INGRANDIMENTO: le foto BCE delle commemorative sono 270 px e il riquadro è ~840 px sul telefono
 * dell'utente (circa 3 volte). Per questo la nitidezza è fatta sulla foto alla sua risoluzione
 * originale, SENZA ridimensionarla, e solo dopo la scala Compose: gli aloni sarebbero larghi tre
 * volte se fosse fatta dopo; l'intensità è 0.45 (0.4-0.6 provato su Germania 2006, Finlandia 2009 e
 * Turingia 2022: a 0.6 compaiono contorni più duri). Sulle foto da 540 px dà poco, e non
 * schiarisce le ombre delle monete scure. Niente contrasto: a questa grandezza il bordo si legge
 * già e alterare i colori reali sarebbe un difetto in un'app da collezionisti.
 */
object DetailSharpen : Transformation() {
    override val cacheKey: String = "DetailSharpen-$DETAIL_SHARPEN_AMOUNT"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = input.width
        val h = input.height
        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)
        val sharp = unsharpMask(pixels, w, h, DETAIL_SHARPEN_AMOUNT)
        return Bitmap.createBitmap(sharp, w, h, Bitmap.Config.ARGB_8888)
    }
}
