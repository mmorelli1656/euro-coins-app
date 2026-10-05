package com.michele.eurocoins.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Data di acquisto di una moneta in collezione: giorni dall'epoca Unix (`LocalDate.toEpochDay()`),
 * senza ora né fuso orario. Facoltativa, vuota di default: chi registra la collezione che già ha
 * non ricorda quando ha comprato, e "oggi" sarebbe un dato sbagliato.
 *
 * Nel database è salvata PER VOCE (una colonna in `collection_items` e in `regular_collection_items`),
 * anche se oggi la UI dà una sola data per moneta (per ogni anno, nelle Regular) valida per tutte le
 * finiture spuntate: un modello a più esemplari per moneta potrà dare una data a ciascuno senza migrare.
 */

/** Primo anno di euro: prima non esistono monete da collezionare, il selettore non scende sotto. */
const val FIRST_PURCHASE_YEAR = 1999

private val PURCHASE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** "12 Mar 2026". */
fun formatPurchaseDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(PURCHASE_FORMAT)

private const val MILLIS_PER_DAY = 86_400_000L

/** Il selettore di Material lavora in millisecondi UTC a mezzanotte: stesso giorno, qualunque fuso. */
fun epochDayToPickerMillis(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

fun pickerMillisToEpochDay(millis: Long): Long = Math.floorDiv(millis, MILLIS_PER_DAY)

/** Si può scegliere dal 1° gennaio 1999 a oggi: niente date nel futuro. */
fun isSelectablePurchaseDay(epochDay: Long, today: LocalDate): Boolean =
    epochDay in LocalDate.of(FIRST_PURCHASE_YEAR, 1, 1).toEpochDay()..today.toEpochDay()

/**
 * Riga del dettaglio sotto le finiture: "Bought 12 Mar 2026"; se le voci hanno date diverse
 * ("Bought on 2 different dates"); null se nessuna ha la data.
 */
fun purchaseLine(dates: Collection<Long?>): String? {
    val distinct = dates.filterNotNull().distinct()
    return when {
        distinct.isEmpty() -> null
        distinct.size == 1 -> "Bought ${formatPurchaseDate(distinct.first())}"
        else -> "Bought on ${distinct.size} different dates"
    }
}

/** Righe di data del dettaglio di una commemorativa (una sola moneta: una riga, o nessuna). */
fun commemorativePurchaseLines(items: List<CollectionItem>): List<String> =
    listOfNotNull(purchaseLine(items.map { it.purchasedOn }))

/**
 * Righe di data del dettaglio di una moneta circolante. Con una sola annata in collezione una riga
 * "Bought 12 Mar 2026"; con più annate una riga per annata CON data ("2002 · Bought 12 Mar 2026",
 * "2002 EFS · …"), perché ogni annata ha la sua.
 */
fun regularPurchaseLines(items: List<RegularCollectionItem>): List<String> {
    val byEntry = items.groupBy { it.anno to it.variety }.mapValues { (_, group) -> group.map { it.purchasedOn } }
    if (byEntry.values.none { dates -> dates.any { it != null } }) return emptyList()
    if (byEntry.size == 1) return listOfNotNull(purchaseLine(byEntry.values.first()))
    return byEntry.entries
        .sortedWith(compareBy({ it.key.first }, { it.key.second }))
        .mapNotNull { (key, dates) ->
            val line = purchaseLine(dates) ?: return@mapNotNull null
            val label = if (key.second.isEmpty()) "${key.first}" else "${key.first} ${key.second}"
            "$label · $line"
        }
}
