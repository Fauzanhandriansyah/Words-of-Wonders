package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow 

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val currentLevel: Int = 1,
    val gems: Int = 350,
    val dailyHintsCount: Int = 3,
    val lastDailyHintResetDate: String = "",
    val lastClaimedDailyPuzzleDate: String = ""
)

@Entity(tableName = "solved_words")
data class SolvedWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val levelNumber: Int,
    val word: String
)

@Dao
interface GameDao {
    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    fun getGameState(): Flow<GameState?>

    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    suspend fun getGameStateSync(): GameState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameState(state: GameState)

    @Query("SELECT * FROM solved_words WHERE levelNumber = :levelNumber")
    fun getSolvedWordsForLevel(levelNumber: Int): Flow<List<SolvedWord>>

    @Query("SELECT * FROM solved_words WHERE levelNumber = :levelNumber")
    suspend fun getSolvedWordsForLevelSync(levelNumber: Int): List<SolvedWord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSolvedWord(solvedWord: SolvedWord)

    @Query("DELETE FROM solved_words WHERE levelNumber = :levelNumber")
    suspend fun clearSolvedWordsForLevel(levelNumber: Int)
}

@Database(entities = [GameState::class, SolvedWord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "word_wheel_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class GameRepository(private val gameDao: GameDao) {
    val gameState: Flow<GameState?> = gameDao.getGameState()
    
    fun getSolvedWords(levelNumber: Int): Flow<List<SolvedWord>> {
        return gameDao.getSolvedWordsForLevel(levelNumber)
    }

    suspend fun getGameStateSync(): GameState {
        return gameDao.getGameStateSync() ?: GameState().also {
            gameDao.saveGameState(it)
        }
    }

    suspend fun updateGameState(state: GameState) {
        gameDao.saveGameState(state)
    }

    suspend fun addSolvedWord(levelNumber: Int, word: String) {
        gameDao.insertSolvedWord(SolvedWord(levelNumber = levelNumber, word = word))
    }

    suspend fun clearLevelProgress(levelNumber: Int) {
        gameDao.clearSolvedWordsForLevel(levelNumber)
    }
}
