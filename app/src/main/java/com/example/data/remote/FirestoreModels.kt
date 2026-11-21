package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class LeagueTier(val label: String, val minXp: Int) {
    BRONZE("Bronze", 0),
    ARGENT("Argent", 250),
    OR("Or", 700),
    DIAMANT("Diamant", 1500);

    companion object {
        fun fromXp(totalXp: Int): LeagueTier {
            return when {
                totalXp >= DIAMANT.minXp -> DIAMANT
                totalXp >= OR.minXp -> OR
                totalXp >= ARGENT.minXp -> ARGENT
                else -> BRONZE
            }
        }

        fun safeLabel(raw: String?): String {
            return entries.firstOrNull { it.label == raw }?.label ?: BRONZE.label
        }
    }
}

@Keep
@IgnoreExtraProperties
data class RemoteUserProfile(
    val userId: String = "",
    val displayName: String = "Candidat",
    val avatarId: String = "hibou_emerald",
    val totalXp: Long = 0L,
    val weeklyXp: Long = 0L,
    val currentStreak: Long = 0L,
    val bestStreak: Long = 0L,
    val hearts: Long = 5L,
    val gems: Long = 60L,
    val dailyGoalXp: Long = 50L,
    val todayXp: Long = 0L,
    val lastActiveDate: String = "",
    val league: String = LeagueTier.BRONZE.label,
    val completedLevelsCount: Long = 0L,
    val totalCorrect: Long = 0L,
    val totalAnswered: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RemoteLevelProgress(
    val levelId: String = "",
    val unitId: String = "",
    val stars: Long = 0L,
    val bestScore: Long = 0L,
    val attempts: Long = 0L,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RemoteLeaderboardEntry(
    val userId: String = "",
    val displayName: String = "Candidat",
    val avatarId: String = "hibou_emerald",
    val totalXp: Long = 0L,
    val weeklyXp: Long = 0L,
    val currentStreak: Long = 0L,
    val league: String = LeagueTier.BRONZE.label,
    val completedLevelsCount: Long = 0L,
    val publicVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
