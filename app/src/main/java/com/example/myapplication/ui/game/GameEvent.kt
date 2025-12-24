package com.example.myapplication.ui.game

/**
 * Events that can be triggered from game UI
 */
sealed class GameEvent {
    // Multiple Choice Events
    data class SelectAnswer(val index: Int) : GameEvent()
    object NextQuestion : GameEvent()
    object RestartGame : GameEvent()

    // Match Word Events
    data class SelectLeftItem(val index: Int) : GameEvent()
    data class SelectRightItem(val index: Int) : GameEvent()
    object NextRound : GameEvent()
    object RestartMatchGame : GameEvent()
}

