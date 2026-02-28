package com.offlinegames.games.dotsandboxes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinegames.core.*
import com.offlinegames.engine.HapticController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Dots & Boxes.
 */
class DotsAndBoxesViewModel(
    private val vsAi: Boolean,
    private val hapticController: HapticController
) : ViewModel() {

    private val reducer = DotsAndBoxesReducer()
    private val ai = DotsAndBoxesAI()

    private val _state = MutableStateFlow(
        DotsAndBoxesState(
            gameState = DotsAndBoxesReducer.createInitialGameState(vsAi),
            vsAi = vsAi
        )
    )
    val state: StateFlow<DotsAndBoxesState> = _state.asStateFlow()

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

    private fun triggerAiMove(state: DotsAndBoxesState) {
        viewModelScope.launch {
            val aiMove = withContext(Dispatchers.Default) {
                ai.selectMove(state.gameState, state.currentPlayer)
            } ?: return@launch
            dispatch(GameIntent.MakeMove(aiMove))
        }
    }
}

class DotsAndBoxesViewModelFactory(
    private val vsAi: Boolean,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DotsAndBoxesViewModel(
            vsAi = vsAi,
            hapticController = HapticController(context)
        ) as T
    }
}
