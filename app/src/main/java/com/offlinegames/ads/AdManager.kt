package com.offlinegames.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.games.offlinegames.BuildConfig

/**
 * Manages interstitial ads with frequency control and cooldown logic.
 *
 * Uses Google's test ad unit IDs in debug builds so ads always appear during development.
 * In release builds, uses real ad unit IDs from BuildConfig.
 *
 * Ads are skippable after 5 seconds (AdMob default for video ads).
 */
class AdManager private constructor(private val context: Context, private val adUnitId: String) {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading: Boolean = false
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * The actual ad unit ID to use:
     * - Debug builds: Google's test interstitial ID (always fills)
     * - Release builds: Real ad unit ID from BuildConfig
     */
    private val effectiveAdUnitId: String = if (BuildConfig.DEBUG) {
        TEST_INTERSTITIAL_AD_UNIT_ID
    } else {
        adUnitId
    }

    companion object {
        private const val PREFS_NAME = "ad_manager_prefs"
        private const val KEY_LAST_AD_TIMESTAMP = "last_ad_timestamp"
        private const val KEY_GAME_SESSION_COUNT = "game_session_count"

        // Show an ad every 3 game starts, with a 2-minute cooldown
        private const val COOLDOWN_MINUTES = 2
        private const val SESSIONS_BETWEEN_ADS = 3

        // Google's official test ad unit IDs — always fill with test ads
        private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        private const val TAG = "AdManager"

        @Volatile
        private var instance: AdManager? = null

        fun getInstance(context: Context, adUnitId: String): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager(context.applicationContext, adUnitId).also { instance = it }
            }
        }
    }

    init {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "═══════════════════════════════════════════")
            Log.d(TAG, "AdManager initializing...")
            Log.d(TAG, "  Debug build: ${BuildConfig.DEBUG}")
            Log.d(TAG, "  Real ad unit: $adUnitId")
            Log.d(TAG, "  Effective ad unit: $effectiveAdUnitId")
            Log.d(TAG, "═══════════════════════════════════════════")
        }

        MobileAds.initialize(context) { initStatus ->
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "MobileAds initialized: ${initStatus.adapterStatusMap}")
            }
            // Load the first ad after SDK initialization
            loadInterstitialAd()
        }
    }

    /**
     * Loads the next interstitial ad in the background.
     */
    private fun loadInterstitialAd() {
        if (isLoading) {
            Log.d(TAG, "Already loading an ad, skipping duplicate request")
            return
        }
        if (interstitialAd != null) {
            Log.d(TAG, "Ad already loaded, skipping load request")
            return
        }

        isLoading = true
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Loading interstitial ad with unit: $effectiveAdUnitId")
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            effectiveAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoading = false
                    Log.e(TAG, "══ Ad FAILED to load ══")
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "  Error code: ${adError.code}")
                        Log.e(TAG, "  Message: ${adError.message}")
                        Log.e(TAG, "  Domain: ${adError.domain}")
                        Log.e(TAG, "  Ad Unit ID used: $effectiveAdUnitId")
                    }
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    Log.d(TAG, "══ Ad LOADED successfully! ══")
                    interstitialAd = ad
                }
            }
        )
    }

    /**
     * Increments the game session counter.
     * Call this when a game is launched.
     */
    fun onGameLaunched() {
        val currentCount = prefs.getInt(KEY_GAME_SESSION_COUNT, 0)
        val newCount = currentCount + 1
        prefs.edit().putInt(KEY_GAME_SESSION_COUNT, newCount).apply()
        Log.d(TAG, "Game launched: session count = $newCount")
    }

    /**
     * Determines if an ad should be shown based on:
     * 1. Cooldown period (2 minutes since last ad)
     * 2. Session count (every 3 games)
     */
    private fun shouldShowAd(): Boolean {
        val lastAdTimestamp = prefs.getLong(KEY_LAST_AD_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastAdTimestamp
        val cooldownMillis = COOLDOWN_MINUTES * 60 * 1000L

        val sessionCount = prefs.getInt(KEY_GAME_SESSION_COUNT, 0)

        Log.d(TAG, "shouldShowAd check: sessionCount=$sessionCount, timeSinceLastAd=${timeSinceLastAd/1000}s, cooldown=${cooldownMillis/1000}s")

        // Always allow first ad
        if (lastAdTimestamp == 0L && sessionCount >= 1) {
            Log.d(TAG, "  → YES: first ad, at least 1 game played")
            return true
        }

        if (timeSinceLastAd < cooldownMillis) {
            Log.d(TAG, "  → NO: cooldown active")
            return false
        }

        if (sessionCount < SESSIONS_BETWEEN_ADS) {
            Log.d(TAG, "  → NO: session count $sessionCount < $SESSIONS_BETWEEN_ADS")
            return false
        }

        Log.d(TAG, "  → YES: eligible")
        return true
    }

    /**
     * Shows an ad if conditions are met (cooldown + session count).
     *
     * @param activity The activity to show the ad on
     * @param onAdDismissed Callback when ad is dismissed or skipped
     */
    fun showAdIfEligible(activity: Activity, onAdDismissed: () -> Unit) {
        Log.d(TAG, "showAdIfEligible called. Ad loaded: ${interstitialAd != null}")

        if (!shouldShowAd()) {
            onAdDismissed()
            return
        }

        showAdNow(activity, onAdDismissed)
    }

    /**
     * Force show an ad immediately (used on game exit).
     */
    fun showAdOnExit(activity: Activity, onAdDismissed: () -> Unit) {
        Log.d(TAG, "showAdOnExit called. Ad loaded: ${interstitialAd != null}")
        showAdNow(activity, onAdDismissed)
    }

    /**
     * Internal: actually show an ad if one is loaded.
     */
    private fun showAdNow(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            Log.d(TAG, "══ SHOWING AD NOW ══")
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed by user")
                    interstitialAd = null
                    recordAdShown()
                    loadInterstitialAd() // Preload next
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed full screen")
                }
            }
            ad.show(activity)
        } else {
            Log.w(TAG, "No ad loaded! Loading one for next time...")
            loadInterstitialAd()
            onAdDismissed()
        }
    }

    private fun recordAdShown() {
        prefs.edit()
            .putLong(KEY_LAST_AD_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_GAME_SESSION_COUNT, 0)
            .apply()
    }
}
