package com.offlinegames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.offlinegames.ui.navigation.AppNavigation
import com.offlinegames.ui.theme.OfflineGamesTheme
import com.offlinegames.persistence.PreferencesRepository

/**
 * Single-activity host for the entire app.
 * All navigation is managed by the UI module via Compose NavHost.
 */
class MainActivity : ComponentActivity() {

    // Manual DI: create the shared preferences repository once
    private val preferencesRepository by lazy {
        PreferencesRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OfflineGamesTheme {
                AppNavigation(preferencesRepository = preferencesRepository)
            }
        }
    }
}
