package com.offlinegames.games.tictactoe

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.offlinegames.ai.DifficultyProfile
import com.offlinegames.core.GameEffect
import com.offlinegames.core.GameIntent
import com.offlinegames.engine.GameSurfaceView
import com.offlinegames.ui.components.DistractionFreeGameScreen
import com.offlinegames.ui.theme.OfflineGamesTheme
import com.offlinegames.ui.components.showAdBeforeExit
import kotlinx.coroutines.launch

/**
 * Activity that hosts the TicTacToe game surface wrapped in DistractionFreeGameScreen.
 */
class TicTacToeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VS_AI      = "VS_AI"
        const val EXTRA_DIFFICULTY = "difficulty"
    }

    private val vsAi by lazy {
        intent.getBooleanExtra(EXTRA_VS_AI, false)
    }

    private val difficulty by lazy {
        val name = intent.getStringExtra(EXTRA_DIFFICULTY) ?: "MEDIUM"
        DifficultyProfile.entries.firstOrNull { it.name == name } ?: DifficultyProfile.MEDIUM
    }

    private val viewModel: TicTacToeViewModel by viewModels {
        TicTacToeViewModelFactory(vsAi, difficulty, applicationContext)
    }

    private lateinit var surfaceView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tttRenderer = TicTacToeRenderer()
        val inputHandler = TicTacToeInputHandler { intent ->
            viewModel.dispatch(intent)
        }

        surfaceView = GameSurfaceView(
            context       = this,
            boardRenderer = tttRenderer,
            pieceRenderer = tttRenderer,
            inputHandler  = inputHandler
        )

        setContent {
            val state by viewModel.state.collectAsState()
            val turnString = state.currentPlayer.name

            OfflineGamesTheme {
                DistractionFreeGameScreen(
                    playerNameTurn = turnString,
                    surfaceView = surfaceView,
                    onRestart = { viewModel.dispatch(GameIntent.RestartGame) },
                    onExit = { 
                        showAdBeforeExit {
                            finish()
                        }
                    },
                    soundEnabled = true, // Hook to real settings later
                    onToggleSound = { /* Hook to settings */ }
                )
            }
        }

        observeState()
        observeEffects()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { tttState ->
                    surfaceView.updateState(tttState.gameState)
                    if (tttState.showResultDialog) {
                        showResultDialog(tttState)
                    }
                }
            }
        }
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is GameEffect.ShowMessage -> {
                            // Toast-style messages can be added here
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showResultDialog(state: TicTacToeState) {
        val title = when {
            state.result == com.offlinegames.core.GameResult.DRAW -> "It's a Draw!"
            else -> "${state.gameState.winner()?.name ?: "Player"} Wins! 🎉"
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Would you like to play again?")
            .setPositiveButton("Play Again") { _, _ ->
                viewModel.dispatch(GameIntent.RestartGame)
            }
            .setNegativeButton("Exit") { _, _ ->
                showAdBeforeExit {
                    finish()
                }
            }
            .setCancelable(false)
            .show()
    }
}
