package com.lelebox.app.game.gomoku

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlinx.coroutines.delay

/** 五子棋入口（L1 原生）：难度选择 → 棋盘（玩家黑先手 vs 电脑白） */
@Composable
fun GomokuScreen(
    prefs: SharedPreferences,
    modifier: Modifier = Modifier,
) {
    var level by remember { mutableStateOf<GomokuLevel?>(null) }
    // 物理返回键：对局页 → 难度页 →（难度页交给壳退出）
    if (level != null) BackHandler { level = null }
    when (val lv = level) {
        null -> GomokuLevelSelect(onStart = { level = it }, modifier = modifier)
        else -> GomokuBoard(lv, onBackToLevels = { level = null }, modifier = modifier)
    }
}

@Composable
private fun GomokuLevelSelect(
    onStart: (GomokuLevel) -> Unit,
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
            painter = painterResource(R.drawable.ic_game_gomoku),
            contentDescription = "五子棋",
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("五子棋", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "你先下黑棋，电脑下白棋。横、竖、斜哪边先连成五个就赢。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        GomokuLevel.entries.forEach { lv ->
            ElderButton(text = lv.label, onClick = { onStart(lv) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GomokuBoard(
    level: GomokuLevel,
    onBackToLevels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val game = remember { GomokuGame() }
    var round by remember { mutableStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var thinking by remember { mutableStateOf(false) }
    // 走子历史（用于撤销）
    val history = remember { mutableListOf<Pair<Int, Int>>() }

    fun resetBoard() {
        game.reset()
        history.clear()
        thinking = false
        round++
        tick++
    }

    // 玩家落子后，电脑延时思考
    LaunchedEffect(round, game.current, game.over) {
        if (!game.over && game.current == WHITE && !thinking) {
            thinking = true
            delay(500)
            game.aiMove(level)?.let { (x, y) ->
                if (game.place(x, y)) {
                    history.add(x to y)
                    tick++
                }
            }
            thinking = false
        }
    }

    fun onBoardTap(x: Int, y: Int) {
        if (game.over || thinking || game.current != BLACK) return
        if (game.place(x, y)) {
            history.add(x to y)
            Sfx.click(context)
            tick++ // 触发重绘，显示棋子
        }
    }

    fun undo() {
        if (thinking) return
        // 撤销：若电脑刚走（当前轮到玩家）先撤电脑，再撤玩家
        if (game.current == BLACK && history.isNotEmpty()) {
            val (ax, ay) = history.removeAt(history.size - 1)
            game.board[ay * game.size + ax] = NONE
        }
        if (history.isNotEmpty()) {
            val (px, py) = history.removeAt(history.size - 1)
            game.board[py * game.size + px] = NONE
        }
        game.winner = 0
        game.over = false
        game.current = BLACK
        thinking = false
        Sfx.click(context)
        round++
        tick++
    }

    val status = when {
        game.over && game.winner == BLACK -> "你赢啦！"
        game.over && game.winner == WHITE -> "电脑赢啦"
        game.over -> "平局"
        thinking -> "机器思考中…"
        else -> "该你下了（黑棋）"
    }

    // 结算音效
    LaunchedEffect(game.over) {
        if (game.over) {
            if (game.winner == BLACK) Sfx.success(context) else Sfx.fail(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(status, style = MaterialTheme.typography.titleMedium, color = if (game.over && game.winner == BLACK) ElderGreen else MaterialTheme.colorScheme.onSurface)
            ElderButton(text = "撤销", onClick = ::undo, minHeight = 52.dp)
        }
        Spacer(Modifier.height(10.dp))

        // 棋盘（key(tick) 强制在每次落子后重绘棋子）
        key(tick) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFEDE3D0))
                    .semantics { contentDescription = "五子棋棋盘，${game.size}乘${game.size}" }
                    .pointerInput(round, tick) {
                        detectTapGestures { offset ->
                            val cell = size.width / game.size
                            val x = (offset.x / cell).toInt().coerceIn(0, game.size - 1)
                            val y = (offset.y / cell).toInt().coerceIn(0, game.size - 1)
                            onBoardTap(x, y)
                        }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cell = size.width / game.size
                    val pad = cell / 2f
                    // 网格线
                    val lineColor = Color(0xFFA89070)
                    for (i in 0 until game.size) {
                        val p = pad + i * cell
                        drawLine(lineColor, Offset(p, pad), Offset(p, size.height - pad), strokeWidth = 1.5f)
                        drawLine(lineColor, Offset(pad, p), Offset(size.width - pad, p), strokeWidth = 1.5f)
                    }
                    // 棋子
                    for (y in 0 until game.size) {
                        for (x in 0 until game.size) {
                            val s = game.get(x, y)
                            if (s != NONE) {
                                val cx = pad + x * cell
                                val cy = pad + y * cell
                                val r = cell * 0.42f
                                val c = if (s == BLACK) Color(0xFF2E2A25) else Color.White
                                drawCircle(color = c, radius = r, center = Offset(cx, cy))
                                if (s == WHITE) {
                                    drawCircle(
                                        color = Color(0xFFB9A98D),
                                        radius = r,
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 2f),
                                    )
                                }
                            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (game.winner == BLACK) "🎉" else "😊", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                if (game.winner == BLACK) "你赢啦！五子连珠！" else if (game.winner == WHITE) "电脑赢啦，再接再厉！" else "平局，棋逢对手！",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            // 用户互动：步数 + 星级（赢得越利落星越多）
            val moves = history.size
            val stars = if (game.winner == BLACK) {
                if (moves <= 15) 3 else if (moves <= 27) 2 else 1
            } else 1
            Text(
                "⭐".repeat(stars) + "☆".repeat(3 - stars),
                fontSize = 40.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    game.winner == BLACK -> "你用了 $moves 步就赢啦！"
                    game.winner == WHITE -> "电脑赢啦，下次加油！"
                    else -> "平局！棋逢对手"
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
