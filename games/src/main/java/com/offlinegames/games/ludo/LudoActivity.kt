package com.offlinegames.games.ludo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.offlinegames.core.GameAction
import com.offlinegames.core.GameEffect
import com.offlinegames.core.GameIntent
import com.offlinegames.engine.GameSurfaceView
import com.offlinegames.ui.components.rememberAdController
import kotlinx.coroutines.launch

/**
 * Ludo Game Activity.
 *
 * Hosts the SurfaceView-based renderer and dispatches touch events.
 * Supports 2–4 local players, with optional AI opponents.
 */
class LudoActivity : ComponentActivity() {

    private val viewModel: LudoViewModel by viewModels {
        val playerCount = intent.getIntExtra("PLAYER_COUNT", 2)
        val vsAi = intent.getBooleanExtra("VS_AI", false)
        LudoViewModelFactory(playerCount, vsAi, this)
    }

    private lateinit var renderer: LudoRenderer
    private lateinit var surfaceView: GameSurfaceView
    private lateinit var inputHandler: LudoInputHandler

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderer = LudoRenderer()
        inputHandler = LudoInputHandler(
            renderer = renderer,
            onRollDice = { viewModel.rollDice() },
            onSelectToken = { position -> viewModel.selectTokenAtPosition(position) }
        )

        surfaceView = GameSurfaceView(
            context = this,
            boardRenderer = renderer,
            pieceRenderer = renderer,
            inputHandler = inputHandler
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    handleEffect(effect)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    surfaceView.updateState(state.gameState)
                    renderer.setMovableTokens(state.movableTokenIds)
                    // Use animatingDiceValue so dice always shows last rolled face (never blank)
                    renderer.setDiceState(state.animatingDiceValue, state.board.diceRolled)
                }
            }
        }

        setContent {
            val adController = rememberAdController()
            
            LudoScreen(
                viewModel = viewModel,
                surfaceView = surfaceView,
                inputHandler = inputHandler,
                onBackPressed = {
                    adController.showAdOnExit(this) {
                        finish()
                    }
                }
            )
        }
    }

    private fun handleEffect(effect: GameEffect) {
        when (effect) {
            is GameEffect.ShowMessage -> {
                // Could show a toast
            }
            else -> Unit
        }
    }

    // Don't auto-save on pause — always start fresh
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LudoScreen(
    viewModel: LudoViewModel,
    surfaceView: GameSurfaceView,
    inputHandler: LudoInputHandler,
    onBackPressed: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val adController = rememberAdController()
    val activity = androidx.compose.ui.platform.LocalContext.current as LudoActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ludo") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.restart() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Game board
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { surfaceView },
                    modifier = Modifier.fillMaxSize()
                ) { view ->
                    view.setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_UP -> {
                                inputHandler.onTouch(
                                    event.x, event.y,
                                    view.width.toFloat(), view.height.toFloat()
                                )
                                true
                            }
                            else -> true
                        }
                    }
                }
            }

            // Game over dialog
            if (state.showResultDialog) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Game Over") },
                    text = {
                        Text("${state.gameState.winner()?.name ?: state.currentPlayer.name} wins!")
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.restart() }) {
                            Text("Play Again")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            adController.showAdOnExit(activity) {
                                onBackPressed()
                            }
                        }) {
                            Text("Exit")
                        }
                    }
                )
            }
        }
    }
}
