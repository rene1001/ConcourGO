package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CachedLeaderboardEntity
import com.example.data.local.LevelEntity
import com.example.data.local.LocalLevelProgressEntity
import com.example.data.local.LocalUserStatsEntity
import com.example.data.local.UnitEntity
import com.example.data.local.UserPreferences
import com.example.data.payment.BictorysConfig
import com.example.data.payment.BictorysPaymentMethod
import com.example.data.payment.BictorysPaymentRepository
import com.example.data.payment.PaymentInitiationResult
import com.example.data.payment.PaymentVerificationResult
import com.example.data.payment.PendingPaymentRecord
import com.example.data.payment.StorePack
import com.example.data.remote.LeagueTier
import com.example.data.repository.ConcoursRepository
import com.example.data.repository.GamificationEngine
import com.example.notifications.DailyReminderScheduler
import com.example.notifications.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LevelNodeUi(
    val level: LevelEntity,
    val stars: Int,
    val bestScore: Int,
    val attempts: Int,
    val completed: Boolean,
    val unlocked: Boolean,
    val isCurrentTarget: Boolean
)

data class UnitWithLevelsUi(
    val unit: UnitEntity,
    val levels: List<LevelNodeUi>,
    val completedCount: Int
)

data class MainDashboardState(
    val isLoading: Boolean = true,
    val unitsWithLevels: List<UnitWithLevelsUi> = emptyList(),
    val userStats: LocalUserStatsEntity = LocalUserStatsEntity(),
    val preferences: UserPreferences = UserPreferences(),
    val offlineQuestionCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val isSignedIn: Boolean = false
)

data class LeaderboardEntryUi(
    val userId: String,
    val displayName: String,
    val avatarId: String,
    val totalXp: Int,
    val weeklyXp: Int,
    val currentStreak: Int,
    val league: String,
    val completedLevelsCount: Int,
    val isCurrentUser: Boolean
)

sealed interface LeaderboardStreamState {
    data object GuestOffline : LeaderboardStreamState
    data object Loading : LeaderboardStreamState
    data class Error(val message: String) : LeaderboardStreamState
    data class Ready(
        val entries: List<LeaderboardEntryUi>,
        val isFromOfflineCache: Boolean,
        val warningMessage: String? = null
    ) : LeaderboardStreamState
}

data class ShopUiState(
    val packs: List<StorePack> = emptyList(),
    val selectedPackId: String = "pack_major",
    val selectedMethod: BictorysPaymentMethod = BictorysPaymentMethod.HOSTED_CHECKOUT,
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "+226",
    val countryCode: String = "BF",
    val isSubmitting: Boolean = false,
    val isVerifying: Boolean = false,
    val pendingPayment: PendingPaymentRecord? = null,
    val checkoutUrlToOpen: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSandboxMode: Boolean = BictorysConfig.isSandboxKey(),
    val isApiKeyConfigured: Boolean = BictorysConfig.isConfigured()
) {
    val selectedPack: StorePack?
        get() = packs.firstOrNull { it.id == selectedPackId } ?: packs.firstOrNull()
}

