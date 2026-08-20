package com.lelebox.app.game.spot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.lelebox.app.ui.OutlineWarm
import kotlinx.coroutines.delay

/** 找不同入口 */
@Composable
fun SpotGameScreen(
    modifier: Modifier = Modifier,
) {
    var level by remember { mutableStateOf<SpotLevel?>(null) }
    when (val lv = level) {
        null -> SpotLevelSelect(onStart = { level = it }, modifier = modifier)
        else -> SpotBoard(lv, onBackToLevels = { level = null }, modifier = modifier)
    }
}

@Composable
private fun SpotLevelSelect(
    onStart: (SpotLevel) -> Unit,
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
            painter = painterResource(R.drawable.ic_game_spot),
            contentDescription = "找不同",
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("找不同", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "左边和右边两幅图，找一找哪里不一样，点一下就算找到。全部找齐就赢啦！",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        SpotLevel.entries.forEach { lv ->
            ElderButton(text = lv.label, onClick = { onStart(lv) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SpotBoard(
    level: SpotLevel,
    onBackToLevels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val game = remember(level) { SpotGame(level) }
    var tick by remember { mutableIntStateOf(0) }
    var hintIdx by remember { mutableIntStateOf(-1) }

    fun newRound() {
        game.restart()
        hintIdx = -1
        tick++
    }

    fun onTap(nx: Float, ny: Float) {
        if (game.over) return
        if (game.checkTap(nx, ny)) {
            Sfx.success(context)
        } else {
            Sfx.fail(context)
        }
        tick++
    }

    fun onHint() {
        hintIdx = game.hint()
        Sfx.click(context)
    }

    LaunchedEffect(hintIdx) {
        if (hintIdx >= 0) {
            delay(2200)
            hintIdx = -1
        }
    }
    LaunchedEffect(game.over) {
        if (game.over) Sfx.success(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 状态 + 上下两张图（key(tick) 强制找到差异后重绘绿圈与计数）
        key(tick) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("已找到 ${game.found.size} / ${game.diffs.size} 处", style = MaterialTheme.typography.titleMedium)
                        if (game.misses > 0) {
                            Text("点错 ${game.misses} 次", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ElderButton(text = "提示", onClick = ::onHint, minHeight = 52.dp)
                }
                Spacer(Modifier.height(10.dp))

                // 上下结构：上面原图，下面变体图
                ScenePanel(
                    variant = false,
                    game = game,
                    hintIdx = hintIdx,
                    onTap = ::onTap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                Spacer(Modifier.height(10.dp))
                ScenePanel(
                    variant = true,
                    game = game,
                    hintIdx = hintIdx,
                    onTap = ::onTap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ElderButton(text = "换难度", onClick = onBackToLevels, modifier = Modifier.weight(1f))
            ElderButton(text = "再玩一局", onClick = ::newRound, modifier = Modifier.weight(1f))
        }
    }

    if (game.over) {
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
            Text("全找到了！你真棒！", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(
                if (game.misses > 0) "点错了 ${game.misses} 次也没关系，慢慢来" else "一次都没点错！",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFE0B2),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            ElderButton(text = "再玩一局", onClick = ::newRound, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            ElderButton(text = "换难度", onClick = onBackToLevels, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ScenePanel(
    variant: Boolean,
    game: SpotGame,
    hintIdx: Int,
    onTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, OutlineWarm, shape)
            .semantics {
                contentDescription = if (variant) "下面的图片" else "上面的图片，${game.sceneName}场景，找不同"
            }
            .pointerInput(game.found.size, hintIdx) {
                detectTapGestures { offset ->
                    val nx = offset.x / size.width * 1000f
                    val ny = offset.y / size.height * 1000f
                    onTap(nx, ny)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (game.level == SpotLevel.EASY) {
                drawGarden(this, variant)
            } else {
                drawPark(this, variant)
            }
            // 已找到：绿圈（两图同步）
            game.found.forEach { i ->
                val d = game.diffs[i].rect
                val s = SceneCtx(this)
                drawCircle(
                    Color(0xFF2E7D32),
                    s.r(58f),
                    Offset(s.x(d.center.x), s.y(d.center.y)),
                    style = Stroke(width = s.r(8f)),
                )
            }
            // 提示：琥珀圈
            if (hintIdx >= 0 && hintIdx < game.diffs.size) {
                val d = game.diffs[hintIdx].rect
                val s = SceneCtx(this)
                drawCircle(
                    Color(0xFFF0A93C),
                    s.r(72f),
                    Offset(s.x(d.center.x), s.y(d.center.y)),
                    style = Stroke(width = s.r(10f)),
                )
            }
        }
    }
}
