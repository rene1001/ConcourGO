package com.example.data.repository

import com.example.data.local.LevelEntity
import com.example.data.local.LocalLevelProgressEntity
import com.example.data.remote.LeagueTier
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class QuizOutcome(
    val stars: Int,
    val scorePercent: Int,
    val passed: Boolean,
    val xpEarned: Int,
    val gemsEarned: Int,
    val newStreak: Int,
    val newBestStreak: Int,
    val newLeague: LeagueTier
)

object GamificationEngine {

    private val dateFormatter: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }

    fun todayDateString(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return dateFormatter.format(cal.time)
    }

    fun yesterdayDateString(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return dateFormatter.format(cal.time)
    }

    fun calculateStars(correctCount: Int, totalCount: Int): Int {
        if (totalCount <= 0) return 0
        val percent = (correctCount * 100) / totalCount
        return when {
            percent >= 90 -> 3
            percent >= 70 -> 2
            percent >= 50 -> 1
            else -> 0
        }
    }

    fun computeStreak(
        lastActiveDate: String,
        currentStreak: Int,
        bestStreak: Int,
        todayDate: String,
        yesterdayDate: String
    ): Pair<Int, Int> {
        val nextStreak = when (lastActiveDate) {
            todayDate -> currentStreak.coerceAtLeast(1)
            yesterdayDate -> currentStreak + 1
            else -> 1
        }
        val nextBest = maxOf(bestStreak, nextStreak)
        return nextStreak to nextBest
    }

    /**
     * Validates whether [currentStreak] is still active as of [nowMillis].
     * A streak remains intact if activity occurred today or yesterday; otherwise it resets to 0
     * while preserving [bestStreak].
     */
    fun resolveActiveStreak(
        lastActiveDate: String,
        currentStreak: Int,
        bestStreak: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Int, Int> {
        val today = todayDateString(nowMillis)
        val yesterday = yesterdayDateString(nowMillis)
        val activeStreak = if (lastActiveDate == today || lastActiveDate == yesterday) {
            currentStreak.coerceAtLeast(0)
        } else {
            0
        }
        val safeBest = maxOf(bestStreak, activeStreak)
        return activeStreak to safeBest
    }

    /**
     * Merges local and remote streak state using the freshest activity date so an expired
     * streak from one device never overwrites or inflates an active streak on another.
     */
    fun mergeStreakStats(
        localLastDate: String,
        localCurrentStreak: Int,
        localBestStreak: Int,
        remoteLastDate: String,
        remoteCurrentStreak: Int,
        remoteBestStreak: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Triple<String, Int, Int> {
        val (localValidStreak, localValidBest) = resolveActiveStreak(
            localLastDate, localCurrentStreak, localBestStreak, nowMillis
        )
        val (remoteValidStreak, remoteValidBest) = resolveActiveStreak(
            remoteLastDate, remoteCurrentStreak, remoteBestStreak, nowMillis
        )

        val mergedLastDate = maxOf(localLastDate, remoteLastDate)
        val mergedCurrent = when {
            localLastDate > remoteLastDate -> localValidStreak
            remoteLastDate > localLastDate -> remoteValidStreak
            else -> maxOf(localValidStreak, remoteValidStreak)
        }
        val mergedBest = maxOf(localValidBest, remoteValidBest, mergedCurrent)
        return Triple(mergedLastDate, mergedCurrent, mergedBest)
    }

    fun calculateQuizOutcome(
        baseXpReward: Int,
        correctCount: Int,
        totalCount: Int,
        maxCombo: Int,
        isFirstCompletion: Boolean,
        previousTotalXp: Int,
        lastActiveDate: String,
        currentStreak: Int,
        bestStreak: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): QuizOutcome {
        val safeTotal = totalCount.coerceAtLeast(1)
        val safeCorrect = correctCount.coerceIn(0, safeTotal)
        val scorePercent = (safeCorrect * 100) / safeTotal
        val stars = calculateStars(safeCorrect, safeTotal)
        val passed = stars > 0

        val xpEarned = if (passed) {
            val base = if (isFirstCompletion) baseXpReward else (baseXpReward / 2).coerceAtLeast(15)
            val comboBonus = (maxCombo.coerceAtLeast(0) * 2).coerceAtMost(12)
            val perfectBonus = if (safeCorrect == safeTotal) 15 else 0
            base + comboBonus + perfectBonus
        } else {
            (safeCorrect * 3).coerceAtLeast(5)
        }

        val gemsEarned = if (passed) {
            if (isFirstCompletion) stars * 5 else if (stars == 3) 3 else 1
        } else {
            0
        }

        val today = todayDateString(nowMillis)
        val yesterday = yesterdayDateString(nowMillis)
        val (newStreak, newBestStreak) = computeStreak(
            lastActiveDate = lastActiveDate,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            todayDate = today,
            yesterdayDate = yesterday
        )

        val newLeague = LeagueTier.fromXp(previousTotalXp + xpEarned)

        return QuizOutcome(
            stars = stars,
            scorePercent = scorePercent,
            passed = passed,
            xpEarned = xpEarned,
            gemsEarned = gemsEarned,
            newStreak = newStreak,
            newBestStreak = newBestStreak,
            newLeague = newLeague
        )
    }

    fun isLevelUnlocked(
        level: LevelEntity,
        unitLevels: List<LevelEntity>,
        progressByLevelId: Map<String, LocalLevelProgressEntity>
    ): Boolean {
        if (level.orderIndex <= 1) return true
        val previousLevel = unitLevels
            .filter { it.unitId == level.unitId && it.orderIndex < level.orderIndex }
            .maxByOrNull { it.orderIndex }
            ?: return true
        return progressByLevelId[previousLevel.levelId]?.completed == true
    }
}
