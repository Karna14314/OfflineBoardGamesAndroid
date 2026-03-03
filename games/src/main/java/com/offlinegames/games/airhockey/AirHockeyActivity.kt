package com.offlinegames.games.airhockey

import android.annotation.SuppressLint
import android.os.Bundle
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.offlinegames.core.GameEffect
import com.offlinegames.core.TickListener
import com.offlinegames.engine.GameSurfaceView
import com.offlinegames.ui.components.rememberAdController
import kotlinx.coroutines.launch

/**
 * Air Hockey Game Activity.
 *
 * Real-time game with continuous rendering and physics.
 * Uses GameSurfaceView in continuous touch mode.
 */
class AirHockeyActivity : ComponentActivity() {

    private val viewModel: AirHockeyViewModel by viewModels {
        val vsAi = intent.getBooleanExtra("VS_AI", true)
        AirHockeyViewModelFactory(this, vsAi)
    }
    
    private val isVsAi: Boolean by lazy { intent.getBooleanExtra("VS_AI", true) }

    private lateinit var renderer: AirHockeyRenderer
    private lateinit var surfaceView: GameSurfaceView
    private lateinit var inputHandler: AirHockeyInputHandler

    /** Tick listener for physics + AI update on render thread. */
    private val physicsTickListener = object : TickListener {
        override fun onTick(deltaSeconds: Float) {
            viewModel.onPhysicsTick()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderer = AirHockeyRenderer(viewModel.physicsWorld)
        inputHandler = AirHockeyInputHandler(
            renderer = renderer,
            onPaddleMove = { x, y -> viewModel.movePlayer1Paddle(x, y) },
            onPlayer2Move = if (!isVsAi) {
                { x: Float, y: Float -> viewModel.movePlayer2Paddle(x, y) }
            } else null
        )

        surfaceView = GameSurfaceView(
            context = this,
            boardRenderer = renderer,
            pieceRenderer = renderer,
            inputHandler = inputHandler,
            continuousTouch = true  // Enable drag input
        )

        // Register physics tick listener and AI on surface creation
        surfaceView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                surfaceView.getGameThread()?.let { thread ->
                    thread.addTickListener(viewModel.physicsWorld)  // PhysicsWorld IS a TickListener
                    thread.addTickListener(physicsTickListener)      // AI updates
                }
            }
            override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, h2: Int) {}
            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {}
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    handleEffect(effect)
                }
            }
        }

        // Sync state periodically for UI scoreboards
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    surfaceView.updateState(state.gameState)
                }
            }
        }

        setContent {
            val adController = rememberAdController()
            
            AirHockeyScreen(
                viewModel = viewModel,
                surfaceView = surfaceView,
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
            is GameEffect.ShowMessage -> { /* toast */ }
            else -> Unit
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncPhysicsToState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AirHockeyScreen(
    viewModel: AirHockeyViewModel,
    surfaceView: GameSurfaceView,
    onBackPressed: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val adController = rememberAdController()
    val activity = androidx.compose.ui.platform.LocalContext.current as AirHockeyActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Air Hockey") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { surfaceView },
                modifier = Modifier.fillMaxSize()
            )

            // Game over dialog
            if (state.showResultDialog) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Game Over") },
                    text = {
                        val winner = if (state.score1 >= 7) "Player 1" else "CPU"
                        Text("$winner wins!\n\n${state.score1} – ${state.score2}")
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
