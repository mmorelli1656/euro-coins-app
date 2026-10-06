package com.michele.eurocoins.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.michele.eurocoins.BuildConfig
import kotlinx.coroutines.delay

/** Pausa prima di ritentare dopo un banner non caricato (niente rete, nessun annuncio disponibile). */
private const val RETRY_DELAY_MS = 60_000L

/**
 * Banner AdMob "adattivo ancorato" in fondo alla schermata, a tutta larghezza e sopra la barra di
 * navigazione di sistema. Lo spazio è riservato mentre l'annuncio si carica (le schede della Home non
 * saltano quando arriva) e si chiude se non c'è niente da mostrare, per poi ritentare dopo un minuto.
 * Da mostrare solo quando `Monetization.adsEnabled` è vero.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        // La versione "large" (consigliata da Google, più alta e più redditizia) non è ancora stata scelta: ruberebbe
        // ~40 dp alle schede della Home. Questa funziona ancora; se sparisse dall'SDK si passa a getLargeAnchored...
        val adSize = remember(maxWidth) {
            @Suppress("DEPRECATION")
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, maxWidth.value.toInt())
        }
        val adView = remember(adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        failed = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        failed = true
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        // Come per ogni View che fa lavoro in background: in pausa quando l'app non è in primo piano.
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(adView, lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }
        LaunchedEffect(failed, adView) {
            if (failed) {
                delay(RETRY_DELAY_MS)
                adView.loadAd(AdRequest.Builder().build())
            }
        }
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().height(if (failed) 0.dp else adSize.height.dp),
        )
    }
}
