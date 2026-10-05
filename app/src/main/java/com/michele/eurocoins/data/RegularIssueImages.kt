package com.michele.eurocoins.data

/**
 * Immagini della fonte che NON vanno mostrate come foto del singolo taglio, e il perche':
 * - `1-2-euro.png` della BCL (Lussemburgo, serie 2026 del Granduca Guglielmo): un'unica immagine con
 *   DUE monete affiancate (1 e 2 euro, 417x211), assegnata a entrambi i tagli. Ritagliata a cerchio
 *   mostrava un pezzo di ciascuna. Non esiste di meglio: la pagina BCE del Lussemburgo descrive ancora
 *   solo il Granduca Enrico, e Numista non e' usabile (licenza). I centesimi della stessa serie
 *   (`125-cents.png`, `102050.png`) sono invece monete singole e restano.
 * Al loro posto va il segnaposto generico (icona dell'euro), come per ogni taglio senza foto. Quando la
 * BCE pubblichera' le immagini vere basta toglierle da qui (o dalla pipeline, che andra' aggiornata).
 */
private val UNUSABLE_IMAGE_URLS = setOf(
    "https://www.bcl.lu/fr/media_actualites/communiques/2025/09/nouvelles-faces/1-2-euro.png",
)

/**
 * L'immagine com'e' o, se l'URL e' in [UNUSABLE_IMAGE_URLS], senza foto: stesso aspetto di un taglio
 * che la fonte non ha mai pubblicato (nessun URL, e senza fonte/licenza/credito dell'immagine, che
 * altrimenti comparirebbero nei crediti del dettaglio senza immagine da accreditare). Il testo, le
 * tirature e il resto del taglio non cambiano.
 */
fun RegularIssueImage.withUsableImage(): RegularIssueImage =
    if (urlImmagineFonte in UNUSABLE_IMAGE_URLS) {
        copy(urlImmagineFonte = null, fonteDati = "", licenzaImmagine = "", attribuzioneImmagineRaw = null)
    } else {
        this
    }

/** La serie con [withUsableImage] su ogni immagine; la stessa istanza se non cambia niente. */
fun RegularIssueSeries.withUsableImages(): RegularIssueSeries {
    val cleaned = immagini.map { it.withUsableImage() }
    return if (cleaned == immagini) this else copy(immagini = cleaned)
}
