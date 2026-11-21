package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.QuizAnimatedProgressBar
import com.example.ui.components.Tactile3DButton
import com.example.ui.theme.LocalConcoursColors
import com.example.ui.viewmodel.QuizSessionUiState
import com.example.ui.viewmodel.QuizViewModel

@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onExitToPath: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = !state.isFinished) {
        showExitConfirm = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Tactile3DButton(
                        text = stringResource(R.string.quiz_summary_back_to_path),
                        onClick = onExitToPath
                    )
                }
            }
            state.isFinished -> {
                QuizSummaryContent(
                    state = state,
                    onBackToPath = onExitToPath
                )
            }
            else -> {
                QuizQuestionContent(
                    state = state,
                    onRequestClose = { showExitConfirm = true },
                    onSelectOption = viewModel::selectOption,
                    onCheckAnswer = viewModel::checkAnswer,
                    onContinue = viewModel::continueToNext
                )
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(R.string.quiz_exit_title)) },
            text = { Text(stringResource(R.string.quiz_exit_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirm = false
                        onExitToPath()
                    },
                    modifier = Modifier.testTag("quiz_confirm_exit_button")
                ) {
                    Text(
                        text = stringResource(R.string.quiz_exit_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.quiz_exit_stay))
                }
            }
        )
    }
}

