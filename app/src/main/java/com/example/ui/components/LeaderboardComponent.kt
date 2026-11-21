package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.remote.LeagueTier
import com.example.ui.theme.LocalConcoursColors
import com.example.ui.viewmodel.LeaderboardEntryUi
import com.example.ui.viewmodel.LeaderboardStreamState
import kotlin.math.abs

object LeaderboardHelper {
    fun computeInitials(displayName: String): String {
        val tokens = displayName.trim()
            .split(Regex("\\s+"))
            .map { token -> token.filter { it.isLetterOrDigit() } }
            .filter { it.isNotEmpty() }
        return when {
            tokens.isEmpty() -> ""
            tokens.size == 1 -> tokens.first().take(2).uppercase()
            else -> "${tokens.first().first()}${tokens.last().first()}".uppercase()
        }
    }

    fun placeholderColorIndex(seed: String, paletteSize: Int): Int {
        if (paletteSize <= 0) return 0
        val hash = seed.ifBlank { "candidate" }.hashCode()
        return abs(hash) % paletteSize
    }

    fun filterAndSortEntries(
        entries: List<LeaderboardEntryUi>,
        selectedLeague: String?,
        sortByWeekly: Boolean
    ): List<LeaderboardEntryUi> {
        return entries
            .filter { selectedLeague == null || it.league == selectedLeague }
            .sortedWith(
                compareByDescending<LeaderboardEntryUi> {
                    if (sortByWeekly) it.weeklyXp else it.totalXp
                }
                    .thenByDescending { it.totalXp }
                    .thenBy { it.displayName.lowercase() }
            )
    }

    fun findCurrentUserRank(sortedEntries: List<LeaderboardEntryUi>): Int? {
        val index = sortedEntries.indexOfFirst { it.isCurrentUser }
        return if (index >= 0) index + 1 else null
    }
}

