package com.lelebox.app.game.paijiu

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lelebox.app.R
import com.lelebox.app.audio.Sfx
import com.lelebox.app.ui.ElderButton
import kotlinx.coroutines.delay

/**
 * 牌九（打天九）对局界面：横屏全屏。
 * 玩家(0) 当庄领出；三家电脑 AI 自动行动。
 * 玩家回合：领出阶段点牌出牌；接牌阶段可选可接的牌出牌或点「过」垫牌。
 */
@Composable
fun PaiJiuScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = androidx.compose.ui.platform.LocalContext.current.findActivity()
    val game = remember { PaiJiuGame() }
    var tick by remember { mutableIntStateOf(0) }
    // 玩家当前选中的牌（领出 1 张 / 接牌 1 张）
    var selected by remember { mutableStateOf<PaiJiuTile?>(null) }
    var autoPass by remember { mutableStateOf(false) }
    val difficulty = PaiJiuDifficulty.MEDIUM

    // 横屏 + 沉浸式全屏
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.let { act ->
            WindowCompat.setDecorFitsSystemWindows(act.window, false)
            val c = WindowCompat.getInsetsController(act.window, act.window.decorView)
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { act ->
                WindowCompat.setDecorFitsSystemWindows(act.window, true)
                WindowCompat.getInsetsController(act.window, act.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    fun refresh() { tick++ }

    fun seatName(p: Int): String = when (p) { 0 -> "你"; 1 -> "孙权"; 2 -> "曹操"; 3 -> "刘备"; else -> "?" }

    /** 玩家出牌 */
    fun playerPlay(tiles: List<PaiJiuTile>) {
        if (game.play(0, tiles)) { Sfx.click(context); selected = null; refresh() }
    }

    /** 玩家垫牌：自动垫最小价值牌 */
    fun playerPass() {
        val t = PaiJiuAi.choosePass(game.hands[0], difficulty)
        if (game.pass(0, t)) { Sfx.click(context); selected = null; refresh() }
    }

    // AI 回合循环：单个常驻协程按 current 推进（不再依赖 tick 重启，
    // 避免 refresh() 取消/重启协程造成的竞态卡住）
    LaunchedEffect(Unit) {
        while (true) {
            if (!game.gameOver && game.current != 0) {
                delay(700)
                if (!game.gameOver && game.current != 0) {
                    val p = game.current
                    if (game.isLeadPhase) {
                        game.play(p, PaiJiuAi.chooseLead(game.hands[p], difficulty))
                    } else {
                        val f = PaiJiuAi.chooseFollow(game.hands[p], game.currentPlay!!, difficulty)
                        if (f != null) game.play(p, f) else game.pass(p, PaiJiuAi.choosePass(game.hands[p], difficulty))
                    }
                    Sfx.click(context)
                }
            }
            refresh()
            delay(120)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF2C4A3B))) {
        key(tick) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 三家电脑：位置参考麻将（曹操顶部居中、刘备左上、孙权右上），带头像
                // 曹操 seat 2：顶部居中
                Column(modifier = Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeatAvatar(R.drawable.avatar_caocao)
                        Spacer(Modifier.width(4.dp))
                        Text("曹操 ${game.hands[2].size}张", fontSize = 12.sp, color = Color(0xFFE8F0E4))
                    }
                    if (game.isFinished(2)) {
                        Text("第${game.rankOf(2)}名", fontSize = 13.sp, color = Color(0xFFF6C453))
                    }
                    Spacer(Modifier.height(4.dp))
                    // 牌背
                    Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                        repeat(game.hands[2].size) { PaiJiuTileBack(Modifier.size(13.dp, 19.dp)) }
                    }
                }
                // 刘备 seat 3：左上
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 56.dp, start = 12.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeatAvatar(R.drawable.avatar_liubei)
                        Spacer(Modifier.width(4.dp))
                        Text("刘备 ${game.hands[3].size}张", fontSize = 12.sp, color = Color(0xFFE8F0E4))
                    }
                    if (game.isFinished(3)) {
                        Text("第${game.rankOf(3)}名", fontSize = 13.sp, color = Color(0xFFF6C453))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                        repeat(game.hands[3].size) { PaiJiuTileBack(Modifier.size(13.dp, 19.dp)) }
                    }
                }
                // 孙权 seat 1：右上
                Column(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 12.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeatAvatar(R.drawable.avatar_sunquan)
                        Spacer(Modifier.width(4.dp))
                        Text("孙权 ${game.hands[1].size}张", fontSize = 12.sp, color = Color(0xFFE8F0E4))
                    }
                    if (game.isFinished(1)) {
                        Text("第${game.rankOf(1)}名", fontSize = 13.sp, color = Color(0xFFF6C453))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                        repeat(game.hands[1].size) { PaiJiuTileBack(Modifier.size(13.dp, 19.dp)) }
                    }
                }
                // 中央：当前桌面牌 + 本轮状态
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (game.isLeadPhase) "${seatName(game.leader)} 领出" else "${seatName(game.currentPlayPlayer)} 出了 ${currentPlayLabel(game.currentPlay!!)}",
                        fontSize = 18.sp,
                        color = Color(0xFFFFE082),
                    )
                    Spacer(Modifier.height(6.dp))
                    game.currentPlay?.let { play ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            play.tiles.forEach { t -> PaiJiuTileFace(t, Modifier.size(52.dp, 84.dp)) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // 玩家可接的牌提示
                    if (game.current == 0 && !game.isLeadPhase) {
                        val legal = PaiJiuRules.legalPlays(game.hands[0], game.currentPlay!!)
                        Text(
                            if (legal.isEmpty()) "接不住，请点「过」垫牌" else "可以出：${legal.size} 种",
                            fontSize = 14.sp,
                            color = Color(0xFFE8F0E4),
                        )
                    }
                }
                // 返回按钮（左上角小圆钮）
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(36.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        // 玩家手牌区（key(tick) 内，保证按钮行随状态整体重绘）
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 4.dp)) {
            key(tick) {
                PlayerHandArea(
                    game = game,
                    selected = selected,
                    onSelect = { t -> selected = if (selected == t) null else t },
                    onPlay = ::playerPlay,
                    onPass = ::playerPass,
                )
            }
        }

        // 结束面板
        if (game.gameOver) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xEEF7F1E6),
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val myRank = game.rankOf(0)
                    Text(
                        when (myRank) {
                            1 -> "🏆 你第一名，真厉害！"
                            2 -> "😊 你第二名，不错！"
                            3 -> "👍 你第三名，再试试！"
                            else -> "💪 你最后一名，下次加油！"
                        },
                        fontSize = 22.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    // 完整名次
                    game.finished.forEachIndexed { idx, p ->
                        Text("第${idx + 1}名：${seatName(p)}", fontSize = 15.sp, color = Color(0xFF6E6A63))
                    }
                    Spacer(Modifier.height(14.dp))
                    ElderButton(text = "再来一局", onClick = { game.newRound(); selected = null; refresh() }, minHeight = 44.dp)
                    Spacer(Modifier.height(6.dp))
                    ElderButton(text = "退出", onClick = onBack, minHeight = 44.dp,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface))
                }
            }
        }
    }
}

