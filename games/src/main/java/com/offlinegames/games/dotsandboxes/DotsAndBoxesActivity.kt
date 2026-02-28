package com.offlinegames.games.dotsandboxes

import android.app.AlertDialog
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.offlinegames.core.GameEffect
import com.offlinegames.core.GameIntent
import com.offlinegames.core.GameResult
import com.offlinegames.engine.GameSurfaceView
import kotlinx.coroutines.launch

/**
 * Activity hosting the Dots & Boxes game surface.
 */
class DotsAndBoxesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VS_AI = "vs_ai"
    }

    private val vsAi by lazy { intent.getBooleanExtra(EXTRA_VS_AI, false) }

    private val viewModel: DotsAndBoxesViewModel by viewModels {
        DotsAndBoxesViewModelFactory(vsAi, applicationContext)
    }

    private lateinit var surfaceView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val renderer = DotsAndBoxesRenderer()
        val inputHandler = DotsAndBoxesInputHandler { intent ->
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
                viewModel.state.collect { dabState ->
                    surfaceView.updateState(dabState.gameState)
                    if (dabState.showResultDialog) {
                        showResultDialog(dabState)
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

    private fun showResultDialog(state: DotsAndBoxesState) {
        val rules = DotsAndBoxesRules()
        val p1 = state.scores[1] ?: 0
        val p2 = state.scores[2] ?: 0
        val title = when {
            state.result == GameResult.DRAW -> "It's a Draw! ($p1 - $p2)"
            else -> {
                val winner = rules.getWinner(state.gameState)
                "${winner?.name ?: "Player"} Wins! ($p1 - $p2) 🎉"
            }
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Would you like to play again?")
            .setPositiveButton("Play Again") { _, _ ->
                viewModel.dispatch(GameIntent.RestartGame)
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
