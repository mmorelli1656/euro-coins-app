package com.michele.eurocoins.data

/**
 * La frase standard della Commissione europea sul bordo esterno ("The coin's outer ring depicts the
 * 12 stars of the European flag."): identica in 7 serie su 41 (Andorra, Belgio, Lituania, Paesi
 * Bassi) e uguale su tutte le monete dell'Eurozona, quindi non dice niente su QUESTA serie.
 * Non tocca la frase belga "not in the outer ring" né quella spagnola sulle dodici stelle, che
 * hanno un'altra forma e un altro contenuto.
 */
private val OUTER_RING_BOILERPLATE =
    Regex("""\s*The coin['’]s outer ring depicts the 12 stars of the European flag\.""")

/**
 * Descrizione della serie per la UI, senza la frase standard sul bordo esterno. Solo in
 * visualizzazione: `descrizione` resta com'è nel dataset e nel database, e l'asset non cambia
 * (quindi nessun ripopolamento).
 */
fun RegularIssueSeries.displayDescription(): String =
    descrizione.replace(OUTER_RING_BOILERPLATE, "").trim()

/**
 * Titolo della serie in UI: sempre "Series N", mai l'intestazione della fonte (`intestazioneRaw`:
 * "2022 – second series 1 and 2 euro coins", "Coat of arms / Leo XIV"...), lunga e disomogenea da
 * paese a paese. [number] è la posizione (1-based) della serie nella lista del paese, NON
 * `numeroSerieIpotesi`: quel campo raggruppa varianti minori sotto lo stesso numero (Belgio: 2002 e
 * 2008 sono entrambe "1") e può ripetersi, due chip dicevano entrambi "Series 1".
 */
fun seriesTitle(number: Int): String = "Series $number"

/** Primo anno di circolazione dell'euro: le monete datate 1999-2001 (Belgio, Francia...) non fanno partire una serie prima. */
private const val FIRST_CIRCULATION_YEAR = 2002

/** Periodo di una serie: [to] null = serie ancora aperta; [from] null = inizio non ricavabile dai dati. */
data class SeriesPeriod(val from: Int?, val to: Int?) {
    /** "2022 – 2023", "2005" se è un anno solo, "2024 – today" se aperta, null senza inizio. */
    val label: String?
        get() = when {
            from == null -> null
            to == null -> "$from – today"
            to == from -> "$from"
            else -> "$from – $to"
        }
}

/**
 * Periodo di [series] dentro [allForCountry]. Inizio: il primo anno delle sue immagini (non prima
 * del 2002); senza immagini (Vaticano 2026, non ancora fotografato) l'anno dopo la fine della
 * serie precedente. Fine: se TUTTE le sue immagini hanno una fine esplicita, la più tarda; se
 * qualcuna è ancora aperta (Francia 2002: i 5 cent non sono mai cambiati) è la vigilia della serie
 * successiva, e senza successiva la serie è aperta. Stessa logica delle finestre di tempo di
 * [denominationsOf], che però lavora per singolo taglio.
 */
fun seriesPeriod(allForCountry: List<RegularIssueSeries>, series: RegularIssueSeries): SeriesPeriod {
    val ordered = allForCountry.sortedBy { it.ordineCronologico }
    fun start(s: RegularIssueSeries): Int? = s.immagini.mapNotNull { it.annoInizio }.minOrNull()?.coerceAtLeast(FIRST_CIRCULATION_YEAR)
    fun period(s: RegularIssueSeries): SeriesPeriod {
        val from = start(s) ?: ordered.lastOrNull { it.ordineCronologico < s.ordineCronologico }
            ?.let { previous -> period(previous).to?.plus(1) }
        val to = when {
            s.immagini.isEmpty() -> null
            s.immagini.all { it.annoFine != null } -> s.immagini.maxOf { it.annoFine!! }
            else -> ordered.firstOrNull { it.ordineCronologico > s.ordineCronologico && start(it) != null }
                ?.let { next -> start(next)!! - 1 }
        }
        return SeriesPeriod(from, to)
    }
    return period(series)
}

/** Titolo del chip: "Series 2 · 2022", solo "Series 2" se l'inizio non è noto. */
fun seriesChipLabel(number: Int, period: SeriesPeriod): String =
    period.from?.let { "${seriesTitle(number)} · $it" } ?: seriesTitle(number)

/**
 * Etichetta della card della descrizione: "ABOUT THIS SERIES" se il testo è solo di [series],
 * "ABOUT SERIES 1–3" se lo stesso testo compare identico su più serie dello stesso paese (Belgio,
 * Francia, Spagna, Paesi Bassi, Vaticano 1-5, Monaco 1-2: la Commissione europea pubblica UN testo
 * per paese e la pipeline lo copia su ogni serie). Senza questo, passando da un chip all'altro la
 * stessa descrizione sembra un errore; con l'etichetta dice invece da sé di essere comune. I numeri
 * sono le posizioni (1-based) nella lista del paese, come nei chip.
 */
fun seriesTextLabel(allForCountry: List<RegularIssueSeries>, series: RegularIssueSeries): String {
    val ordered = allForCountry.sortedBy { it.ordineCronologico }
    val text = series.displayDescription()
    val numbers = ordered.mapIndexedNotNull { index, s -> (index + 1).takeIf { s.displayDescription() == text } }
    if (numbers.size < 2) return "ABOUT THIS SERIES"
    val consecutive = numbers.zipWithNext().all { (a, b) -> b == a + 1 }
    return if (consecutive) "ABOUT SERIES ${numbers.first()}–${numbers.last()}" else "ABOUT SERIES ${numbers.joinToString(", ")}"
}
