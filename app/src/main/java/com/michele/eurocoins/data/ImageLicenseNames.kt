package com.michele.eurocoins.data

/**
 * Testo inglese della licenza dell'immagine per la UI. La pipeline salva
 * `licenzaImmagine` in italiano (valori dell'enum `LicenzaImmagine`), e a
 * differenza del nome paese non conserva un originale inglese da cui
 * attingere: questa mappa è quindi una traduzione nostra.
 *
 * Nel dataset attuale ci sono solo due valori (495 + 4 monete). Il rischio
 * di disallineamento con l'enum della pipeline è coperto dal ripiego: un
 * valore non presente qui si mostra com'è (in italiano) invece di sparire o
 * far crashare, quindi si nota subito e basta aggiungere una riga. La
 * soluzione definitiva è che la pipeline emetta anche il testo inglese.
 */
private val IMAGE_LICENSE_NAMES = mapOf(
    "Copyright zecca emittente (uso editoriale)" to "Copyright of the issuing mint (editorial use)",
    "Sconosciuta - da verificare" to "Unknown, to be verified",
)

fun Coin.displayImageLicense(): String = displayImageLicense(licenzaImmagine)

/** Stessa mappa delle commemorative: `RegularIssueImage.licenzaImmagine` usa gli stessi valori italiani della pipeline. */
fun RegularIssueImage.displayImageLicense(): String = displayImageLicense(licenzaImmagine)

private fun displayImageLicense(raw: String): String = IMAGE_LICENSE_NAMES[raw] ?: raw
