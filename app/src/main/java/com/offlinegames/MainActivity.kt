package com.offlinegames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.offlinegames.ui.navigation.AppNavigation
import com.offlinegames.ui.theme.OfflineGamesTheme
import com.offlinegames.persistence.PreferencesRepository
import com.offlinegames.ads.AdManager
import com.games.offlinegames.BuildConfig
/**
 * Single-activity host for the entire app.
 * All navigation is managed by the UI module via Compose NavHost.
 */
class MainActivity : ComponentActivity() {

    // Manual DI: create the shared preferences repository once
    private val preferencesRepository by lazy {
        PreferencesRepository(applicationContext)
    }

    private val adManager by lazy {
        AdManager.getInstance(applicationContext, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OfflineGamesTheme {
                AppNavigation(
                    preferencesRepository = preferencesRepository,
                    adManager = adManager,
                    activity = this
                )
            }
        }
    }
}
