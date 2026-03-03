package com.offlinegames.games.minesweeper

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinegames.core.GameAction
import com.offlinegames.core.GameEffect
import com.offlinegames.core.GameIntent
import com.offlinegames.engine.HapticController
import com.offlinegames.persistence.SaveManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Minesweeper game screen.
 */
class MinesweeperViewModel(
    private val difficulty: MinesweeperDifficulty,
    private val hapticController: HapticController,
    private val saveManager: SaveManager
) : ViewModel() {

    private val reducer = MinesweeperReducer()

    private val _state = MutableStateFlow(
        MinesweeperState(
            gameState = MinesweeperReducer.createInitialGameState(difficulty)
        )
    )
    val state: StateFlow<MinesweeperState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    init {
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

            if (action is GameAction.RevealCellsAction || action is GameAction.FlagCellAction || action is GameAction.ChordAction) {
                saveGame(newState)
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

    private fun saveGame(state: MinesweeperState) {
        viewModelScope.launch {
            // TODO: Serialize and save
        }
    }
}

/** ViewModelFactory for MinesweeperViewModel. */
class MinesweeperViewModelFactory(
    private val difficulty: MinesweeperDifficulty,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MinesweeperViewModel(
            difficulty = difficulty,
            hapticController = HapticController(context),
            saveManager = SaveManager(context)
        ) as T
    }
}
