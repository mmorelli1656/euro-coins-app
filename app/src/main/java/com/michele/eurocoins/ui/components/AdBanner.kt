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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.michele.eurocoins.data.pro.BannerAd
import kotlinx.coroutines.delay

/**
 * Banner AdMob "adattivo ancorato" in fondo alla schermata, a tutta larghezza e sopra la barra di
 * navigazione di sistema. Lo spazio è riservato mentre l'annuncio si carica (le schede della Home non
 * saltano quando arriva) e si chiude se non c'è niente da mostrare, per poi ritentare (15 s, 30 s, 60 s).
 * La vista vive in [BannerAd] e non nella composizione: uscire e rientrare nella Home non rifà la richiesta
 * (prima ogni ritorno mostrava per qualche secondo solo lo spazio vuoto).
 * Da mostrare solo quando `Monetization.adsEnabled` è vero.
 */
@Composable
fun AdBanner(banner: BannerAd, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val failed by banner.failed.collectAsState()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        val widthDp = maxWidth.value.toInt()
        val height = remember(widthDp) { banner.heightFor(context, widthDp) }
        val adView = remember(widthDp) { banner.attach(context, widthDp) }
        // In pausa quando l'app non è in primo piano, come ogni View che fa lavoro in background. La vista NON si
        // distrugge uscendo dalla Home (resta in BannerAd con il suo annuncio): solo si stacca dall'Activity.
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
                banner.detach()
            }
        }
        LaunchedEffect(failed, adView) {
            if (failed) {
                delay(banner.retryDelayMs())
                banner.retry()
            }
        }
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().height(if (failed) 0.dp else height.dp),
        )
    }
}
