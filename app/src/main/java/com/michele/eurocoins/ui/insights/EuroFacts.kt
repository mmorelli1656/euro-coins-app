package com.michele.eurocoins.ui.insights

/**
 * Dati fissi della scheda "The euro" di Insights (2026-10-25): storia dell'euro, materiali delle monete e lato
 * comune. Non vengono dalla pipeline né da Numista: sono fatti pubblici ricavati dalle pagine della BCE, della
 * Commissione europea e della Bundesbank (URL in [EURO_SOURCES]). Verificati a mano il 2026-10-22/25: se cambiano
 * (un nuovo paese entra nell'euro) si aggiornano qui, a mano, e `EuroFactsTest` ricorda i conti.
 */

/**
 * Un paese dell'area euro. [rate] è quanto vale 1 euro in [code], scritto come lo scrive la BCE. [joined] è quando
 * ha adottato l'euro, [coins] quando sono arrivati monete e banconote, [onlyEuro] il primo giorno in cui la vecchia
 * valuta non vale più (null dove non è noto con certezza: il primo gruppo e la Grecia).
 */
internal class EuroMember(
    val iso: String,
    val name: String,
    val former: String,
    val code: String,
    val rate: String,
    val joined: String,
    val coins: String,
    val onlyEuro: String?,
)

/**
 * I paesi entrati lo stesso anno, o una tappa senza paesi (le monete del 2002). [title] dice cosa succede quell'anno:
 * senza, "1999" faceva pensare che l'euro circolasse da allora. [note] è una riga sotto i paesi.
 */
internal class EuroEntry(val year: String, val title: String?, val note: String?, val members: List<EuroMember>)

/** Una tappa verso la moneta unica, prima del 1999. */
internal class RoadStep(val year: String, val title: String, val text: String)

/** Uno stato che usa l'euro per accordo con l'UE, senza essere nell'area euro. */
internal class AgreementState(
    val iso: String,
    val name: String,
    val official: String,
    val coinsFrom: String,
    val agreement: String,
)

/** Solo le tappe che servono a capire come si è arrivati al 1999: una riga ciascuna. */
internal val ROAD_STEPS = listOf(
    RoadStep("1957", "Treaty of Rome", "Creates the European Economic Community."),
    RoadStep("1979", "European Monetary System", "Keeps currencies within a narrow band."),
    RoadStep("1989", "Delors Report", "Three stages to a single currency."),
    RoadStep("1992", "Maastricht Treaty", "The legal basis for the euro."),
    RoadStep("1995", "The name \"euro\"", "Leaders pick the name and the start date."),
    RoadStep("1998", "The first eleven", "Eleven countries are chosen and the ECB starts."),
)

private const val FIRST_GROUP = "1 Jan 1999"
private const val COINS_2002 = "1 Jan 2002"

