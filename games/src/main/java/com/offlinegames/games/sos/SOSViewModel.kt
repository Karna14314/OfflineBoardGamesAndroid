package com.offlinegames.games.sos

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
 * ViewModel for the SOS game.
 */
class SOSViewModel(
    private val vsAi: Boolean,
    private val hapticController: HapticController
) : ViewModel() {

    private val reducer = SOSReducer()
    private val ai = SOSAI()

    private val _state = MutableStateFlow(
        SOSState(
            gameState = SOSReducer.createInitialGameState(vsAi),
            vsAi = vsAi
        )
    )
    val state: StateFlow<SOSState> = _state.asStateFlow()

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

    /** Toggle selected piece between S and O. */
    fun togglePieceType() {
        val current = _state.value.selectedPieceType
        _state.value = _state.value.copy(
            selectedPieceType = if (current == SOSRules.PIECE_S) SOSRules.PIECE_O else SOSRules.PIECE_S
        )
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

    private fun triggerAiMove(state: SOSState) {
        viewModelScope.launch {
            val aiMove = withContext(Dispatchers.Default) {
                ai.selectMove(state.gameState, state.currentPlayer)
            } ?: return@launch
            dispatch(GameIntent.MakeMove(aiMove))
        }
    }
}

class SOSViewModelFactory(
    private val vsAi: Boolean,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SOSViewModel(
            vsAi = vsAi,
            hapticController = HapticController(context)
        ) as T
    }
}
