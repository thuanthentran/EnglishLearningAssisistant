package com.example.myapplication.ui.game

import com.example.myapplication.data.model.DictionaryWord

/**
 * Represents the UI state for all game screens
 */

// Main game selection screen state
data class GameSelectionUiState(
    val isLoading: Boolean = false
)

// Multiple Choice Game State
data class MultipleChoiceUiState(
    val isLoading: Boolean = true,
    val currentQuestion: Int = 0,
    val totalQuestions: Int = 10,
    val score: Int = 0,
    val currentWord: DictionaryWord? = null,
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = -1,
    val selectedAnswerIndex: Int? = null,
    val isAnswered: Boolean = false,
    val isGameFinished: Boolean = false,
    val errorMessage: String? = null
)

// Match Word Game State
data class MatchWordUiState(
    val isLoading: Boolean = true,
    val words: List<DictionaryWord> = emptyList(),
    val leftItems: List<MatchItem> = emptyList(),
    val rightItems: List<MatchItem> = emptyList(),
    val selectedLeftIndex: Int? = null,
    val selectedRightIndex: Int? = null,
    val matchedPairs: Set<Int> = emptySet(), // Indices of matched left items
    val wrongPairIndices: Pair<Int, Int>? = null, // Temporary wrong match highlight
    val currentRound: Int = 1,
    val totalRounds: Int = 3,
    val score: Int = 0,
    val isRoundComplete: Boolean = false,
    val isGameFinished: Boolean = false,
    val errorMessage: String? = null
)

data class MatchItem(
    val index: Int,
    val text: String,
    val originalWordId: Int
)

/**
 * Helper function to get first meaning from definition
 */
fun getFirstMeaning(definition: String): String {
    return definition.split(",").first().trim()
}

