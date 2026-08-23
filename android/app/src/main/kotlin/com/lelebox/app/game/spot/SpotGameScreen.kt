package com.lelebox.app.game.spot

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.layout.ContentScale
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

/** 找不同入口：难度（简单/中等/困难）→ 选关（每难度 3 关）→ 对局 */
@Composable
fun SpotGameScreen(
    modifier: Modifier = Modifier,
) {
    var difficulty by remember { mutableStateOf<SpotLevel?>(null) }
    var stage by remember { mutableIntStateOf(-1) } // -1 = 选关

    // 物理返回键：对局页 → 选关页 → 难度页 →（难度页交给壳退出）
    if (difficulty != null) {
        BackHandler {
            if (stage >= 0) stage = -1 else difficulty = null
        }
    }

    val lv = difficulty
    when {
        lv == null -> SpotDifficultySelect(
            onPick = { difficulty = it; stage = -1 },
            modifier = modifier,
        )
        stage == -1 -> SpotStageSelect(
            level = lv,
            onPick = { stage = it },
            onBack = { difficulty = null },
            modifier = modifier,
        )
        else -> SpotBoard(
            level = lv,
            scene = lv.scenes[stage],
            onNext = {
                if (stage + 1 < lv.scenes.size) stage++ else { stage = -1; difficulty = null }
            },
            onPickStage = { stage = it },
            onBackToLevels = { difficulty = null; stage = -1 },
            modifier = modifier,
        )
    }
}

@Composable
private fun SpotDifficultySelect(
    onPick: (SpotLevel) -> Unit,
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
            "上面和下面两幅图，找一找哪里不一样，点一下就算找到。共 9 关。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        SpotLevel.entries.forEach { lv ->
            ElderButton(
                text = "${lv.label}（每关 ${lv.diffCount} 处）",
                onClick = { onPick(lv) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SpotStageSelect(
    level: SpotLevel,
    onPick: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${level.label} · 选一关", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        level.scenes.forEachIndexed { i, scene ->
            ElderButton(
                text = "第${i + 1}关 · ${scene.name}",
                onClick = { onPick(i) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
        }
        Spacer(Modifier.height(10.dp))
        ElderButton(
            text = "返回选难度",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

@Composable
private fun SpotBoard(
    level: SpotLevel,
    scene: SpotSceneDef,
    onNext: () -> Unit,
    onPickStage: (Int) -> Unit,
    onBackToLevels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // scene 作为 key：切关（下一关/选其他关）时整个状态重置，不会残留上一关的过关遮罩
    val game = remember(scene) { SpotGame(scene) }
    var tick by remember(scene) { mutableIntStateOf(0) }
    var hintIdx by remember(scene) { mutableIntStateOf(-1) }
    // UI 状态：通关后置 true（game.over 是普通字段，不触发重组）
    var overState by remember(scene) { mutableStateOf(false) }

    fun newRound() {
        game.restart()
        hintIdx = -1
        overState = false
        tick++
    }

    fun onTap(nx: Float, ny: Float) {
        if (game.over) return
        if (game.checkTap(nx, ny)) Sfx.success(context) else Sfx.fail(context)
        if (game.over) overState = true
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

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        key(tick) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("${scene.name} · 已找到 ${game.found.size} / ${game.diffs.size}", style = MaterialTheme.typography.titleMedium)
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
                    scene = scene,
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
                    scene = scene,
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
            ElderButton(text = "重玩本关", onClick = ::newRound, modifier = Modifier.weight(1f))
        }
    }

    // 遮罩包进 key(tick)：tick 变化强制重估（found.size/over 是普通字段）
    key("overlay") { if (overState || game.over || game.found.size >= game.diffs.size) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎉", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("全找到了！你真棒！", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            // 星级互动：点错越少星越多
            val stars = if (game.misses == 0) 3 else if (game.misses <= 2) 2 else 1
            Text(
                "⭐".repeat(stars) + "☆".repeat(3 - stars),
                fontSize = 40.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when (stars) {
                    3 -> "完美通关！一次都没点错！"
                    2 -> "真棒！只点错了 ${game.misses} 次"
                    else -> "找到啦！点错 ${game.misses} 次也没关系，慢慢来"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFE0B2),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            // 下一关（还有的话）
            if (scene != level.scenes.last()) {
                ElderButton(text = "下一关 ▶", onClick = onNext, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            // 选其他关（回到本难度选关页，可自由进任意关）
            ElderButton(
                text = "选其他关",
                onClick = { onPickStage(-1) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                ),
            )
            Spacer(Modifier.height(12.dp))
            ElderButton(text = "重玩本关", onClick = ::newRound, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            ElderButton(
                text = "换难度",
                onClick = onBackToLevels,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
        }
        }
        }
    }
}

@Composable
private fun ScenePanel(
    variant: Boolean,
    scene: SpotSceneDef,
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
                contentDescription = if (variant) "下面的图片" else "上面的图片，${scene.name}场景，找不同"
            }
            .pointerInput(game.found.size, hintIdx) {
                detectTapGestures { offset ->
                    // 与 SceneCtx 一致的逆映射：minDimension 等比缩放 + 居中
                    val k = minOf(size.width, size.height) / 1000f
                    val ox = (size.width - 1000f * k) / 2f
                    val oy = (size.height - 1000f * k) / 2f
                    val nx = (offset.x - ox) / k
                    val ny = (offset.y - oy) / k
                    onTap(nx, ny)
                }
            },
    ) {
        // 即梦底图（可选）：铺满后差异层叠加其上
        if (scene.bgRes != null) {
            Image(
                painter = painterResource(scene.bgRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            scene.draw(this, variant)
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
