package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.LocalLevelProgressEntity
import com.example.data.local.LocalUserStatsEntity
import com.example.data.local.QuestionBankSeed
import com.example.data.payment.BictorysConfig
import com.example.data.payment.BictorysValidator
import com.example.data.payment.NormalizedPaymentStatus
import com.example.data.remote.LeagueTier
import com.example.data.repository.GamificationEngine
import com.example.notifications.DailyReminderScheduler
import com.example.ui.components.LeaderboardHelper
import com.example.ui.viewmodel.LeaderboardEntryUi
import com.example.ui.viewmodel.QuizSessionUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context matches ConcoursGo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ConcoursGo", appName)
    }

    @Test
    fun `question bank seed has 5 units 20 levels and 120 valid QCMs`() {
        val questions = QuestionBankSeed.allQuestions()
        assertEquals(5, QuestionBankSeed.units.size)
        assertEquals(20, QuestionBankSeed.levels.size)
        assertEquals(120, questions.size)

        val uniqueIds = questions.map { it.questionId }.toSet()
        assertEquals(120, uniqueIds.size)

        questions.forEach { q ->
            assertTrue("Question ${q.questionId} invalid correctIndex", q.correctIndex in 0..3)
            assertTrue("Question ${q.questionId} blank prompt", q.prompt.isNotBlank())
            assertTrue("Question ${q.questionId} blank explanation", q.explanation.isNotBlank())
            assertEquals("Question ${q.questionId} needs 4 distinct options", 4, q.options().toSet().size)
        }
    }

    @Test
    fun `gamification engine calculates stars xp streak and league accurately`() {
        assertEquals(3, GamificationEngine.calculateStars(6, 6))
        assertEquals(2, GamificationEngine.calculateStars(5, 6))
        assertEquals(1, GamificationEngine.calculateStars(3, 6))
        assertEquals(0, GamificationEngine.calculateStars(2, 6))

        val outcome = GamificationEngine.calculateQuizOutcome(
            baseXpReward = 40,
            correctCount = 6,
            totalCount = 6,
            maxCombo = 6,
            isFirstCompletion = true,
            previousTotalXp = 220,
            lastActiveDate = GamificationEngine.yesterdayDateString(1_750_000_000_000L),
            currentStreak = 4,
            bestStreak = 4,
            nowMillis = 1_750_000_000_000L
        )
        assertTrue(outcome.passed)
        assertEquals(3, outcome.stars)
        assertEquals(67, outcome.xpEarned) // 40 base + 12 combo + 15 perfect
        assertEquals(15, outcome.gemsEarned)
        assertEquals(5, outcome.newStreak)
        assertEquals(5, outcome.newBestStreak)
        assertEquals(LeagueTier.ARGENT, outcome.newLeague)
    }

    @Test
    fun `offline Room database stores curriculum progress and pending sync items`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.concoursDao()
            dao.seedCurriculum(
                units = QuestionBankSeed.units,
                levels = QuestionBankSeed.levels,
                questions = QuestionBankSeed.allQuestions()
            )
            assertEquals(120, dao.getQuestionCount())
            val u1Questions = dao.getQuestionsForLevel("u1_l1")
            assertEquals(6, u1Questions.size)

            val stats = LocalUserStatsEntity(
                totalXp = 75,
                currentStreak = 2,
                hasPendingProfileSync = true
            )
            val progress = LocalLevelProgressEntity(
                levelId = "u1_l1",
                unitId = "unit_1",
                stars = 3,
                bestScore = 100,
                attempts = 1,
                completed = true,
                updatedAtMillis = System.currentTimeMillis(),
                needsSync = true
            )
            dao.saveQuizOutcome(stats, progress)

            assertEquals(1, dao.observePendingLevelSyncCount().first())
            val loadedStats = dao.getUserStats()
            assertEquals(75, loadedStats?.totalXp)
            assertTrue(loadedStats?.hasPendingProfileSync == true)

            dao.upsertLevelProgress(progress.copy(needsSync = false))
            assertEquals(0, dao.observePendingLevelSyncCount().first())
        } finally {
            db.close()
        }
    }

    @Test
    fun `level unlocking opens first level of unit and unlocks sequential levels on completion`() {
        val u1Levels = QuestionBankSeed.levels.filter { it.unitId == "unit_1" }
        val level1 = u1Levels.first { it.orderIndex == 1 }
        val level2 = u1Levels.first { it.orderIndex == 2 }

        assertTrue(GamificationEngine.isLevelUnlocked(level1, u1Levels, emptyMap()))
        assertFalse(GamificationEngine.isLevelUnlocked(level2, u1Levels, emptyMap()))

        val completedMap = mapOf(
            level1.levelId to LocalLevelProgressEntity(
                levelId = level1.levelId,
                unitId = level1.unitId,
                stars = 2,
                bestScore = 83,
                attempts = 1,
                completed = true,
                updatedAtMillis = 1L,
                needsSync = false
            )
        )
        assertTrue(GamificationEngine.isLevelUnlocked(level2, u1Levels, completedMap))
    }

    @Test
    fun `daily reminder scheduler registers and cancels without throwing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DailyReminderScheduler.scheduleDailyReminder(context, 19, 30)
        DailyReminderScheduler.cancelDailyReminder(context)
    }

    @Test
    fun `streak validation keeps today or yesterday active and resets missed days while preserving bestStreak`() {
        val now = 1_750_000_000_000L
        val today = GamificationEngine.todayDateString(now)
        val yesterday = GamificationEngine.yesterdayDateString(now)
        val threeDaysAgo = GamificationEngine.todayDateString(now - 3 * 86_400_000L)

        // Same day keeps streak
        val sameDay = GamificationEngine.resolveActiveStreak(today, currentStreak = 6, bestStreak = 8, nowMillis = now)
        assertEquals(6 to 8, sameDay)

        // Yesterday keeps streak alive until user completes today's activity
        val activeYesterday = GamificationEngine.resolveActiveStreak(yesterday, currentStreak = 6, bestStreak = 6, nowMillis = now)
        assertEquals(6 to 6, activeYesterday)

        // Activity 3 days ago breaks current streak to 0, but preserves bestStreak
        val broken = GamificationEngine.resolveActiveStreak(threeDaysAgo, currentStreak = 6, bestStreak = 6, nowMillis = now)
        assertEquals(0 to 6, broken)
    }

    @Test
    fun `streak merge prefers freshest date and never resurrects an expired remote streak`() {
        val now = 1_750_000_000_000L
        val today = GamificationEngine.todayDateString(now)
        val fourDaysAgo = GamificationEngine.todayDateString(now - 4 * 86_400_000L)

        val merged = GamificationEngine.mergeStreakStats(
            localLastDate = today,
            localCurrentStreak = 2,
            localBestStreak = 2,
            remoteLastDate = fourDaysAgo,
            remoteCurrentStreak = 11,
            remoteBestStreak = 11,
            nowMillis = now
        )
        assertEquals(today, merged.first)
        assertEquals(2, merged.second)
        assertEquals(11, merged.third)
    }

    @Test
    fun `leaderboard helper computes avatar placeholder initials and sorts rankings accurately`() {
        assertEquals("MK", LeaderboardHelper.computeInitials("Marie Koné"))
        assertEquals("AM", LeaderboardHelper.computeInitials("Aminata"))
        assertEquals("", LeaderboardHelper.computeInitials("   "))

        val entries = listOf(
            LeaderboardEntryUi(
                userId = "u1",
                displayName = "Benoît Ouédraogo",
                avatarId = "placeholder",
                totalXp = 450,
                weeklyXp = 120,
                currentStreak = 4,
                league = "Or",
                completedLevelsCount = 6,
                isCurrentUser = false
            ),
            LeaderboardEntryUi(
                userId = "u2",
                displayName = "Salimata Traoré",
                avatarId = "",
                totalXp = 320,
                weeklyXp = 280,
                currentStreak = 7,
                league = "Argent",
                completedLevelsCount = 4,
                isCurrentUser = true
            ),
            LeaderboardEntryUi(
                userId = "u3",
                displayName = "Issa Kaboré",
                avatarId = "hibou_emerald",
                totalXp = 900,
                weeklyXp = 90,
                currentStreak = 9,
                league = "Diamant",
                completedLevelsCount = 12,
                isCurrentUser = false
            )
        )

        val sortedTotal = LeaderboardHelper.filterAndSortEntries(
            entries = entries,
            selectedLeague = null,
            sortByWeekly = false
        )
        assertEquals(listOf("u3", "u1", "u2"), sortedTotal.map { it.userId })
        assertEquals(3, LeaderboardHelper.findCurrentUserRank(sortedTotal))

        val sortedWeekly = LeaderboardHelper.filterAndSortEntries(
            entries = entries,
            selectedLeague = null,
            sortByWeekly = true
        )
        assertEquals(listOf("u2", "u1", "u3"), sortedWeekly.map { it.userId })
        assertEquals(1, LeaderboardHelper.findCurrentUserRank(sortedWeekly))

        val argentOnly = LeaderboardHelper.filterAndSortEntries(
            entries = entries,
            selectedLeague = "Argent",
            sortByWeekly = false
        )
        assertEquals(listOf("u2"), argentOnly.map { it.userId })
    }

    @Test
    fun `quiz session ui state computes completed steps and progress fraction across questions`() {
        val sampleQuestions = QuestionBankSeed.allQuestions().take(6)

        val q1Unchecked = QuizSessionUiState(
            isLoading = false,
            questions = sampleQuestions,
            currentIndex = 0,
            isChecked = false
        )
        assertEquals(0, q1Unchecked.completedSteps)
        assertEquals(0f, q1Unchecked.progressFraction, 0.001f)

        val q1Checked = q1Unchecked.copy(isChecked = true)
        assertEquals(1, q1Checked.completedSteps)
        assertEquals(1f / 6f, q1Checked.progressFraction, 0.001f)

        val q6Checked = q1Unchecked.copy(currentIndex = 5, isChecked = true)
        assertEquals(6, q6Checked.completedSteps)
        assertEquals(1f, q6Checked.progressFraction, 0.001f)
    }

    @Test
    fun `bictorys config and validator enforce sandbox routing trusted urls and input checks`() {
        assertFalse(BictorysConfig.isConfigured(BictorysConfig.PLACEHOLDER_KEY))
        assertFalse(BictorysConfig.isConfigured(""))

        assertTrue(BictorysConfig.isConfigured("test_public-demo-key"))
        assertTrue(BictorysConfig.isSandboxKey("test_public-demo-key"))
        assertEquals(BictorysConfig.SANDBOX_BASE_URL, BictorysConfig.resolveBaseUrl("test_public-demo-key"))

        assertTrue(BictorysConfig.isConfigured("live_public-demo-key"))
        assertFalse(BictorysConfig.isSandboxKey("live_public-demo-key"))
        assertEquals(BictorysConfig.LIVE_BASE_URL, BictorysConfig.resolveBaseUrl("live_public-demo-key"))

        assertTrue(BictorysValidator.isTrustedCheckoutUrl("https://checkout.bictorys.com/session/123"))
        assertTrue(BictorysValidator.isTrustedCheckoutUrl("https://bictorys.com/pay"))
        assertFalse(BictorysValidator.isTrustedCheckoutUrl("http://checkout.bictorys.com/session/123"))
        assertFalse(BictorysValidator.isTrustedCheckoutUrl("https://evil-bictorys.example.com/pay"))
        assertFalse(BictorysValidator.isTrustedCheckoutUrl("javascript:alert(1)"))

        assertEquals(null, BictorysValidator.validateCustomerName("Aminata Ouédraogo"))
        assertTrue(BictorysValidator.validateCustomerName("A") != null)
        assertEquals(null, BictorysValidator.validatePhone("+226 70 12 34 56"))
        assertTrue(BictorysValidator.validatePhone("123") != null)
        assertEquals(null, BictorysValidator.validateEmail(""))
        assertEquals(null, BictorysValidator.validateEmail("candidat@example.bf"))
        assertTrue(BictorysValidator.validateEmail("invalide") != null)
        assertTrue(BictorysValidator.isAllowedCountry("BF"))
        assertFalse(BictorysValidator.isAllowedCountry("ZZ"))

        assertEquals(NormalizedPaymentStatus.SUCCEEDED, BictorysValidator.normalizeStatus("succeeded"))
        assertEquals(NormalizedPaymentStatus.SUCCEEDED, BictorysValidator.normalizeStatus("succès"))
        assertEquals(NormalizedPaymentStatus.FAILED, BictorysValidator.normalizeStatus("failed"))
        assertEquals(NormalizedPaymentStatus.PENDING, BictorysValidator.normalizeStatus("initiated"))
    }
}
