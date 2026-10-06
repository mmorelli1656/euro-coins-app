package com.michele.eurocoins.data.pro

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Pro e pubblicità in un solo oggetto, per non allungare ogni firma di navigazione: [billing] sa se
 * l'utente ha pagato, [consent] se la legge permette di chiedere annunci, e [adsEnabled] le unisce.
 * Banner (Home) e annuncio a tutto schermo ([interstitial], uscendo dal dettaglio) compaiono solo se
 * [adsEnabled] è vero: nessun ramo "Pro" nei layout, l'annuncio c'è o non c'è.
 */
class Monetization(context: Context) {
    val billing = ProBilling(context)
    val consent = AdsConsent(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** true = non Pro e consenso a posto: si può mostrare pubblicità. */
    val adsEnabled: StateFlow<Boolean> = combine(billing.state, consent.canRequestAds) { pro, canRequest ->
        !pro.isPro && canRequest
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val interstitial = InterstitialAds(context, adsEnabled)
}
