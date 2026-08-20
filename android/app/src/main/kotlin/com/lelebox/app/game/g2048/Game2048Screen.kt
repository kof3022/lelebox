package com.lelebox.app.game.g2048

import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelebox.app.audio.Sfx
import com.lelebox.app.ui.ElderButton
import com.lelebox.app.ui.WarmCreamDeep
import com.lelebox.app.ui.WarmGray
import kotlin.math.abs

/** 2048 入口（L1 原生）：滑动 + 四方向大按钮双操作，最高分存档，合并动画可关 */
@Composable
fun Game2048Screen(
    prefs: SharedPreferences,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bestKey = "native_2048_best"
    var best by remember { mutableIntStateOf(prefs.getInt(bestKey, 0)) }
    var animEnabled by remember { mutableStateOf(prefs.getBoolean("anim_enabled", true)) }

    val game = remember { Game2048().apply { newGame() } }
    var cells by remember { mutableStateOf(game.board.copyOf()) }
    var score by remember { mutableIntStateOf(game.score) }
    var won by remember { mutableStateOf(game.won) }
    var over by remember { mutableStateOf(game.over) }
    var winDismissed by remember { mutableStateOf(false) }

    fun refresh() {
        cells = game.board.copyOf()
        score = game.score
        won = game.won
        over = game.over
        if (game.score > best) {
            best = game.score
            prefs.edit().putInt(bestKey, best).apply()
        }
    }

    fun newGame() {
        game.newGame()
        winDismissed = false
        refresh()
    }

    fun doMove(dir: Dir) {
        if (over) return
        if (game.move(dir)) {
            Sfx.click(context)
            refresh()
        }
    }

    // 一局结束反馈音效
    LaunchedEffect(won, winDismissed) {
        if (won && !winDismissed && !over) Sfx.success(context)
    }
    LaunchedEffect(over) {
        if (over) Sfx.fail(context)
    }

    val encouragement = when {
        score >= 10000 -> "太厉害了！离 2048 只差一点！"
        score >= 3000 -> "很棒，继续加油！"
        score >= 1000 -> "不错，越来越接近 2048 啦！"
        else -> "再来一局，争取更高分！"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 计分行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("得分：$score", style = MaterialTheme.typography.titleLarge)
                Text("最高：$best", style = MaterialTheme.typography.bodyLarge)
            }
            ElderButton(text = "重新开始", onClick = ::newGame)
        }

        Spacer(Modifier.height(12.dp))

        // 棋盘（支持滑动）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFBBADA0))
                .padding(6.dp)
                .semantics { contentDescription = "2048 棋盘，得分 $score" }
                .pointerInput(Unit) {
                    var accX = 0f
                    var accY = 0f
                    detectDragGestures(
                        onDragStart = {
                            accX = 0f
                            accY = 0f
                        },
                        onDrag = { change, drag ->
                            accX += drag.x
                            accY += drag.y
                            change.consume()
                        },
                        onDragEnd = {
                            val threshold = 30f
                            if (abs(accX) > abs(accY) && abs(accX) > threshold) {
                                doMove(if (accX > 0) Dir.RIGHT else Dir.LEFT)
                            } else if (abs(accY) > threshold) {
                                doMove(if (accY > 0) Dir.DOWN else Dir.UP)
                            }
                        },
                    )
                },
        ) {
            Column(Modifier.fillMaxSize()) {
                repeat(Game2048.SIZE) { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        repeat(Game2048.SIZE) { c ->
                            val value = cells[r * Game2048.SIZE + c]
                            Tile2048(
                                value = value,
                                animEnabled = animEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 街机风方向键：紧凑十字 + 弧形垫板，上下/左右对称均衡，箭头清晰
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp)),
        ) {
            ArcadeButton(
                dir = Dir.UP,
                onClick = { doMove(Dir.UP) },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            )
            ArcadeButton(
                dir = Dir.DOWN,
                onClick = { doMove(Dir.DOWN) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            )
            ArcadeButton(
                dir = Dir.LEFT,
                onClick = { doMove(Dir.LEFT) },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
            )
            ArcadeButton(
                dir = Dir.RIGHT,
                onClick = { doMove(Dir.RIGHT) },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "向左向右滑动，或点方向按钮；相同数字碰到一起会合并",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }

    // 达成 2048 提示（可继续）
    if (won && !winDismissed && !over) {
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
            Text("2048 达成！你真棒！", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("得分：$score", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Spacer(Modifier.height(32.dp))
            ElderButton(text = "继续挑战", onClick = { winDismissed = true }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            ElderButton(text = "再来一局", onClick = ::newGame, modifier = Modifier.fillMaxWidth())
        }
    }

    // 本局结束
    if (over) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("😊", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("本局结束", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(
                "得分：$score　　最高：$best",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                encouragement,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFE0B2),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            ElderButton(text = "再来一局", onClick = ::newGame, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 街机风方向按钮：凸起圆形 + 主色箭头 + 上/下/左/右文字标识 + 按压缩放 */
@Composable
private fun ArcadeButton(dir: Dir, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f)
    val label = when (dir) {
        Dir.UP -> "上"
        Dir.DOWN -> "下"
        Dir.LEFT -> "左"
        Dir.RIGHT -> "右"
    }
    Box(
        modifier = modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (pressed) 1.dp else 3.dp,
                shape = CircleShape,
                ambientColor = WarmCreamDeep,
                spotColor = WarmGray.copy(alpha = 0.4f),
            )
            .clip(CircleShape)
            .background(if (pressed) MaterialTheme.colorScheme.surfaceVariant else Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(interactionSource = interaction, indication = ripple()) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        // 文字居中，箭头作为上方小标识（方向依然一目了然）
        Icon(
            imageVector = DirArrows.of(dir),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .size(20.dp),
        )
        Text(
            label,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun Tile2048(
    value: Int,
    animEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(value) {
        if (value != 0 && animEnabled) {
            scale.snapTo(0.7f)
            scale.animateTo(1f, tween(140))
        } else {
            scale.snapTo(1f)
        }
    }
    Box(
        modifier = modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tileColor(value))
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                "$value",
                fontSize = when {
                    value < 100 -> 30.sp
                    value < 1000 -> 24.sp
                    else -> 20.sp
                },
                color = if (value < 8) Color(0xFF776E65) else Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun tileColor(value: Int): Color = when (value) {
    0 -> Color(0xFFCDC1B4)
    2 -> Color(0xFFEEE4DA)
    4 -> Color(0xFFEDE0C8)
    8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563)
    32 -> Color(0xFFF67C5F)
    64 -> Color(0xFFF65E3B)
    128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61)
    512 -> Color(0xFFEDC850)
    1024 -> Color(0xFFEDC53F)
    2048 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}
