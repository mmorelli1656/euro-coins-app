package com.michele.eurocoins.ui.insights

/**
 * Dati fissi della scheda "The euro" di Insights (2026-10-25): storia dell'euro, materiali delle monete e lato
 * comune. Non vengono dalla pipeline né da Numista: sono fatti pubblici ricavati dalle pagine della BCE, della
 * Commissione europea e della Bundesbank (URL in [EURO_SOURCES]). Verificati a mano il 2026-10-22/25: se cambiano
 * (un nuovo paese entra nell'euro) si aggiornano qui, a mano, e `EuroFactsTest` ricorda i conti.
 */

/**
 * Un paese dell'area euro. [rate] è quanto vale 1 euro in [code], scritto come lo scrive la BCE. [joined] è quando
 * ha adottato l'euro, [coins] quando sono arrivati monete e banconote. Il riquadro ha quattro righe uguali per tutti:
 * c'era una quinta ("Only euro from", l'ultimo giorno di doppia circolazione) solo per i paesi dal 2007, perché per i
 * primi 12 la data non era verificata; tolta il 2026-10-25 perché disomogenea e poco chiara.
 */
internal class EuroMember(
    val iso: String,
    val name: String,
    val former: String,
    val code: String,
    val rate: String,
    val joined: String,
    val coins: String,
)

/**
 * Una voce della storia: l'anno e un sottotesto, SEMPRE (niente titoli: l'anno fa da titolo; e dove c'era solo l'anno
 * con le bandiere, accanto a voci con il sottotesto, "stonava": scelta del 2026-10-25). Per gli ingressi il sottotesto
 * dice il numero d'ordine ("The 16th country joins."), che non ripete i nomi dei paesi. [members] sono i paesi
 * entrati quell'anno (vuoto per una tappa come le monete del 2002). [maps]: la voce porta il confronto della mappa
 * prima e dopo il 2007 (a comparsa).
 */
internal class EuroEntry(
    val year: String,
    val text: String,
    val members: List<EuroMember>,
    val maps: Boolean = false,
)

/** Una tappa verso la moneta unica, prima del 1999. */
internal class RoadStep(val year: String, val title: String, val text: String)

/**
 * Uno stato che usa l'euro per accordo con l'UE, senza essere nell'area euro. Ha le stesse righe di un paese dell'area
 * ([since] = "Uses the euro since", [coins], [replaced], [rate]; scelta del 2026-10-25: nel riquadro non devono mancare
 * le informazioni degli altri paesi) più due in più: [ownCoinsFrom] e [agreement]. [replaced] e [rate] sono già testo:
 * Monaco aveva il franco francese, San Marino e il Vaticano la lira, Andorra non aveva una moneta sua e usava franco e
 * peseta senza che avessero corso legale.
 */
internal class AgreementState(
    val iso: String,
    val name: String,
    val since: String,
    val coins: String,
    val replaced: String,
    val rate: String,
    val ownCoinsFrom: String,
    val agreement: String,
)

/** Solo le tappe che servono a capire come si è arrivati al 1999: una riga ciascuna. */
internal val ROAD_STEPS = listOf(
    RoadStep("1957", "Treaty of Rome", "Creates the European Economic Community."),
    RoadStep("1979", "European Monetary System", "Keeps currencies within a narrow band."),
    RoadStep("1989", "Delors Report", "3 stages to a single currency."),
    RoadStep("1992", "Maastricht Treaty", "The legal basis for the euro."),
    RoadStep("1995", "The name \"euro\"", "Leaders pick the name and the start date."),
    RoadStep("1998", "The first eleven", "11 countries are chosen and the ECB starts."),
)

/** La voce chiusa che contiene le tappe, nello stesso formato delle altre: anno e sottotesto. */
internal const val ROAD_YEARS = "1957–1998"
internal const val ROAD_TEXT = "6 steps to the single currency."

private const val FIRST_GROUP = "1 Jan 1999"
private const val COINS_2002 = "1 Jan 2002"

