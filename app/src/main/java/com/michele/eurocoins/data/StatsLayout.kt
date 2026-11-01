package com.michele.eurocoins.data

/**
 * Le card della scheda Statistics di Insights che l'utente Pro può mostrare, nascondere e riordinare (2026-11-01, mockup
 * approvato). [key] è il nome stabile salvato nelle preferenze: non si cambia, anche se il titolo mostrato cambia.
 */
enum class StatsCard(val key: String) {
    COLLECTION("collection"),
    PROGRESS("progress"),
    MILESTONES("milestones"),
    BADGES("badges"),
    SHINE("shine"),
    EXPENSES("expenses"),
}

/**
 * Ordine e card nascoste della scheda Statistics. Una preferenza di VISUALIZZAZIONE sul telefono (come il tema): non va nel
 * backup e non tocca i dati, che si calcolano comunque. Chi non è Pro vede sempre la pagina predefinita (la sfocata d'invito).
 *
 * Il salvataggio è tollerante, perché una card nuova aggiunta in un aggiornamento non deve sparire per chi ha già salvato:
 * i nomi sconosciuti si ignorano, i doppioni si tolgono e le card che mancano si accodano in fondo nell'ordine predefinito.
 */
data class StatsLayout(
    val order: List<StatsCard> = StatsCard.entries,
    val hidden: Set<StatsCard> = emptySet(),
) {
    /** Le card da disegnare, nell'ordine scelto. */
    val visible: List<StatsCard> get() = order.filter { it !in hidden }

    /** `true` = ordine e visibilità sono quelli di fabbrica (il Reset si spegne). */
    val isDefault: Boolean get() = order == StatsCard.entries && hidden.isEmpty()

    /**
     * Accende o spegne una card, anche il riepilogo (2026-11-01, scelta del proprietario: il blocco era una difesa contro la pagina
     * vuota, ma l'icona nella barra resta sempre e se le card sono tutte spente la pagina lo dice e offre Customize).
     */
    fun toggle(card: StatsCard): StatsLayout = copy(hidden = if (card in hidden) hidden - card else hidden + card)

    /** Sposta [card] alla posizione [to] (0-based, dentro i limiti). */
    fun move(card: StatsCard, to: Int): StatsLayout {
        val from = order.indexOf(card)
        if (from < 0) return this
        val target = to.coerceIn(0, order.lastIndex)
        if (target == from) return this
        val list = order.toMutableList()
        list.removeAt(from)
        list.add(target, card)
        return copy(order = list)
    }

    /** `collection,progress,...;shine` : prima l'ordine, dopo il punto e virgola le card nascoste. */
    fun encode(): String = order.joinToString(",") { it.key } + ";" + hidden.joinToString(",") { it.key }

    companion object {
        fun decode(text: String?): StatsLayout {
            if (text.isNullOrBlank()) return StatsLayout()
            val parts = text.split(";", limit = 2)
            val byKey = StatsCard.entries.associateBy { it.key }
            val saved = parts[0].split(",").mapNotNull { byKey[it.trim()] }.distinct()
            // Le card che il salvataggio non conosce (arrivate con un aggiornamento) vanno in fondo.
            val order = saved + StatsCard.entries.filter { it !in saved }
            val hidden = parts.getOrNull(1).orEmpty().split(",").mapNotNull { byKey[it.trim()] }.toSet()
            return StatsLayout(order, hidden)
        }
    }
}
