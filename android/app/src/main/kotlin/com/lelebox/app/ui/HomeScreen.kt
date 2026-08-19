package com.lelebox.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lelebox.app.game.GameEntry
import com.lelebox.app.game.Games

/** 首页：柔和暖调渐变 + 大字问候 + 游戏宫格 + 铁律脚注 */
@Composable
fun HomeScreen(
    onOpenGame: (GameEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        WarmCreamDeep.copy(alpha = 0.6f),
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "欢迎回来，今天想玩点什么？",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(Games.firstBatch, key = { it.id }) { game ->
                    ElderCard(
                        emoji = game.emoji,
                        title = game.title,
                        subtitle = game.subtitle,
                        accent = game.accent,
                        onClick = { onOpenGame(game) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Text(
                "完全离线 · 无广告 · 永久免费",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 18.dp),
            )
        }
    }
}