internal val EURO_ENTRIES = listOf(
    EuroEntry(
        year = "1999",
        text = "The euro starts, as money in accounts and payments. Coins and notes come later.",
        members = listOf(
            EuroMember("AT", "Austria", "Austrian schilling", "ATS", "13.7603", FIRST_GROUP, COINS_2002),
            EuroMember("BE", "Belgium", "Belgian franc", "BEF", "40.3399", FIRST_GROUP, COINS_2002),
            EuroMember("FI", "Finland", "Finnish markka", "FIM", "5.94573", FIRST_GROUP, COINS_2002),
            EuroMember("FR", "France", "French franc", "FRF", "6.55957", FIRST_GROUP, COINS_2002),
            EuroMember("DE", "Germany", "Deutsche Mark", "DEM", "1.95583", FIRST_GROUP, COINS_2002),
            EuroMember("IE", "Ireland", "Irish pound", "IEP", "0.787564", FIRST_GROUP, COINS_2002),
            EuroMember("IT", "Italy", "Italian lira", "ITL", "1,936.27", FIRST_GROUP, COINS_2002),
            EuroMember("LU", "Luxembourg", "Luxembourg franc", "LUF", "40.3399", FIRST_GROUP, COINS_2002),
            EuroMember("NL", "Netherlands", "Dutch guilder", "NLG", "2.20371", FIRST_GROUP, COINS_2002),
            EuroMember("PT", "Portugal", "Portuguese escudo", "PTE", "200.482", FIRST_GROUP, COINS_2002),
            EuroMember("ES", "Spain", "Spanish peseta", "ESP", "166.386", FIRST_GROUP, COINS_2002),
        ),
    ),
    EuroEntry("2001", "The 12th country joins. Its coins and notes follow in 2002.", listOf(EuroMember("GR", "Greece", "Greek drachma", "GRD", "340.750", "1 Jan 2001", COINS_2002))),
    // Una tappa senza paesi: è qui che l'euro arriva in tasca (le banconote e le monete dei 12).
    EuroEntry("2002", "Coins and notes begin, in the 12 countries.", emptyList()),
    EuroEntry(
        year = "2007",
        text = "The 13th country joins, with coins and notes from the first day. " +
            "It is the first to use the redrawn common side.",
        members = listOf(EuroMember("SI", "Slovenia", "Slovenian tolar", "SIT", "239.640", "1 Jan 2007", "1 Jan 2007")),
        maps = true,
    ),
    EuroEntry(
        "2008", "The 14th and 15th countries join.",
        listOf(
            EuroMember("CY", "Cyprus", "Cypriot pound", "CYP", "0.585274", "1 Jan 2008", "1 Jan 2008"),
            EuroMember("MT", "Malta", "Maltese lira", "MTL", "0.429300", "1 Jan 2008", "1 Jan 2008"),
        ),
    ),
    EuroEntry("2009", "The 16th country joins.", listOf(EuroMember("SK", "Slovakia", "Slovak koruna", "SKK", "30.1260", "1 Jan 2009", "1 Jan 2009"))),
    EuroEntry("2011", "The 17th country joins.", listOf(EuroMember("EE", "Estonia", "Estonian kroon", "EEK", "15.6466", "1 Jan 2011", "1 Jan 2011"))),
    EuroEntry("2014", "The 18th country joins.", listOf(EuroMember("LV", "Latvia", "Latvian lats", "LVL", "0.702804", "1 Jan 2014", "1 Jan 2014"))),
    EuroEntry("2015", "The 19th country joins.", listOf(EuroMember("LT", "Lithuania", "Lithuanian litas", "LTL", "3.45280", "1 Jan 2015", "1 Jan 2015"))),
    EuroEntry("2023", "The 20th country joins.", listOf(EuroMember("HR", "Croatia", "Croatian kuna", "HRK", "7.53450", "1 Jan 2023", "1 Jan 2023"))),
    EuroEntry("2026", "The 21st country joins.", listOf(EuroMember("BG", "Bulgaria", "Bulgarian lev", "BGN", "1.95583", "1 Jan 2026", "1 Jan 2026"))),
)

/** Gli anni delle prime monete proprie: 2002 per Monaco, San Marino e Vaticano, 2014 per Andorra. */
internal val EURO_AGREEMENTS = listOf(
    AgreementState(
        "AD", "Andorra", since = "2002", coins = COINS_2002,
        // Due monete: una per riga, a destra, invece di una frase lunga che va a capo a metà parola.
        replaced = "French franc (FRF)\nSpanish peseta (ESP)",
        rate = "€1 = 6.55957 FRF\n€1 = 166.386 ESP",
        ownCoinsFrom = "Jan 2014", agreement = "Signed 30 Jun 2011",
    ),
    AgreementState(
        "MC", "Monaco", since = FIRST_GROUP, coins = COINS_2002,
        replaced = "French franc (FRF)", rate = "€1 = 6.55957 FRF",
        ownCoinsFrom = "2002", agreement = "In force 1 Dec 2011",
    ),
    AgreementState(
        "SM", "San Marino", since = FIRST_GROUP, coins = COINS_2002,
        replaced = "Italian lira (ITL)", rate = "€1 = 1,936.27 ITL",
        ownCoinsFrom = "2002", agreement = "In force 1 Sep 2012",
    ),
    AgreementState(
        "VA", "Vatican City", since = FIRST_GROUP, coins = COINS_2002,
        replaced = "Italian lira (ITL)", rate = "€1 = 1,936.27 ITL",
        ownCoinsFrom = "2002", agreement = "In force 1 Jan 2010",
    ),
)

/**
 * Una moneta con le misure ufficiali della BCE e la spiegazione del suo materiale: è ciò che mostra la card sotto le
 * otto monete (la versione "una card che spiega" piaciuta al proprietario, al posto delle tre famiglie).
 * Misure come stringhe già pronte (mm e g).
 */
internal class CoinMaterial(
    val taglio: String,
    val title: String,
    val text: String,
    val diameter: String,
    val weight: String,
    val thickness: String,
    val edge: String,
    val magnet: String,
)

