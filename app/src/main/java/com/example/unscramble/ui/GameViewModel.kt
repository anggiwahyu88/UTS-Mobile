package com.example.unscramble.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.WordsDao
import com.example.unscramble.data.WordsModel
import com.example.unscramble.data.allWords
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
        _uiState.value = GameUiState(
            currentScrambledWord = pickRandomWordAndShuffle()
        )
    }

    private fun pickRandomWordAndShuffle(): String {

        val availableWords = wordsList.filter { !usedWords.contains(it) }

        if (availableWords.isEmpty()) return ""

        val randomWord = availableWords.random()

        usedWords.add(randomWord)
        currentWord = randomWord

        return shuffleCurrentWord(currentWord)
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        tempWord.shuffle()
        while (String(tempWord) == word) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }

    fun updateUserGuess(guessedWord: String) {
        userGuess = guessedWord
    }

    fun checkUserGuess() {
        if (userGuess.equals(currentWord, ignoreCase = true)) {
            val updatedScore = _uiState.value.score + SCORE_INCREASE
            updateGameState(updatedScore)
        } else {
            _uiState.update {
                it.copy(isGuessedWordWrong = true)
            }
        }
        updateUserGuess("")
    }

    fun addWord() {
        if (userGuess.isBlank()) return

        val newWord = WordsModel(word = userGuess)

        viewModelScope.launch {
            dao.insert(listOf(newWord))

            wordsList = wordsList + userGuess
        }

        updateUserGuess("")
    }

    fun skipWord() {
        updateGameState(_uiState.value.score)
        updateUserGuess("")
    }

    private fun updateGameState(updatedScore: Int) {
        if (usedWords.size == MAX_NO_OF_WORDS) {
            _uiState.update {
                it.copy(
                    isGuessedWordWrong = false,
                    score = updatedScore,
                    isGameOver = true
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isGuessedWordWrong = false,
                    currentScrambledWord = pickRandomWordAndShuffle(),
                    currentWordCount = it.currentWordCount + 1,
                    score = updatedScore
                )
            }
        }
    }
}