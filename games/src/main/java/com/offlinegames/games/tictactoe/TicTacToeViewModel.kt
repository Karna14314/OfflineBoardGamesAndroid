package com.offlinegames.games.tictactoe

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
 * ViewModel for the TicTacToe game screen.
 *
 * Responsibilities (following MVI):
 * 1. Hold [TicTacToeState] as a [StateFlow]
 * 2. Accept [GameIntent] from the UI / input handler
 * 3. Delegate to [TicTacToeReducer] and update state
 * 4. Dispatch [GameEffect]s (sound, haptic, navigation)
 * 5. Trigger AI moves on the coroutine dispatcher
 *
 * No game rules live here — everything is in [TicTacToeReducer] / [TicTacToeRules].
 */
class TicTacToeViewModel(
    private val vsAi: Boolean,
    private val difficulty: DifficultyProfile,
    private val hapticController: HapticController
) : ViewModel() {

    private val reducer = TicTacToeReducer()
    private val ai = TicTacToeAI(difficulty)

    private val _state = MutableStateFlow(
        TicTacToeState(
            gameState = TicTacToeReducer.createInitialGameState(vsAi),
            vsAi = vsAi
        )
    )
    val state: StateFlow<TicTacToeState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    /** Entry point for all UI actions. */
    fun dispatch(intent: GameIntent) {
        viewModelScope.launch {
            val (newState, effects) = reducer.reduce(_state.value, intent)
            _state.value = newState
            effects.forEach { _effects.tryEmit(it) }
            dispatchEffects(effects)

            // If versus AI and game is ongoing and it's the AI's turn
            if (vsAi && newState.isOngoing && !newState.currentPlayer.isHuman) {
                triggerAiMove(newState)
            }
        }
    }

    private fun dispatchEffects(effects: List<GameEffect>) {
        effects.forEach { effect ->
            when (effect) {
                is GameEffect.TriggerHaptic  -> hapticController.click()
                is GameEffect.PlayWinSound   -> hapticController.gameEnd()
                else                         -> Unit
            }
        }
    }

    private fun triggerAiMove(state: TicTacToeState) {
        viewModelScope.launch {
            val aiMove = withContext(Dispatchers.Default) {
                ai.selectMove(state.gameState, state.currentPlayer)
            } ?: return@launch

            dispatch(GameIntent.MakeMove(aiMove))
        }
    }
}

/** Manual ViewModelFactory for TicTacToeViewModel. */
class TicTacToeViewModelFactory(
    private val vsAi: Boolean,
    private val difficulty: DifficultyProfile,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TicTacToeViewModel(
            vsAi = vsAi,
            difficulty = difficulty,
            hapticController = HapticController(context)
        ) as T
    }
}
