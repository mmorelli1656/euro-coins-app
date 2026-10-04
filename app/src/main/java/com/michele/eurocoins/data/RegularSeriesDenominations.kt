package com.michele.eurocoins.data

/**
 * Un taglio mostrato in una serie.
 *
 * - [series]: la serie a cui il taglio APPARTIENE (chiave della collezione); diversa da quella
 *   guardata per i tagli rimasti invariati ([inherited]).
 * - [image]: l'immagine già RITAGLIATA alla finestra di tempo della serie guardata (vedi
 *   [denominationsOf]): `annoInizio`/`annoFine`, tirature e zecche per anno comprendono solo gli
 *   anni di questa serie. Tutto ciò che la UI ne ricava (elenco anni, somma delle tirature,
 *   tabella "by year") è quindi già nella finestra giusta, senza che ogni schermata ci pensi.
 */
data class SeriesDenomination(
    val series: RegularIssueSeries,
    val image: RegularIssueImage,
    val inherited: Boolean,
) {
    /** `true` se [year] cade nella finestra di questa serie (estremi mancanti = nessun limite). */
    fun contains(year: Int): Boolean =
        (image.annoInizio?.let { year >= it } ?: true) && (image.annoFine?.let { year <= it } ?: true)
}

/**
 * Gli 8 tagli di [selected], ciascuno nella finestra di tempo di [selected]: quelli propri più, per
 * i tagli che la serie NON ha cambiato, quelli ancora in circolazione delle serie precedenti. La
 * Francia 2022 cambia solo 1 e 2 euro e la 2024 solo 10-20-50 cent, ma nel portafoglio c'è sempre
 * il set intero: mostrare solo i tagli nuovi lasciava la serie con 2 o 3 monete.
 *
 * **Ogni serie è una finestra di tempo**: dal suo primo anno (il minimo degli `annoInizio` delle
 * sue immagini) alla vigilia della serie successiva che ha immagini. La serie 2 francese è
 * 2022-2023, e il suo 5 cent va dal 2022 al 2023 — non dal 1999 della serie 1, di cui è lo stesso
 * disegno: elenco anni, tirature e zecche si fermano ai confini della serie. Il taglio ereditato è
 * infatti la STESSA moneta (stessa chiave di collezione, stesso conteggio in Home): [series] è la
 * serie di origine, e le annate salvate da una finestra e dall'altra non si sovrappongono.
 * - **Inizio**: tagli ereditati partono dall'inizio della serie guardata; quelli propri dal loro
 *   `annoInizio`.
 * - **Fine**: un'immagine ancora aperta (`annoFine == null`, es. 5 cent francese serie 1) finisce
 *   alla vigilia della serie successiva; una con fine esplicita la mantiene (il Vaticano 2005
 *   appartiene sia alla serie 1 sia alla 2: tagliarla sull'inizio della successiva perderebbe
 *   l'annata).
 *
 * **Quando un taglio continua**: `annoFine == null` oppure `annoFine >=` inizio della serie
 * guardata (la Francia 2022 ha ancora i 10-20-50 cent "seminatore", fino al 2023, ma non la serie
 * 2024). Vince la serie precedente più recente.
 *
 * **Serie senza immagini proprie** (Vaticano 2026, non ancora fotografata): non eredita niente. Lì
 * i tagli cambiano TUTTI, e mostrare quelli della serie precedente sarebbe sbagliato; senza un
 * inizio noto non c'è nemmeno un criterio per decidere.
 */
fun denominationsOf(allForCountry: List<RegularIssueSeries>, selected: RegularIssueSeries): List<SeriesDenomination> {
    val start = selected.immagini.mapNotNull { it.annoInizio }.minOrNull()
        ?: return selected.immagini.map { SeriesDenomination(selected, it, inherited = false) }
    val end = allForCountry
        .filter { it.ordineCronologico > selected.ordineCronologico }
        .sortedBy { it.ordineCronologico }
        .firstNotNullOfOrNull { next -> next.immagini.mapNotNull { it.annoInizio }.minOrNull() }
        ?.minus(1)

    fun make(owner: RegularIssueSeries, image: RegularIssueImage, inherited: Boolean): SeriesDenomination {
        val from = if (inherited) maxOf(image.annoInizio ?: start, start) else image.annoInizio
        val to = when {
            image.annoFine == null -> end
            inherited -> minOf(image.annoFine, end ?: image.annoFine)
            else -> image.annoFine
        }
        fun inWindow(year: Int) = (from?.let { year >= it } ?: true) && (to?.let { year <= it } ?: true)
        return SeriesDenomination(
            series = owner,
            image = image.copy(
                annoInizio = from,
                annoFine = to,
                tirature = image.tirature.filter { inWindow(it.anno) },
                zecchePerAnno = image.zecchePerAnno.filter { inWindow(it.anno) },
            ),
            inherited = inherited,
        )
    }

    val own = selected.immagini.map { make(selected, it, inherited = false) }
    val covered = selected.immagini.map { it.taglio }.toMutableSet()
    val inherited = allForCountry
        .filter { it.ordineCronologico < selected.ordineCronologico }
        .sortedByDescending { it.ordineCronologico }
        .flatMap { earlier ->
            earlier.immagini
                .filter { image -> image.taglio !in covered && (image.annoFine?.let { it >= start } ?: true) }
                .onEach { covered += it.taglio }
                .map { make(earlier, it, inherited = true) }
        }
    return own + inherited
}