internal val EURO_ENTRIES = listOf(
    EuroEntry(
        year = "1999",
        title = "The euro starts",
        note = "Money in accounts and payments only. Coins and notes come later.",
        members = listOf(
            EuroMember("AT", "Austria", "Austrian schilling", "ATS", "13.7603", FIRST_GROUP, COINS_2002, null),
            EuroMember("BE", "Belgium", "Belgian franc", "BEF", "40.3399", FIRST_GROUP, COINS_2002, null),
            EuroMember("FI", "Finland", "Finnish markka", "FIM", "5.94573", FIRST_GROUP, COINS_2002, null),
            EuroMember("FR", "France", "French franc", "FRF", "6.55957", FIRST_GROUP, COINS_2002, null),
            EuroMember("DE", "Germany", "Deutsche Mark", "DEM", "1.95583", FIRST_GROUP, COINS_2002, null),
            EuroMember("IE", "Ireland", "Irish pound", "IEP", "0.787564", FIRST_GROUP, COINS_2002, null),
            EuroMember("IT", "Italy", "Italian lira", "ITL", "1,936.27", FIRST_GROUP, COINS_2002, null),
            EuroMember("LU", "Luxembourg", "Luxembourg franc", "LUF", "40.3399", FIRST_GROUP, COINS_2002, null),
            EuroMember("NL", "Netherlands", "Dutch guilder", "NLG", "2.20371", FIRST_GROUP, COINS_2002, null),
            EuroMember("PT", "Portugal", "Portuguese escudo", "PTE", "200.482", FIRST_GROUP, COINS_2002, null),
            EuroMember("ES", "Spain", "Spanish peseta", "ESP", "166.386", FIRST_GROUP, COINS_2002, null),
        ),
    ),
    EuroEntry(
        year = "2001",
        title = "A twelfth country",
        note = null,
        members = listOf(EuroMember("GR", "Greece", "Greek drachma", "GRD", "340.750", "1 Jan 2001", COINS_2002, null)),
    ),
    // Una tappa senza paesi: è qui che l'euro arriva in tasca (le banconote e le monete dei 12).
    EuroEntry(
        year = "2002",
        title = "Coins and notes begin",
        note = "The euro reaches people's pockets in all twelve countries, on 1 January.",
        members = emptyList(),
    ),
    EuroEntry(
        year = "2007",
        title = null,
        note = "From here on, coins and notes arrive the same day.",
        members = listOf(EuroMember("SI", "Slovenia", "Slovenian tolar", "SIT", "239.640", "1 Jan 2007", "1 Jan 2007", "15 Jan 2007")),
    ),
    EuroEntry(
        year = "2008",
        title = null,
        note = null,
        members = listOf(
            EuroMember("CY", "Cyprus", "Cypriot pound", "CYP", "0.585274", "1 Jan 2008", "1 Jan 2008", "1 Feb 2008"),
            EuroMember("MT", "Malta", "Maltese lira", "MTL", "0.429300", "1 Jan 2008", "1 Jan 2008", "1 Feb 2008"),
        ),
    ),
    EuroEntry("2009", null, null, listOf(EuroMember("SK", "Slovakia", "Slovak koruna", "SKK", "30.1260", "1 Jan 2009", "1 Jan 2009", "17 Jan 2009"))),
    EuroEntry("2011", null, null, listOf(EuroMember("EE", "Estonia", "Estonian kroon", "EEK", "15.6466", "1 Jan 2011", "1 Jan 2011", "15 Jan 2011"))),
    EuroEntry("2014", null, null, listOf(EuroMember("LV", "Latvia", "Latvian lats", "LVL", "0.702804", "1 Jan 2014", "1 Jan 2014", "15 Jan 2014"))),
    EuroEntry("2015", null, null, listOf(EuroMember("LT", "Lithuania", "Lithuanian litas", "LTL", "3.45280", "1 Jan 2015", "1 Jan 2015", "16 Jan 2015"))),
    EuroEntry("2023", null, null, listOf(EuroMember("HR", "Croatia", "Croatian kuna", "HRK", "7.53450", "1 Jan 2023", "1 Jan 2023", "15 Jan 2023"))),
    EuroEntry("2026", null, null, listOf(EuroMember("BG", "Bulgaria", "Bulgarian lev", "BGN", "1.95583", "1 Jan 2026", "1 Jan 2026", "1 Feb 2026"))),
)

/** Gli anni delle prime monete proprie: 2002 per Monaco, San Marino e Vaticano, 2014 per Andorra. */
internal val EURO_AGREEMENTS = listOf(
    AgreementState("AD", "Andorra", "Agreement applied from 1 Apr 2012", "Jan 2014", "Signed 30 Jun 2011"),
    AgreementState("MC", "Monaco", FIRST_GROUP, "2002", "In force 1 Dec 2011"),
    AgreementState("SM", "San Marino", FIRST_GROUP, "2002", "In force 1 Sep 2012"),
    AgreementState("VA", "Vatican City", FIRST_GROUP, "2002", "In force 1 Jan 2010"),
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
