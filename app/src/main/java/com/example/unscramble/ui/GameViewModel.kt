package com.example.unscramble.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.allWords
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.unscramble.data.WordsDao
import com.example.unscramble.data.WordsModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(private val dao: WordsDao) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var userGuess by mutableStateOf("")
        private set

    private var usedWords: MutableSet<String> = mutableSetOf()
    private lateinit var currentWord: String
    private var wordsList by mutableStateOf<List<String>>(emptyList())

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            initializeWordsIfEmpty()
            wordsList = dao.getAllWords().mapNotNull { it.word }
            resetGame()
        }
    }

    private suspend fun initializeWordsIfEmpty() {
        if (dao.getAllWords().isEmpty()) {
            dao.insert(allWords.map { WordsModel(word = it) })
        }
    }

    fun resetGame() {
        usedWords.clear()
        val scrambled = pickRandomWordAndShuffle()
        if (scrambled.isNotEmpty()) {
            _uiState.value = GameUiState(currentScrambledWord = scrambled)
        }
    }

    fun addWord() {
        if (userGuess.isBlank()) return
        val newWord = userGuess.trim()
        val newModel = WordsModel(word = newWord)

        viewModelScope.launch {
            dao.insert(listOf(newModel))
            wordsList = wordsList + newWord
        }

        updateUserGuess("")
    }

    fun updateUserGuess(guessedWord: String) {
        userGuess = guessedWord
    }

    fun checkUserGuess() {
        if (userGuess.equals(currentWord, ignoreCase = true)) {
            val updatedScore = _uiState.value.score.plus(SCORE_INCREASE)
            updateGameState(updatedScore)
        } else {
            _uiState.update { currentState ->
                currentState.copy(isGuessedWordWrong = true)
            }
        }
        updateUserGuess("")
    }

    fun skipWord() {
        updateGameState(_uiState.value.score)
        updateUserGuess("")
    }

    private fun updateGameState(updatedScore: Int) {
        val availableWords = wordsList.filter { it !in usedWords }

        if (usedWords.size >= MAX_NO_OF_WORDS || availableWords.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    score = updatedScore,
                    isGameOver = true
                )
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    currentScrambledWord = pickRandomWordAndShuffle(),
                    currentWordCount = currentState.currentWordCount.inc(),
                    score = updatedScore
                )
            }
        }
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        tempWord.shuffle()
        while (String(tempWord) == word) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }

    private fun pickRandomWordAndShuffle(): String {
        val availableWords = wordsList.filter { it !in usedWords }

        if (availableWords.isEmpty()) {
            _uiState.update { it.copy(isGameOver = true) }
            return ""
        }

        currentWord = availableWords.random()
        usedWords.add(currentWord)
        return shuffleCurrentWord(currentWord)
    }
}