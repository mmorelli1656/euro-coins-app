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
 * e la UI ne dà una per FINITURA (per finitura di ogni annata e varietà, nelle Regular): prima era una
 * sola per moneta, e aggiungere una finitura dopo faceva ereditare o sovrascrivere la data dell'altra.
 * Un modello a più esemplari della stessa finitura (non costruito) dovrà cambiare la chiave primaria.
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
 * Riga di data sotto il nome di una finitura nel dettaglio: "12 Mar 2026", o "No date" se manca.
 * Senza "Bought": una moneta può essere stata trovata, ricevuta o ereditata, non solo comprata. Nella
 * UI la data è sempre preceduta dall'icona del calendario (`PurchaseDateLine`), come nel pannello.
 * Le righe di una card mostrano questa riga tutte insieme o nessuna (vedi [anyPurchaseDate]).
 */
fun purchaseDateLabel(epochDay: Long?): String =
    if (epochDay == null) "No date" else formatPurchaseDate(epochDay)

/** true se almeno una voce ha la data: solo allora le righe del dettaglio hanno la seconda riga. */
fun anyPurchaseDate(dates: Collection<Long?>): Boolean = dates.any { it != null }
