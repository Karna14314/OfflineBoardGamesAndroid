package com.offlinegames.games.minesweeper

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.offlinegames.core.GameAction
import com.offlinegames.core.GameEffect
import com.offlinegames.engine.GameSurfaceView
import com.offlinegames.ui.components.rememberAdController
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

/**
 * Minesweeper Game Activity.
 */
class MinesweeperActivity : ComponentActivity() {

    private val viewModel: MinesweeperViewModel by viewModels {
        val difficultyOrdinal = intent.getIntExtra("DIFFICULTY", 0)
        val difficulty = MinesweeperDifficulty.entries.getOrElse(difficultyOrdinal) { MinesweeperDifficulty.BEGINNER }
        MinesweeperViewModelFactory(difficulty, this)
    }

    private lateinit var renderer: MinesweeperRenderer
    private lateinit var surfaceView: GameSurfaceView
    private lateinit var inputHandler: MinesweeperInputHandler
    private lateinit var gestureDetector: GestureDetector

    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderer = MinesweeperRenderer()
        inputHandler = MinesweeperInputHandler(
            onAction = { action -> viewModel.dispatch(action) },
            getBoardState = {
                try {
                    viewModel.state.value.board
                } catch (_: Exception) {
                    null
                }
            }
        )

        // Setup gesture detector for long press
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                inputHandler.onLongPress()
            }
        })

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
                }
            }
        }

        // Timer
        startTimer()

        setContent {
            val adController = rememberAdController()
            
            MinesweeperScreen(
                viewModel = viewModel,
                surfaceView = surfaceView,
                inputHandler = inputHandler,
                gestureDetector = gestureDetector,
                onBackPressed = { 
                    adController.showAdOnExit(this) {
                        finish()
                    }
                }
            )
        }
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                // Update timer if game is ongoing
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(timerRunnable!!, 1000)
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
        timerRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunnable?.let { handler.removeCallbacks(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinesweeperScreen(
    viewModel: MinesweeperViewModel,
    surfaceView: GameSurfaceView,
    inputHandler: MinesweeperInputHandler,
    gestureDetector: GestureDetector,
    onBackPressed: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val board = state.board
    var flagMode by remember { mutableStateOf(false) }
    val adController = rememberAdController()
    val activity = LocalContext.current as MinesweeperActivity

    // Sync flag mode with input handler
    LaunchedEffect(flagMode) {
        inputHandler.flagMode = flagMode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minesweeper") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Flag mode toggle
                    IconButton(onClick = { flagMode = !flagMode }) {
                        Icon(
                            imageVector = if (flagMode) Icons.Default.Flag else Icons.Default.Search,
                            contentDescription = if (flagMode) "Flag Mode (tap to flag)" else "Dig Mode (tap to reveal)",
                            tint = if (flagMode)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
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
            // Game info cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mines", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${state.minesRemaining}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // Mode indicator card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (flagMode)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mode", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (flagMode) "🚩 Flag" else "⛏ Dig",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Time", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${state.timerSeconds}",
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
                    view.setOnTouchListener { _, event ->
                        gestureDetector.onTouchEvent(event)

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                inputHandler.onTouchDown(event.x, event.y)
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                inputHandler.onTouchMove(event.x, event.y)
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                inputHandler.onTouchUp(
                                    event.x,
                                    event.y,
                                    view.width.toFloat(),
                                    view.height.toFloat(),
                                    board.width,
                                    board.height
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
                    title = {
                        Text(if (state.gameState.result == com.offlinegames.core.GameResult.WIN) "You Win!" else "Game Over!")
                    },
                    text = {
                        Text("Time: ${state.timerSeconds}s")
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
