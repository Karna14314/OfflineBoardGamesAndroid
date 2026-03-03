package com.offlinegames.games.twenty48

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.offlinegames.core.GameAction
import com.offlinegames.core.GameEffect
import com.offlinegames.core.GameIntent
import com.offlinegames.core.SwipeDirection
import com.offlinegames.engine.GameSurfaceView
import com.offlinegames.ui.components.rememberAdController
import kotlinx.coroutines.launch

/**
 * 2048 Game Activity.
 *
 * Uses a SurfaceView for game rendering with Canvas-based animations.
 * Swipe gestures are detected using Android's GestureDetector and
 * converted to [GameAction.MergeTilesAction].
 */
class Game2048Activity : ComponentActivity() {

    private val viewModel: Game2048ViewModel by viewModels {
        Game2048ViewModelFactory(this)
    }

    private lateinit var renderer: Game2048Renderer
    private lateinit var surfaceView: GameSurfaceView
    private lateinit var gestureDetector: GestureDetector
    private lateinit var inputHandler: Game2048InputHandler

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderer = Game2048Renderer()
        inputHandler = Game2048InputHandler { action ->
            viewModel.dispatch(action)
        }

        // Setup gesture detector for swipe detection
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                return when {
                    abs(diffX) > abs(diffY) -> {
                        if (diffX > 0) inputHandler.onSwipe(SwipeDirection.RIGHT)
                        else inputHandler.onSwipe(SwipeDirection.LEFT)
                        true
                    }
                    else -> {
                        if (diffY > 0) inputHandler.onSwipe(SwipeDirection.DOWN)
                        else inputHandler.onSwipe(SwipeDirection.UP)
                        true
                    }
                }
            }
        })

        // Create surface view
        surfaceView = GameSurfaceView(
            context = this,
            boardRenderer = renderer,
            pieceRenderer = renderer,
            inputHandler = inputHandler
        )

        // Handle effects
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    handleEffect(effect)
                }
            }
        }

        // Update surface view when state changes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    surfaceView.updateState(state.gameState)
                    renderer.onBoardChanged(viewModel.getPreviousBoard(), state.board)
                }
            }
        }

        setContent {
            val adController = rememberAdController()
            
            Game2048Screen(
                viewModel = viewModel,
                surfaceView = surfaceView,
                gestureDetector = gestureDetector,
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
                // Could show a toast/snackbar
            }
            else -> Unit
        }
    }

    override fun onPause() {
        super.onPause()
        // Autosave
        viewModel.dispatch(GameIntent.SaveAndExit)
    }

    private inline fun <T : Comparable<T>> max(a: T, b: T): T = if (a > b) a else b
    private inline fun <T : Comparable<T>> min(a: T, b: T): T = if (a < b) a else b
    private fun abs(value: Float): Float = if (value < 0) -value else value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Game2048Screen(
    viewModel: Game2048ViewModel,
    surfaceView: GameSurfaceView,
    gestureDetector: GestureDetector,
    inputHandler: Game2048InputHandler,
    onBackPressed: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val adController = rememberAdController()
    val activity = context as Game2048Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("2048") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Undo button
                    IconButton(
                        onClick = { viewModel.dispatch(GameAction.UndoAction) },
                        enabled = state.canUndo
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    // Restart button
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
            // Score display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Score", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${state.currentScore}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Best", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${state.bestScore}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
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
                    // Forward touch events to gesture detector
                    view.setOnTouchListener { _, event ->
                        gestureDetector.onTouchEvent(event)
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                inputHandler.onTouchDown(event.x, event.y)
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                inputHandler.onTouchUp(event.x, event.y)
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
                    title = {
                        Text(
                            if (state.hasWinningTile()) "You Win!" else "Game Over!"
                        )
                    },
                    text = {
                        Text("Final Score: ${state.currentScore}")
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
