package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.LeaderboardComponent
import com.example.ui.theme.LocalConcoursColors
import com.example.ui.viewmodel.LeaderboardStreamState

@Composable
fun LeaderboardScreen(
    leaderboardState: LeaderboardStreamState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRequestSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalConcoursColors.current

    LeaderboardComponent(
        leaderboardState = leaderboardState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onRequestSignIn = onRequestSignIn,
        modifier = modifier,
        headerContent = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = extraColors.goldContainer,
                border = BorderStroke(1.5.dp, extraColors.gold.copy(alpha = 0.5f))
            ) {
                Column {
                    Image(
                        painter = painterResource(R.drawable.img_league_trophy),
                        contentDescription = stringResource(R.string.leaderboard_title),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.leaderboard_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = extraColors.onGoldContainer
                        )
                        Text(
                            text = stringResource(R.string.leaderboard_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extraColors.onGoldContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    )
}
