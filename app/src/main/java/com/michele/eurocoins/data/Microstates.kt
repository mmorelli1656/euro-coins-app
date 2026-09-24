package com.michele.eurocoins.data

/**
 * I quattro microstati dell'Eurozona che l'utente può nascondere dal catalogo
 * (Impostazioni → "Hide microstates"). Sono i valori stabili di `Coin.paese`,
 * non i nomi mostrati (vedi CountryNames.kt).
 */
val MICROSTATE_PAESI: Set<String> = setOf("Andorra", "Città del Vaticano", "Monaco", "San Marino")

val Coin.isMicrostate: Boolean get() = paese in MICROSTATE_PAESI
