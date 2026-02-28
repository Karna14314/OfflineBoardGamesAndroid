package com.offlinegames.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.offlinegames.persistence.PreferencesRepository
import com.offlinegames.ui.screens.GameSelectScreen
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
    const val GAME_SELECT = "game_select"
    const val SETTINGS    = "settings"
    const val STATISTICS  = "statistics"
}

/**
 * Launches a game Activity by [gameId].
 * Each game registers its Activity class name here.
 */
private fun launchGame(context: Context, gameId: String) {
    val (activityClassName, vsAi) = when (gameId) {
        "tictactoe"    -> "com.offlinegames.games.tictactoe.TicTacToeActivity" to false
        "tictactoe_ai" -> "com.offlinegames.games.tictactoe.TicTacToeActivity" to true
        "connect4"     -> "com.offlinegames.games.connect4.Connect4Activity" to false
        "connect4_ai"  -> "com.offlinegames.games.connect4.Connect4Activity" to true
        "sos"          -> "com.offlinegames.games.sos.SOSActivity" to false
        "sos_ai"       -> "com.offlinegames.games.sos.SOSActivity" to true
        "dotsandboxes"    -> "com.offlinegames.games.dotsandboxes.DotsAndBoxesActivity" to false
        "dotsandboxes_ai" -> "com.offlinegames.games.dotsandboxes.DotsAndBoxesActivity" to true
        else           -> return
    }
    try {
        val cls = Class.forName(activityClassName)
        val intent = Intent(context, cls).apply {
            putExtra("vs_ai", vsAi)
        }
        context.startActivity(intent)
    } catch (_: ClassNotFoundException) {
        // Game module not linked (shouldn't happen in the assembled APK)
    }
}

/**
 * Root navigation graph.
 * Start destination is the main menu.
 */
@Composable
fun AppNavigation(preferencesRepository: PreferencesRepository) {
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
                onPlayClicked    = { navController.navigate(Routes.GAME_SELECT) },
                onSettingsClicked= { navController.navigate(Routes.SETTINGS) },
                onStatsClicked   = { navController.navigate(Routes.STATISTICS) }
            )
        }

        composable(Routes.GAME_SELECT) {
            GameSelectScreen(
                onGameSelected = { gameId -> launchGame(context, gameId) },
                onBack         = { navController.popBackStack() }
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
