package com.offlinegames.games.connect4

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinegames.ai.DifficultyProfile
import com.offlinegames.core.*
import com.offlinegames.engine.HapticController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Connect 4, following the same MVI pattern as TicTacToe.
 */
class Connect4ViewModel(
    private val vsAi: Boolean,
    private val difficulty: DifficultyProfile,
    private val hapticController: HapticController
) : ViewModel() {

    private val reducer = Connect4Reducer()
    private val ai = Connect4AI(difficulty)

    private val _state = MutableStateFlow(
        Connect4State(
            gameState = Connect4Reducer.createInitialGameState(vsAi),
            vsAi = vsAi
        )
    )
    val state: StateFlow<Connect4State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

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

    private fun dispatchEffects(effects: List<GameEffect>) {
        effects.forEach { effect ->
            when (effect) {
                is GameEffect.TriggerHaptic -> hapticController.click()
                is GameEffect.PlayWinSound -> hapticController.gameEnd()
                else -> Unit
            }
        }
    }

    private fun triggerAiMove(state: Connect4State) {
        viewModelScope.launch {
            val aiMove = withContext(Dispatchers.Default) {
                ai.selectMove(state.gameState, state.currentPlayer)
            } ?: return@launch
            dispatch(GameIntent.MakeMove(aiMove))
        }
    }
}

class Connect4ViewModelFactory(
    private val vsAi: Boolean,
    private val difficulty: DifficultyProfile,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return Connect4ViewModel(
            vsAi = vsAi,
            difficulty = difficulty,
            hapticController = HapticController(context)
        ) as T
    }
}
