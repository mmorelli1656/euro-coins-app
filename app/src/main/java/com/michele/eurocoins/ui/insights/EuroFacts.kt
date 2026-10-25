package com.michele.eurocoins.ui.insights

/**
 * Dati fissi della scheda "The euro" di Insights (2026-10-25): storia dell'euro, materiali delle monete e lato
 * comune. Non vengono dalla pipeline né da Numista: sono fatti pubblici ricavati dalle pagine della BCE, della
 * Commissione europea e della Bundesbank (URL in [EURO_SOURCES]). Verificati a mano il 2026-10-22/25: se cambiano
 * (un nuovo paese entra nell'euro) si aggiornano qui, a mano, e `EuroFactsTest` ricorda i conti.
 */

/** Un paese dell'area euro: [rate] è quanto vale 1 euro in [code], scritto come lo scrive la BCE. */
internal class EuroMember(
    val iso: String,
    val name: String,
    val former: String,
    val code: String,
    val rate: String,
) {
    /** Una frase sola, mostrata sotto il gruppo quando si tocca il paese. */
    fun detail(): String = "$name replaced the $former. €1 = $rate $code."
}

/** I paesi entrati lo stesso anno. [note] è una riga facoltativa sotto i paesi. */
internal class EuroEntry(val year: String, val note: String?, val members: List<EuroMember>)

/** Una tappa verso la moneta unica, prima del 1999. */
internal class RoadStep(val year: String, val title: String, val text: String)

/** Uno stato che usa l'euro per accordo con l'UE, senza essere nell'area euro. */
internal class AgreementState(val iso: String, val name: String, val coinsFrom: String) {
    fun detail(): String = "$name mints its own euro coins from $coinsFrom."
}

/** Solo le tappe che servono a capire come si è arrivati al 1999: una riga ciascuna. */
internal val ROAD_STEPS = listOf(
    RoadStep("1957", "Treaty of Rome", "Creates the European Economic Community."),
    RoadStep("1979", "European Monetary System", "Keeps currencies within a narrow band."),
    RoadStep("1989", "Delors Report", "Three stages to a single currency."),
    RoadStep("1992", "Maastricht Treaty", "The legal basis for the euro."),
    RoadStep("1995", "The name \"euro\"", "Leaders pick the name and the start date."),
    RoadStep("1998", "The first eleven", "Eleven countries are chosen and the ECB starts."),
)

private const val COINS_NOTE = "Coins and notes followed on 1 January 2002."

internal val EURO_ENTRIES = listOf(
    EuroEntry(
        "1999", COINS_NOTE,
        listOf(
            EuroMember("AT", "Austria", "Austrian schilling", "ATS", "13.7603"),
            EuroMember("BE", "Belgium", "Belgian franc", "BEF", "40.3399"),
            EuroMember("FI", "Finland", "Finnish markka", "FIM", "5.94573"),
            EuroMember("FR", "France", "French franc", "FRF", "6.55957"),
            EuroMember("DE", "Germany", "Deutsche Mark", "DEM", "1.95583"),
            EuroMember("IE", "Ireland", "Irish pound", "IEP", "0.787564"),
            EuroMember("IT", "Italy", "Italian lira", "ITL", "1,936.27"),
            EuroMember("LU", "Luxembourg", "Luxembourg franc", "LUF", "40.3399"),
            EuroMember("NL", "Netherlands", "Dutch guilder", "NLG", "2.20371"),
            EuroMember("PT", "Portugal", "Portuguese escudo", "PTE", "200.482"),
            EuroMember("ES", "Spain", "Spanish peseta", "ESP", "166.386"),
        ),
    ),
    EuroEntry("2001", COINS_NOTE, listOf(EuroMember("GR", "Greece", "Greek drachma", "GRD", "340.750"))),
    EuroEntry("2007", null, listOf(EuroMember("SI", "Slovenia", "Slovenian tolar", "SIT", "239.640"))),
    EuroEntry(
        "2008", null,
        listOf(
            EuroMember("CY", "Cyprus", "Cypriot pound", "CYP", "0.585274"),
            EuroMember("MT", "Malta", "Maltese lira", "MTL", "0.429300"),
        ),
    ),
    EuroEntry("2009", null, listOf(EuroMember("SK", "Slovakia", "Slovak koruna", "SKK", "30.1260"))),
    EuroEntry("2011", null, listOf(EuroMember("EE", "Estonia", "Estonian kroon", "EEK", "15.6466"))),
    EuroEntry("2014", null, listOf(EuroMember("LV", "Latvia", "Latvian lats", "LVL", "0.702804"))),
    EuroEntry("2015", null, listOf(EuroMember("LT", "Lithuania", "Lithuanian litas", "LTL", "3.45280"))),
    EuroEntry("2023", null, listOf(EuroMember("HR", "Croatia", "Croatian kuna", "HRK", "7.53450"))),
    EuroEntry("2026", null, listOf(EuroMember("BG", "Bulgaria", "Bulgarian lev", "BGN", "1.95583"))),
)

