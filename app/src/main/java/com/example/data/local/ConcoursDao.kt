package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ConcoursDao {

    @Query("SELECT * FROM units ORDER BY orderIndex ASC")
    fun observeUnits(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM levels ORDER BY unitId ASC, orderIndex ASC")
    fun observeLevels(): Flow<List<LevelEntity>>

    @Query("SELECT * FROM levels ORDER BY unitId ASC, orderIndex ASC")
    suspend fun getAllLevelsSnapshot(): List<LevelEntity>

    @Query("SELECT * FROM levels WHERE levelId = :levelId LIMIT 1")
    suspend fun getLevelById(levelId: String): LevelEntity?

    @Query("SELECT * FROM quiz_questions WHERE levelId = :levelId ORDER BY orderIndex ASC")
    suspend fun getQuestionsForLevel(levelId: String): List<QuizQuestionEntity>

    @Query("SELECT COUNT(*) FROM quiz_questions")
    fun observeQuestionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM quiz_questions")
    suspend fun getQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevels(levels: List<LevelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestionEntity>)

    @Transaction
    suspend fun seedCurriculum(
        units: List<UnitEntity>,
        levels: List<LevelEntity>,
        questions: List<QuizQuestionEntity>
    ) {
        insertUnits(units)
        insertLevels(levels)
        insertQuestions(questions)
    }

    @Query("SELECT * FROM local_level_progress")
    fun observeLevelProgress(): Flow<List<LocalLevelProgressEntity>>

    @Query("SELECT * FROM local_level_progress WHERE levelId = :levelId LIMIT 1")
    suspend fun getLevelProgress(levelId: String): LocalLevelProgressEntity?

    @Query("SELECT * FROM local_level_progress")
    suspend fun getAllLevelProgressSnapshot(): List<LocalLevelProgressEntity>

    @Query("SELECT * FROM local_level_progress WHERE needsSync = 1")
    suspend fun getPendingLevelProgress(): List<LocalLevelProgressEntity>

    @Query("SELECT COUNT(*) FROM local_level_progress WHERE needsSync = 1")
    fun observePendingLevelSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevelProgress(progress: LocalLevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevelProgressList(progressList: List<LocalLevelProgressEntity>)

    @Query("SELECT * FROM local_user_stats WHERE id = :id LIMIT 1")
    fun observeUserStats(id: String = LocalUserStatsEntity.ACTIVE_ID): Flow<LocalUserStatsEntity?>

    @Query("SELECT * FROM local_user_stats WHERE id = :id LIMIT 1")
    suspend fun getUserStats(id: String = LocalUserStatsEntity.ACTIVE_ID): LocalUserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserStats(stats: LocalUserStatsEntity)

    @Transaction
    suspend fun saveQuizOutcome(
        stats: LocalUserStatsEntity,
        levelProgress: LocalLevelProgressEntity
    ) {
        upsertUserStats(stats)
        upsertLevelProgress(levelProgress)
    }

    @Query("DELETE FROM local_level_progress")
    suspend fun clearLevelProgress()

    @Query("DELETE FROM local_user_stats")
    suspend fun clearUserStats()

    @Query("SELECT * FROM cached_leaderboard ORDER BY totalXp DESC")
    fun observeCachedLeaderboard(): Flow<List<CachedLeaderboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaderboardCache(entries: List<CachedLeaderboardEntity>)

    @Query("DELETE FROM cached_leaderboard")
    suspend fun clearLeaderboardCache()

    @Transaction
    suspend fun replaceLeaderboardCache(entries: List<CachedLeaderboardEntity>) {
        clearLeaderboardCache()
        if (entries.isNotEmpty()) {
            upsertLeaderboardCache(entries)
        }
    }
}