private data class ShopFormDraft(
    val selectedPackId: String = "pack_major",
    val selectedMethod: BictorysPaymentMethod = BictorysPaymentMethod.HOSTED_CHECKOUT,
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "+226",
    val countryCode: String = "BF",
    val isSubmitting: Boolean = false,
    val isVerifying: Boolean = false,
    val checkoutUrlToOpen: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: ConcoursRepository,
    val currentUserId: String?,
    private val currentUserDisplayName: String?,
    private val paymentRepository: BictorysPaymentRepository? = null
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isRefreshingLeaderboard = MutableStateFlow(false)
    val isRefreshingLeaderboard: StateFlow<Boolean> = _isRefreshingLeaderboard.asStateFlow()

    private val _leaderboardRefreshTick = MutableStateFlow(0)

    private val _shopFormDraft = MutableStateFlow(
        ShopFormDraft(
            customerName = currentUserDisplayName?.takeIf { it.isNotBlank() && it != "Candidat" } ?: ""
        )
    )

    val shopState: StateFlow<ShopUiState> = combine(
        _shopFormDraft,
        paymentRepository?.pendingPaymentFlow ?: flowOf(null)
    ) { draft, pending ->
        ShopUiState(
            packs = paymentRepository?.catalog ?: emptyList(),
            selectedPackId = draft.selectedPackId,
            selectedMethod = draft.selectedMethod,
            customerName = draft.customerName,
            customerEmail = draft.customerEmail,
            customerPhone = draft.customerPhone,
            countryCode = draft.countryCode,
            isSubmitting = draft.isSubmitting,
            isVerifying = draft.isVerifying,
            pendingPayment = pending,
            checkoutUrlToOpen = draft.checkoutUrlToOpen,
            errorMessage = draft.errorMessage,
            successMessage = draft.successMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ShopUiState(
            packs = paymentRepository?.catalog ?: emptyList(),
            customerName = _shopFormDraft.value.customerName
        )
    )

    init {
        viewModelScope.launch {
            repository.ensureSeededAndInitialized(currentUserId, currentUserDisplayName)
            if (!currentUserId.isNullOrBlank()) {
                repository.syncBidirectionalWithCloud(currentUserId, currentUserDisplayName)
            }
        }
    }

    val dashboardState: StateFlow<MainDashboardState> = combine(
        repository.unitsFlow,
        repository.levelsFlow,
        repository.levelProgressFlow,
        repository.userStatsFlow,
        repository.preferencesFlow,
        repository.questionCountFlow,
        repository.pendingLevelSyncCountFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val units = values[0] as List<UnitEntity>
        @Suppress("UNCHECKED_CAST")
        val levels = values[1] as List<LevelEntity>
        @Suppress("UNCHECKED_CAST")
        val progressList = values[2] as List<LocalLevelProgressEntity>
        val stats = values[3] as LocalUserStatsEntity
        val prefs = values[4] as UserPreferences
        val qCount = values[5] as Int
        val pendingLevels = values[6] as Int

        val progressById = progressList.associateBy { it.levelId }
        val levelsByUnit = levels.groupBy { it.unitId }

        var firstIncompleteMarked = false
        val mappedUnits = units.map { unit ->
            val unitLevels = (levelsByUnit[unit.unitId] ?: emptyList()).sortedBy { it.orderIndex }
            val nodeList = unitLevels.map { lvl ->
                val prog = progressById[lvl.levelId]
                val unlocked = GamificationEngine.isLevelUnlocked(lvl, unitLevels, progressById)
                val completed = prog?.completed == true
                val isTarget = unlocked && !completed && !firstIncompleteMarked
                if (isTarget) firstIncompleteMarked = true
                LevelNodeUi(
                    level = lvl,
                    stars = prog?.stars ?: 0,
                    bestScore = prog?.bestScore ?: 0,
                    attempts = prog?.attempts ?: 0,
                    completed = completed,
                    unlocked = unlocked,
                    isCurrentTarget = isTarget
                )
            }
            UnitWithLevelsUi(
                unit = unit,
                levels = nodeList,
                completedCount = nodeList.count { it.completed }
            )
        }

        val totalPending = pendingLevels + if (stats.hasPendingProfileSync) 1 else 0
        MainDashboardState(
            isLoading = false,
            unitsWithLevels = mappedUnits,
            userStats = stats,
            preferences = prefs,
            offlineQuestionCount = qCount,
            pendingSyncCount = totalPending,
            isSignedIn = !currentUserId.isNullOrBlank()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MainDashboardState(isSignedIn = !currentUserId.isNullOrBlank())
    )

    val leaderboardState: StateFlow<LeaderboardStreamState> =
        if (currentUserId.isNullOrBlank()) {
            MutableStateFlow<LeaderboardStreamState>(LeaderboardStreamState.GuestOffline).asStateFlow()
        } else {
            combine(
                repository.preferencesFlow.map { it.offlineModeEnabled }.distinctUntilChanged(),
                _leaderboardRefreshTick
            ) { offlineForced, _ -> offlineForced }
                .flatMapLatest { offlineForced ->
                    if (offlineForced || !repository.isNetworkConnected()) {
                        repository.cachedLeaderboardFlow.map { cached ->
                            LeaderboardStreamState.Ready(
                                entries = cached.toUiEntries(currentUserId),
                                isFromOfflineCache = true
                            )
                        }
                    } else {
                        repository.observeCloudLeaderboard()
                            .onEach { entries -> repository.cacheRemoteLeaderboard(entries) }
                            .map<List<com.example.data.remote.RemoteLeaderboardEntry>, LeaderboardStreamState> { entries ->
                                LeaderboardStreamState.Ready(
                                    entries = entries.map { entry ->
                                        LeaderboardEntryUi(
                                            userId = entry.userId,
                                            displayName = entry.displayName,
                                            avatarId = entry.avatarId,
                                            totalXp = entry.totalXp.toInt(),
                                            weeklyXp = entry.weeklyXp.toInt(),
                                            currentStreak = entry.currentStreak.toInt(),
                                            league = LeagueTier.safeLabel(entry.league),
                                            completedLevelsCount = entry.completedLevelsCount.toInt(),
                                            isCurrentUser = entry.userId == currentUserId
                                        )
                                    },
                                    isFromOfflineCache = false
                                )
                            }
                            .catch { error ->
                                val firestoreError = generateSequence(error) { it.cause }
                                    .filterIsInstance<FirebaseFirestoreException>()
                                    .firstOrNull()
                                if (firestoreError == null && error is CancellationException) throw error
                                val rootError = firestoreError ?: error
                                Log.w(TAG, "Erreur classement Firestore, bascule sur le cache", rootError)
                                // Fall back gracefully to cached Room entries or Error if cache is empty
                                repository.cachedLeaderboardFlow.collect { cached ->
                                    if (cached.isEmpty()) {
                                        emit(
                                            LeaderboardStreamState.Error(
                                                message = rootError.localizedMessage
                                                    ?: "Impossible de récupérer le classement depuis Firestore."
                                            )
                                        )
                                    } else {
                                        emit(
                                            LeaderboardStreamState.Ready(
                                                entries = cached.toUiEntries(currentUserId),
                                                isFromOfflineCache = true,
                                                warningMessage = rootError.localizedMessage
                                            )
                                        )
                                    }
                                }
                            }
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = LeaderboardStreamState.Loading
                )
        }

    private fun List<CachedLeaderboardEntity>.toUiEntries(uid: String): List<LeaderboardEntryUi> {
        return map { item ->
            LeaderboardEntryUi(
                userId = item.userId,
                displayName = item.displayName,
                avatarId = item.avatarId,
                totalXp = item.totalXp,
                weeklyXp = item.weeklyXp,
                currentStreak = item.currentStreak,
                league = LeagueTier.safeLabel(item.league),
                completedLevelsCount = item.completedLevelsCount,
                isCurrentUser = item.userId == uid
            )
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun refreshStreakOnResume() {
        viewModelScope.launch {
            repository.refreshDailyStreakIfNeeded(currentUserId = currentUserId, syncIfChanged = true)
            if (shopState.value.pendingPayment != null) {
                verifyPendingPayment(silent = true)
            }
        }
    }

    fun selectShopPack(packId: String) {
        _shopFormDraft.value = _shopFormDraft.value.copy(
            selectedPackId = packId,
            errorMessage = null
        )
    }

    fun selectPaymentMethod(method: BictorysPaymentMethod) {
        _shopFormDraft.value = _shopFormDraft.value.copy(
            selectedMethod = method,
            errorMessage = null
        )
    }

    fun updateShopCustomerName(name: String) {
        _shopFormDraft.value = _shopFormDraft.value.copy(
            customerName = name,
            errorMessage = null
        )
    }

    fun updateShopCustomerPhone(phone: String) {
        _shopFormDraft.value = _shopFormDraft.value.copy(
            customerPhone = phone,
            errorMessage = null
        )
    }

    fun updateShopCustomerEmail(email: String) {
        _shopFormDraft.value = _shopFormDraft.value.copy(
            customerEmail = email,
            errorMessage = null
        )
    }

    fun selectShopCountry(code: String) {
        _shopFormDraft.value = _shopFormDraft.value.copy(
            countryCode = code,
            errorMessage = null
        )
    }

    fun clearShopMessages() {
        _shopFormDraft.value = _shopFormDraft.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun clearCheckoutUrlToOpen() {
        _shopFormDraft.value = _shopFormDraft.value.copy(checkoutUrlToOpen = null)
    }

    fun startBictorysCheckout() {
        val payRepo = paymentRepository ?: return
        val current = shopState.value
        val pack = current.selectedPack ?: return
        if (current.isSubmitting) return

        viewModelScope.launch {
            _shopFormDraft.value = _shopFormDraft.value.copy(
                isSubmitting = true,
                errorMessage = null,
                successMessage = null
            )
            try {
                when (
                    val res = payRepo.initiatePayment(
                        pack = pack,
                        customerName = current.customerName.ifBlank {
                            dashboardState.value.userStats.displayName
                        },
                        customerEmail = current.customerEmail,
                        customerPhone = current.customerPhone,
                        countryCode = current.countryCode,
                        paymentMethod = current.selectedMethod
                    )
                ) {
                    is PaymentInitiationResult.CheckoutReady -> {
                        _shopFormDraft.value = _shopFormDraft.value.copy(
                            checkoutUrlToOpen = res.checkoutUrl,
                            successMessage = "Session Bictorys créée (${res.record.paymentReference}). Finalise le paiement puis reviens ici."
                        )
                    }

                    is PaymentInitiationResult.DirectPushInitiated -> {
                        _shopFormDraft.value = _shopFormDraft.value.copy(
                            successMessage = "Demande envoyée sur ton téléphone (${res.record.paymentReference}). Valide puis clique sur Vérifier le statut."
                        )
                    }

                    is PaymentInitiationResult.Error -> {
                        _shopFormDraft.value = _shopFormDraft.value.copy(
                            errorMessage = res.message
                        )
                    }
                }
            } finally {
                _shopFormDraft.value = _shopFormDraft.value.copy(isSubmitting = false)
            }
        }
    }

    fun verifyPendingPayment(silent: Boolean = false) {
        val payRepo = paymentRepository ?: return
        if (_shopFormDraft.value.isVerifying) return

        viewModelScope.launch {
            _shopFormDraft.value = _shopFormDraft.value.copy(
                isVerifying = true,
                errorMessage = if (silent) _shopFormDraft.value.errorMessage else null
            )
            try {
                when (val result = payRepo.verifyPendingPayment(currentUserId)) {
                    is PaymentVerificationResult.Succeeded -> {
                        val rec = result.record
                        val passText = if (rec.grantsPass) " et Pass ConcoursGo activé" else ""
                        _shopFormDraft.value = _shopFormDraft.value.copy(
                            successMessage = "Paiement confirmé ! +${rec.gemsReward} gemmes créditées$passText."
                        )
                    }

                    is PaymentVerificationResult.StillPending -> {
                        if (!silent) {
                            _shopFormDraft.value = _shopFormDraft.value.copy(
                                successMessage = "Paiement toujours en attente (${result.record.status}). Finalise sur la page Bictorys."
                            )
                        }
                    }

                    is PaymentVerificationResult.Failed -> {
                        _shopFormDraft.value = _shopFormDraft.value.copy(
                            errorMessage = result.message
                        )
                    }

                    is PaymentVerificationResult.Error -> {
                        if (!silent) {
                            _shopFormDraft.value = _shopFormDraft.value.copy(
                                errorMessage = result.message
                            )
                        }
                    }

                    PaymentVerificationResult.NoPendingPayment -> Unit
                }
            } finally {
                _shopFormDraft.value = _shopFormDraft.value.copy(isVerifying = false)
            }
        }
    }

    fun dismissPendingPayment() {
        val payRepo = paymentRepository ?: return
        viewModelScope.launch {
            payRepo.clearPendingPayment()
            _shopFormDraft.value = _shopFormDraft.value.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun refreshLeaderboard() {
        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            _statusMessage.value = "Connecte ton compte Google pour actualiser le classement."
            return
        }
        viewModelScope.launch {
            _isRefreshingLeaderboard.value = true
            try {
                _leaderboardRefreshTick.value += 1
                val result = repository.refreshLeaderboardFromCloud()
                result.onFailure { err ->
                    _statusMessage.value = err.localizedMessage
                        ?: "Impossible d'actualiser le classement."
                }
            } finally {
                _isRefreshingLeaderboard.value = false
            }
        }
    }

    fun toggleOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setOfflineModeEnabled(enabled)
            if (!enabled && !currentUserId.isNullOrBlank()) {
                syncNow()
            } else if (enabled) {
                _statusMessage.value = "Mode hors-ligne activé : tes révisions restent sauvegardées localement."
            }
        }
    }

    fun syncNow() {
        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            _statusMessage.value = "Connecte ton compte Google pour synchroniser."
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val summary = repository.syncBidirectionalWithCloud(uid, currentUserDisplayName)
                _statusMessage.value = if (summary.errorMessage == null && summary.profileSynced) {
                    "Synchronisation réussie (${summary.syncedLevelsCount} niveau(x) envoyé(s))."
                } else {
                    summary.errorMessage ?: "Synchronisation partielle."
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun refillHeartsWithGems() {
        viewModelScope.launch {
            val ok = repository.refillHeartsWithGems(40)
            _statusMessage.value = if (ok) {
                "5 cœurs rechargés !"
            } else {
                "Pas assez de gemmes (40 requises). Utilise la récupération gratuite !"
            }
        }
    }

    fun refillHeartsFree() {
        viewModelScope.launch {
            repository.refillHeartsFreePractice()
            _statusMessage.value = "5 cœurs restaurés pour continuer à réviser !"
        }
    }

    fun selectAvatar(avatarId: String) {
        viewModelScope.launch {
            repository.updateAvatar(avatarId, currentUserId)
        }
    }

    fun updateDailyGoal(goalXp: Int) {
        viewModelScope.launch {
            repository.updateDailyGoal(goalXp, currentUserId)
            _statusMessage.value = "Objectif quotidien réglé à $goalXp XP."
        }
    }

    fun setNotificationsEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
            val prefs = dashboardState.value.preferences
            if (enabled) {
                DailyReminderScheduler.scheduleDailyReminder(
                    context = context.applicationContext,
                    hour = prefs.reminderHour,
                    minute = prefs.reminderMinute
                )
                _statusMessage.value = String.format(
                    "Rappel quotidien activé à %02d:%02d.",
                    prefs.reminderHour,
                    prefs.reminderMinute
                )
            } else {
                DailyReminderScheduler.cancelDailyReminder(context.applicationContext)
                _statusMessage.value = "Rappels quotidiens désactivés."
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int, context: Context) {
        viewModelScope.launch {
            repository.setReminderTime(hour, minute)
            if (dashboardState.value.preferences.notificationsEnabled) {
                DailyReminderScheduler.scheduleDailyReminder(context.applicationContext, hour, minute)
            }
            _statusMessage.value = String.format("Heure de rappel mise à jour : %02d:%02d.", hour, minute)
        }
    }

    fun sendTestNotification(context: Context) {
        val stats = dashboardState.value.userStats
        val sent = NotificationHelper.showMotivationalNotification(
            context = context.applicationContext,
            streak = stats.currentStreak,
            totalXp = stats.totalXp,
            isTest = true
        )
        _statusMessage.value = if (sent) {
            "Notification de test envoyée !"
        } else {
            "Permission de notification requise dans les paramètres Android."
        }
    }

    fun exitGuestMode() {
        viewModelScope.launch {
            repository.setOfflineGuestEntered(false)
        }
    }

    private companion object {
        const val TAG = "MainVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
