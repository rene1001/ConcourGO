package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.LocalLevelProgressEntity
import com.example.data.local.LocalUserStatsEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google.")
    }

    private fun sanitizedDisplayName(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.isEmpty()) "Candidat" else trimmed.take(60)
    }

    private fun sanitizedAvatarId(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.isEmpty()) "hibou_emerald" else trimmed.take(40)
    }

    fun observeLeaderboard(): Flow<List<RemoteLeaderboardEntry>> = flow {
        requireUid()
        val path = "leaderboard"
        emitAll(
            db.collection(path)
                .whereEqualTo("publicVisible", true)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RemoteLeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.totalXp }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun fetchTopLeaderboard(limitCount: Long = 100): Result<List<RemoteLeaderboardEntry>> {
        val path = "leaderboard"
        return try {
            requireUid()
            val snapshot = db.collection(path)
                .whereEqualTo("publicVisible", true)
                .limit(limitCount.coerceIn(1L, 100L))
                .get()
                .await()
            val entries = snapshot.toObjects(
                RemoteLeaderboardEntry::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.totalXp }
            Result.success(entries)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchUserProfile(userId: String): Result<RemoteUserProfile?> {
        val path = "users/$userId"
        return try {
            val uid = requireUid()
            check(uid == userId) { "Refusing cross-user read" }
            val snap = db.collection("users").document(userId).get().await()
            val profile = if (snap.exists()) {
                snap.toObject(
                    RemoteUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun fetchLevelProgress(userId: String): Result<List<RemoteLevelProgress>> {
        val path = "users/$userId/level_progress"
        return try {
            val uid = requireUid()
            check(uid == userId) { "Refusing cross-user list" }
            val snap = db.collection("users").document(userId)
                .collection("level_progress")
                .get()
                .await()
            val items = snap.toObjects(
                RemoteLevelProgress::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun pushUserProfileAndLeaderboard(stats: LocalUserStatsEntity): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Non connecté"))
        val userPath = "users/$uid"
        val leaderboardPath = "leaderboard/$uid"
        val displayName = sanitizedDisplayName(stats.displayName)
        val avatarId = sanitizedAvatarId(stats.avatarId)
        val league = LeagueTier.safeLabel(stats.league)
        val clampedHearts = stats.hearts.coerceIn(0, 5)
        val clampedGoal = stats.dailyGoalXp.coerceIn(10, 500)
        val safeTotalXp = stats.totalXp.coerceIn(0, 10_000_000)
        val safeWeeklyXp = stats.weeklyXp.coerceIn(0, 10_000_000)
        val safeStreak = stats.currentStreak.coerceIn(0, 36_500)
        val safeBestStreak = stats.bestStreak.coerceIn(0, 36_500)
        val safeGems = stats.gems.coerceIn(0, 1_000_000)
        val safeTodayXp = stats.todayXp.coerceIn(0, 100_000)
        val safeLastActive = stats.lastActiveDate.take(20)
        val safeCompleted = stats.completedLevelsCount.coerceIn(0, 1000)
        val safeCorrect = stats.totalCorrect.coerceIn(0, 1_000_000)
        val safeAnswered = stats.totalAnswered.coerceIn(0, 1_000_000)

        try {
            val userRef = db.collection("users").document(uid)
            val existingUser = userRef.get().await()
            if (!existingUser.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "totalXp" to safeTotalXp,
                    "weeklyXp" to safeWeeklyXp,
                    "currentStreak" to safeStreak,
                    "bestStreak" to safeBestStreak,
                    "hearts" to clampedHearts,
                    "gems" to safeGems,
                    "dailyGoalXp" to clampedGoal,
                    "todayXp" to safeTodayXp,
                    "lastActiveDate" to safeLastActive,
                    "league" to league,
                    "completedLevelsCount" to safeCompleted,
                    "totalCorrect" to safeCorrect,
                    "totalAnswered" to safeAnswered,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "totalXp" to safeTotalXp,
                    "weeklyXp" to safeWeeklyXp,
                    "currentStreak" to safeStreak,
                    "bestStreak" to safeBestStreak,
                    "hearts" to clampedHearts,
                    "gems" to safeGems,
                    "dailyGoalXp" to clampedGoal,
                    "todayXp" to safeTodayXp,
                    "lastActiveDate" to safeLastActive,
                    "league" to league,
                    "completedLevelsCount" to safeCompleted,
                    "totalCorrect" to safeCorrect,
                    "totalAnswered" to safeAnswered,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.update(updatePayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userPath)
            return Result.failure(e)
        }

        try {
            val lbRef = db.collection("leaderboard").document(uid)
            val existingLb = lbRef.get().await()
            if (!existingLb.exists()) {
                val createLbPayload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "totalXp" to safeTotalXp,
                    "weeklyXp" to safeWeeklyXp,
                    "currentStreak" to safeStreak,
                    "league" to league,
                    "completedLevelsCount" to safeCompleted,
                    "publicVisible" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                lbRef.set(createLbPayload).await()
            } else {
                val updateLbPayload = mapOf(
                    "displayName" to displayName,
                    "avatarId" to avatarId,
                    "totalXp" to safeTotalXp,
                    "weeklyXp" to safeWeeklyXp,
                    "currentStreak" to safeStreak,
                    "league" to league,
                    "completedLevelsCount" to safeCompleted,
                    "publicVisible" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                lbRef.update(updateLbPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, leaderboardPath)
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    suspend fun pushLevelProgress(progress: LocalLevelProgressEntity): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Non connecté"))
        val path = "users/$uid/level_progress/${progress.levelId}"
        return try {
            val docRef = db.collection("users").document(uid)
                .collection("level_progress").document(progress.levelId)
            val existing = docRef.get().await()
            val safeStars = progress.stars.coerceIn(0, 3)
            val safeBestScore = progress.bestScore.coerceIn(0, 100)
            val safeAttempts = progress.attempts.coerceIn(0, 10_000)

            if (!existing.exists()) {
                val createPayload = mapOf(
                    "levelId" to progress.levelId,
                    "unitId" to progress.unitId,
                    "stars" to safeStars,
                    "bestScore" to safeBestScore,
                    "attempts" to safeAttempts,
                    "completed" to progress.completed,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "stars" to safeStars,
                    "bestScore" to safeBestScore,
                    "attempts" to safeAttempts,
                    "completed" to progress.completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
