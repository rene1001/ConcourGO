package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalConcoursColors

data class AvatarOption(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val unitColorKey: String
)

object AvatarCatalog {
    val options: List<AvatarOption> = listOf(
        AvatarOption("hibou_emerald", "Hibou", Icons.Rounded.School, "emerald"),
        AvatarOption("lion_gold", "Lion", Icons.Rounded.EmojiEvents, "amber"),
        AvatarOption("aigle_cobalt", "Aigle", Icons.Rounded.RocketLaunch, "cobalt"),
        AvatarOption("gazelle_teal", "Gazelle", Icons.Rounded.MenuBook, "teal"),
        AvatarOption("panda_violet", "Génie", Icons.Rounded.Psychology, "violet"),
        AvatarOption("faucon_flame", "Éclair", Icons.Rounded.Bolt, "amber"),
        AvatarOption("etoile_gold", "Étoile", Icons.Rounded.AutoAwesome, "cobalt"),
        AvatarOption("couronne_emerald", "Major", Icons.Rounded.WorkspacePremium, "emerald")
    )

    fun resolve(id: String): AvatarOption {
        return options.firstOrNull { it.id == id } ?: options.first()
    }
}

@Composable
fun AvatarBadge(
    avatarId: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val option = AvatarCatalog.resolve(avatarId)
    val extraColors = LocalConcoursColors.current
    val pair = extraColors.unitColors[option.unitColorKey]
        ?: (MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(pair.first.copy(alpha = 0.16f))
            .border(2.dp, pair.first, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.label,
            tint = pair.first,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

@Composable
fun Tactile3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    shadowColor: Color = LocalConcoursColors.current.unitColors["emerald"]?.second ?: MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    leadingIcon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val effectiveContainer = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant
    val effectiveShadow = if (enabled) shadowColor else MaterialTheme.colorScheme.outline
    val effectiveContent = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
    val pressOffset = if (isPressed && enabled) 3.dp else 0.dp

    Box(
        modifier = modifier
            .height(54.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(effectiveShadow)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .offset(y = pressOffset)
                .clip(RoundedCornerShape(16.dp))
                .background(effectiveContainer),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = effectiveContent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = effectiveContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TopGamifiedHud(
    streak: Int,
    totalXp: Int,
    gems: Int,
    hearts: Int,
    offlineMode: Boolean,
    pendingSyncCount: Int,
    onToggleOffline: () -> Unit,
    onHeartsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalConcoursColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    onClick = onToggleOffline,
                    modifier = Modifier.testTag("hud_offline_toggle_chip"),
                    shape = RoundedCornerShape(50),
                    color = if (offlineMode) extraColors.goldContainer else MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(
                        1.dp,
                        if (offlineMode) extraColors.gold else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (offlineMode) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = if (offlineMode) extraColors.onGoldContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (offlineMode) {
                                stringResource(R.string.status_offline_mode)
                            } else if (pendingSyncCount > 0) {
                                stringResource(R.string.status_sync_pending, pendingSyncCount)
                            } else {
                                stringResource(R.string.status_online)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (offlineMode) extraColors.onGoldContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudStatPill(
                    icon = Icons.Rounded.LocalFireDepartment,
                    value = streak.toString(),
                    tint = extraColors.streak,
                    bgColor = extraColors.streakContainer,
                    contentDesc = stringResource(R.string.hud_streak_cd)
                )
                HudStatPill(
                    icon = Icons.Rounded.Bolt,
                    value = totalXp.toString(),
                    tint = extraColors.goldDark,
                    bgColor = extraColors.goldContainer,
                    contentDesc = stringResource(R.string.hud_xp_cd)
                )
                HudStatPill(
                    icon = Icons.Rounded.Diamond,
                    value = gems.toString(),
                    tint = extraColors.gem,
                    bgColor = extraColors.gemContainer,
                    contentDesc = stringResource(R.string.hud_gems_cd)
                )
                HudStatPill(
                    icon = Icons.Rounded.Favorite,
                    value = "$hearts/5",
                    tint = extraColors.heart,
                    bgColor = extraColors.heartContainer,
                    contentDesc = stringResource(R.string.hud_hearts_cd),
                    onClick = onHeartsClicked,
                    modifier = Modifier.testTag("hud_hearts_pill")
                )
            }
        }
    }
}

@Composable
private fun HudStatPill(
    icon: ImageVector,
    value: String,
    tint: Color,
    bgColor: Color,
    contentDesc: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = tint
            )
        }
    }
}

@Composable
fun DailyGoalCard(
    todayXp: Int,
    dailyGoalXp: Int,
    league: String,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalConcoursColors.current
    val safeGoal = dailyGoalXp.coerceAtLeast(10)
    val rawProgress = (todayXp.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, label = "dailyGoalProgress")
    val reached = todayXp >= safeGoal
    val leagueColor = extraColors.leagueColors[league] ?: extraColors.gold

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (reached) stringResource(R.string.daily_goal_completed)
                        else stringResource(R.string.daily_goal_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.daily_goal_progress, todayXp, safeGoal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = leagueColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, leagueColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MilitaryTech,
                            contentDescription = league,
                            tint = leagueColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Ligue $league",
                            style = MaterialTheme.typography.labelMedium,
                            color = leagueColor
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (reached) MaterialTheme.colorScheme.primary else extraColors.gold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
