package com.offlinegames.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.*

/**
 * Composable wrapper for ad integration.
 * Provides a simple way to show ads on game exit.
 */
@Composable
fun rememberAdController(): AdController {
    return remember { AdController() }
}

/**
 * Controller for managing ad display in games.
 */
class AdController {
    
    /**
     * Shows an ad when user exits a game, then executes the callback.
     * This ALWAYS tries to show an ad (force display on exit).
     */
    fun showAdOnExit(activity: Activity, onComplete: () -> Unit) {
        try {
            android.util.Log.d("AdIntegration", "showAdOnExit: starting reflection...")
            val appContext = activity.applicationContext
            val adManagerClass = Class.forName("com.offlinegames.ads.AdManager")
            
            // Get the companion object for getInstance
            val companionField = adManagerClass.getDeclaredField("Companion")
            val companion = companionField.get(null)
            val companionClass = companion.javaClass
            
            // Get ad unit ID from BuildConfig
            val buildConfigClass = Class.forName("com.offlinegames.BuildConfig")
            val adUnitIdField = buildConfigClass.getField("ADMOB_INTERSTITIAL_AD_UNIT_ID")
            val adUnitId = adUnitIdField.get(null) as String
            android.util.Log.d("AdIntegration", "showAdOnExit: adUnitId=$adUnitId")
            
            // Get AdManager instance via companion.getInstance(context, adUnitId)
            val getInstanceMethod = companionClass.getMethod("getInstance", Context::class.java, String::class.java)
            val adManager = getInstanceMethod.invoke(companion, appContext, adUnitId)
            android.util.Log.d("AdIntegration", "showAdOnExit: got AdManager instance")
            
            // Call showAdOnExit(activity, callback) using the JVM Function0 type
            val function0Class = Class.forName("kotlin.jvm.functions.Function0")
            val showAdMethod = adManagerClass.getMethod("showAdOnExit", Activity::class.java, function0Class)
            showAdMethod.invoke(adManager, activity, onComplete)
            android.util.Log.d("AdIntegration", "showAdOnExit: reflection call succeeded!")
        } catch (e: Exception) {
            android.util.Log.e("AdIntegration", "showAdOnExit FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            onComplete()
        }
    }
}

/**
 * Extension function for non-Compose activities.
 */
fun Activity.showAdBeforeExit(onComplete: () -> Unit) {
    try {
        android.util.Log.d("AdIntegration", "showAdBeforeExit: starting reflection...")
        val appContext = applicationContext
        val adManagerClass = Class.forName("com.offlinegames.ads.AdManager")
        
        // Get companion for getInstance
        val companionField = adManagerClass.getDeclaredField("Companion")
        val companion = companionField.get(null)
        val companionClass = companion.javaClass
        
        // Get ad unit ID from BuildConfig
        val buildConfigClass = Class.forName("com.offlinegames.BuildConfig")
        val adUnitIdField = buildConfigClass.getField("ADMOB_INTERSTITIAL_AD_UNIT_ID")
        val adUnitId = adUnitIdField.get(null) as String
        android.util.Log.d("AdIntegration", "showAdBeforeExit: adUnitId=$adUnitId")
        
        // Get AdManager instance
        val getInstanceMethod = companionClass.getMethod("getInstance", Context::class.java, String::class.java)
        val adManager = getInstanceMethod.invoke(companion, appContext, adUnitId)
        android.util.Log.d("AdIntegration", "showAdBeforeExit: got AdManager instance")
        
        // Call showAdOnExit with proper JVM Function0 type
        val function0Class = Class.forName("kotlin.jvm.functions.Function0")
        val showAdMethod = adManagerClass.getMethod("showAdOnExit", Activity::class.java, function0Class)
        showAdMethod.invoke(adManager, this, onComplete)
        android.util.Log.d("AdIntegration", "showAdBeforeExit: reflection call succeeded!")
    } catch (e: Exception) {
        android.util.Log.e("AdIntegration", "showAdBeforeExit FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
        onComplete()
    }
}