private const val STEEL_TITLE = "Copper-covered steel"
private const val STEEL_TEXT = "A steel disc with a copper covering. The steel is why a magnet picks the coin up."
private const val NORDIC_TITLE = "Nordic gold"
private const val NORDIC_TEXT = "A gold-coloured alloy of copper, aluminium, zinc and tin. There is no gold in it. " +
    "The ECB says it is difficult to melt and used only for coins."
private const val TWO_METALS_TITLE = "Two metals"

/** Dall'1 cent al 2 euro; l'ordine è quello del selettore. */
internal val COIN_MATERIALS = listOf(
    CoinMaterial("1 cent", STEEL_TITLE, STEEL_TEXT, "16.25", "2.30", "1.67", "Smooth", "Strongly magnetic"),
    CoinMaterial("2 cent", STEEL_TITLE, STEEL_TEXT, "18.75", "3.06", "1.67", "Smooth with a groove", "Strongly magnetic"),
    CoinMaterial("5 cent", STEEL_TITLE, STEEL_TEXT, "21.25", "3.92", "1.67", "Smooth", "Strongly magnetic"),
    CoinMaterial("10 cent", NORDIC_TITLE, NORDIC_TEXT, "19.75", "4.10", "1.93", "Fine scallops", "Not magnetic"),
    CoinMaterial("20 cent", NORDIC_TITLE, NORDIC_TEXT, "22.25", "5.74", "2.14", "Plain (flower shape)", "Not magnetic"),
    CoinMaterial("50 cent", NORDIC_TITLE, NORDIC_TEXT, "24.25", "7.80", "2.38", "Fine scallops", "Not magnetic"),
    CoinMaterial(
        taglio = "1 euro",
        title = TWO_METALS_TITLE,
        text = "A gold-coloured ring of nickel brass around a silver-coloured centre in three layers: cupronickel, " +
            "nickel and cupronickel. Nickel brass is brass, which is copper and zinc, with nickel added. " +
            "Cupronickel is copper with nickel.",
        diameter = "23.25", weight = "7.50", thickness = "2.33", edge = "Interrupted milled",
        magnet = "Centre slightly magnetic",
    ),
    CoinMaterial(
        taglio = "2 euro",
        title = TWO_METALS_TITLE,
        text = "A silver-coloured ring of cupronickel around a gold-coloured centre in three layers: nickel brass, " +
            "nickel and nickel brass. Cupronickel is copper with nickel. " +
            "Nickel brass is brass, which is copper and zinc, with nickel added.",
        diameter = "25.75", weight = "8.50", thickness = "2.20", edge = "Edge lettering, fine milled",
        magnet = "Centre slightly magnetic",
    ),
)

/** Frase sotto i materiali: le fonti nominano i metalli ma non danno le percentuali, e non le inventiamo. */
internal const val MATERIALS_NOTE = "The ECB and the Bundesbank name the metals but give no percentages."

/** Sotto le due foto del 2007, a comparsa: cosa è cambiato e perché, in due frasi. */
internal const val COMMON_SIDE_TEXT = "The old map drew the 15 EU countries as separate pieces, with gaps. " +
    "The new one is a single Europe, from Iberia to the east. The EU had grown on 1 May 2004. " +
    "It applies to 10, 20, 50 cent, 1 and 2 euro."

private const val ECB_COMMON_IMAGES = "https://www.ecb.europa.eu/euro/coins/common/shared/img"

/** Le foto BCE del 50 cent prima e dopo il 2007 (hotlink, come le altre foto: mai ospitate nell'APK). */
internal const val COMMON_SIDE_OLD_URL = "$ECB_COMMON_IMAGES/common_50cent_800.jpg"
internal const val COMMON_SIDE_NEW_URL = "$ECB_COMMON_IMAGES/newcommon_50cent_800.jpg"

/** Le fonti della scheda History, nello stesso formato dei crediti dei dettagli (etichetta, fonte, link). */
internal val HISTORY_SOURCES = listOf(
    Triple("History", "ECB", "https://www.ecb.europa.eu/ecb/history/emu/html/index.en.html"),
    Triple("Joining", "ECB", "https://www.ecb.europa.eu/euro/changeover/html/index.en.html"),
    Triple("Agreements", "European Commission", "https://economy-finance.ec.europa.eu/euro/euro-coins-and-notes/euro-coins/legislation-euro-coins_en"),
    Triple("Design", "European Commission", "https://economy-finance.ec.europa.eu/euro/euro-coins-and-notes/euro-coins/common-sides-euro-coins_en"),
)

/** Le fonti della scheda Coins. */
internal val COIN_SOURCES = listOf(
    Triple("Coins", "ECB", "https://www.ecb.europa.eu/euro/coins/common/html/index.en.html"),
    Triple("Metals", "Bundesbank", "https://www.bundesbank.de/en/tasks/cash-management/frequently-asked-questions/frequently-asked-questions-euro-coins"),
)
