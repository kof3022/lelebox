package com.lelebox.app.game.memory

import android.content.SharedPreferences
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.lelebox.app.ui.SuccessSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 记忆翻牌入口：先选难度，再进入棋盘（L1 原生） */
@Composable
fun MemoryGameScreen(
    prefs: SharedPreferences,
    modifier: Modifier = Modifier,
) {
    var level by remember { mutableStateOf<MemoryLevel?>(null) }
    when (val lv = level) {
        null -> MemoryLevelSelect(onStart = { level = it }, modifier = modifier)
        else -> MemoryBoard(lv, prefs, onBackToLevels = { level = null }, modifier = modifier)
    }
}

@Composable
private fun MemoryLevelSelect(
    onStart: (MemoryLevel) -> Unit,
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
            painter = painterResource(R.drawable.ic_game_memory),
            contentDescription = "记忆翻牌",
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("记忆翻牌", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "翻开两张相同的牌，全部配对成功就赢啦！",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        MemoryLevel.entries.forEach { lv ->
            ElderButton(text = lv.label, onClick = { onStart(lv) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MemoryBoard(
    level: MemoryLevel,
    prefs: SharedPreferences,
    onBackToLevels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var round by remember { mutableStateOf(0) }
    val board = remember(level, round) { buildMemoryBoard(level) }
    var flipped by remember { mutableStateOf<List<Int>>(emptyList()) }
    var matched by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var moves by remember { mutableStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var best by remember { mutableStateOf(prefs.getInt("native_memory_best_${level.name}", Int.MAX_VALUE)) }
    var isNewRecord by remember { mutableStateOf(false) }
    var oldBest by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val won = matched.size == board.size

    fun reset() {
        round++
        flipped = emptyList()
        matched = emptySet()
        moves = 0
        locked = false
        isNewRecord = false
    }

    fun onCardTap(index: Int) {
        if (locked || won || index in flipped || index in matched) return
        val newFlipped = flipped + index
        moves++
        Sfx.click(context)
        if (newFlipped.size == 2) {
            val a = newFlipped[0]
            val b = newFlipped[1]
            if (board[a] == board[b]) {
                matched = matched + a + b
                flipped = emptyList()
                Sfx.success(context)
            } else {
                locked = true
                flipped = newFlipped
                scope.launch {
                    delay(900)
                    flipped = emptyList()
                    locked = false
                }
            }
        } else {
            flipped = newFlipped
        }
    }

    // 获胜：更新最优步数（无计时、无惩罚，只记最优）
    LaunchedEffect(won) {
        if (won) {
            Sfx.success(context)
            if (moves < best) {
                oldBest = best
                prefs.edit().putInt("native_memory_best_${level.name}", moves).apply()
                best = moves
                isNewRecord = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("步数：$moves", style = MaterialTheme.typography.titleLarge)
            Text(
                if (best == Int.MAX_VALUE) "最佳：—" else "最佳：$best",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(12.dp))

        val rows = board.size / level.columns
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(rows) { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(level.columns) { c ->
                        val index = r * level.columns + c
                        MemoryCard(
                            animal = board[index],
                            faceUp = index in matched || index in flipped,
                            matched = index in matched,
                            index = index,
                            onClick = { onCardTap(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ElderButton(text = "换难度", onClick = onBackToLevels, modifier = Modifier.weight(1f))
            ElderButton(text = "重新开始", onClick = ::reset, modifier = Modifier.weight(1f))
        }
    }

    if (won) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎉", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("你真棒！全部配对成功！", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(
                if (isNewRecord) "新纪录！只用 $moves 步（原来最佳 ${if (oldBest == Int.MAX_VALUE) "—" else "$oldBest 步"}）"
                else "用了 $moves 步完成，最佳纪录 $best 步",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFE0B2),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            ElderButton(text = "再来一局", onClick = ::reset, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            ElderButton(text = "换难度", onClick = onBackToLevels, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MemoryCard(
    animal: MemoryAnimal,
    faceUp: Boolean,
    matched: Boolean,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        matched -> SuccessSoft // 已配对：暖调浅绿
        faceUp -> Color.White
        else -> MaterialTheme.colorScheme.primary
    }
    val desc = when {
        faceUp -> "第${index + 1}张牌，动物${animal.name}"
        else -> "第${index + 1}张牌，背面"
    }
    Box(
        modifier = modifier
            .semantics { contentDescription = desc }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(2.dp, Color(0xFFD8CFC2), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (faceUp) {
            if (animal.iconRes != null) {
                Image(
                    painter = painterResource(animal.iconRes),
                    contentDescription = animal.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            } else {
                Text(animal.emoji, fontSize = 40.sp)
            }
        } else {
            Text("？", fontSize = 32.sp, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
