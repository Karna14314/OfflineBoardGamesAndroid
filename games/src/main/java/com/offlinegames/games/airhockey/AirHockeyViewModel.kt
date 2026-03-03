package com.offlinegames.games.airhockey

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinegames.core.*
import com.offlinegames.engine.HapticController
import com.offlinegames.engine.PhysicsWorld
import com.offlinegames.persistence.SaveManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Air Hockey game.
 *
 * Unlike turn-based viewmodels, this one:
 * - Owns the PhysicsWorld (shared with renderer and AI)
 * - Syncs physics state → GameState periodically for persistence
 * - Does NOT process frame updates through the reducer
 */
class AirHockeyViewModel(
    private val hapticController: HapticController,
    private val saveManager: SaveManager,
    private val vsAi: Boolean
) : ViewModel() {

    val physicsWorld = PhysicsWorld()
    val ai = AirHockeyAI()

    private val _state = MutableStateFlow(
        AirHockeyState(
            gameState = createInitialGameState(vsAi)
        )
    )
    val state: StateFlow<AirHockeyState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    init {
        // Set up goal callback
        physicsWorld.onGoalScored = { scoringPlayer ->
            hapticController.click()
            syncPhysicsToState()

            if (physicsWorld.isGameOver) {
                hapticController.gameEnd()
                _state.value = _state.value.copy(showResultDialog = true)
                _effects.tryEmit(GameEffect.PlayWinSound)
            }
        }

        // Serve the puck
        physicsWorld.servePuck()
    }

    /** Move player 1's paddle (touch input). */
    fun movePlayer1Paddle(x: Float, y: Float) {
        physicsWorld.movePlayer1(x, y)
    }

    /** Move player 2's paddle (touch input in 2P mode). */
    fun movePlayer2Paddle(x: Float, y: Float) {
        physicsWorld.movePlayer2(x, y)
    }

    /** Called by the render thread's tick listener. */
    fun onPhysicsTick() {
        if (!physicsWorld.isGameOver && vsAi) {
            ai.update(physicsWorld)
        }
    }

    /** Sync physics state to GameState for UI display and persistence. */
    fun syncPhysicsToState() {
        val board = AirHockeyBoard(
            score1 = physicsWorld.score1,
            score2 = physicsWorld.score2,
            physics = physicsWorld.snapshot()
        )

        val currentState = _state.value.gameState
        val result = if (physicsWorld.isGameOver) GameResult.WIN else GameResult.IN_PROGRESS

        // Determine winner for GameState.winner()
        val winnerPlayer = if (physicsWorld.isGameOver) {
            if (physicsWorld.score1 >= physicsWorld.winScore) currentState.players[0]
            else currentState.players[1]
        } else currentState.currentPlayer

        val newGameState = currentState.copy(
            boardData = board,
            result = result,
            currentPlayer = winnerPlayer
        )

        _state.value = _state.value.copy(gameState = newGameState)
    }

    /** Restart the game. */
    fun restart() {
        physicsWorld.reset()
        physicsWorld.servePuck()
        _state.value = AirHockeyState(gameState = createInitialGameState(vsAi))
    }

    /** Pause the physics. */
    fun pause() {
        // Physics is paused via the TickListener in GameThread
        syncPhysicsToState()
        viewModelScope.launch {
            try {
                val snapshot = physicsWorld.snapshot()
                val json = Gson().toJson(snapshot)
                saveManager.writeSave("airhockey", json)
            } catch (_: Exception) { }
        }
    }

    companion object {
        fun createInitialGameState(vsAi: Boolean): GameState {
            val players = listOf(
                Player(id = 1, name = "Player 1"),
                Player(id = 2, name = if (vsAi) "CPU" else "Player 2", isHuman = !vsAi)
            )
            return GameState(
                gameId = "airhockey",
                players = players,
                currentPlayer = players.first(),
                boardData = AirHockeyBoard(),
                result = GameResult.IN_PROGRESS
            )
        }
    }
}

/** ViewModelFactory for AirHockeyViewModel. */
class AirHockeyViewModelFactory(
    private val context: Context,
    private val vsAi: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AirHockeyViewModel(
            hapticController = HapticController(context),
            saveManager = SaveManager(context),
            vsAi = vsAi
        ) as T
    }
}
