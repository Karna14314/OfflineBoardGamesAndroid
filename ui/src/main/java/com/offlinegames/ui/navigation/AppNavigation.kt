package com.offlinegames.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.offlinegames.persistence.PreferencesRepository
import com.offlinegames.ui.screens.MainMenuScreen
import com.offlinegames.ui.screens.SettingsScreen
import com.offlinegames.ui.screens.StatisticsScreen
import com.offlinegames.ui.viewmodel.MainMenuViewModel
import com.offlinegames.ui.viewmodel.MainMenuViewModelFactory
import com.offlinegames.ui.viewmodel.SettingsViewModel
import com.offlinegames.ui.viewmodel.SettingsViewModelFactory

/** Route constants. */
object Routes {
    const val MAIN_MENU   = "main_menu"
    const val SETTINGS    = "settings"
    const val STATISTICS  = "statistics"
}

/**
 * Launches a game Activity by [gameId].
 * Each game registers its Activity class name here.
 */
private fun launchGame(context: Context, gameId: String) {
    val intent = when (gameId) {
        // Legacy placement-based games
        "tictactoe"    -> createGameIntent(context, "com.offlinegames.games.tictactoe.TicTacToeActivity", vsAi = false)
        "tictactoe_ai" -> createGameIntent(context, "com.offlinegames.games.tictactoe.TicTacToeActivity", vsAi = true)
        "connect4"     -> createGameIntent(context, "com.offlinegames.games.connect4.Connect4Activity", vsAi = false)
        "connect4_ai"  -> createGameIntent(context, "com.offlinegames.games.connect4.Connect4Activity", vsAi = true)
        "sos"          -> createGameIntent(context, "com.offlinegames.games.sos.SOSActivity", vsAi = false)
        "sos_ai"       -> createGameIntent(context, "com.offlinegames.games.sos.SOSActivity", vsAi = true)
        "dotsandboxes"    -> createGameIntent(context, "com.offlinegames.games.dotsandboxes.DotsAndBoxesActivity", vsAi = false)
        "dotsandboxes_ai" -> createGameIntent(context, "com.offlinegames.games.dotsandboxes.DotsAndBoxesActivity", vsAi = true)
        // New ActionBased games
        "game2048"     -> createGameIntent(context, "com.offlinegames.games.twenty48.Game2048Activity")
        "checkers"     -> createGameIntent(context, "com.offlinegames.games.checkers.CheckersActivity", vsAi = false)
        "checkers_ai"  -> createGameIntent(context, "com.offlinegames.games.checkers.CheckersActivity", vsAi = true)
        "minesweeper"       -> createMinesweeperIntent(context, 0) // BEGINNER
        "minesweeper_medium"-> createMinesweeperIntent(context, 1) // INTERMEDIATE
        "minesweeper_expert"-> createMinesweeperIntent(context, 2) // EXPERT
        // Ludo
        "ludo"       -> createLudoIntent(context, playerCount = 2, vsAi = false)
        "ludo_4p"    -> createLudoIntent(context, playerCount = 4, vsAi = false)
        "ludo_ai"    -> createLudoIntent(context, playerCount = 2, vsAi = true)
        "ludo_3bots" -> createLudoIntent(context, playerCount = 4, vsAi = true)
        // Air Hockey
        "airhockey"  -> createGameIntent(context, "com.offlinegames.games.airhockey.AirHockeyActivity", vsAi = false)
        "airhockey_ai" -> createGameIntent(context, "com.offlinegames.games.airhockey.AirHockeyActivity", vsAi = true)
        else           -> null
    }
    intent?.let { context.startActivity(it) }
}

private fun createGameIntent(context: Context, className: String, vsAi: Boolean? = null): Intent? {
    return try {
        val cls = Class.forName(className)
        Intent(context, cls).apply {
            vsAi?.let { putExtra("VS_AI", it) }
        }
    } catch (_: ClassNotFoundException) {
        null
    }
}

private fun createMinesweeperIntent(context: Context, difficulty: Int): Intent? {
    return try {
        val cls = Class.forName("com.offlinegames.games.minesweeper.MinesweeperActivity")
        Intent(context, cls).apply {
            putExtra("DIFFICULTY", difficulty)
        }
    } catch (_: ClassNotFoundException) {
        null
    }
}

private fun createLudoIntent(context: Context, playerCount: Int, vsAi: Boolean): Intent? {
    return try {
        val cls = Class.forName("com.offlinegames.games.ludo.LudoActivity")
        Intent(context, cls).apply {
            putExtra("PLAYER_COUNT", playerCount)
            putExtra("VS_AI", vsAi)
        }
    } catch (_: ClassNotFoundException) {
        null
    }
}

/**
 * Show an ad before launching a game, using the AdManager instance directly.
 * The adManager is passed as Any from the app module to avoid a direct dependency.
 * We use reflection once here (since this is the app boundary), but it's reliable
 * because we have the real object reference.
 */
private fun showAdThenLaunchGame(adManager: Any, activity: Activity, context: Context, gameId: String) {
    try {
        // Track game launch for ad frequency
        val onGameLaunchedMethod = adManager.javaClass.getMethod("onGameLaunched")
        onGameLaunchedMethod.invoke(adManager)
        Log.d("AppNavigation", "onGameLaunched called")

        // Try to show ad — showAdIfEligible(activity, callback)
        val function0Class = Class.forName("kotlin.jvm.functions.Function0")
        val showAdMethod = adManager.javaClass.getMethod("showAdIfEligible", Activity::class.java, function0Class)

        val callback: () -> Unit = {
            Log.d("AppNavigation", "Ad dismissed/skipped, launching game: $gameId")
            launchGame(context, gameId)
        }

        showAdMethod.invoke(adManager, activity, callback)
        Log.d("AppNavigation", "showAdIfEligible invoked for game: $gameId")
    } catch (e: Exception) {
        Log.e("AppNavigation", "Ad show failed: ${e.javaClass.simpleName}: ${e.message}", e)
        // If ad fails, just launch the game directly
        launchGame(context, gameId)
    }
}

/**
 * Root navigation graph.
 * Start destination is the main menu.
 */
@Composable
fun AppNavigation(
    preferencesRepository: PreferencesRepository,
    adManager: Any, // AdManager from app module
    activity: Activity
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN_MENU
    ) {
        composable(Routes.MAIN_MENU) {
            val vm: MainMenuViewModel = viewModel(
                factory = MainMenuViewModelFactory(preferencesRepository)
            )
            MainMenuScreen(
                viewModel        = vm,
                onGameSelected   = { gameId ->
                    // Show ad BEFORE launching the game
                    showAdThenLaunchGame(adManager, activity, context, gameId)
                },
                onSettingsClicked= { navController.navigate(Routes.SETTINGS) },
                onStatsClicked   = { navController.navigate(Routes.STATISTICS) }
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(preferencesRepository)
            )
            SettingsScreen(
                viewModel = vm,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen(onBack = { navController.popBackStack() })
        }
    }
}
