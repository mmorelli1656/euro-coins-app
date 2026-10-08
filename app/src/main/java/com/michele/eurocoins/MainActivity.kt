package com.michele.eurocoins

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.michele.eurocoins.ui.home.preloadHomeShowcase
import com.michele.eurocoins.ui.detail.LocalSwipeHint
import com.michele.eurocoins.ui.navigation.EuroCoinsNavHost
import com.michele.eurocoins.ui.theme.EuroCoinsTheme
import com.michele.eurocoins.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    /** L'utente lascia l'app: se la collezione è cambiata dall'ultimo backup parte il salvataggio automatico (se acceso). */
    override fun onStop() {
        super.onStop()
        (application as EuroCoinsApplication).autoBackup.onAppLeft()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as EuroCoinsApplication
        // Decodifica delle foto della Home già adesso, in parallelo alla prima composizione.
        preloadHomeShowcase(this, app.userSettings)

        // Ogni volta che l'app torna in primo piano: si riallinea il Pro con Play (un rimborso o un
        // acquisto fatto su un altro telefono) e, se l'utente NON è Pro, si controlla il consenso
        // GDPR (UMP mostra il modulo solo se serve). Gli utenti Pro non vedono mai il modulo.
        val monetization = app.monetization
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                monetization.billing.refresh()
                monetization.billing.state.map { it.isPro }.distinctUntilChanged().collect { pro ->
                    if (!pro) monetization.consent.gatherConsent(this@MainActivity)
                }
            }
        }

        setContent {
            val themeMode by app.themePreference.mode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // enableEdgeToEdge() da solo sceglie il colore delle icone di barra di stato e di
            // navigazione dal tema del *telefono*: con un tema forzato dall'app (es. chiaro su
            // telefono scuro) le icone sparirebbero. Si riapplica a ogni cambio di tema; gli
            // scrim della barra di navigazione sono quelli di default di enableEdgeToEdge.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                    ) { darkTheme },
                )
                onDispose { }
            }

            EuroCoinsTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CompositionLocalProvider(LocalSwipeHint provides app.swipeHint) {
                        EuroCoinsNavHost(
                            repository = app.repository,
                            regularIssueRepository = app.regularIssueRepository,
                            backupService = app.backupService,
                            accountManager = app.accountManager,
                            autoBackup = app.autoBackup,
                            themePreference = app.themePreference,
                            userSettings = app.userSettings,
                            monetization = app.monetization,
                        )
                    }
                }
            }
        }
    }
}
