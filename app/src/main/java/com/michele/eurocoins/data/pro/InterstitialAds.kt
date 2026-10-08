package com.michele.eurocoins.data.pro

import android.app.Activity
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.michele.eurocoins.BuildConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Ogni quante monete guardate può comparire un annuncio a tutto schermo. Sotto le 8 monete di una serie
 * Regular Issues: con una soglia più alta chi guarda una serie sola non lo vedrebbe mai.
 */
internal const val INTERSTITIAL_EVERY_COINS = 6

/** Pausa minima tra due annunci a tutto schermo, anche tra un avvio dell'app e l'altro. */
internal const val INTERSTITIAL_MIN_GAP_MS = 30 * 1000L

/** Un annuncio caricato scade dopo un'ora: oltre questa età lo si scarta invece di mostrarlo. */
internal const val INTERSTITIAL_MAX_AGE_MS = 55 * 60 * 1000L

/** Pausa minima tra un caricamento fallito e il successivo (senza, ogni moneta rifarebbe la richiesta). */
internal const val INTERSTITIAL_LOAD_RETRY_MS = 30 * 1000L

/**
 * Quando mostrare un annuncio a tutto schermo: servono ENTRAMBE, abbastanza monete guardate dall'ultimo
 * e abbastanza tempo passato. Il tempo da solo non basta (chi sfoglia piano non viene interrotto), le
 * monete da sole nemmeno (chi scorre veloce vedrebbe un annuncio ogni pochi secondi).
 */
internal fun isInterstitialDue(coinsViewed: Int, millisSinceLast: Long): Boolean =
    coinsViewed >= INTERSTITIAL_EVERY_COINS && millisSinceLast >= INTERSTITIAL_MIN_GAP_MS

/**
 * Tempo trascorso dall'ultimo annuncio, con l'orologio di sistema (sopravvive ai riavvii). Se l'orario del
 * telefono è stato portato indietro, [last] è nel futuro: si riparte da zero invece di bloccare gli annunci
 * fino a quella data.
 */
internal fun elapsedSince(last: Long, now: Long): Long = if (last > now) 0L else now - last

/**
 * Annuncio a tutto schermo ("interstitial"). Regole scelte per rispettare le policy di Google e
 * l'utente:
 * - solo in un punto di TRANSIZIONE: quando si esce dal dettaglio di una moneta, oppure mentre si sfoglia, SUBITO
 *   dopo che una pagina si è assestata (dito già alzato; vedi [showWhenSettled] per il rischio noto), mai mentre si
 *   scrive o si registra qualcosa, mai all'apertura dell'app;
 * - conta le monete GUARDATE (aperte dall'elenco e le pagine scorse nel dettaglio), non i tocchi:
 *   spuntare una casella o salvare una moneta non avvicina l'annuncio;
 * - contatore e ora dell'ultimo annuncio sono SALVATI (SharedPreferences `interstitial`): le sessioni
 *   brevi si sommano invece di ripartire da zero a ogni avvio (prima l'orologio partiva all'apertura
 *   dell'app e chi la usava meno di 3 minuti non vedeva mai niente). Alla prima installazione l'orologio
 *   parte da ora: nessun annuncio nei primi 30 secondi;
 * - si precarica a metà strada; se non è pronto (o è scaduto) non compare, senza attese né rotelle, e il
 *   caricamento si ritenta alla moneta successiva, non più spesso di ogni [INTERSTITIAL_LOAD_RETRY_MS].
 * Solo quando [enabled] (non Pro e consenso a posto). Chiamare dal thread principale.
 */
class InterstitialAds(context: Context, private val enabled: StateFlow<Boolean>) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("interstitial", Context.MODE_PRIVATE)
    private var ad: InterstitialAd? = null
    private var adLoadedAt = 0L
    private var loading = false
    private var lastLoadFailedAt = 0L

    private var coinsViewed = prefs.getInt(KEY_COINS, 0)
    private var lastShownAt = prefs.getLong(KEY_LAST_SHOWN, 0L).takeIf { it > 0L }
        ?: System.currentTimeMillis().also { prefs.edit().putLong(KEY_LAST_SHOWN, it).apply() }

    /** Una moneta è stata guardata (aperta dall'elenco o raggiunta scorrendo il dettaglio). */
    fun onCoinViewed() {
        coinsViewed++
        prefs.edit().putInt(KEY_COINS, coinsViewed).apply()
        // Si comincia a caricare a metà strada: pronto per quando servirà, niente richieste inutili prima.
        if (coinsViewed >= INTERSTITIAL_EVERY_COINS / 2) preload()
    }

    /**
     * Si sta sfogliando il dettaglio e una pagina si è appena assestata (il dito ha già lasciato lo schermo): se è
     * il momento, l'annuncio compare SUBITO. Scelta del proprietario (2026-10-08). La prima versione aspettava 1,5 s
     * di pagina ferma, ma ogni nuovo scorrimento annullava l'attesa e chi sfoglia di continuo (23 monete di fila)
     * non lo vedeva finché non si fermava. Rischio noto: l'annuncio può comparire mentre inizia lo scorrimento
     * successivo e un tocco può finirci sopra.
     */
    fun showWhenSettled(activity: Activity) = showIfDue(activity)

    /** L'utente ha lasciato il dettaglio (o una pagina si è assestata): se è il momento, mostra l'annuncio (già caricato). */
    fun showIfDue(activity: Activity) {
        if (!enabled.value || !isDueNow()) return
        // Non si consuma il turno se l'app non è davanti all'utente (schermo spento, altra app): riproverà.
        if ((activity as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return
        val loaded = ad
        if (loaded == null || System.currentTimeMillis().let { elapsedSince(adLoadedAt, it) } > INTERSTITIAL_MAX_AGE_MS) {
            ad = null
            return preload()
        }
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = release()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = release()
        }
        coinsViewed = 0
        lastShownAt = System.currentTimeMillis()
        prefs.edit().putInt(KEY_COINS, 0).putLong(KEY_LAST_SHOWN, lastShownAt).apply()
        loaded.show(activity)
    }

    private fun isDueNow(): Boolean =
        isInterstitialDue(coinsViewed, elapsedSince(lastShownAt, System.currentTimeMillis()))

    private fun release() {
        ad = null
        preload()
    }

    private fun preload() {
        if (!enabled.value || loading) return
        val now = System.currentTimeMillis()
        if (ad != null) {
            // Un annuncio fermo da più di un'ora è scaduto: lo si sostituisce invece di tenerlo.
            if (elapsedSince(adLoadedAt, now) <= INTERSTITIAL_MAX_AGE_MS) return
            ad = null
        }
        if (lastLoadFailedAt != 0L && elapsedSince(lastLoadFailedAt, now) < INTERSTITIAL_LOAD_RETRY_MS) return
        loading = true
        InterstitialAd.load(
            appContext,
            BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    loading = false
                    ad = loaded
                    adLoadedAt = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    lastLoadFailedAt = System.currentTimeMillis()
                }
            },
        )
    }

    private companion object {
        const val KEY_COINS = "coins_since_last"
        const val KEY_LAST_SHOWN = "last_shown_at"
    }
}
