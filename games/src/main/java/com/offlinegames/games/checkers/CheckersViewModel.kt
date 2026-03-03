package com.offlinegames.games.checkers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinegames.ai.DifficultyProfile
import com.offlinegames.core.*
import com.offlinegames.engine.HapticController
import com.offlinegames.persistence.SaveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Checkers game screen.
 */
class CheckersViewModel(
    private val vsAi: Boolean,
    private val difficulty: DifficultyProfile,
    private val hapticController: HapticController,
    private val saveManager: SaveManager
) : ViewModel() {

    private val reducer = CheckersReducer()
    private val ai = CheckersActionAI(difficulty)

    private val _state = MutableStateFlow(
        CheckersState(
            gameState = CheckersReducer.createInitialGameState(vsAi),
            vsAi = vsAi
        )
    )
    val state: StateFlow<CheckersState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    init {
        // Load saved game if exists
        viewModelScope.launch {
            val gameId = _state.value.gameState.gameId
            saveManager.loadSave(gameId).collect { saveData ->
                // TODO: Deserialize and restore
            }
        }
    }

    /** Entry point for all game actions. */
    fun dispatch(action: GameAction) {
        viewModelScope.launch {
            val (newState, effects) = reducer.reduce(_state.value, action)
            _state.value = newState
            effects.forEach { _effects.tryEmit(it) }
            dispatchEffects(effects)

            // Trigger AI if needed
            if (vsAi && newState.isOngoing && !newState.currentPlayer.isHuman && !newState.isChainCapture) {
                triggerAiMove(newState)
            }
        }
    }

    /** Legacy intent support. */
    fun dispatch(intent: GameIntent) {
        viewModelScope.launch {
            val (newState, effects) = reducer.reduce(_state.value, intent)
            _state.value = newState
            effects.forEach { _effects.tryEmit(it) }
            dispatchEffects(effects)

            if (vsAi && newState.isOngoing && !newState.currentPlayer.isHuman) {
                triggerAiMove(newState)
            }
        }
    }

    /**
     * Handle piece selection (for UI).
     */
    fun selectPiece(position: Position) {
        val newState = reducer.selectPiece(_state.value, position)
        _state.value = newState
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

    private fun triggerAiMove(state: CheckersState) {
        viewModelScope.launch {
            val aiAction = withContext(Dispatchers.Default) {
                ai.selectAction(state.gameState, state.currentPlayer)
            } ?: return@launch

            dispatch(aiAction)
        }
    }

    private fun saveGame(state: CheckersState) {
        viewModelScope.launch {
            // TODO: Serialize and save
            // val json = serializeState(state)
            // saveManager.writeSave(state.gameState.gameId, json)
        }
    }
}

/** ViewModelFactory for CheckersViewModel. */
class CheckersViewModelFactory(
    private val vsAi: Boolean,
    private val difficulty: DifficultyProfile,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CheckersViewModel(
            vsAi = vsAi,
            difficulty = difficulty,
            hapticController = HapticController(context),
            saveManager = SaveManager(context)
        ) as T
    }
}
