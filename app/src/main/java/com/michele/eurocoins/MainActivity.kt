package com.michele.eurocoins

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.michele.eurocoins.ui.navigation.EuroCoinsNavHost
import com.michele.eurocoins.ui.theme.EuroCoinsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as EuroCoinsApplication

        setContent {
            EuroCoinsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EuroCoinsNavHost(
                        repository = app.repository,
                        backupService = app.backupService,
                        accountManager = app.accountManager,
                    )
                }
            }
        }
    }
}
