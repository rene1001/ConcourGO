package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UnitEntity
import com.example.ui.components.DailyGoalCard
import com.example.ui.components.TopGamifiedHud
import com.example.ui.theme.LocalConcoursColors
import com.example.ui.viewmodel.LevelNodeUi
import com.example.ui.viewmodel.MainDashboardState
import com.example.ui.viewmodel.UnitWithLevelsUi
import kotlinx.coroutines.launch

@Composable
fun PathScreen(
    state: MainDashboardState,
    onStartLevel: (String) -> Unit,
    onToggleOffline: (Boolean) -> Unit,
    onRefillHeartsGems: () -> Unit,
    onRefillHeartsFree: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showNoHeartsDialog by remember { mutableStateOf(false) }
    val lockedMessage = stringResource(R.string.level_locked)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopGamifiedHud(
                streak = state.userStats.currentStreak,
                totalXp = state.userStats.totalXp,
                gems = state.userStats.gems,
                hearts = state.userStats.hearts,
                offlineMode = state.preferences.offlineModeEnabled,
                pendingSyncCount = state.pendingSyncCount,
                onToggleOffline = { onToggleOffline(!state.preferences.offlineModeEnabled) },
                onHeartsClicked = { showNoHeartsDialog = true }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("learning_path_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item(key = "daily_goal_header") {
                    Box(modifier = Modifier.widthIn(max = 600.dp)) {
                        DailyGoalCard(
                            todayXp = state.userStats.todayXp,
                            dailyGoalXp = state.userStats.dailyGoalXp,
                            league = state.userStats.league
                        )
                    }
                }

                state.unitsWithLevels.forEachIndexed { unitIdx, unitGroup ->
                    item(key = "unit_banner_${unitGroup.unit.unitId}") {
                        Box(modifier = Modifier.widthIn(max = 600.dp)) {
                            UnitBannerCard(
                                unit = unitGroup.unit,
                                unitNumber = unitIdx + 1,
                                completedCount = unitGroup.completedCount,
                                totalCount = unitGroup.levels.size
                            )
                        }
                    }

                    itemsIndexed(
                        items = unitGroup.levels,
                        key = { _, node -> node.level.levelId }
                    ) { levelIdx, node ->
                        val offsetFraction = when (levelIdx % 4) {
                            0 -> 0f
                            1 -> 0.22f
                            2 -> 0f
                            else -> -0.22f
                        }
                        val nextOffsetFraction = when ((levelIdx + 1) % 4) {
                            0 -> 0f
                            1 -> 0.22f
                            2 -> 0f
                            else -> -0.22f
                        }
                        val unitColors = LocalConcoursColors.current.unitColors[unitGroup.unit.colorKey]
                            ?: (MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer)

                        Box(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                        ) {
                            WindingLevelNodeRow(
                                node = node,
                                unitPrimary = unitColors.first,
                                unitShadow = unitColors.second,
                                horizontalBias = offsetFraction,
                                hasNextInUnit = levelIdx < unitGroup.levels.lastIndex,
                                nextBias = nextOffsetFraction,
                                onClick = {
                                    when {
                                        !node.unlocked -> {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(lockedMessage)
                                            }
                                        }
                                        state.userStats.hearts <= 0 -> {
                                            showNoHeartsDialog = true
                                        }
                                        else -> {
                                            onStartLevel(node.level.levelId)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                item(key = "path_bottom_spacer") {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (showNoHeartsDialog) {
        val hasEnoughGems = state.userStats.gems >= 40
        AlertDialog(
            onDismissRequest = { showNoHeartsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = LocalConcoursColors.current.heart,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text(stringResource(R.string.hearts_empty_title)) },
            text = {
                Text(
                    text = stringResource(R.string.hearts_empty_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onRefillHeartsGems()
                            showNoHeartsDialog = false
                        },
                        enabled = hasEnoughGems && state.userStats.hearts < 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refill_hearts_gems_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalConcoursColors.current.gem
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Diamond,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.hearts_refill_gems))
                    }

                    OutlinedButton(
                        onClick = {
                            onRefillHeartsFree()
                            showNoHeartsDialog = false
                        },
                        enabled = state.userStats.hearts < 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refill_hearts_free_button")
                    ) {
                        Text(stringResource(R.string.hearts_free_practice))
                    }

                    TextButton(
                        onClick = { showNoHeartsDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            }
        )
    }
}

@Composable
private fun UnitBannerCard(
    unit: UnitEntity,
    unitNumber: Int,
    completedCount: Int,
    totalCount: Int
) {
    val extraColors = LocalConcoursColors.current
    val (primary, shadow) = extraColors.unitColors[unit.colorKey]
        ?: (MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primary)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("unit_banner_${unit.unitId}"),
        shape = RoundedCornerShape(22.dp),
        color = primary,
        border = BorderStroke(2.dp, shadow),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = stringResource(R.string.unit_label, unitNumber),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = unit.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Text(
                    text = unit.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = unit.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = "$completedCount/$totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun WindingLevelNodeRow(
    node: LevelNodeUi,
    unitPrimary: Color,
    unitShadow: Color,
    horizontalBias: Float,
    hasNextInUnit: Boolean,
    nextBias: Float,
    onClick: () -> Unit
) {
    val extraColors = LocalConcoursColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "targetPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (node.isCurrentTarget) 1.07f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val nodeColor = when {
        !node.unlocked -> extraColors.lockedSurface
        node.completed -> extraColors.gold
        else -> unitPrimary
    }
    val nodeBorderColor = when {
        !node.unlocked -> extraColors.lockedBorder
        node.completed -> extraColors.goldDark
        else -> unitShadow
    }
    val nodeContentColor = when {
        !node.unlocked -> extraColors.lockedIcon
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (horizontalBias * 180).dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (node.isCurrentTarget) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, unitPrimary),
                        shadowElevation = 3.dp
                    ) {
                        Text(
                            text = stringResource(R.string.level_start).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = unitPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else if (node.completed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (starIdx in 1..3) {
                            Icon(
                                imageVector = if (starIdx <= node.stars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = null,
                                tint = extraColors.gold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else if (node.level.isBossChallenge) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = extraColors.goldContainer
                    ) {
                        Text(
                            text = stringResource(R.string.level_boss_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = extraColors.onGoldContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // 3D Tactile Circle Node
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(84.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("level_node_${node.level.levelId}")
                        .clickable(
                            role = Role.Button,
                            onClick = onClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (node.isCurrentTarget) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .border(3.dp, unitPrimary.copy(alpha = 0.35f), CircleShape)
                        )
                    }

                    // Bottom 3D lip
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .offset(y = 5.dp)
                            .clip(CircleShape)
                            .background(nodeBorderColor)
                    )

                    // Top tactile circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(nodeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                !node.unlocked -> Icons.Rounded.Lock
                                node.level.isBossChallenge -> Icons.Rounded.EmojiEvents
                                node.completed -> Icons.Rounded.Check
                                else -> Icons.Rounded.PlayArrow
                            },
                            contentDescription = node.level.title,
                            tint = nodeContentColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = node.level.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (node.unlocked) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${node.level.questionCount} QCM • +${node.level.xpReward} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (hasNextInUnit) {
            val pathColor = MaterialTheme.colorScheme.outline
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            ) {
                val startX = size.width / 2f + (horizontalBias * 180.dp.toPx())
                val endX = size.width / 2f + (nextBias * 180.dp.toPx())
                drawLine(
                    color = pathColor,
                    start = Offset(startX, 2f),
                    end = Offset(endX, size.height - 2f),
                    strokeWidth = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            }
        }
    }
}
