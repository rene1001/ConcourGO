package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.CachedLeaderboardEntity
import com.example.data.local.ConcoursDao
import com.example.data.local.LevelEntity
import com.example.data.local.LocalLevelProgressEntity
import com.example.data.local.LocalUserStatsEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.QuestionBankSeed
import com.example.data.local.QuizQuestionEntity
import com.example.data.local.UnitEntity
import com.example.data.local.UserPreferences
import com.example.data.remote.CloudSyncRepository
import com.example.data.remote.LeagueTier
import com.example.data.remote.RemoteLeaderboardEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class QuizCompletionReport(
    val outcome: QuizOutcome,
    val syncedToCloud: Boolean
)

data class SyncSummary(
    val syncedLevelsCount: Int,
    val profileSynced: Boolean,
    val errorMessage: String? = null
)

class ConcoursRepository(
    private val context: Context,
    private val dao: ConcoursDao,
    private val preferencesRepository: PreferencesRepository,
    private val cloudSyncRepository: CloudSyncRepository
) {
    val unitsFlow: Flow<List<UnitEntity>> = dao.observeUnits()
    val levelsFlow: Flow<List<LevelEntity>> = dao.observeLevels()
    val levelProgressFlow: Flow<List<LocalLevelProgressEntity>> = dao.observeLevelProgress()
    val userStatsFlow: Flow<LocalUserStatsEntity> = dao.observeUserStats()
        .map { it ?: LocalUserStatsEntity() }
    val cachedLeaderboardFlow: Flow<List<CachedLeaderboardEntity>> = dao.observeCachedLeaderboard()
    val questionCountFlow: Flow<Int> = dao.observeQuestionCount()
    val pendingLevelSyncCountFlow: Flow<Int> = dao.observePendingLevelSyncCount()
    val preferencesFlow: Flow<UserPreferences> = preferencesRepository.preferencesFlow

    suspend fun ensureSeededAndInitialized(userId: String?, displayName: String?) {
        if (dao.getQuestionCount() < QuestionBankSeed.allQuestions().size) {
            dao.seedCurriculum(
                units = QuestionBankSeed.units,
                levels = QuestionBankSeed.levels,
                questions = QuestionBankSeed.allQuestions()
            )
        }

        val current = dao.getUserStats()
        if (current == null) {
            val cleanName = displayName?.takeIf { it.isNotBlank() } ?: "Candidat"
            dao.upsertUserStats(
                LocalUserStatsEntity(
                    userId = userId.orEmpty(),
                    displayName = cleanName.take(60),
                    hearts = 5,
                    gems = 60,
                    dailyGoalXp = 50,
                    lastActiveDate = ""
                )
            )
        } else if (!userId.isNullOrBlank()) {
            val switchedAccount = current.userId.isNotBlank() && current.userId != userId
            if (switchedAccount) {
                dao.clearLevelProgress()
                dao.upsertUserStats(
                    LocalUserStatsEntity(
                        userId = userId,
                        displayName = (displayName?.takeIf { it.isNotBlank() } ?: "Candidat").take(60)
                    )
                )
            } else if (current.userId.isBlank()) {
                val updatedName = if (current.displayName == "Candidat" && !displayName.isNullOrBlank()) {
                    displayName.take(60)
                } else {
                    current.displayName
                }
                dao.upsertUserStats(
                    current.copy(
                        userId = userId,
                        displayName = updatedName,
                        hasPendingProfileSync = true
                    )
                )
            }
        }
        refreshDailyStreakIfNeeded(currentUserId = userId, syncIfChanged = false)
    }

    suspend fun refreshDailyStreakIfNeeded(
        currentUserId: String?,
        syncIfChanged: Boolean = true,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val current = dao.getUserStats() ?: return
        val today = GamificationEngine.todayDateString(nowMillis)
        val (validStreak, validBest) = GamificationEngine.resolveActiveStreak(
            lastActiveDate = current.lastActiveDate,
            currentStreak = current.currentStreak,
            bestStreak = current.bestStreak,
            nowMillis = nowMillis
        )
        val updatedTodayXp = if (current.lastActiveDate == today) current.todayXp else 0

        if (validStreak != current.currentStreak ||
            validBest != current.bestStreak ||
            updatedTodayXp != current.todayXp
        ) {
            val refreshed = current.copy(
                currentStreak = validStreak,
                bestStreak = validBest,
                todayXp = updatedTodayXp,
                hasPendingProfileSync = true
            )
            dao.upsertUserStats(refreshed)
            val prefs = preferencesRepository.getCurrentPreferences()
            if (syncIfChanged &&
                !currentUserId.isNullOrBlank() &&
                !prefs.offlineModeEnabled &&
                isNetworkConnected()
            ) {
                val pushRes = cloudSyncRepository.pushUserProfileAndLeaderboard(refreshed)
                if (pushRes.isSuccess) {
                    dao.upsertUserStats(refreshed.copy(hasPendingProfileSync = false))
                }
            }
        }
    }

    suspend fun getLevelWithQuestions(levelId: String): Pair<LevelEntity?, List<QuizQuestionEntity>> {
        if (dao.getQuestionCount() == 0) {
            dao.seedCurriculum(
                units = QuestionBankSeed.units,
                levels = QuestionBankSeed.levels,
                questions = QuestionBankSeed.allQuestions()
            )
        }
        return dao.getLevelById(levelId) to dao.getQuestionsForLevel(levelId)
    }

    fun observeCloudLeaderboard(): Flow<List<RemoteLeaderboardEntry>> =
        cloudSyncRepository.observeLeaderboard()

    suspend fun refreshLeaderboardFromCloud(limitCount: Long = 100): Result<List<RemoteLeaderboardEntry>> {
        val prefs = preferencesRepository.getCurrentPreferences()
        if (prefs.offlineModeEnabled) {
            return Result.failure(IllegalStateException("Mode hors-ligne activé"))
        }
        if (!isNetworkConnected()) {
            return Result.failure(IllegalStateException("Aucune connexion internet détectée"))
        }
        val result = cloudSyncRepository.fetchTopLeaderboard(limitCount)
        result.onSuccess { entries ->
            cacheRemoteLeaderboard(entries)
        }
        return result
    }

    suspend fun cacheRemoteLeaderboard(entries: List<RemoteLeaderboardEntry>) {
        val now = System.currentTimeMillis()
        val cached = entries.map { entry ->
            CachedLeaderboardEntity(
                userId = entry.userId,
                displayName = entry.displayName,
                avatarId = entry.avatarId,
                totalXp = entry.totalXp.toInt(),
                weeklyXp = entry.weeklyXp.toInt(),
                currentStreak = entry.currentStreak.toInt(),
                league = LeagueTier.safeLabel(entry.league),
                completedLevelsCount = entry.completedLevelsCount.toInt(),
                cachedAtMillis = now
            )
        }
        dao.replaceLeaderboardCache(cached)
    }

    fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun recordQuizCompletion(
        level: LevelEntity,
        correctCount: Int,
        totalCount: Int,
        maxCombo: Int,
        remainingHearts: Int,
        currentUserId: String?
    ): QuizCompletionReport {
        val now = System.currentTimeMillis()
        val today = GamificationEngine.todayDateString(now)
        val currentStats = dao.getUserStats() ?: LocalUserStatsEntity(userId = currentUserId.orEmpty())
        val existingLevelProgress = dao.getLevelProgress(level.levelId)
        val isFirstCompletion = existingLevelProgress?.completed != true

        val outcome = GamificationEngine.calculateQuizOutcome(
            baseXpReward = level.xpReward,
            correctCount = correctCount,
            totalCount = totalCount,
            maxCombo = maxCombo,
            isFirstCompletion = isFirstCompletion,
            previousTotalXp = currentStats.totalXp,
            lastActiveDate = currentStats.lastActiveDate,
            currentStreak = currentStats.currentStreak,
            bestStreak = currentStats.bestStreak,
            nowMillis = now
        )

        val newLevelCompleted = (existingLevelProgress?.completed == true) || outcome.passed
        val updatedProgress = LocalLevelProgressEntity(
            levelId = level.levelId,
            unitId = level.unitId,
            stars = maxOf(existingLevelProgress?.stars ?: 0, outcome.stars),
            bestScore = maxOf(existingLevelProgress?.bestScore ?: 0, outcome.scorePercent),
            attempts = (existingLevelProgress?.attempts ?: 0) + 1,
            completed = newLevelCompleted,
            updatedAtMillis = now,
            needsSync = true
        )
        dao.upsertLevelProgress(updatedProgress)

        val allCompletedCount = dao.getAllLevelProgressSnapshot().count { it.completed }
        val sameDayTodayXp = if (currentStats.lastActiveDate == today) {
            currentStats.todayXp + outcome.xpEarned
        } else {
            outcome.xpEarned
        }

        val updatedStats = currentStats.copy(
            userId = currentUserId ?: currentStats.userId,
            totalXp = currentStats.totalXp + outcome.xpEarned,
            weeklyXp = currentStats.weeklyXp + outcome.xpEarned,
            currentStreak = outcome.newStreak,
            bestStreak = outcome.newBestStreak,
            hearts = remainingHearts.coerceIn(0, 5),
            gems = currentStats.gems + outcome.gemsEarned,
            todayXp = sameDayTodayXp,
            lastActiveDate = today,
            league = outcome.newLeague.label,
            completedLevelsCount = allCompletedCount,
            totalCorrect = currentStats.totalCorrect + correctCount.coerceAtLeast(0),
            totalAnswered = currentStats.totalAnswered + totalCount.coerceAtLeast(0),
            hasPendingProfileSync = true
        )
        dao.upsertUserStats(updatedStats)

        val prefs = preferencesRepository.getCurrentPreferences()
        var synced = false
        if (!currentUserId.isNullOrBlank() && !prefs.offlineModeEnabled && isNetworkConnected()) {
            val summary = syncBidirectionalWithCloud(currentUserId, displayNameFallback = updatedStats.displayName)
            synced = summary.errorMessage == null && summary.profileSynced
        }

        return QuizCompletionReport(outcome = outcome, syncedToCloud = synced)
    }

    suspend fun refillHeartsWithGems(cost: Int = 40): Boolean {
        val current = dao.getUserStats() ?: return false
        if (current.gems < cost) return false
        dao.upsertUserStats(
            current.copy(
                hearts = 5,
                gems = current.gems - cost,
                hasPendingProfileSync = true
            )
        )
        return true
    }

    suspend fun refillHeartsFreePractice() {
        val current = dao.getUserStats() ?: LocalUserStatsEntity()
        dao.upsertUserStats(
            current.copy(
                hearts = 5,
                hasPendingProfileSync = true
            )
        )
    }

    suspend fun applyPurchasedReward(
        gemsToAdd: Int,
        refillHearts: Boolean,
        grantsPass: Boolean,
        currentUserId: String?
    ) {
        val current = dao.getUserStats() ?: LocalUserStatsEntity(userId = currentUserId.orEmpty())
        val updated = current.copy(
            gems = (current.gems + gemsToAdd.coerceAtLeast(0)).coerceAtMost(1_000_000),
            hearts = if (refillHearts) 5 else current.hearts,
            hasPendingProfileSync = true
        )
        dao.upsertUserStats(updated)
        if (grantsPass) {
            preferencesRepository.setPremiumPass(true)
        }
        val prefs = preferencesRepository.getCurrentPreferences()
        if (!currentUserId.isNullOrBlank() && !prefs.offlineModeEnabled && isNetworkConnected()) {
            val pushResult = cloudSyncRepository.pushUserProfileAndLeaderboard(updated)
            if (pushResult.isSuccess) {
                dao.upsertUserStats(updated.copy(hasPendingProfileSync = false))
            }
        }
    }

    suspend fun updateAvatar(avatarId: String, currentUserId: String?) {
        val current = dao.getUserStats() ?: LocalUserStatsEntity()
        val updated = current.copy(avatarId = avatarId, hasPendingProfileSync = true)
        dao.upsertUserStats(updated)
        val prefs = preferencesRepository.getCurrentPreferences()
        if (!currentUserId.isNullOrBlank() && !prefs.offlineModeEnabled && isNetworkConnected()) {
            val pushResult = cloudSyncRepository.pushUserProfileAndLeaderboard(updated)
            if (pushResult.isSuccess) {
                dao.upsertUserStats(updated.copy(hasPendingProfileSync = false))
            }
        }
    }

    suspend fun updateDailyGoal(goalXp: Int, currentUserId: String?) {
        val clamped = goalXp.coerceIn(10, 500)
        val current = dao.getUserStats() ?: LocalUserStatsEntity()
        val updated = current.copy(dailyGoalXp = clamped, hasPendingProfileSync = true)
        dao.upsertUserStats(updated)
        val prefs = preferencesRepository.getCurrentPreferences()
        if (!currentUserId.isNullOrBlank() && !prefs.offlineModeEnabled && isNetworkConnected()) {
            val pushResult = cloudSyncRepository.pushUserProfileAndLeaderboard(updated)
            if (pushResult.isSuccess) {
                dao.upsertUserStats(updated.copy(hasPendingProfileSync = false))
            }
        }
    }

    suspend fun syncBidirectionalWithCloud(
        userId: String,
        displayNameFallback: String? = null
    ): SyncSummary {
        val prefs = preferencesRepository.getCurrentPreferences()
        if (prefs.offlineModeEnabled) {
            return SyncSummary(0, false, "Mode hors-ligne activé")
        }
        if (!isNetworkConnected()) {
            return SyncSummary(0, false, "Aucune connexion internet détectée")
        }

        val remoteProfileRes = cloudSyncRepository.fetchUserProfile(userId)
        val remoteLevelsRes = cloudSyncRepository.fetchLevelProgress(userId)

        if (remoteProfileRes.isFailure) {
            return SyncSummary(
                0,
                false,
                remoteProfileRes.exceptionOrNull()?.localizedMessage ?: "Erreur de synchronisation"
            )
        }

        // Merge remote levels with local levels (keep highest stars, bestScore, attempts, completed)
        val remoteLevels = remoteLevelsRes.getOrDefault(emptyList())
        val localLevelsById = dao.getAllLevelProgressSnapshot().associateBy { it.levelId }.toMutableMap()
        val mergedRemoteToInsert = mutableListOf<LocalLevelProgressEntity>()

        for (remote in remoteLevels) {
            val local = localLevelsById[remote.levelId]
            val merged = if (local == null) {
                LocalLevelProgressEntity(
                    levelId = remote.levelId,
                    unitId = remote.unitId,
                    stars = remote.stars.toInt().coerceIn(0, 3),
                    bestScore = remote.bestScore.toInt().coerceIn(0, 100),
                    attempts = remote.attempts.toInt().coerceAtLeast(0),
                    completed = remote.completed,
                    updatedAtMillis = System.currentTimeMillis(),
                    needsSync = false
                )
            } else {
                val remoteBetter = remote.stars.toInt() > local.stars ||
                    remote.bestScore.toInt() > local.bestScore ||
                    (remote.completed && !local.completed)
                val localBetter = local.stars > remote.stars.toInt() ||
                    local.bestScore > remote.bestScore.toInt() ||
                    local.attempts > remote.attempts.toInt() ||
                    (local.completed && !remote.completed)
                LocalLevelProgressEntity(
                    levelId = local.levelId,
                    unitId = local.unitId,
                    stars = maxOf(local.stars, remote.stars.toInt()).coerceIn(0, 3),
                    bestScore = maxOf(local.bestScore, remote.bestScore.toInt()).coerceIn(0, 100),
                    attempts = maxOf(local.attempts, remote.attempts.toInt()),
                    completed = local.completed || remote.completed,
                    updatedAtMillis = maxOf(local.updatedAtMillis, System.currentTimeMillis()),
                    needsSync = local.needsSync || localBetter || !remoteBetter
                )
            }
            mergedRemoteToInsert.add(merged)
            localLevelsById[merged.levelId] = merged
        }
        if (mergedRemoteToInsert.isNotEmpty()) {
            dao.upsertLevelProgressList(mergedRemoteToInsert)
        }

        // Push pending local level progress
        var syncedLevels = 0
        val pendingLevels = dao.getPendingLevelProgress()
        for (pending in pendingLevels) {
            val pushRes = cloudSyncRepository.pushLevelProgress(pending)
            if (pushRes.isSuccess) {
                dao.upsertLevelProgress(pending.copy(needsSync = false))
                syncedLevels++
            }
        }

        // Merge local stats with remote profile
        val localStats = dao.getUserStats() ?: LocalUserStatsEntity(userId = userId)
        val remoteProfile = remoteProfileRes.getOrNull()
        val completedCount = dao.getAllLevelProgressSnapshot().count { it.completed }

        val mergedStats = if (remoteProfile != null) {
            val mergedTotalXp = maxOf(localStats.totalXp, remoteProfile.totalXp.toInt())
            val resolvedName = localStats.displayName.takeIf { it != "Candidat" && it.isNotBlank() }
                ?: remoteProfile.displayName.takeIf { it.isNotBlank() }
                ?: displayNameFallback?.takeIf { it.isNotBlank() }
                ?: "Candidat"
            val nowMillis = System.currentTimeMillis()
            val today = GamificationEngine.todayDateString(nowMillis)
            val mergedTodayXp = when {
                localStats.lastActiveDate == today && remoteProfile.lastActiveDate == today ->
                    maxOf(localStats.todayXp, remoteProfile.todayXp.toInt())
                localStats.lastActiveDate == today -> localStats.todayXp
                remoteProfile.lastActiveDate == today -> remoteProfile.todayXp.toInt()
                else -> 0
            }
            val (mergedLastDate, mergedCurrentStreak, mergedBestStreak) =
                GamificationEngine.mergeStreakStats(
                    localLastDate = localStats.lastActiveDate,
                    localCurrentStreak = localStats.currentStreak,
                    localBestStreak = localStats.bestStreak,
                    remoteLastDate = remoteProfile.lastActiveDate,
                    remoteCurrentStreak = remoteProfile.currentStreak.toInt(),
                    remoteBestStreak = remoteProfile.bestStreak.toInt(),
                    nowMillis = nowMillis
                )
            localStats.copy(
                userId = userId,
                displayName = resolvedName.take(60),
                avatarId = localStats.avatarId.ifBlank { remoteProfile.avatarId },
                totalXp = mergedTotalXp,
                weeklyXp = maxOf(localStats.weeklyXp, remoteProfile.weeklyXp.toInt()),
                currentStreak = mergedCurrentStreak,
                bestStreak = mergedBestStreak,
                hearts = if (localStats.hasPendingProfileSync) localStats.hearts else remoteProfile.hearts.toInt().coerceIn(0, 5),
                gems = maxOf(localStats.gems, remoteProfile.gems.toInt()),
                dailyGoalXp = localStats.dailyGoalXp,
                todayXp = mergedTodayXp,
                lastActiveDate = mergedLastDate,
                league = LeagueTier.fromXp(mergedTotalXp).label,
                completedLevelsCount = maxOf(completedCount, remoteProfile.completedLevelsCount.toInt()),
                totalCorrect = maxOf(localStats.totalCorrect, remoteProfile.totalCorrect.toInt()),
                totalAnswered = maxOf(localStats.totalAnswered, remoteProfile.totalAnswered.toInt())
            )
        } else {
            val (validStreak, validBest) = GamificationEngine.resolveActiveStreak(
                lastActiveDate = localStats.lastActiveDate,
                currentStreak = localStats.currentStreak,
                bestStreak = localStats.bestStreak
            )
            localStats.copy(
                userId = userId,
                displayName = (displayNameFallback?.takeIf { it.isNotBlank() } ?: localStats.displayName).take(60),
                currentStreak = validStreak,
                bestStreak = validBest,
                completedLevelsCount = completedCount,
                league = LeagueTier.fromXp(localStats.totalXp).label
            )
        }

        val pushProfileRes = cloudSyncRepository.pushUserProfileAndLeaderboard(mergedStats)
        return if (pushProfileRes.isSuccess) {
            dao.upsertUserStats(mergedStats.copy(hasPendingProfileSync = false))
            SyncSummary(syncedLevels, true, null)
        } else {
            dao.upsertUserStats(mergedStats.copy(hasPendingProfileSync = true))
            SyncSummary(
                syncedLevels,
                false,
                pushProfileRes.exceptionOrNull()?.localizedMessage ?: "Échec de synchronisation"
            )
        }
    }

    suspend fun setOfflineModeEnabled(enabled: Boolean) {
        preferencesRepository.setOfflineModeEnabled(enabled)
    }

    suspend fun setOfflineGuestEntered(entered: Boolean) {
        preferencesRepository.setOfflineGuestEntered(entered)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        preferencesRepository.setNotificationsEnabled(enabled)
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        preferencesRepository.setReminderTime(hour, minute)
    }
}
