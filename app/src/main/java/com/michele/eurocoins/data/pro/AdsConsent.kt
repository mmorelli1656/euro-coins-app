package com.michele.eurocoins.data.pro

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consenso GDPR (Google UMP) e avvio dell'SDK pubblicitario. Il messaggio di consenso e i suoi
 * testi si configurano nella console AdMob (Privacy e messaggi), non qui. Regole che rispetta:
 * - la pubblicità si richiede solo con [canRequestAds] vero (UMP: consenso raccolto, o non richiesto);
 * - `MobileAds.initialize` parte solo allora, mai prima: l'SDK raccoglie dati dal momento in cui parte;
 * - chi è nello spazio economico europeo/Regno Unito può rivedere la scelta quando vuole
 *   ([privacyOptionsRequired] → [showPrivacyOptions], pulsante nelle Impostazioni).
 *
 * Un utente Pro non passa da qui: non vede pubblicità e non gli si chiede nessun consenso.
 */
class AdsConsent(context: Context) {
    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(appContext)
    private val sdkStarted = AtomicBoolean(false)
    private val sdkReady = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(false)
    private val _privacyOptionsRequired = MutableStateFlow(false)

    /** true = si può chiedere un annuncio (consenso a posto e SDK avviato). */
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    /** true = l'utente è in una regione che richiede di poter cambiare la scelta sulla privacy. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /**
     * Aggiorna lo stato del consenso e, se serve, mostra il modulo. Da chiamare a ogni apertura con
     * un'Activity visibile (`MainActivity.onCreate`): UMP lo salta da solo se la scelta è già valida.
     * Un errore (rete, modulo non configurato) non blocca niente: resta ciò che UMP sapeva già.
     */
    fun gatherConsent(activity: Activity) {
        // La scelta dell'ultima volta vale subito, senza aspettare la rete.
        publish()
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { publish() }
            },
            { publish() },
        )
    }

    /** Riapre il modulo con le scelte sulla privacy (voce delle Impostazioni, solo dove richiesto). */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { publish() }
    }

    private fun publish() {
        _privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        val allowed = consentInformation.canRequestAds()
        if (allowed) startSdk()
        _canRequestAds.value = allowed && sdkReady.get()
    }

    /** Avvio unico dell'SDK; fuori dal thread principale come raccomanda Google (l'inizializzazione può bloccare). */
    private fun startSdk() {
        if (!sdkStarted.compareAndSet(false, true)) return
        Thread {
            MobileAds.initialize(appContext)
            sdkReady.set(true)
            publish()
        }.start()
    }
}
