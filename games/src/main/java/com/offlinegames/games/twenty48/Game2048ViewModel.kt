package com.offlinegames.games.twenty48

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinegames.core.*
import com.offlinegames.engine.HapticController
import com.offlinegames.persistence.SaveManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the 2048 game screen.
 *
 * Responsibilities:
 * 1. Hold [Game2048State] as a [StateFlow]
 * 2. Accept [GameAction] from the UI / gesture handler
 * 3. Delegate to [Game2048Reducer] and update state
 * 4. Dispatch [GameEffect]s (sound, haptic, navigation)
 * 5. Handle persistence (autosave)
 */
class Game2048ViewModel(
    private val hapticController: HapticController,
    private val saveManager: SaveManager
) : ViewModel() {

    private val reducer = Game2048Reducer()

    private val _state = MutableStateFlow(
        Game2048State(
            gameState = Game2048Reducer.createInitialGameState()
        )
    )
    val state: StateFlow<Game2048State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    // Track previous board for animation
    private var previousBoard: Game2048Board? = null

    init {
        // Load saved game if exists
        viewModelScope.launch {
            val gameId = _state.value.gameState.gameId
            saveManager.loadSave(gameId).collect { saveData ->
                if (saveData != null) {
                    // TODO: Deserialize and restore state
                    // For now, start fresh
                }
            }
        }
    }

    /** Entry point for all game actions (swipes). */
    fun dispatch(action: GameAction) {
        viewModelScope.launch {
            val currentState = _state.value

            // Save previous board for animation
            previousBoard = currentState.board.copy()

            val (newState, effects) = reducer.reduce(currentState, action)
            _state.value = newState

            effects.forEach { _effects.tryEmit(it) }
            dispatchEffects(effects)

            // Autosave after each move
            if (action is GameAction.MergeTilesAction) {
                saveGame(newState)
            }
        }
    }

    /** Legacy support for intents. */
    fun dispatch(intent: GameIntent) {
        viewModelScope.launch {
            val (newState, effects) = reducer.reduce(_state.value, intent)
            _state.value = newState
            effects.forEach { _effects.tryEmit(it) }
            dispatchEffects(effects)

            if (intent is GameIntent.SaveAndExit) {
                saveGame(newState)
            }
        }
    }

    private fun dispatchEffects(effects: List<GameEffect>) {
        effects.forEach { effect ->
            when (effect) {
                is GameEffect.TriggerHaptic -> hapticController.click()
                is GameEffect.PlayWinSound -> hapticController.gameEnd()
                else -> Unit
            }
        }
    }

    private fun saveGame(state: Game2048State) {
        viewModelScope.launch {
            // Serialize and save
            // val json = serializeState(state)
            // saveManager.writeSave(state.gameState.gameId, json)
        }
    }

    /** Get the previous board for animation interpolation. */
    fun getPreviousBoard(): Game2048Board? = previousBoard

    companion object {
        const val SAVE_KEY_2048 = "game_2048"
    }
}

/** Manual ViewModelFactory for Game2048ViewModel. */
class Game2048ViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return Game2048ViewModel(
            hapticController = HapticController(context),
            saveManager = SaveManager(context)
        ) as T
    }
}
