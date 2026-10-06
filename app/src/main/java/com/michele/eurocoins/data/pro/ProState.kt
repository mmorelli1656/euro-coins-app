package com.michele.eurocoins.data.pro

/**
 * Stato del Pro come lo vede la UI. [isPro] viene dall'ultimo esito noto del Play Store (salvato in
 * locale: un utente Pro offline non deve rivedere la pubblicità); [price] è il prezzo già formattato
 * da Play nella valuta dell'utente, vuoto finché il prodotto non è stato caricato; [message] è un
 * testo d'uso per l'utente (mai tecnico) dopo un acquisto o un ripristino.
 */
data class ProState(
    val isPro: Boolean = false,
    val price: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

/** Un acquisto del Play Store ridotto a ciò che serve alla decisione, per poterla provare senza la libreria. */
data class PurchaseSnapshot(val productIds: List<String>, val purchased: Boolean, val pending: Boolean)

enum class Ownership { OWNED, PENDING, NONE }

/**
 * Il prodotto Pro è posseduto solo con un acquisto COMPLETATO; un pagamento in sospeso (contanti,
 * bonifico: Play lo conferma dopo) non sblocca niente finché non si completa. Se ci sono entrambi
 * vince il completato.
 */
internal fun resolveOwnership(purchases: List<PurchaseSnapshot>, productId: String): Ownership {
    val mine = purchases.filter { productId in it.productIds }
    return when {
        mine.any { it.purchased } -> Ownership.OWNED
        mine.any { it.pending } -> Ownership.PENDING
        else -> Ownership.NONE
    }
}

/**
 * Il valore da salvare in locale dopo una verifica con Play. Una verifica che FALLISCE (rete,
 * servizio non raggiungibile) non è un "no": non deve togliere il Pro a chi lo ha pagato, quindi
 * [ownership] nullo lascia il valore com'era. Un esito certo di "nessun acquisto" (rimborso) lo toglie.
 */
internal fun resolveCachedPro(previous: Boolean, ownership: Ownership?): Boolean = when (ownership) {
    null -> previous
    Ownership.OWNED -> true
    Ownership.PENDING, Ownership.NONE -> false
}