/** 桌面牌型文字 */
private fun currentPlayLabel(play: PaiJiuPlay): String = when {
    play.isSupreme -> "至尊对"
    play.kind == PlayKind.PAIR -> "${play.tiles[0].name} 对"
    else -> play.tiles[0].name
}

/** 座位头像（圆形裁剪，复用麻将头像素材） */
@Composable
private fun SeatAvatar(res: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = modifier.clip(CircleShape).size(30.dp),
        contentScale = ContentScale.Crop,
    )
}

/** 玩家手牌 + 操作按钮 */
@Composable
private fun PlayerHandArea(
    game: PaiJiuGame,
    selected: PaiJiuTile?,
    onSelect: (PaiJiuTile) -> Unit,
    onPlay: (List<PaiJiuTile>) -> Unit,
    onPass: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (game.gameOver) {
                // 已结束，无按钮
            } else if (game.current != 0) {
                Text("${if (game.isLeadPhase) "${seatName2(game.leader)} 领出中…" else "等待接牌…"}",
                    fontSize = 14.sp, color = Color(0xFFE8F0E4))
            } else if (game.isLeadPhase) {
                if (selected != null) {
                    ElderButton(text = "出牌", onClick = { onPlay(listOf(selected!!)) }, minHeight = 44.dp)
                } else {
                    Text("选一张牌出（先出牌的人）", fontSize = 14.sp, color = Color(0xFFE8F0E4))
                }
            } else {
                if (selected != null) {
                    ElderButton(text = "出牌", onClick = { onPlay(listOf(selected!!)) }, minHeight = 44.dp)
                    Spacer(Modifier.width(8.dp))
                }
                ElderButton(text = "过（垫牌）", onClick = onPass, minHeight = 44.dp,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface))
            }
        }
        Spacer(Modifier.height(6.dp))
        // 手牌：按规则文档 1.4 的顺序（rank.order 从小到大 = 天…杂五、猴六、猴三）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            game.hands[0].sortedBy { it.rank.order }.forEach { t ->
                val isSelected = selected == t
                val canUse = game.current == 0 && !game.gameOver && (
                    game.isLeadPhase || PaiJiuRules.legalPlays(game.hands[0], game.currentPlay!!).any { it.tiles.contains(t) }
                    )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .then(
                            if (isSelected) Modifier.border(3.dp, Color(0xFFF6C453), RoundedCornerShape(6.dp))
                            else if (canUse) Modifier.border(2.dp, Color(0xFF8BC34A), RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .clickable(enabled = game.current == 0 && !game.gameOver) { onSelect(t) },
                ) {
                    PaiJiuTileFace(t, Modifier.size(PaiJiuTileWidth, PaiJiuTileHeight))
                }
            }
        }
    }
}

private fun seatName2(p: Int): String = when (p) { 0 -> "你"; 1 -> "孙权"; 2 -> "曹操"; 3 -> "刘备"; else -> "?" }

/** 找 Activity（与麻将一致） */
internal fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
