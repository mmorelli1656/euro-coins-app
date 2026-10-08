package com.michele.eurocoins.data.pro

import android.content.Context
import android.content.MutableContextWrapper
import android.os.SystemClock
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.michele.eurocoins.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Dopo quanto un banner già mostrato si considera vecchio e si richiede di nuovo tornando nella Home. */
internal const val BANNER_STALE_MS = 2 * 60 * 1000L

/** Pause prima di ritentare dopo un banner non caricato: 15 s, 30 s, poi 60 s (il valore resta a 60 s). */
internal fun bannerRetryDelayMs(failures: Int): Long = when {
    failures <= 1 -> 15_000L
    failures == 2 -> 30_000L
    else -> 60_000L
}

/**
 * Il banner della Home, tenuto vivo FUORI dalla composizione. Prima la `AdView` nasceva a ogni ingresso nella
 * Home e ogni volta partiva una richiesta nuova: uscire e rientrare mostrava per qualche secondo solo lo spazio
 * riservato (e se la richiesta falliva, niente per un minuto). Ora la stessa vista, con l'annuncio già caricato,
 * viene riagganciata alla schermata, e una nuova richiesta parte solo se l'annuncio è vecchio o l'ultimo
 * tentativo è fallito.
 *
 * La `AdView` vuole un contesto Activity (per aprire i clic) ma vive più dell'Activity (rotazione): per questo
 * ha un `MutableContextWrapper`, che punta all'Activity solo mentre la vista è agganciata e
 * torna al contesto dell'applicazione quando se ne va, senza trattenere l'Activity.
 */
class BannerAd(context: Context) {
    private val appContext = context.applicationContext
    private val holderContext = MutableContextWrapper(appContext)
    private var adView: AdView? = null
    private var widthDp = 0
    private var loadedAt = 0L
    private var failures = 0

    private val _failed = MutableStateFlow(false)

    /** true = l'ultima richiesta non ha trovato un annuncio: la Home toglie lo spazio e ritenta dopo [retryDelayMs]. */
    val failed: StateFlow<Boolean> = _failed

    /** Quanto aspettare prima del prossimo tentativo, secondo i fallimenti consecutivi. */
    fun retryDelayMs(): Long = bannerRetryDelayMs(failures)

    /** Altezza del banner per una larghezza in dp (adattivo ancorato, come la richiesta). */
    fun heightFor(activity: Context, widthDp: Int): Int = sizeFor(activity, widthDp).height

    /**
     * Aggancia il banner a [activity] e lo restituisce, creandolo se manca o se la larghezza è cambiata
     * (rotazione). Da chiamare quando la Home entra in composizione; [detach] quando esce.
     */
    fun attach(activity: Context, widthDp: Int): AdView {
        holderContext.baseContext = activity
        val current = adView
        if (current != null && this.widthDp == widthDp) {
            (current.parent as? ViewGroup)?.removeView(current)
            if (_failed.value || SystemClock.elapsedRealtime() - loadedAt > BANNER_STALE_MS) load(current)
            return current
        }
        current?.destroy()
        this.widthDp = widthDp
        return AdView(holderContext).apply {
            setAdSize(sizeFor(activity, widthDp))
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    failures = 0
                    loadedAt = SystemClock.elapsedRealtime()
                    _failed.value = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    failures++
                    _failed.value = true
                }
            }
            adView = this
            load(this)
        }
    }

    /** La Home è uscita di scena: il banner resta in memoria con il suo annuncio, senza trattenere l'Activity. */
    fun detach() {
        holderContext.baseContext = appContext
    }

    /** Ritenta dopo un fallimento (chiamata dalla Home dopo [retryDelayMs]). */
    fun retry() {
        adView?.let(::load)
    }

    private fun load(view: AdView) {
        view.loadAd(AdRequest.Builder().build())
    }

    private fun sizeFor(activity: Context, widthDp: Int): AdSize {
        // La versione "large" (consigliata da Google, più alta e più redditizia) ruberebbe ~40 dp alle schede
        // della Home; questa è deprecata ma funziona ancora (CLAUDE.md § Pro e pubblicità).
        @Suppress("DEPRECATION")
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
    }
}
