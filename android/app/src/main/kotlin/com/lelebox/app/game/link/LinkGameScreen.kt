package com.lelebox.app.game.link

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelebox.app.R
import com.lelebox.app.audio.Sfx
import com.lelebox.app.ui.ElderButton
import com.lelebox.app.ui.ElderGreen

/** 牌面图案（水果，颜色鲜明易辨；后续可换即梦图标） */
private val TILE_EMOJIS = listOf(
    "🍎", "🍊", "🍇", "🍓", "🍑", "🍒", "🍋", "🍉",
)

/** 连连看入口 */
@Composable
fun LinkGameScreen(
    modifier: Modifier = Modifier,
) {
    var level by remember { mutableStateOf<LinkLevel?>(null) }
    // 物理返回键：对局页 → 难度页 →（难度页交给壳退出）
    if (level != null) BackHandler { level = null }
    when (val lv = level) {
        null -> LinkLevelSelect(onStart = { level = it }, modifier = modifier)
        else -> LinkBoard(lv, onBackToLevels = { level = null }, modifier = modifier)
    }
}

@Composable
private fun LinkLevelSelect(
    onStart: (LinkLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_game_link),
            contentDescription = "连连看",
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("连连看", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "点两张一样的图案，中间的路没有挡住的牌就能消掉（最多拐两个弯）。全部消完就赢啦！",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        LinkLevel.entries.forEach { lv ->
            ElderButton(text = lv.label, onClick = { onStart(lv) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LinkBoard(
    level: LinkLevel,
    onBackToLevels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val game = remember(level) { LinkGame(level) }
    var round by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var hintCount by remember { mutableIntStateOf(0) }

    fun resetBoard() {
        game.newBoard()
        selected = null
        round++
    }

    fun onTileTap(idx: Int) {
        if (game.over) return
        if (game.isRemoved(idx)) return
        val sel = selected
        if (sel == null) {
            selected = idx
            Sfx.click(context)
        } else if (sel == idx) {
            selected = null
        } else {
            if (game.removePair(sel, idx)) {
                selected = null
                Sfx.success(context)
                round++ // 触发重绘（消除可见）
            } else {
                Sfx.fail(context)
                selected = null
            }
        }
    }

    fun onHint() {
        val pair = game.findHint()
        if (pair != null) {
            selected = pair.first
            hintCount++
            Sfx.click(context)
        } else {
            Sfx.fail(context)
        }
    }

    fun onShuffle() {
        game.shuffleRemaining()
        selected = null
        Sfx.click(context)
        round++
    }

    // 胜利音效
    LaunchedEffect(game.over) {
        if (game.over) Sfx.success(context)
    }

    // 外层 Box：让结束遮罩能叠在棋盘上方（兄弟节点在壳 Column 里会被堆到屏幕外）
    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("剩余 ${game.remainingPairs} 对", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ElderButton(text = "提示", onClick = ::onHint, minHeight = 52.dp)
                ElderButton(text = "洗牌", onClick = ::onShuffle, minHeight = 52.dp)
            }
        }
        if (hintCount > 0) {
            Text("已提示 $hintCount 次", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))

        // 棋盘（key(round) 强制在消除/洗牌/重开后重绘）
        key(round) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (y in 0 until game.rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (x in 0 until game.cols) {
                            val idx = y * game.cols + x
                            LinkTile(
                                emoji = if (game.symbols[idx] > 0) TILE_EMOJIS[(game.symbols[idx] - 1) % TILE_EMOJIS.size] else "",
                                removed = game.symbols[idx] == 0,
                                selected = selected == idx,
                                x = x, y = y,
                                onClick = { onTileTap(idx) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ElderButton(text = "换难度", onClick = onBackToLevels, modifier = Modifier.weight(1f))
            ElderButton(text = "重新开始", onClick = ::resetBoard, modifier = Modifier.weight(1f))
        }
    }

    if (game.over) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎉", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("全部消完！你真棒！", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            // 星级互动：没用提示星最多
            val stars = if (hintCount == 0) 3 else if (hintCount <= 2) 2 else 1
            Text(
                "⭐".repeat(stars) + "☆".repeat(3 - stars),
                fontSize = 40.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when (stars) {
                    3 -> "没有用提示，全凭自己！"
                    2 -> "真棒！只用了 $hintCount 次提示"
                    else -> "消完啦！用了 $hintCount 次提示，慢慢来没关系"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFE0B2),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            ElderButton(text = "再来一局", onClick = ::resetBoard, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            ElderButton(text = "换难度", onClick = onBackToLevels, modifier = Modifier.fillMaxWidth())
        }
        }
    }
    }
}

/** 提示目标（当前高亮的第一张） */

@Composable
private fun LinkTile(
    emoji: String,
    removed: Boolean,
    selected: Boolean,
    x: Int,
    y: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        selected -> Color(0xFFFFF3D6) // 选中：暖黄
        else -> Color(0xFFF7F1E6)
    }
    Box(
        modifier = modifier
            .semantics { contentDescription = if (removed) "第${y + 1}行第${x + 1}列，已消" else "第${y + 1}行第${x + 1}列，图案$emoji" }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) ElderGreen else Color(0xFFE0D5C2),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(enabled = !removed, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!removed) {
            Text(emoji, fontSize = 26.sp)
        }
    }
}
