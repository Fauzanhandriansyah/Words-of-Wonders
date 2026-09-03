package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.GameState
import com.example.game.LevelData
import com.example.game.LevelGenerator
import com.example.game.WordPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: GameRepository
    private var loadLevelJob: Job? = null

    // Lucky Spin Cooldown State (5 minutes)
    var luckySpinCooldownRemaining by mutableStateOf(0L)
        private set
    
    // Core Game State saved in Room Database
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Loaded Level Data
    var levelData by mutableStateOf<LevelData?>(null)
        private set

    // Active letters on the wheel (can be shuffled)
    var activeWheelLetters by mutableStateOf<List<Char>>(emptyList())
        private set

    // Set of solved words for the current level
    var solvedWords by mutableStateOf<Set<String>>(emptySet())
        private set

    // Interactive indices of letters swiped/dragged on the word wheel
    var selectedIndices by mutableStateOf<List<Int>>(emptyList())
        private set

    // Interactive letters derived from selectedIndices
    val selectedLetters: List<Char>
        get() {
            return selectedIndices.mapNotNull { idx ->
                if (idx in activeWheelLetters.indices) activeWheelLetters[idx] else null
            }
        }

    // Temporary toast-like message to show on screen
    var gameMessage by mutableStateOf<String?>(null)
        private set

    // UI Overlay controllers
    var isHomeScreen by mutableStateOf(true)
    var showLuckyWheel by mutableStateOf(false)
    var showIapShop by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showLevelComplete by mutableStateOf(false)
    var showDailyComplete by mutableStateOf(false)

    // Daily Puzzle states
    var isDailyPuzzleMode by mutableStateOf(false)
        private set
    var dailyPuzzleLevelData by mutableStateOf<LevelData?>(null)
        private set

    // Revealed cells for Hammer Hint targeting (contains coordinates: "x,y" of single letters revealed)
    var customRevealedCells by mutableStateOf<Set<String>>(emptySet())
        private set

    // Selection coordinate to show drag-connect lines in UI
    var dragLineStartPoint by mutableStateOf<Pair<Float, Float>?>(null)
    var dragLineCurrentPoint by mutableStateOf<Pair<Float, Float>?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
        
        // Listen to Database state changes
        viewModelScope.launch {
            repository.gameState.collectLatest { state ->
                if (state != null) {
                    val prevLevel = _gameState.value?.currentLevel
                    _gameState.value = state
                    checkAndResetDailyState(state)
                    if (!isDailyPuzzleMode && (prevLevel != state.currentLevel || levelData == null)) {
                        loadCurrentLevelData(state.currentLevel)
                    }
                } else {
                    // Populate initial state if DB is empty
                    val initialState = GameState()
                    repository.updateGameState(initialState)
                }
            }
        }
        startLuckySpinCooldownTicker()
    }

    private fun startLuckySpinCooldownTicker() {
        viewModelScope.launch {
            while (true) {
                val lastSpinTime = getApplication<Application>()
                    .getSharedPreferences("lucky_spin_prefs", Context.MODE_PRIVATE)
                    .getLong("last_spin_time", 0L)
                val elapsed = System.currentTimeMillis() - lastSpinTime
                val remaining = 300000L - elapsed // 5 minutes in ms
                luckySpinCooldownRemaining = if (remaining > 0) remaining else 0L
                delay(1000)
            }
        }
    }

    fun canSpinLuckyWheel(): Boolean {
        return luckySpinCooldownRemaining <= 0L
    }

    fun recordLuckySpin() {
        getApplication<Application>()
            .getSharedPreferences("lucky_spin_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_spin_time", System.currentTimeMillis())
            .apply()
    }

    private fun checkAndResetDailyState(state: GameState) {
        val currentDate = getCurrentDateString()
        if (state.lastDailyHintResetDate != currentDate) {
            viewModelScope.launch {
                repository.updateGameState(
                    state.copy(
                        dailyHintsCount = 3,
                        lastDailyHintResetDate = currentDate
                    )
                )
            }
        }
    }

    private fun loadCurrentLevelData(levelNum: Int) {
        loadLevelJob?.cancel()
        loadLevelJob = viewModelScope.launch {
            // Generate level (or regenerate to match the database's level)
            val generated = LevelGenerator.generateLevel(levelNum)
            levelData = generated
            activeWheelLetters = generated.baseLetters.split(",").map { it.first() }
            
            // Sync already solved words from Room
            repository.getSolvedWords(levelNum).collect { solvedList ->
                val solvedSet = solvedList.map { it.word.uppercase() }.toSet()
                solvedWords = solvedSet
            }
        }
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Interactive Swipe Handling
    fun startSelection() {
        selectedIndices = emptyList()
    }

    fun selectLetter(index: Int) {
        if (!selectedIndices.contains(index)) {
            selectedIndices = selectedIndices + index
            GameAudioHelper.playDragTone()
        }
    }

    fun submitSelection() {
        val word = selectedLetters.joinToString("").uppercase()
        if (word.isEmpty()) {
            selectedIndices = emptyList()
            return
        }

        val activeLevel = if (isDailyPuzzleMode) dailyPuzzleLevelData else levelData
        if (activeLevel == null) {
            selectedIndices = emptyList()
            return
        }

        val isTarget = activeLevel.targetWords.contains(word)
        val isExtra = activeLevel.extraWords.contains(word)

        when {
            isTarget -> {
                if (solvedWords.contains(word)) {
                    showTemporaryMessage("Kata '$word' sudah terjawab!")
                } else {
                    solveWord(word)
                }
            }
            isExtra -> {
                // Award gems for extra words found (extra dictionary reward!)
                viewModelScope.launch {
                    val currentGems = _gameState.value.gems
                    repository.updateGameState(_gameState.value.copy(gems = currentGems + 10))
                    showTemporaryMessage("Kata Ekstra! +10 Gems 💎")
                }
            }
            else -> {
                showTemporaryMessage("Kata '$word' tidak valid!")
            }
        }
        
        selectedIndices = emptyList()
        dragLineStartPoint = null
        dragLineCurrentPoint = null
    }

    private fun solveWord(word: String) {
        viewModelScope.launch {
            val levelNum = if (isDailyPuzzleMode) -100 else _gameState.value.currentLevel
            
            // Save in DB
            repository.addSolvedWord(levelNum, word)
            solvedWords = solvedWords + word
            
            val activeLevel = if (isDailyPuzzleMode) dailyPuzzleLevelData else levelData
            if (activeLevel != null && solvedWords.size == activeLevel.targetWords.size) {
                // LEVEL COMPLETE!
                GameAudioHelper.playLevelCompleteTone()
                handleLevelCompletion()
            } else {
                GameAudioHelper.playWordSolvedTone()
                showTemporaryMessage("Mantap! Kata ketemu! ✨")
            }
        }
    }

    private fun handleLevelCompletion() {
        if (isDailyPuzzleMode) {
            // Daily puzzle completed
            viewModelScope.launch {
                val state = _gameState.value
                val todayDate = getCurrentDateString()
                repository.updateGameState(
                    state.copy(
                        gems = state.gems + 150, // +150 Gems for daily completion
                        lastClaimedDailyPuzzleDate = todayDate
                    )
                )
                showDailyComplete = true
            }
        } else {
            // Main puzzle completed
            showLevelComplete = true
        }
    }

    fun proceedToNextLevel() {
        showLevelComplete = false
        viewModelScope.launch {
            val state = _gameState.value
            val levelCompletionsAward = when {
                state.currentLevel <= 300 -> 25  // Easy
                state.currentLevel <= 700 -> 50  // Medium
                else -> 100                     // Hard
            }
            
            // Clear current solved words cache in database for this level
            // so if they replay it, it's fresh, but we increment current level progress
            val nextLevelNum = minOf(1000, state.currentLevel + 1)
            
            repository.updateGameState(
                state.copy(
                    currentLevel = nextLevelNum,
                    gems = state.gems + levelCompletionsAward
                )
            )
            customRevealedCells = emptySet() // Reset hammer hints
            isHomeScreen = true
        }
    }

    // Hint Systems
    fun triggerRandomHint() {
        val state = _gameState.value
        val activeLevel = (if (isDailyPuzzleMode) dailyPuzzleLevelData else levelData) ?: return
        
        // Find all unrevealed cell indices
        val unrevealedWords = activeLevel.targetWords.filter { !solvedWords.contains(it) }
        if (unrevealedWords.isEmpty()) return

        val costGems = 50

        if (state.gems < costGems) {
            showTemporaryMessage("Gems tidak cukup! 💎")
            return
        }

        // Deduct currency
        viewModelScope.launch {
            repository.updateGameState(state.copy(gems = state.gems - costGems))
            showTemporaryMessage("Petunjuk dibeli! -50 Gems 💎")

            // Reveal a random word's letter (we do this by adding coordinates to revealed cell list)
            revealRandomLetter(activeLevel)
            checkAndSolveFullyRevealedWords()
        }
    }

    private fun revealRandomLetter(activeLevel: LevelData) {
        val unrevealedWordPlacements = activeLevel.wordPlacements.filter { !solvedWords.contains(it.word) }
        if (unrevealedWordPlacements.isEmpty()) return
        
        val randomPlacement = unrevealedWordPlacements.random()
        val word = randomPlacement.word
        
        // Find indices of unrevealed characters
        val unrevealedCharCoords = mutableListOf<String>()
        for (idx in word.indices) {
            val cx = if (randomPlacement.isHorizontal) randomPlacement.startX + idx else randomPlacement.startX
            val cy = if (randomPlacement.isHorizontal) randomPlacement.startY else randomPlacement.startY + idx
            val coordKey = "$cx,$cy"
            
            if (!customRevealedCells.contains(coordKey)) {
                unrevealedCharCoords.add(coordKey)
            }
        }

        if (unrevealedCharCoords.isNotEmpty()) {
            val pickedCoord = unrevealedCharCoords.random()
            customRevealedCells = customRevealedCells + pickedCoord
        } else {
            // Entire word is almost solved manually, pick another one
            val allWordCoords = mutableListOf<String>()
            for (p in activeLevel.wordPlacements) {
                if (solvedWords.contains(p.word)) continue
                for (idx in p.word.indices) {
                    val cx = if (p.isHorizontal) p.startX + idx else p.startX
                    val cy = if (p.isHorizontal) p.startY else p.startY + idx
                    val coordKey = "$cx,$cy"
                    if (!customRevealedCells.contains(coordKey)) {
                        allWordCoords.add(coordKey)
                    }
                }
            }
            if (allWordCoords.isNotEmpty()) {
                customRevealedCells = customRevealedCells + allWordCoords.random()
            }
        }
    }

    private fun checkAndSolveFullyRevealedWords() {
        val activeLevel = (if (isDailyPuzzleMode) dailyPuzzleLevelData else levelData) ?: return
        for (placement in activeLevel.wordPlacements) {
            val word = placement.word.uppercase()
            if (solvedWords.contains(word)) continue
            
            // Check if all cells of this placement are revealed
            var allRevealed = true
            for (idx in word.indices) {
                val cx = if (placement.isHorizontal) placement.startX + idx else placement.startX
                val cy = if (placement.isHorizontal) placement.startY else placement.startY + idx
                val coordKey = "$cx,$cy"
                val cellRevealed = customRevealedCells.contains(coordKey) || solvedWords.any { solvedWord ->
                    val otherPlacement = activeLevel.wordPlacements.find { it.word.uppercase() == solvedWord }
                    if (otherPlacement != null) {
                        var covers = false
                        for (otherIdx in otherPlacement.word.indices) {
                            val ocx = if (otherPlacement.isHorizontal) otherPlacement.startX + otherIdx else otherPlacement.startX
                            val ocy = if (otherPlacement.isHorizontal) otherPlacement.startY else otherPlacement.startY + otherIdx
                            if (ocx == cx && ocy == cy) {
                                covers = true
                                break
                            }
                        }
                        covers
                    } else false
                }
                if (!cellRevealed) {
                    allRevealed = false
                    break
                }
            }
            if (allRevealed) {
                solveWord(word)
            }
        }
    }

    // Hammer hint reveals a specific cell tapped by the player
    fun triggerHammerHint(x: Int, y: Int) {
        val state = _gameState.value
        val coordKey = "$x,$y"
        if (customRevealedCells.contains(coordKey)) return // Already revealed!

        val hasFreeHammer = state.dailyHintsCount > 0
        val costGems = 100

        if (!hasFreeHammer && state.gems < costGems) {
            showTemporaryMessage("Gems atau Alat Palu tidak cukup! 💎🔨")
            return
        }

        viewModelScope.launch {
            if (hasFreeHammer) {
                repository.updateGameState(state.copy(dailyHintsCount = state.dailyHintsCount - 1))
                showTemporaryMessage("Palu Keras digunakan! 🔨")
            } else {
                repository.updateGameState(state.copy(gems = state.gems - costGems))
                showTemporaryMessage("Palu dibeli! -100 Gems 💎")
            }
            customRevealedCells = customRevealedCells + coordKey
            checkAndSolveFullyRevealedWords()
        }
    }

    // Daily Puzzle mode
    fun launchDailyPuzzle() {
        val today = getCurrentDateString()
        if (_gameState.value.lastClaimedDailyPuzzleDate == today) {
            showTemporaryMessage("Anda sudah menyelesaikan tantangan hari ini!")
            return
        }

        loadLevelJob?.cancel()

        // Generate level with a daily-seeded level number
        val dailySeed = today.hashCode() % 1000 + 1000
        val generated = LevelGenerator.generateLevel(dailySeed, isDailyPuzzle = true)
        dailyPuzzleLevelData = generated
        activeWheelLetters = generated.baseLetters.split(",").map { it.first() }
        customRevealedCells = emptySet()
        isDailyPuzzleMode = true

        // Observe solved words for the daily level (-100)
        loadLevelJob = viewModelScope.launch {
            repository.getSolvedWords(-100).collect { solvedList ->
                val solvedSet = solvedList.map { it.word.uppercase() }.toSet()
                solvedWords = solvedSet
            }
        }
    }

    fun exitDailyPuzzle() {
        isDailyPuzzleMode = false
        dailyPuzzleLevelData = null
        loadCurrentLevelData(_gameState.value.currentLevel)
    }

    fun shuffleLetters() {
        activeWheelLetters = activeWheelLetters.shuffled()
        showTemporaryMessage("Huruf diacak! 🔀")
    }

    // Shop functions (Simulated Purchase)
    fun buyGems(packIndex: Int) {
        val currentGems = _gameState.value.gems
        val addedGems = when (packIndex) {
            0 -> 100  // Rp 5.000
            1 -> 500  // Rp 15.000
            2 -> 1500 // Rp 39.000
            3 -> 4000 // Rp 99.000
            else -> 0
        }
        
        if (addedGems > 0) {
            viewModelScope.launch {
                repository.updateGameState(_gameState.value.copy(gems = currentGems + addedGems))
                showTemporaryMessage("Pembelian Sukses! +$addedGems Gems 💎")
                showIapShop = false
            }
        }
    }

    // Dev Helper to skip levels
    fun skipLevel() {
        viewModelScope.launch {
            val state = _gameState.value
            val nextLevelNum = minOf(1000, state.currentLevel + 1)
            repository.updateGameState(state.copy(currentLevel = nextLevelNum))
            customRevealedCells = emptySet()
            showTemporaryMessage("Level dilewati!")
        }
    }

    // Reset game to default progress
    fun resetAllProgress() {
        viewModelScope.launch {
            repository.updateGameState(GameState())
            customRevealedCells = emptySet()
            solvedWords = emptySet()
            showTemporaryMessage("Semua progres direset!")
            showSettings = false
        }
    }

    fun awardGems(amount: Int) {
        viewModelScope.launch {
            val state = _gameState.value
            repository.updateGameState(state.copy(gems = state.gems + amount))
        }
    }

    fun awardDailyHints(amount: Int) {
        viewModelScope.launch {
            val state = _gameState.value
            repository.updateGameState(state.copy(dailyHintsCount = state.dailyHintsCount + amount))
        }
    }

    fun showTemporaryMessage(message: String) {
        gameMessage = message
        // Automatically hide message after 2.5 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            if (gameMessage == message) {
                gameMessage = null
            }
        }
    }
}
