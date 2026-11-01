package com.michele.eurocoins.ui.settings

import android.content.Context
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.StatsLayout
import com.michele.eurocoins.ui.browse.BrowseMode
import com.michele.eurocoins.ui.regular.RegularBrowseMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferenze dell'utente sul catalogo, salvate in SharedPreferences (file `settings`).
 * Il tema ha la sua classe ([com.michele.eurocoins.ui.theme.ThemePreference]) perché
 * `MainActivity` lo legge prima di disegnare qualunque schermata.
 *
 * Gli stati sono [StateFlow] così il catalogo si aggiorna subito quando l'utente
 * cambia opzione dalla schermata Impostazioni, senza riaprire le schermate.
 */
class UserSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // La chiave `hide_microstates` esisteva prima della divisione per catalogo: ora vale per le commemorative.
    private val _hideCommemorativeMicrostates = MutableStateFlow(prefs.getBoolean(KEY_HIDE_MICROSTATES, false))
    private val _hideRegularMicrostates = MutableStateFlow(
        resolveHideRegularMicrostates(
            stored = if (prefs.contains(KEY_HIDE_REGULAR_MICROSTATES)) prefs.getBoolean(KEY_HIDE_REGULAR_MICROSTATES, false) else null,
            commemorative = _hideCommemorativeMicrostates.value,
        ),
    )
    private val _defaultTab = MutableStateFlow(loadDefaultTab())
    private val _defaultRegularTab = MutableStateFlow(parseRegularBrowseMode(prefs.getString(KEY_DEFAULT_REGULAR_TAB, null)))
    private val _rotateHomeCoins = MutableStateFlow(prefs.getBoolean(KEY_ROTATE_HOME_COINS, true))
    private val _quickAdd = MutableStateFlow(prefs.getBoolean(KEY_QUICK_ADD, false))
    private val _showMints = MutableStateFlow(prefs.getBoolean(KEY_SHOW_MINTS, true))
    private val _statsLayout = MutableStateFlow(StatsLayout.decode(prefs.getString(KEY_STATS_LAYOUT, null)))
    private val _setupTour = MutableStateFlow(loadSetupTour(context))

    /** true = nasconde monete e card di Andorra, Monaco, San Marino e Città del Vaticano nelle commemorative. */
    val hideCommemorativeMicrostates: StateFlow<Boolean> = _hideCommemorativeMicrostates.asStateFlow()

    /** Come [hideCommemorativeMicrostates] per le Regular Issues: impostazione indipendente. */
    val hideRegularMicrostates: StateFlow<Boolean> = _hideRegularMicrostates.asStateFlow()

    /** Scheda di Commemorative che si apre per prima. */
    val defaultTab: StateFlow<BrowseMode> = _defaultTab.asStateFlow()

    /** Scheda di Regular Issues che si apre per prima (Countries o Denominations). */
    val defaultRegularTab: StateFlow<RegularBrowseMode> = _defaultRegularTab.asStateFlow()

    /** true = le monete della Home cambiano ogni giorno; false = set fisso. Vale anche per le future Regular Issues. */
    val rotateHomeCoins: StateFlow<Boolean> = _rotateHomeCoins.asStateFlow()

    fun setHideCommemorativeMicrostates(value: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_MICROSTATES, value).apply()
        _hideCommemorativeMicrostates.value = value
    }

    fun setHideRegularMicrostates(value: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_REGULAR_MICROSTATES, value).apply()
        _hideRegularMicrostates.value = value
    }

    fun setDefaultRegularTab(mode: RegularBrowseMode) {
        prefs.edit().putString(KEY_DEFAULT_REGULAR_TAB, mode.name).apply()
        _defaultRegularTab.value = mode
    }

    /**
     * URL delle 4 foto dell'ultimo set della Home mostrato per intero con successo (le foto stanno già nella
     * cache su disco di Coil): ripiego per quando le foto del giorno non si caricano. Non è una
     * preferenza dell'utente ma vive qui per non aprire un altro file di SharedPreferences.
     */
    val lastShowcase: List<String>
        get() = prefs.getString(KEY_LAST_SHOWCASE, null).orEmpty().split(SHOWCASE_SEPARATOR).filter { it.isNotBlank() }

    /** Foto del set di un giorno (`LocalDate.toEpochDay()`), se già calcolato in una sessione precedente: permette alla Home di mostrare subito le monete giuste, senza aspettare il database. */
    fun showcaseUrlsFor(day: Long): List<String>? =
        prefs.getString(KEY_SHOWCASE_DAY_PREFIX + day, null)?.split(SHOWCASE_SEPARATOR)?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }

    /** Salva i set di oggi e domani, scartando quelli dei giorni passati. */
    fun saveShowcaseUrls(sets: Map<Long, List<String>>) {
        if (sets.all { (day, urls) -> showcaseUrlsFor(day) == urls }) return
        prefs.edit().apply {
            prefs.all.keys.filter { it.startsWith(KEY_SHOWCASE_DAY_PREFIX) }.forEach { remove(it) }
            sets.forEach { (day, urls) -> putString(KEY_SHOWCASE_DAY_PREFIX + day, urls.joinToString(SHOWCASE_SEPARATOR)) }
        }.apply()
    }

    fun setLastShowcase(urls: List<String>) {
        if (urls != lastShowcase) prefs.edit().putString(KEY_LAST_SHOWCASE, urls.joinToString(SHOWCASE_SEPARATOR)).apply()
    }

    /**
     * Stesso ripiego di [lastShowcase], per la fascia di Regular Issues — ma qui una posizione può
     * essere `null` (nessuna serie ha una foto per quella fascia di taglio, vedi
     * `pickRegularIssueShowcaseUrls`): a differenza delle commemorative (sempre 4 monete dense),
     * filtrare le righe vuote sposterebbe gli URL nelle posizioni sbagliate rispetto a
     * `regularIssueShowcase` (il confronto in `ShowcaseCoin` è posizionale, slot per slot) — per
     * questo niente `.filter { isNotBlank() }` qui, una riga vuota diventa `null`, non sparisce.
     */
    val regularIssueLastShowcase: List<String?>
        get() = prefs.getString(KEY_LAST_SHOWCASE_REGULAR, null)
            ?.split(SHOWCASE_SEPARATOR)?.map { it.ifBlank { null } }.orEmpty()

    fun regularIssueShowcaseUrlsFor(day: Long): List<String?>? =
        prefs.getString(KEY_SHOWCASE_DAY_PREFIX_REGULAR + day, null)?.split(SHOWCASE_SEPARATOR)?.map { it.ifBlank { null } }

    /** Salva i set di oggi e domani per Regular Issues, posizioni comprese (vedi [regularIssueLastShowcase]). */
    fun saveRegularIssueShowcaseUrls(sets: Map<Long, List<String?>>) {
        if (sets.all { (day, urls) -> regularIssueShowcaseUrlsFor(day) == urls }) return
        prefs.edit().apply {
            prefs.all.keys.filter { it.startsWith(KEY_SHOWCASE_DAY_PREFIX_REGULAR) }.forEach { remove(it) }
            sets.forEach { (day, urls) -> putString(KEY_SHOWCASE_DAY_PREFIX_REGULAR + day, urls.joinToString(SHOWCASE_SEPARATOR) { it.orEmpty() }) }
        }.apply()
    }

    fun setRegularIssueLastShowcase(urls: List<String?>) {
        if (urls != regularIssueLastShowcase) {
            prefs.edit().putString(KEY_LAST_SHOWCASE_REGULAR, urls.joinToString(SHOWCASE_SEPARATOR) { it.orEmpty() }).apply()
        }
    }

    /**
     * true = il tocco sulla casella aggiunge o toglie la moneta senza aprire il pannello (Quick add). Vale per
     * tutta l'app, spento di default. Una moneta con dettagli (prezzo, data, BU, Proof, anno) apre comunque il
     * pannello, vedi `quickTapFor`.
     */
    val quickAdd: StateFlow<Boolean> = _quickAdd.asStateFlow()

    fun setQuickAdd(value: Boolean) {
        prefs.edit().putBoolean(KEY_QUICK_ADD, value).apply()
        _quickAdd.value = value
    }

    /**
     * true = i pannelli di collezione, i dettagli e le tabelle mostrano le zecche delle monete tedesche (la riga
     * Mint, i cerchi A D F G J, "View by mint"). Acceso di default. Spento nasconde la FUNZIONE, mai i dati: le
     * voci con zecca già registrate restano e si salvano intatte. Vale per tutti (anche senza Pro: chi non la
     * vuole non vede nemmeno gli inviti).
     */
    val showMints: StateFlow<Boolean> = _showMints.asStateFlow()

    fun setShowMints(value: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_MINTS, value).apply()
        _showMints.value = value
    }

    /**
     * Ordine e card nascoste della scheda Statistics di Insights (solo Pro). Preferenza di VISUALIZZAZIONE del telefono, come il
     * tema: non va nel backup e non tocca i dati. Vedi [StatsLayout].
     */
    val statsLayout: StateFlow<StatsLayout> = _statsLayout.asStateFlow()

    fun setStatsLayout(layout: StatsLayout) {
        prefs.edit().putString(KEY_STATS_LAYOUT, layout.encode()).apply()
        _statsLayout.value = layout
    }

    fun setRotateHomeCoins(value: Boolean) {
        prefs.edit().putBoolean(KEY_ROTATE_HOME_COINS, value).apply()
        _rotateHomeCoins.value = value
    }

    fun setDefaultTab(mode: BrowseMode) {
        prefs.edit().putString(KEY_DEFAULT_TAB, mode.name).apply()
        _defaultTab.value = mode
    }

    /** Percorso di primo avvio (scheda di benvenuto e Impostazioni guidate): [SetupTour.PENDING] solo su un'installazione nuova. */
    val setupTour: StateFlow<SetupTour> = _setupTour.asStateFlow()

    /** Il percorso è finito, o l'utente non lo vuole: non si ripropone. */
    fun finishSetupTour() {
        prefs.edit().putString(KEY_SETUP_TOUR, SetupTour.DONE.name).apply()
        _setupTour.value = SetupTour.DONE
    }

    /** Solo per la sezione Developer delle build di sviluppo: rimette il percorso a "da fare", così si rivede senza `adb`. */
    fun restartSetupTour() {
        prefs.edit().putString(KEY_SETUP_TOUR, SetupTour.PENDING.name).apply()
        _setupTour.value = SetupTour.PENDING
    }

    // Si decide qui, alla prima lettura delle preferenze, perché il seeding del catalogo (che lascia l'hash) parte dopo;
    // il risultato si salva subito, vedi resolveSetupTour.
    private fun loadSetupTour(context: Context): SetupTour {
        val hasPriorData = context.applicationContext
            .getSharedPreferences(CoinRepository.PREFS_NAME, Context.MODE_PRIVATE)
            .contains(CoinRepository.KEY_ASSET_HASH)
        val resolved = resolveSetupTour(prefs.getString(KEY_SETUP_TOUR, null), hasPriorData)
        prefs.edit().putString(KEY_SETUP_TOUR, resolved.name).apply()
        return resolved
    }

    // Valore sconosciuto o assente (prima installazione, enum rinominato): parte da Years.
    private fun loadDefaultTab(): BrowseMode =
        runCatching { BrowseMode.valueOf(prefs.getString(KEY_DEFAULT_TAB, null).orEmpty()) }.getOrDefault(BrowseMode.YEARS)

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_HIDE_MICROSTATES = "hide_microstates"
        const val KEY_DEFAULT_TAB = "default_tab"
        const val KEY_HIDE_REGULAR_MICROSTATES = "hide_regular_microstates"
        const val KEY_DEFAULT_REGULAR_TAB = "default_regular_tab"
        const val KEY_ROTATE_HOME_COINS = "rotate_home_coins"
        const val KEY_QUICK_ADD = "quick_add"
        const val KEY_SHOW_MINTS = "show_mints"
        const val KEY_STATS_LAYOUT = "stats_layout"
        const val KEY_SETUP_TOUR = "setup_tour"
        const val KEY_LAST_SHOWCASE = "last_showcase"
        const val KEY_SHOWCASE_DAY_PREFIX = "showcase_day_"
        const val KEY_LAST_SHOWCASE_REGULAR = "last_showcase_regular"
        // "regular_showcase_day_" e non "showcase_day_regular_": deve restare un prefisso DIVERSO
        // da KEY_SHOWCASE_DAY_PREFIX, non solo una variante — altrimenti il filtro a prefisso di
        // saveShowcaseUrls() cancellerebbe anche le chiavi di Regular Issues (e viceversa).
        const val KEY_SHOWCASE_DAY_PREFIX_REGULAR = "regular_showcase_day_"
        const val SHOWCASE_SEPARATOR = "\n"
    }
}

/**
 * Valore iniziale dell'interruttore dei microstati di Regular Issues. Finché l'utente non lo ha mai
 * toccato ([stored] nullo) eredita quello delle commemorative: chi aveva "Hide microstates" acceso
 * prima della divisione per catalogo non vede cambiare niente.
 */
internal fun resolveHideRegularMicrostates(stored: Boolean?, commemorative: Boolean): Boolean = stored ?: commemorative

/** Scheda iniziale di Regular Issues: valore assente o sconosciuto (prima installazione, enum rinominato) = Countries. */
internal fun parseRegularBrowseMode(name: String?): RegularBrowseMode =
    RegularBrowseMode.entries.firstOrNull { it.name == name } ?: RegularBrowseMode.COUNTRIES
