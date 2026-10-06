package com.michele.eurocoins.data.pro

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.michele.eurocoins.BuildConfig
import kotlinx.coroutines.flow.StateFlow

/** Ogni quante monete guardate può comparire un annuncio a tutto schermo. */
internal const val INTERSTITIAL_EVERY_COINS = 10

/** Pausa minima tra due annunci a tutto schermo; vale anche dall'apertura dell'app (mai nei primi minuti). */
internal const val INTERSTITIAL_MIN_GAP_MS = 3 * 60 * 1000L

/**
 * Quando mostrare un annuncio a tutto schermo: servono ENTRAMBE, abbastanza monete guardate dall'ultimo
 * e abbastanza tempo passato. Il tempo da solo non basta (chi sfoglia piano non viene interrotto), le
 * monete da sole nemmeno (chi scorre veloce vedrebbe un annuncio ogni pochi secondi).
 */
internal fun isInterstitialDue(coinsViewed: Int, millisSinceLast: Long): Boolean =
    coinsViewed >= INTERSTITIAL_EVERY_COINS && millisSinceLast >= INTERSTITIAL_MIN_GAP_MS

/**
 * Annuncio a tutto schermo ("interstitial"). Regole scelte per rispettare le policy di Google e
 * l'utente:
 * - solo in un PUNTO DI PAUSA naturale: quando si esce dal dettaglio di una moneta, mai mentre si
 *   guarda, si scrive o si registra qualcosa, mai all'apertura dell'app;
 * - conta le monete GUARDATE (aperte dall'elenco e le pagine scorse nel dettaglio), non i tocchi:
 *   spuntare una casella o salvare una moneta non avvicina l'annuncio;
 * - contatore e orologio sono in memoria: un nuovo avvio riparte da zero e dà qualche minuto di calma;
 * - si precarica dopo l'apertura di qualche moneta, così quando serve è già pronto e, se non lo è,
 *   semplicemente non compare (niente attese né rotelle).
 * Solo quando [enabled] (non Pro e consenso a posto). Chiamare dal thread principale.
 */
class InterstitialAds(context: Context, private val enabled: StateFlow<Boolean>) {
    private val appContext = context.applicationContext
    private var ad: InterstitialAd? = null
    private var loading = false
    private var coinsViewed = 0
    private var lastShownAt = SystemClock.elapsedRealtime()

    /** Una moneta è stata guardata (aperta dall'elenco o raggiunta scorrendo il dettaglio). */
    fun onCoinViewed() {
        coinsViewed++
        // Si comincia a caricare a metà strada: pronto per quando servirà, niente richieste inutili prima.
        if (coinsViewed >= INTERSTITIAL_EVERY_COINS / 2) preload()
    }

    /** L'utente ha lasciato il dettaglio: se è il momento, mostra l'annuncio (già caricato) sopra la lista. */
    fun showIfDue(activity: Activity) {
        if (!enabled.value) return
        val now = SystemClock.elapsedRealtime()
        if (!isInterstitialDue(coinsViewed, now - lastShownAt)) return
        val loaded = ad ?: return preload()
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = release()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = release()
        }
        coinsViewed = 0
        lastShownAt = now
        loaded.show(activity)
    }

    private fun release() {
        ad = null
        preload()
    }

    private fun preload() {
        if (!enabled.value || ad != null || loading) return
        loading = true
        InterstitialAd.load(
            appContext,
            BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    loading = false
                    ad = loaded
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            },
        )
    }
}
