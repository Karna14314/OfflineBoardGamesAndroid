package com.offlinegames.games.checkers

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.offlinegames.ai.DifficultyProfile
import com.offlinegames.core.GameAction
import com.offlinegames.core.GameEffect
import com.offlinegames.core.GameIntent
import com.offlinegames.core.Move
import com.offlinegames.core.MoveType
import com.offlinegames.core.Position
import com.offlinegames.engine.GameSurfaceView
import com.offlinegames.ui.components.rememberAdController
import kotlinx.coroutines.launch

/**
 * Checkers Game Activity.
 */
class CheckersActivity : ComponentActivity() {

    private val viewModel: CheckersViewModel by viewModels {
        val vsAi = intent.getBooleanExtra("VS_AI", true)
        val difficultyOrdinal = intent.getIntExtra("DIFFICULTY", 1)
        val difficulty = DifficultyProfile.entries.getOrElse(difficultyOrdinal) { DifficultyProfile.MEDIUM }
        CheckersViewModelFactory(vsAi, difficulty, this)
    }

    private lateinit var renderer: CheckersRenderer
    private lateinit var surfaceView: GameSurfaceView
    private lateinit var inputHandler: CheckersInputHandler

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderer = CheckersRenderer()
        inputHandler = CheckersInputHandler(
            onSelectPiece = { position ->
                viewModel.selectPiece(position)
            },
            onMove = { from, to, isCapture ->
                val move = Move(
                    playerId = 0,
                    position = from,
                    type = if (isCapture) MoveType.JUMP else MoveType.SLIDE,
                    metadata = mapOf("toRow" to to.row, "toCol" to to.col)
                )
                if (isCapture) {
                    val capturedRow = (from.row + to.row) / 2
                    val capturedCol = (from.col + to.col) / 2
                    viewModel.dispatch(GameAction.CaptureAction(
                        move = move,
                        capturedPiece = Position(capturedRow, capturedCol)
                    ))
                } else {
                    viewModel.dispatch(GameAction.MovePieceAction(move))
                }
            },
            onAction = { action ->
                viewModel.dispatch(action)
            }
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
                    renderer.setSelection(state.selectedPiece, state.validMoves)
                }
            }
        }

        setContent {
            val adController = rememberAdController()
            
            CheckersScreen(
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

    override fun onPause() {
        super.onPause()
        viewModel.dispatch(GameIntent.SaveAndExit)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckersScreen(
    viewModel: CheckersViewModel,
    surfaceView: GameSurfaceView,
    inputHandler: CheckersInputHandler,
    onBackPressed: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val adController = rememberAdController()
    val activity = LocalContext.current as CheckersActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkers") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.dispatch(GameAction.RestartAction) }) {
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
            // Player info cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.currentPlayer.id == 1)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("White", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${state.board.countPieces(1)} pieces",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.currentPlayer.id == 2)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Black", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${state.board.countPieces(2)} pieces",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Chain capture indicator
            if (state.isChainCapture) {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        "Continue capturing!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

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
                                    event.x,
                                    event.y,
                                    view.width.toFloat(),
                                    view.height.toFloat()
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
                        Text("${state.gameState.winner()?.name ?: "?"} wins!")
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.dispatch(GameAction.RestartAction) }) {
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
