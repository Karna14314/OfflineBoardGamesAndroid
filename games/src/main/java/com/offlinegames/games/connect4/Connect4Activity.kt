package com.offlinegames.games.connect4

import android.app.AlertDialog
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.offlinegames.ai.DifficultyProfile
import com.offlinegames.core.GameEffect
import com.offlinegames.core.GameIntent
import com.offlinegames.core.GameResult
import com.offlinegames.engine.GameSurfaceView
import com.offlinegames.ui.components.showAdBeforeExit
import kotlinx.coroutines.launch

/**
 * Activity hosting the Connect 4 game surface.
 */
class Connect4Activity : AppCompatActivity() {

    companion object {
        const val EXTRA_VS_AI = "VS_AI"
        const val EXTRA_DIFFICULTY = "difficulty"
    }

    private val vsAi by lazy { intent.getBooleanExtra(EXTRA_VS_AI, false) }
    private val difficulty by lazy {
        val name = intent.getStringExtra(EXTRA_DIFFICULTY) ?: "MEDIUM"
        DifficultyProfile.entries.firstOrNull { it.name == name } ?: DifficultyProfile.MEDIUM
    }

    private val viewModel: Connect4ViewModel by viewModels {
        Connect4ViewModelFactory(vsAi, difficulty, applicationContext)
    }

    private lateinit var surfaceView: GameSurfaceView
    private lateinit var renderer: Connect4Renderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderer = Connect4Renderer()
        val inputHandler = Connect4InputHandler { intent ->
            viewModel.dispatch(intent)
        }

        surfaceView = GameSurfaceView(
            context = this,
            boardRenderer = renderer,
            pieceRenderer = renderer,
            inputHandler = inputHandler
        )

        setContentView(FrameLayout(this).apply {
            addView(surfaceView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        })

        observeState()
        observeEffects()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { c4State ->
                    surfaceView.updateState(c4State.gameState)

                    // Trigger drop animation
                    if (c4State.animatingDrop && c4State.lastDropCol >= 0) {
                        renderer.startDropAnimation(c4State.lastDropCol, c4State.lastDropRow)
                    }

                    if (c4State.showResultDialog) {
                        showResultDialog(c4State)
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
                        is GameEffect.ShowMessage -> { /* toast */ }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showResultDialog(state: Connect4State) {
        val title = when {
            state.result == GameResult.DRAW -> "It's a Draw!"
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

    override fun onBackPressed() {
        showAdBeforeExit {
            super.onBackPressed()
        }
    }
}