@Composable
private fun QuizQuestionContent(
    state: QuizSessionUiState,
    onRequestClose: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onCheckAnswer: () -> Unit,
    onContinue: () -> Unit
) {
    val question = state.currentQuestion ?: return
    val extraColors = LocalConcoursColors.current

    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
    ) {
        // Top Quiz Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onRequestClose,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("quiz_close_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.quiz_close_cd)
                )
            }

            QuizAnimatedProgressBar(
                completedSteps = state.completedSteps,
                totalSteps = state.questions.size,
                comboCount = state.currentCombo,
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = extraColors.heartContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = stringResource(R.string.hud_hearts_cd),
                        tint = extraColors.heart,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = state.heartsRemaining.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = extraColors.heart
                    )
                }
            }
        }

        // Question Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = question.sourceTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.currentCombo >= 2) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = extraColors.streakContainer
                        ) {
                            Text(
                                text = stringResource(R.string.quiz_combo_badge, state.currentCombo),
                                style = MaterialTheme.typography.labelMedium,
                                color = extraColors.streak,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "${state.currentIndex + 1} / ${state.questions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = question.prompt,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(4.dp))

            val letters = listOf("A", "B", "C", "D")
            question.options().forEachIndexed { idx, optionText ->
                val isSelected = state.selectedOptionIndex == idx
                val isCorrectOption = idx == question.correctIndex

                val borderColor = when {
                    state.isChecked && isCorrectOption -> MaterialTheme.colorScheme.primary
                    state.isChecked && isSelected && !isCorrectOption -> extraColors.heart
                    isSelected -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.outline
                }
                val bgColor = when {
                    state.isChecked && isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
                    state.isChecked && isSelected && !isCorrectOption -> extraColors.heartContainer
                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                val badgeBg = when {
                    state.isChecked && isCorrectOption -> MaterialTheme.colorScheme.primary
                    state.isChecked && isSelected && !isCorrectOption -> extraColors.heart
                    isSelected -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val badgeTextColor = if (isSelected || (state.isChecked && isCorrectOption)) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("quiz_option_$idx")
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            enabled = !state.isChecked,
                            role = Role.RadioButton,
                            onClick = { onSelectOption(idx) }
                        ),
                    shape = RoundedCornerShape(18.dp),
                    color = bgColor,
                    border = BorderStroke(if (isSelected || (state.isChecked && isCorrectOption)) 2.dp else 1.5.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letters.getOrElse(idx) { "${idx + 1}" },
                                style = MaterialTheme.typography.labelLarge,
                                color = badgeTextColor
                            )
                        }
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // Bottom Verification / Feedback Panel
        AnimatedVisibility(
            visible = state.isChecked,
            enter = fadeIn() + slideInVertically { it / 2 }
        ) {
            val feedbackBg = if (state.isCurrentCorrect) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                extraColors.heartContainer
            }
            val feedbackAccent = if (state.isCurrentCorrect) {
                MaterialTheme.colorScheme.primary
            } else {
                extraColors.heart
            }
            val feedbackOnBg = if (state.isCurrentCorrect) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                extraColors.onHeartContainer
            }
            val isLastOrOutOfHearts =
                (state.currentIndex + 1 >= state.questions.size) || (state.heartsRemaining <= 0)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = feedbackBg,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isCurrentCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = feedbackAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = if (state.isCurrentCorrect) {
                                stringResource(R.string.quiz_correct_banner)
                            } else {
                                stringResource(R.string.quiz_wrong_banner)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = feedbackAccent
                        )
                    }

                    if (!state.isCurrentCorrect) {
                        val correctText = question.options().getOrElse(question.correctIndex) { "" }
                        Text(
                            text = stringResource(R.string.quiz_correct_answer_was, correctText),
                            style = MaterialTheme.typography.titleSmall,
                            color = feedbackOnBg
                        )
                    }

                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = feedbackOnBg
                    )

                    Spacer(Modifier.height(4.dp))

                    Tactile3DButton(
                        text = if (isLastOrOutOfHearts) {
                            stringResource(R.string.quiz_finish)
                        } else {
                            stringResource(R.string.quiz_continue)
                        },
                        containerColor = feedbackAccent,
                        shadowColor = if (state.isCurrentCorrect) {
                            extraColors.unitColors["emerald"]?.second ?: feedbackAccent
                        } else {
                            extraColors.heartDark
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_continue_button"),
                        onClick = onContinue
                    )
                }
            }
        }

        if (!state.isChecked) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Tactile3DButton(
                        text = stringResource(R.string.quiz_check_answer),
                        enabled = state.selectedOptionIndex != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_check_button"),
                        onClick = onCheckAnswer
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizSummaryContent(
    state: QuizSessionUiState,
    onBackToPath: () -> Unit
) {
    val report = state.completionReport
    val outcome = report?.outcome
    val extraColors = LocalConcoursColors.current

    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            shape = RoundedCornerShape(24.dp),
            color = extraColors.goldContainer
        ) {
            Image(
                painter = painterResource(R.drawable.img_league_trophy),
                contentDescription = stringResource(R.string.quiz_summary_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val stars = outcome?.stars ?: 0
            for (i in 1..3) {
                Icon(
                    imageVector = if (i <= stars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = null,
                    tint = extraColors.gold,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (outcome?.passed == true) {
                stringResource(R.string.quiz_summary_title)
            } else {
                stringResource(R.string.quiz_summary_failed_title)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        state.level?.let {
            Text(
                text = it.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryMetricCard(
                label = stringResource(R.string.quiz_summary_xp_label),
                value = "+${outcome?.xpEarned ?: 0}",
                icon = Icons.Rounded.Bolt,
                accent = extraColors.goldDark,
                bg = extraColors.goldContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                label = stringResource(R.string.quiz_summary_accuracy_label),
                value = "${outcome?.scorePercent ?: 0}%",
                icon = Icons.Rounded.CheckCircle,
                accent = MaterialTheme.colorScheme.primary,
                bg = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                label = stringResource(R.string.quiz_summary_gems_label),
                value = "+${outcome?.gemsEarned ?: 0}",
                icon = Icons.Rounded.Diamond,
                accent = extraColors.gem,
                bg = extraColors.gemContainer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        outcome?.let {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = extraColors.streak
                    )
                    Text(
                        text = "Série : ${it.newStreak} jour(s) • Ligue ${it.newLeague.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = if (report?.syncedToCloud == true) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                extraColors.goldContainer
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (report?.syncedToCloud == true) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = if (report?.syncedToCloud == true) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        extraColors.onGoldContainer
                    },
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (report?.syncedToCloud == true) {
                        stringResource(R.string.quiz_summary_synced_cloud)
                    } else {
                        stringResource(R.string.quiz_summary_saved_offline)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (report?.syncedToCloud == true) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        extraColors.onGoldContainer
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Tactile3DButton(
            text = stringResource(R.string.quiz_summary_back_to_path),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quiz_summary_done_button"),
            onClick = onBackToPath
        )
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.85f)
            )
        }
    }
}
