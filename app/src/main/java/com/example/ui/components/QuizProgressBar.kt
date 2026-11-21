package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalConcoursColors
import kotlin.math.roundToInt

/**
 * Animated Duolingo-inspired progress bar displayed at the top of a QCM session.
 * Renders a rounded capsule track with per-question step markers, a spring-animated
 * glossy fill bar (shifting to gold on active answer combos), and accessibility semantics.
 */
@Composable
fun QuizAnimatedProgressBar(
    completedSteps: Int,
    totalSteps: Int,
    comboCount: Int,
    modifier: Modifier = Modifier
) {
    val safeTotal = totalSteps.coerceAtLeast(1)
    val clampedSteps = completedSteps.coerceIn(0, safeTotal)
    val targetFraction = (clampedSteps.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "quizTopProgressFraction"
    )

    val extraColors = LocalConcoursColors.current
    val targetBarColor = when {
        comboCount >= 3 -> extraColors.gold
        else -> MaterialTheme.colorScheme.primary
    }
    val animatedBarColor by animateColorAsState(
        targetValue = targetBarColor,
        animationSpec = tween(durationMillis = 280),
        label = "quizTopProgressColor"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val progressDesc = stringResource(R.string.quiz_progress_cd, clampedSteps, safeTotal)
    val percentValue = (animatedFraction * 100f).roundToInt().coerceIn(0, 100)

    Column(
        modifier = modifier
            .testTag("quiz_progress_bar")
            .semantics {
                contentDescription = progressDesc
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = targetFraction,
                    range = 0f..1f,
                    steps = (safeTotal - 1).coerceAtLeast(0)
                )
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val corner = CornerRadius(size.height / 2f, size.height / 2f)

            // Capsule track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = corner
            )

            // Animated progress fill with Duolingo-style glossy top highlight
            val clampedProgress = animatedFraction.coerceIn(0f, 1f)
            if (clampedProgress > 0.005f) {
                val fillWidth = (size.width * clampedProgress).coerceAtLeast(size.height)
                    .coerceAtMost(size.width)

                drawRoundRect(
                    color = animatedBarColor,
                    topLeft = Offset.Zero,
                    size = Size(width = fillWidth, height = size.height),
                    cornerRadius = corner
                )

                val highlightHorizontalPadding = size.height * 0.32f
                val highlightWidth = (fillWidth - highlightHorizontalPadding * 2f).coerceAtLeast(0f)
                if (highlightWidth > 2f) {
                    val highlightHeight = (size.height * 0.24f).coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.34f),
                        topLeft = Offset(
                            x = highlightHorizontalPadding,
                            y = size.height * 0.20f
                        ),
                        size = Size(width = highlightWidth, height = highlightHeight),
                        cornerRadius = CornerRadius(highlightHeight / 2f, highlightHeight / 2f)
                    )
                }
            }

            // Subtle question step separators
            if (safeTotal in 2..12) {
                for (step in 1 until safeTotal) {
                    val x = size.width * (step.toFloat() / safeTotal.toFloat())
                    val isCovered = (step.toFloat() / safeTotal.toFloat()) <= clampedProgress
                    drawLine(
                        color = if (isCovered) Color.White.copy(alpha = 0.35f) else tickColor,
                        start = Offset(x, size.height * 0.24f),
                        end = Offset(x, size.height * 0.76f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$clampedSteps / $safeTotal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$percentValue%",
                style = MaterialTheme.typography.labelSmall,
                color = animatedBarColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