/** Gli anni delle prime monete proprie: 2002 per Monaco, San Marino e Vaticano, 2014 per Andorra. */
internal val EURO_AGREEMENTS = listOf(
    AgreementState("AD", "Andorra", "2014"),
    AgreementState("MC", "Monaco", "2002"),
    AgreementState("SM", "San Marino", "2002"),
    AgreementState("VA", "Vatican City", "2002"),
)

/** Una moneta con le misure ufficiali (BCE): diametro in mm e peso in g, come stringhe già pronte. */
internal class CoinFact(val taglio: String, val diameter: String, val weight: String)

/** Un materiale e le monete che lo usano. [detail] è una riga facoltativa sotto la frase principale. */
internal class MaterialFamily(
    val title: String,
    val text: String,
    val detail: String?,
    val magnet: String,
    val coins: List<CoinFact>,
)

internal val MATERIAL_FAMILIES = listOf(
    MaterialFamily(
        title = "Copper-covered steel",
        text = "A steel disc with a copper covering.",
        detail = null,
        magnet = "Strongly magnetic: a magnet picks them up.",
        coins = listOf(
            CoinFact("1 cent", "16.25", "2.30"),
            CoinFact("2 cent", "18.75", "3.06"),
            CoinFact("5 cent", "21.25", "3.92"),
        ),
    ),
    MaterialFamily(
        title = "Nordic gold",
        text = "A gold-coloured alloy of copper, aluminium, zinc and tin. There is no gold in it.",
        detail = "The ECB says it is difficult to melt and used only for coins.",
        magnet = "Not magnetic.",
        coins = listOf(
            CoinFact("10 cent", "19.75", "4.10"),
            CoinFact("20 cent", "22.25", "5.74"),
            CoinFact("50 cent", "24.25", "7.80"),
        ),
    ),
    MaterialFamily(
        title = "Two metals",
        text = "A ring around a centre made of three layers. The 2 euro has a silver-coloured ring of " +
            "cupronickel around a gold-coloured centre of nickel brass, nickel and nickel brass. The 1 euro swaps them.",
        detail = "Cupronickel is copper with nickel. Nickel brass is brass, which is copper and zinc, with nickel added.",
        magnet = "The centre is slightly magnetic.",
        coins = listOf(
            CoinFact("1 euro", "23.25", "7.50"),
            CoinFact("2 euro", "25.75", "8.50"),
        ),
    ),
)

/** Frase sotto i materiali: le fonti nominano i metalli ma non danno le percentuali, e non le inventiamo. */
internal const val MATERIALS_NOTE = "The ECB and the Bundesbank name the metals but give no percentages."

private const val ECB_COMMON_IMAGES = "https://www.ecb.europa.eu/euro/coins/common/shared/img"

/** Le foto BCE del 50 cent prima e dopo il 2007 (hotlink, come le altre foto: mai ospitate nell'APK). */
internal const val COMMON_SIDE_OLD_URL = "$ECB_COMMON_IMAGES/common_50cent_800.jpg"
internal const val COMMON_SIDE_NEW_URL = "$ECB_COMMON_IMAGES/newcommon_50cent_800.jpg"

/** Le fonti della scheda, nello stesso formato dei crediti dei dettagli. */
internal val EURO_SOURCES = listOf(
    Triple("History", "ECB", "https://www.ecb.europa.eu/ecb/history/emu/html/index.en.html"),
    Triple("Joining", "ECB", "https://www.ecb.europa.eu/euro/changeover/html/index.en.html"),
    Triple("Coins", "ECB", "https://www.ecb.europa.eu/euro/coins/common/html/index.en.html"),
    Triple("Metals", "Bundesbank", "https://www.bundesbank.de/en/tasks/cash-management/frequently-asked-questions/frequently-asked-questions-euro-coins"),
    Triple("Design", "European Commission", "https://economy-finance.ec.europa.eu/euro/euro-coins-and-notes/euro-coins/common-sides-euro-coins_en"),
)
