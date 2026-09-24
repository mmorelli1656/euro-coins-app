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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.michele.eurocoins.ui.navigation.EuroCoinsNavHost
import com.michele.eurocoins.ui.theme.EuroCoinsTheme
import com.michele.eurocoins.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as EuroCoinsApplication

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
                    EuroCoinsNavHost(
                        repository = app.repository,
                        backupService = app.backupService,
                        accountManager = app.accountManager,
                        themePreference = app.themePreference,
                        userSettings = app.userSettings,
                    )
                }
            }
        }
    }
}
