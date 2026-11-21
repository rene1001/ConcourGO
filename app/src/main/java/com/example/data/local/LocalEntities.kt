package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val unitId: String,
    val orderIndex: Int,
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val colorKey: String
)

@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey val levelId: String,
    val unitId: String,
    val orderIndex: Int,
    val title: String,
    val subtitle: String,
    val xpReward: Int,
    val isBossChallenge: Boolean,
    val questionCount: Int
)

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey val questionId: String,
    val levelId: String,
    val unitId: String,
    val orderIndex: Int,
    val prompt: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val explanation: String,
    val sourceTag: String
) {
    fun options(): List<String> = listOf(optionA, optionB, optionC, optionD)
}

@Entity(tableName = "local_level_progress")
data class LocalLevelProgressEntity(
    @PrimaryKey val levelId: String,
    val unitId: String,
    val stars: Int,
    val bestScore: Int,
    val attempts: Int,
    val completed: Boolean,
    val updatedAtMillis: Long,
    val needsSync: Boolean
)

@Entity(tableName = "local_user_stats")
data class LocalUserStatsEntity(
    @PrimaryKey val id: String = ACTIVE_ID,
    val userId: String = "",
    val displayName: String = "Candidat",
    val avatarId: String = "hibou_emerald",
    val totalXp: Int = 0,
    val weeklyXp: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val hearts: Int = 5,
    val gems: Int = 60,
    val dailyGoalXp: Int = 50,
    val todayXp: Int = 0,
    val lastActiveDate: String = "",
    val league: String = "Bronze",
    val completedLevelsCount: Int = 0,
    val totalCorrect: Int = 0,
    val totalAnswered: Int = 0,
    val hasPendingProfileSync: Boolean = false
) {
    companion object {
        const val ACTIVE_ID = "active"
    }
}

@Entity(tableName = "cached_leaderboard")
data class CachedLeaderboardEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val avatarId: String,
    val totalXp: Int,
    val weeklyXp: Int,
    val currentStreak: Int,
    val league: String,
    val completedLevelsCount: Int,
    val cachedAtMillis: Long
)
