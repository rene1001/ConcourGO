package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.data.repository.GamificationEngine
import com.example.notifications.NotificationHelper
import com.example.ui.components.AvatarBadge
import com.example.ui.components.AvatarCatalog
import com.example.ui.components.Tactile3DButton
import com.example.ui.theme.LocalConcoursColors
import com.example.ui.viewmodel.MainDashboardState
import java.util.Locale

@Composable
fun ProfileScreen(
    state: MainDashboardState,
    isSyncing: Boolean,
    statusMessage: String?,
    onClearStatusMessage: () -> Unit,
    onSelectAvatar: (String) -> Unit,
    onUpdateDailyGoal: (Int) -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onUpdateReminderTime: (Int, Int) -> Unit,
    onTestNotification: () -> Unit,
    onConnectGoogle: () -> Unit,
    onSignOutComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val extraColors = LocalConcoursColors.current

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var permissionDeniedNote by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = false
            onToggleNotifications(true)
        } else {
            permissionDeniedNote = true
        }
    }

    val stats = state.userStats
    val accuracy = if (stats.totalAnswered > 0) {
        (stats.totalCorrect * 100) / stats.totalAnswered
    } else {
        0
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("profile_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (statusMessage != null) {
                item(key = "status_banner") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = onClearStatusMessage
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Profile Header & Avatar Picker
            item(key = "profile_header") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AvatarBadge(avatarId = stats.avatarId, size = 64.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (state.isSignedIn) stats.displayName
                                    else stringResource(R.string.profile_guest_label),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "Ligue ${stats.league} • ${stats.totalXp} XP",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = extraColors.leagueColors[stats.league] ?: MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AvatarCatalog.options.forEach { avatar ->
                                val selected = stats.avatarId == avatar.id
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSelectAvatar(avatar.id) }
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("avatar_pick_${avatar.id}")
                                ) {
                                    AvatarBadge(avatarId = avatar.id, size = 38.dp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = avatar.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Current Daily Streak Highlight Card
            item(key = "profile_current_streak") {
                val today = remember { GamificationEngine.todayDateString() }
                val activeToday = stats.lastActiveDate == today && stats.currentStreak > 0
                val streakStatusText = when {
                    activeToday -> stringResource(R.string.profile_streak_active_today)
                    stats.currentStreak > 0 -> stringResource(R.string.profile_streak_protect_today)
                    else -> stringResource(R.string.profile_streak_start_today)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_current_streak_card"),
                    shape = RoundedCornerShape(22.dp),
                    color = extraColors.streakContainer,
                    border = BorderStroke(1.5.dp, extraColors.streak.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(extraColors.streak.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = stringResource(R.string.profile_current_streak_title),
                                tint = extraColors.streak,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_current_streak_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = extraColors.streak
                            )
                            Text(
                                text = stringResource(
                                    R.string.profile_current_streak_days,
                                    stats.currentStreak
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("profile_current_streak_count")
                            )
                            Text(
                                text = streakStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Stats Grid (including both Current Streak and Best Streak)
            item(key = "profile_stats") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.profile_stats_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile(
                            label = stringResource(R.string.profile_stat_current_streak),
                            value = "${stats.currentStreak} j",
                            icon = Icons.Rounded.LocalFireDepartment,
                            tint = extraColors.streak,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_stat_current_streak")
                        )
                        ProfileStatTile(
                            label = stringResource(R.string.profile_stat_streak),
                            value = "${stats.bestStreak} j",
                            icon = Icons.Rounded.EmojiEvents,
                            tint = extraColors.goldDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile(
                            label = stringResource(R.string.profile_stat_xp),
                            value = "${stats.totalXp}",
                            icon = Icons.Rounded.Bolt,
                            tint = extraColors.goldDark,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = stringResource(R.string.profile_stat_levels),
                            value = "${stats.completedLevelsCount}/20",
                            icon = Icons.Rounded.Star,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ProfileStatTile(
                        label = stringResource(R.string.profile_stat_accuracy),
                        value = "$accuracy%",
                        icon = Icons.Rounded.CheckCircle,
                        tint = extraColors.gem,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Trophies / Badges
            item(key = "profile_badges") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.profile_badges_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BadgeTile(
                            title = "Premier Pas",
                            unlocked = stats.completedLevelsCount >= 1,
                            icon = Icons.Rounded.School
                        )
                        BadgeTile(
                            title = "Série 3 Jours",
                            unlocked = stats.bestStreak >= 3,
                            icon = Icons.Rounded.LocalFireDepartment
                        )
                        BadgeTile(
                            title = "500 XP",
                            unlocked = stats.totalXp >= 500,
                            icon = Icons.Rounded.Bolt
                        )
                        BadgeTile(
                            title = "Ligue Or",
                            unlocked = stats.totalXp >= 700,
                            icon = Icons.Rounded.EmojiEvents
                        )
                        BadgeTile(
                            title = "Élite Concours",
                            unlocked = stats.completedLevelsCount >= 10,
                            icon = Icons.Rounded.WorkspacePremium
                        )
                    }
                }
            }

            // Daily Goal Selector
            item(key = "daily_goal_selector") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.profile_goal_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(20, 50, 100, 150).forEach { goal ->
                                FilterChip(
                                    selected = stats.dailyGoalXp == goal,
                                    onClick = { onUpdateDailyGoal(goal) },
                                    label = { Text("$goal XP") },
                                    modifier = Modifier.testTag("goal_chip_$goal")
                                )
                            }
                        }
                    }
                }
            }

            // Offline Mode Pack Card
            item(key = "offline_mode_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.img_offline_pack),
                                contentDescription = stringResource(R.string.profile_offline_title),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.profile_offline_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(
                                        R.string.profile_offline_desc,
                                        state.offlineQuestionCount
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.preferences.offlineModeEnabled) {
                                        Icons.Rounded.CloudOff
                                    } else {
                                        Icons.Rounded.CloudDone
                                    },
                                    contentDescription = null,
                                    tint = if (state.preferences.offlineModeEnabled) {
                                        extraColors.goldDark
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                                Text(
                                    text = stringResource(R.string.profile_offline_switch),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(
                                checked = state.preferences.offlineModeEnabled,
                                onCheckedChange = onToggleOfflineMode,
                                modifier = Modifier.testTag("profile_offline_switch")
                            )
                        }

                        if (state.isSignedIn) {
                            OutlinedButton(
                                onClick = onSyncNow,
                                enabled = !isSyncing && !state.preferences.offlineModeEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_sync_now_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudSync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = if (state.pendingSyncCount > 0) {
                                        stringResource(R.string.profile_sync_now, state.pendingSyncCount)
                                    } else {
                                        stringResource(R.string.profile_sync_up_to_date)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Daily Notifications Card
            item(key = "notifications_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = extraColors.streakContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = extraColors.streak,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.profile_notif_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.profile_notif_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.profile_notif_switch),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = state.preferences.notificationsEnabled,
                                onCheckedChange = { enable ->
                                    if (!enable) {
                                        permissionDeniedNote = false
                                        onToggleNotifications(false)
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !NotificationHelper.hasNotificationPermission(context)
                                        ) {
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        } else {
                                            permissionDeniedNote = false
                                            onToggleNotifications(true)
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("profile_notifications_switch")
                            )
                        }

                        if (permissionDeniedNote) {
                            Text(
                                text = stringResource(R.string.profile_notif_permission_needed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(
                                        Locale.FRANCE,
                                        stringResource(
                                            R.string.profile_notif_time,
                                            state.preferences.reminderHour,
                                            state.preferences.reminderMinute
                                        )
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            TextButton(
                                onClick = { showTimePickerDialog = true },
                                modifier = Modifier.testTag("profile_change_time_button")
                            ) {
                                Text(stringResource(R.string.profile_notif_change_time))
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    onTestNotification()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_test_notif_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.profile_notif_test))
                        }
                    }
                }
            }

            // Account Action
            item(key = "account_action") {
                if (state.isSignedIn) {
                    OutlinedButton(
                        onClick = {
                            GoogleAuthHelper.signOut(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = onSignOutComplete
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("profile_sign_out_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.profile_sign_out),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Tactile3DButton(
                        text = stringResource(R.string.profile_connect_google),
                        leadingIcon = Icons.Rounded.Login,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_connect_google_button"),
                        onClick = onConnectGoogle
                    )
                }
            }

            item(key = "profile_bottom_spacer") {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showTimePickerDialog) {
        ReminderTimePickerDialog(
            initialHour = state.preferences.reminderHour,
            initialMinute = state.preferences.reminderMinute,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { h, m ->
                onUpdateReminderTime(h, m)
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BadgeTile(
    title: String,
    unlocked: Boolean,
    icon: ImageVector
) {
    val extraColors = LocalConcoursColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (unlocked) extraColors.goldContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (unlocked) extraColors.gold else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.size(width = 112.dp, height = 104.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (unlocked) extraColors.goldDark else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) extraColors.onGoldContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Heure du rappel quotidien") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { hour = (hour + 1) % 24 }) { Text("+1 h") }
                    Text(
                        text = String.format(Locale.US, "%02d", hour),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    TextButton(onClick = { hour = (hour + 23) % 24 }) { Text("-1 h") }
                }
                Text(
                    text = " : ",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { minute = (minute + 15) % 60 }) { Text("+15 m") }
                    Text(
                        text = String.format(Locale.US, "%02d", minute),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    TextButton(onClick = { minute = (minute + 45) % 60 }) { Text("-15 m") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(hour, minute) },
                modifier = Modifier.testTag("confirm_time_picker_button")
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
