package com.offlinegames.games.ludo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinegames.core.*
import com.offlinegames.engine.HapticController
import com.offlinegames.persistence.SaveManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Ludo game screen.
 *
 * Manages:
 * - Game state via LudoReducer
 * - Dice rolling with delay/animation
 * - AI turn automation (fully autonomous)
 * - No save restoration — always starts fresh
 */
class LudoViewModel(
    private val playerCount: Int,
    private val vsAi: Boolean,
    private val hapticController: HapticController,
    private val saveManager: SaveManager
) : ViewModel() {

    private val reducer = LudoReducer()
    private val ai = LudoAI()
    private val gson = Gson()

    private val _state = MutableStateFlow(
        LudoState(
            gameState = createInitialGameState(playerCount, vsAi),
            playerCount = playerCount
        )
    )
    val state: StateFlow<LudoState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    init {
        // Always start fresh — no save restoration
        // Clear any old saves
        viewModelScope.launch {
            try { saveManager.writeSave("ludo", "") } catch (_: Exception) {}
            try { saveManager.writeSave("ludo_${playerCount}_${if (vsAi) "ai" else "2p"}", "") } catch (_: Exception) {}
        }

        // If vs AI and player 1 is human, AI doesn't go first — but if for some
        // reason player 1 is not human, trigger AI
        viewModelScope.launch {
            delay(300)
            checkAndTriggerAi()
        }
    }

    /** Roll the dice for the current player. */
    fun rollDice() {
        val currentState = _state.value
        if (currentState.board.diceRolled || !currentState.isOngoing) return

        val diceResult = (1..6).random()
        dispatch(GameAction.DiceRollAction(diceResult))
    }

    /** Select and move a token (from tapping on the board). */
    fun selectTokenAtPosition(position: Position) {
        val currentState = _state.value
        if (!currentState.board.diceRolled || !currentState.isOngoing) return

        val board = currentState.board
        val playerId = currentState.currentPlayer.id - 1

        val diceValue = board.diceValue ?: return
        val movableTokens = board.getPlayerTokens(playerId)
            .filter { board.canMoveToken(it, diceValue) }

        val clickedToken = movableTokens.find { token ->
            val tokenPos = LudoPath.positionForStep(token.playerId, token.step, token.tokenIdx)
            tokenPos == position
        }

        if (clickedToken != null) {
            dispatch(GameAction.TokenMoveAction(
                tokenId = clickedToken.id,
                playerId = playerId,
                steps = diceValue
            ))
        }
    }

    /** Dispatch a game action through the reducer. */
    fun dispatch(action: GameAction) {
        viewModelScope.launch {
            val (newState, effects) = reducer.reduce(_state.value, action)
            _state.value = newState
            effects.forEach { _effects.tryEmit(it) }
            dispatchEffects(effects)

            if (!newState.isOngoing) return@launch

            when {
                // After a dice roll: handle the result
                action is GameAction.DiceRollAction -> {
                    handlePostDiceRoll(newState)
                }
                // After any other action: check if AI should play next
                else -> {
                    checkAndTriggerAi()
                }
            }
        }
    }

    /**
     * Handle the state after a dice roll.
     * This is where we auto-pass, auto-move (human with 1 option),
     * or trigger AI to pick a token.
     */
    private fun handlePostDiceRoll(state: LudoState) {
        viewModelScope.launch {
            when {
                // No movable tokens → auto-pass after brief delay
                state.movableTokenIds.isEmpty() -> {
                    delay(800)
                    dispatch(GameAction.PassTurnAction(state.currentPlayer.id - 1))
                }

                // AI player has movable tokens → AI picks a token
                !state.currentPlayer.isHuman -> {
                    delay(500) // Brief "thinking" delay
                    val board = state.board
                    val playerId = state.currentPlayer.id - 1
                    val diceValue = board.diceValue ?: return@launch

                    val moveAction = withContext(Dispatchers.Default) {
                        ai.selectAction(board, playerId)
                    }
                    if (moveAction != null) {
                        dispatch(moveAction)
                    } else {
                        // Fallback: no valid moves despite movableIds — pass turn
                        dispatch(GameAction.PassTurnAction(playerId))
                    }
                }

                // Human player with exactly 1 movable token → auto-move
                state.movableTokenIds.size == 1 -> {
                    delay(300)
                    val board = state.board
                    val diceValue = board.diceValue ?: return@launch
                    val tokenId = state.movableTokenIds.first()
                    dispatch(GameAction.TokenMoveAction(
                        tokenId = tokenId,
                        playerId = state.currentPlayer.id - 1,
                        steps = diceValue
                    ))
                }

                // Human player with multiple options → wait for tap
                // (do nothing, let the user pick)
            }
        }
    }

    /** Check if it's an AI player's turn and trigger AI. */
    private fun checkAndTriggerAi() {
        val currentState = _state.value
        if (vsAi && currentState.isOngoing && !currentState.currentPlayer.isHuman) {
            triggerAiTurn(currentState)
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

    /**
     * AI's turn: roll dice if not yet rolled.
     * After rolling, handlePostDiceRoll will pick the token.
     */
    private fun triggerAiTurn(state: LudoState) {
        viewModelScope.launch {
            delay(500) // Brief delay for readability

            val board = state.board

            if (!board.diceRolled) {
                // AI rolls dice — handlePostDiceRoll will then pick the token
                val diceAction = ai.rollDice()
                dispatch(diceAction)
            }
            // If dice already rolled (shouldn't happen normally),
            // handlePostDiceRoll already handles the token selection
        }
    }

    /** Restart the game with the same config. */
    fun restart() {
        _state.value = LudoState(
            gameState = createInitialGameState(playerCount, vsAi),
            playerCount = playerCount
        )
        viewModelScope.launch {
            delay(300)
            checkAndTriggerAi()
        }
    }

    companion object {
        fun createInitialGameState(playerCount: Int, vsAi: Boolean): GameState {
            val players = (1..playerCount).map { i ->
                if (vsAi && i > 1) Player(id = i, name = "CPU $i", isHuman = false)
                else Player(id = i, name = "Player $i")
            }
            val board = LudoBoard.create(playerCount)
            return GameState(
                gameId = "ludo",
                players = players,
                currentPlayer = players.first(),
                boardData = board,
                result = GameResult.IN_PROGRESS
            )
        }
    }
}

// ── Save data classes ────────────────────────────────────────────────────

data class LudoSaveData(
    val playerCount: Int,
    val tokens: List<TokenSave>,
    val turnPlayerId: Int,
    val diceValue: Int?,
    val diceRolled: Boolean,
    val extraTurn: Boolean,
    val winnerId: Int,
    val lastDiceDisplay: Int = 1
)

data class TokenSave(
    val id: Int,
    val playerId: Int,
    val tokenIdx: Int,
    val step: Int
)

/** ViewModelFactory for LudoViewModel. */
class LudoViewModelFactory(
    private val playerCount: Int,
    private val vsAi: Boolean,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LudoViewModel(
            playerCount = playerCount,
            vsAi = vsAi,
            hapticController = HapticController(context),
            saveManager = SaveManager(context)
        ) as T
    }
}
