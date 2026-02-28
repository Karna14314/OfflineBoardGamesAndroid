package com.offlinegames.core

/**
 * MVI Intent hierarchy.
 *
 * Every user or system action is modelled as a sealed [GameIntent].
 * The ViewModel converts raw UI events into intents and passes them
 * to the [Reducer].
 */
sealed class GameIntent {

    /** The active player wants to place a move. */
    data class MakeMove(val move: Move) : GameIntent()

    /** Reset the session to the initial state (new game). */
    object RestartGame : GameIntent()

    /** The user navigated away — save state before the session ends. */
    object SaveAndExit : GameIntent()

    /** Undo the most recent move (if the game rules allow it). */
    object UndoMove : GameIntent()
}

/**
 * Side effects produced by the [Reducer] that require the ViewModel
 * to trigger platform calls (sound, haptics, navigation, etc.).
 */
sealed class GameEffect {
    object PlayMoveSound : GameEffect()
    object PlayWinSound : GameEffect()
    object TriggerHaptic : GameEffect()
    data class ShowMessage(val message: String) : GameEffect()
    object NavigateToResult : GameEffect()
}
