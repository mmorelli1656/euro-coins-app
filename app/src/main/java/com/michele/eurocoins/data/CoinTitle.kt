package com.michele.eurocoins.data

private val ORDINAL_SUFFIX = Regex("""(?<=\d)(Th|St|Nd|Rd)\b""")

/**
 * Titolo della moneta per la UI. Alcune pagine BCE mettono in maiuscola anche la
 * desinenza degli ordinali ("550Th", ora unico caso nel dataset): qui torna "550th".
 * Solo dopo una cifra, per non toccare parole come "Third" o "Stand".
 */
fun Coin.displayTema(): String =
    tema.replace(ORDINAL_SUFFIX) { it.value.lowercase() }

/**
 * Nota "ABOUT THIS COIN" per la UI, sempre come frase: iniziale maiuscola e punto finale (le fonti
 * a volte la scrivono come seguito di una frase, "the twelve stars of…", o la lasciano senza punto).
 * Null se la moneta non ha nota. Solo in visualizzazione, `noteStoriche` resta com'è nel dato.
 */
fun Coin.displayNotes(): String? = noteStoriche?.trim()?.takeIf { it.isNotEmpty() }?.asSentence()
