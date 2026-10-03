package com.michele.eurocoins.data

import java.util.Locale

/**
 * Nome leggibile di una fonte dati nei crediti: `fonteDati` arriva dalla pipeline come slug
 * (`ec_national_sides`, `ecb`...) e finiva a schermo così com'era in maiuscolo. Valore sconosciuto:
 * lo slug in maiuscolo, per notarlo e aggiungere una riga qui.
 */
fun displaySourceName(fonte: String): String = when (fonte.lowercase(Locale.ENGLISH)) {
    "ecb" -> "ECB"
    "bcl" -> "BCL"
    "ec_national_sides" -> "European Commission"
    "vaticanstate_cfn" -> "Vatican City State (CFN)"
    "monaco_tribune" -> "Monaco Tribune"
    else -> fonte.uppercase(Locale.ENGLISH)
}

/** Fonte del testo introduttivo della serie, mostrato nella schermata del paese. */
fun RegularIssueSeries.displayTextSource(): String = displaySourceName(fonteDati)

/** Pagina del type Numista, per il link accanto al N#. */
fun numistaUrl(numistaId: Int): String = "https://en.numista.com/$numistaId"
