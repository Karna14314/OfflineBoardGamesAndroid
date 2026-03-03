package com.offlinegames.games.sos

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
import com.offlinegames.ui.components.showAdBeforeExit
import kotlinx.coroutines.launch

/**
 * Activity hosting the SOS game surface.
 */
class SOSActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VS_AI = "VS_AI"
    }

    private val vsAi by lazy { intent.getBooleanExtra(EXTRA_VS_AI, false) }

    private val viewModel: SOSViewModel by viewModels {
        SOSViewModelFactory(vsAi, applicationContext)
    }

    private lateinit var surfaceView: GameSurfaceView
    private lateinit var renderer: SOSRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderer = SOSRenderer()
        val inputHandler = SOSInputHandler(
            onIntent = { intent -> viewModel.dispatch(intent) },
            getPieceType = { viewModel.state.value.selectedPieceType },
            onTogglePiece = { viewModel.togglePieceType() }
        )

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
                viewModel.state.collect { sosState ->
                    surfaceView.updateState(sosState.gameState)
                    renderer.selectedPieceType = sosState.selectedPieceType
                    if (sosState.showResultDialog) {
                        showResultDialog(sosState)
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

    private fun showResultDialog(state: SOSState) {
        val rules = SOSRules()
        val p1Score = state.scores[1] ?: 0
        val p2Score = state.scores[2] ?: 0
        val title = when {
            state.result == GameResult.DRAW -> "It's a Draw! ($p1Score - $p2Score)"
            else -> {
                val winner = rules.getWinner(state.gameState)
                "${winner?.name ?: "Player"} Wins! ($p1Score - $p2Score) 🎉"
            }
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