@Composable
fun LeaderboardAvatarPlaceholder(
    displayName: String,
    avatarId: String,
    modifier: Modifier = Modifier,
    seed: String = displayName,
    rank: Int? = null,
    size: Dp = 44.dp,
    isHighlighted: Boolean = false
) {
    val extraColors = LocalConcoursColors.current
    val catalogOption = remember(avatarId) {
        val normalized = avatarId.trim()
        if (normalized.isEmpty() ||
            normalized.equals("placeholder", ignoreCase = true) ||
            normalized.equals("default", ignoreCase = true)
        ) {
            null
        } else {
            AvatarCatalog.options.firstOrNull { it.id == normalized }
        }
    }

    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        extraColors.gem,
        extraColors.goldDark,
        extraColors.streak,
        extraColors.unitColors["violet"]?.first ?: MaterialTheme.colorScheme.tertiary,
        extraColors.unitColors["teal"]?.first ?: MaterialTheme.colorScheme.secondary
    )
    val placeholderAccent = palette[LeaderboardHelper.placeholderColorIndex(seed, palette.size)]
    val initials = remember(displayName) { LeaderboardHelper.computeInitials(displayName) }

    val borderColor = when {
        rank == 1 -> extraColors.gold
        rank == 2 -> extraColors.leagueColors["Argent"] ?: MaterialTheme.colorScheme.secondary
        rank == 3 -> extraColors.leagueColors["Bronze"] ?: MaterialTheme.colorScheme.tertiary
        isHighlighted -> MaterialTheme.colorScheme.primary
        catalogOption != null -> extraColors.unitColors[catalogOption.unitColorKey]?.first ?: placeholderAccent
        else -> placeholderAccent
    }

    val rankBadgeColor = when (rank) {
        1 -> extraColors.gold
        2 -> extraColors.leagueColors["Argent"] ?: MaterialTheme.colorScheme.secondary
        3 -> extraColors.leagueColors["Bronze"] ?: MaterialTheme.colorScheme.tertiary
        else -> null
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (catalogOption != null) {
            AvatarBadge(
                avatarId = catalogOption.id,
                size = size
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(placeholderAccent.copy(alpha = 0.16f))
                    .border(2.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = (size.value * 0.36f).sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = placeholderAccent,
                        maxLines = 1
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = stringResource(R.string.leaderboard_avatar_cd, displayName),
                        tint = placeholderAccent,
                        modifier = Modifier.size(size * 0.56f)
                    )
                }
            }
        }

        if (rank != null && rankBadgeColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(rankBadgeColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun LeaderboardComponent(
    leaderboardState: LeaderboardStreamState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRequestSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null
) {
    var sortByWeekly by remember { mutableStateOf(false) }
    var selectedLeague by remember { mutableStateOf<String?>(null) }
    val extraColors = LocalConcoursColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("leaderboard_list"),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (headerContent != null) {
                item(key = "lb_header") {
                    headerContent()
                }
            }

            when (leaderboardState) {
                is LeaderboardStreamState.GuestOffline -> {
                    item(key = "lb_guest_card") {
                        LeaderboardGuestCard(onRequestSignIn = onRequestSignIn)
                    }
                }

                is LeaderboardStreamState.Loading -> {
                    item(key = "lb_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp)
                                .testTag("leaderboard_loading"),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is LeaderboardStreamState.Error -> {
                    item(key = "lb_error") {
                        LeaderboardErrorCard(
                            message = leaderboardState.message,
                            onRetry = onRefresh
                        )
                    }
                }

                is LeaderboardStreamState.Ready -> {
                    if (leaderboardState.isFromOfflineCache) {
                        item(key = "lb_offline_notice") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = extraColors.goldContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        tint = extraColors.onGoldContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.leaderboard_offline_banner),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extraColors.onGoldContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onRefresh) {
                                        Text(stringResource(R.string.leaderboard_retry))
                                    }
                                }
                            }
                        }
                    }

                    item(key = "lb_controls") {
                        LeaderboardFilterControls(
                            sortByWeekly = sortByWeekly,
                            selectedLeague = selectedLeague,
                            isRefreshing = isRefreshing,
                            onSortChange = { sortByWeekly = it },
                            onLeagueSelect = { tier ->
                                selectedLeague = if (selectedLeague == tier) null else tier
                            },
                            onClearLeague = { selectedLeague = null },
                            onRefresh = onRefresh
                        )
                    }

                    val filtered = LeaderboardHelper.filterAndSortEntries(
                        entries = leaderboardState.entries,
                        selectedLeague = selectedLeague,
                        sortByWeekly = sortByWeekly
                    )
                    val currentUserRank = LeaderboardHelper.findCurrentUserRank(filtered)

                    if (currentUserRank != null) {
                        item(key = "lb_user_rank_banner") {
                            CurrentUserRankBanner(
                                rank = currentUserRank,
                                totalCount = filtered.size,
                                entry = filtered[currentUserRank - 1],
                                showWeekly = sortByWeekly
                            )
                        }
                    }

                    if (filtered.isEmpty()) {
                        item(key = "lb_empty") {
                            LeaderboardEmptyCard()
                        }
                    } else {
                        if (filtered.size >= 3) {
                            item(key = "lb_podium") {
                                LeaderboardPodiumRow(
                                    topThree = filtered.take(3),
                                    showWeekly = sortByWeekly
                                )
                            }
                        }

                        itemsIndexed(
                            items = filtered,
                            key = { _, item -> item.userId }
                        ) { idx, entry ->
                            LeaderboardRowCard(
                                rank = idx + 1,
                                entry = entry,
                                showWeekly = sortByWeekly
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardFilterControls(
    sortByWeekly: Boolean,
    selectedLeague: String?,
    isRefreshing: Boolean,
    onSortChange: (Boolean) -> Unit,
    onLeagueSelect: (String) -> Unit,
    onClearLeague: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.weight(1f)
            ) {
                SegmentedButton(
                    selected = sortByWeekly,
                    onClick = { onSortChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("lb_weekly_tab")
                ) {
                    Text(stringResource(R.string.leaderboard_weekly))
                }
                SegmentedButton(
                    selected = !sortByWeekly,
                    onClick = { onSortChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("lb_total_tab")
                ) {
                    Text(stringResource(R.string.leaderboard_all_time))
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("leaderboard_refresh_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.leaderboard_refresh_cd),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedLeague == null,
                onClick = onClearLeague,
                label = { Text(stringResource(R.string.leaderboard_filter_all)) },
                modifier = Modifier.testTag("league_chip_all")
            )
            LeagueTier.entries.reversed().forEach { tier ->
                FilterChip(
                    selected = selectedLeague == tier.label,
                    onClick = { onLeagueSelect(tier.label) },
                    label = { Text(tier.label) },
                    modifier = Modifier.testTag("league_chip_${tier.label}")
                )
            }
        }
    }
}

@Composable
private fun CurrentUserRankBanner(
    rank: Int,
    totalCount: Int,
    entry: LeaderboardEntryUi,
    showWeekly: Boolean
) {
    val xp = if (showWeekly) entry.weeklyXp else entry.totalXp
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_current_user_banner"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LeaderboardAvatarPlaceholder(
                displayName = entry.displayName,
                avatarId = entry.avatarId,
                seed = entry.userId,
                rank = rank,
                size = 38.dp,
                isHighlighted = true
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.leaderboard_your_rank, rank, totalCount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Ligue ${entry.league} • $xp XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun LeaderboardGuestCard(onRequestSignIn: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = stringResource(R.string.leaderboard_guest_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.leaderboard_guest_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Tactile3DButton(
                text = stringResource(R.string.profile_connect_google),
                leadingIcon = Icons.Rounded.Login,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leaderboard_connect_button"),
                onClick = onRequestSignIn
            )
        }
    }
}

@Composable
private fun LeaderboardErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_error_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.leaderboard_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = message.ifBlank { stringResource(R.string.leaderboard_error_load) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Tactile3DButton(
                text = stringResource(R.string.leaderboard_retry),
                leadingIcon = Icons.Rounded.Refresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leaderboard_retry_button"),
                onClick = onRetry
            )
        }
    }
}

@Composable
private fun LeaderboardEmptyCard() {
    val extraColors = LocalConcoursColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_empty_card"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = extraColors.gold,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = stringResource(R.string.leaderboard_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LeaderboardPodiumRow(
    topThree: List<LeaderboardEntryUi>,
    showWeekly: Boolean
) {
    val first = topThree.getOrNull(0)
    val second = topThree.getOrNull(1)
    val third = topThree.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (second != null) {
            PodiumSpot(rank = 2, entry = second, showWeekly = showWeekly, modifier = Modifier.weight(1f))
        }
        if (first != null) {
            PodiumSpot(rank = 1, entry = first, showWeekly = showWeekly, modifier = Modifier.weight(1.1f))
        }
        if (third != null) {
            PodiumSpot(rank = 3, entry = third, showWeekly = showWeekly, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumSpot(
    rank: Int,
    entry: LeaderboardEntryUi,
    showWeekly: Boolean,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalConcoursColors.current
    val badgeColor = when (rank) {
        1 -> extraColors.gold
        2 -> extraColors.leagueColors["Argent"] ?: MaterialTheme.colorScheme.secondary
        else -> extraColors.leagueColors["Bronze"] ?: MaterialTheme.colorScheme.tertiary
    }
    val xp = if (showWeekly) entry.weeklyXp else entry.totalXp

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (rank == 1) 2.dp else 1.dp, badgeColor.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = if (rank == 1) 16.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
            LeaderboardAvatarPlaceholder(
                displayName = entry.displayName,
                avatarId = entry.avatarId,
                seed = entry.userId,
                size = if (rank == 1) 48.dp else 40.dp,
                isHighlighted = entry.isCurrentUser
            )
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$xp XP",
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor
            )
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    rank: Int,
    entry: LeaderboardEntryUi,
    showWeekly: Boolean
) {
    val extraColors = LocalConcoursColors.current
    val leagueColor = extraColors.leagueColors[entry.league] ?: extraColors.gold
    val xpValue = if (showWeekly) entry.weeklyXp else entry.totalXp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.userId}"),
        shape = RoundedCornerShape(16.dp),
        color = if (entry.isCurrentUser) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            if (entry.isCurrentUser) 2.dp else 1.dp,
            if (entry.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) extraColors.goldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(26.dp),
                textAlign = TextAlign.Center
            )

            LeaderboardAvatarPlaceholder(
                displayName = entry.displayName,
                avatarId = entry.avatarId,
                seed = entry.userId,
                rank = rank.takeIf { it <= 3 },
                size = 44.dp,
                isHighlighted = entry.isCurrentUser
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.isCurrentUser) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.leaderboard_you_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ligue ${entry.league}",
                        style = MaterialTheme.typography.labelSmall,
                        color = leagueColor
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = extraColors.streak,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${entry.currentStreak}j",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = stringResource(R.string.leaderboard_levels_count, entry.completedLevelsCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = extraColors.goldDark,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$xpValue XP",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
