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
import androidx.compose.foundation.layout.offset
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
    // 玩家当前选中的牌（多选：对子/副子需选 2-4 张；用索引集合）
    var selectedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
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

    /** 玩家出牌/管上：用选中的多张牌 */
    fun playerPlay() {
        val hand = PaiJiuRules.sortHand(game.hands[0])
        val tiles = selectedIndices.sorted().mapNotNull { hand.getOrNull(it) }
        if (tiles.isEmpty()) return
        if (game.play(0, tiles)) {
            Sfx.click(context); selectedIndices = emptySet(); refresh()
        }
    }

    /** 玩家垫牌：用选中的多张（数量须=首家出牌数）；未选或数量不足则自动垫 */
    fun playerPass() {
        val cur = game.currentPlay ?: return
        val hand = PaiJiuRules.sortHand(game.hands[0])
        val selected = selectedIndices.sorted().mapNotNull { hand.getOrNull(it) }
        val tiles = if (selected.size == cur.count) selected
        else PaiJiuAi.choosePass(game.hands[0], cur, difficulty)
        if (game.pass(0, tiles)) { Sfx.click(context); selectedIndices = emptySet(); refresh() }
    }

    // AI 回合循环：单个常驻协程按 current 推进。
    // 仅在 AI 实际行动（出牌/垫牌成功）后才 refresh()，避免无脑高频重组
    // 导致手牌/按钮点击命中不稳定。
    LaunchedEffect(Unit) {
        while (true) {
            if (!game.gameOver && game.current != 0) {
                delay(700)
                if (!game.gameOver && game.current != 0) {
                    val p = game.current
                    val beforePlay = game.currentPlay
                    val beforeCurrent = game.current
                    if (game.isLeadPhase) {
                        game.play(p, PaiJiuAi.chooseLead(game.hands[p], difficulty))
                    } else {
                        val f = PaiJiuAi.chooseFollow(game.hands[p], game.currentPlay!!, difficulty)
                        if (f != null) game.play(p, f) else game.pass(p, PaiJiuAi.choosePass(game.hands[p], game.currentPlay!!, difficulty))
                    }
                    // 状态确实变化才刷新（AI 行动成功）
                    if (game.current != beforeCurrent || game.currentPlay != beforePlay || game.gameOver) {
                        Sfx.click(context)
                        refresh()
                    }
                }
            } else {
                delay(200) // 玩家回合，空闲轮询
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF2C4A3B))) {
        // 桌面背景（复用麻将牌桌图片）
        Image(
            painter = painterResource(R.drawable.mj_table),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        key(tick) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 三家电脑：位置参考麻将（曹操顶部居中、刘备左上、孙权右上），带头像
                OpponentSeat(
                    p = 2, name = "曹操", avatar = R.drawable.avatar_caocao, game = game,
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                )
                OpponentSeat(
                    p = 3, name = "刘备", avatar = R.drawable.avatar_liubei, game = game,
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 56.dp, start = 12.dp),
                    horizontalAlignment = Alignment.Start,
                )
                OpponentSeat(
                    p = 1, name = "孙权", avatar = R.drawable.avatar_sunquan, game = game,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 12.dp),
                    horizontalAlignment = Alignment.End,
                )
                // 中央：本轮状态提示（出牌显示在台布金圈附近各家方位）
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (game.isLeadPhase) "${seatName(game.leader)} 领出" else "${seatName(game.currentPlayPlayer)} 出了 ${currentPlayLabel(game.currentPlay!!)}",
                        fontSize = 18.sp,
                        color = Color(0xFFFFE082),
                    )
                    Spacer(Modifier.height(8.dp))
                    // 玩家可接的牌提示（没有「过」，只有「垫牌」）
                    if (game.current == 0 && !game.isLeadPhase) {
                        val legal = PaiJiuRules.legalPlays(game.hands[0], game.currentPlay!!)
                        Text(
                            if (legal.isEmpty()) "接不住，请点「垫牌」" else "可以出：${legal.size} 种",
                            fontSize = 14.sp,
                            color = Color(0xFFE8F0E4),
                        )
                    }
                }
                // 本轮出的牌：显示在台布金圈附近各家相应方位（出牌明牌大牌/垫牌背面）
                // 曹操的出牌显示在座位手牌下方（见 OpponentSeat），此处不再重复显示
                ZonePlay(game = game, p = 3, modifier = Modifier.align(Alignment.CenterStart).padding(start = 170.dp)) // 刘备：左（内收）
                ZonePlay(game = game, p = 1, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 170.dp))     // 孙权：右（内收）
                ZonePlay(game = game, p = 0, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 200.dp)) // 玩家：下方
            }
        }
        // 返回按钮（左上角小圆钮，放 key(tick) 外避免重组导致点击失灵）
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
        // 玩家手牌区（key(tick) 内，保证按钮行随状态整体重绘）
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 4.dp)) {
            key(tick) {
                PlayerHandArea(
                    game = game,
                    selectedIndices = selectedIndices,
                    onSelect = { idx ->
                        selectedIndices = if (idx in selectedIndices) selectedIndices - idx else selectedIndices + idx
                    },
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
                    // 得牌 + 结牌类型 + 扇
                    val dePaiName = if (game.dePai == 0) "你" else seatName(game.dePai)
                    val jieText = when (game.jieType) {
                        PaiJiuGame.JieType.DA_DUI -> "大对结"
                        PaiJiuGame.JieType.HOU -> "猴结"
                        PaiJiuGame.JieType.XIAO_DUI -> "小对结"
                        else -> ""
                    }
                    val fanText = (0..3).mapNotNull { q -> game.fanOf(q)?.let { "${seatName(q)} $it" } }.joinToString("、")
                    val resultText = if (game.dePai == 0) {
                        "🎉 你得牌！赢 ${game.settlement} 墩！"
                    } else {
                        "😊 $dePaiName 得牌，赢 ${game.settlement} 墩"
                    }
                    Text(resultText, fontSize = 22.sp, color = Color(0xFFB23A3A))
                    if (jieText.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("结牌：$jieText", fontSize = 15.sp, color = Color(0xFF6E6A63))
                    }
                    if (fanText.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("扇：$fanText", fontSize = 15.sp, color = Color(0xFF6E6A63))
                    }
                    Spacer(Modifier.height(8.dp))
                    // 各家结算（输/赢墩数）
                    (0..3).forEach { p ->
                        val detail = game.settlementDetail.getOrElse(p) { 0 }
                        val detailText = when {
                            p == game.dePai -> "（得牌）赢 ${game.settlement} 墩"
                            detail > 0 -> "赢 $detail 墩"
                            detail < 0 -> "输 ${-detail} 墩"
                            else -> "平"
                        }
                        Text("${seatName(p)}：${game.trickCount[p]}墩 $detailText", fontSize = 15.sp, color = Color(0xFF6E6A63))
                    }
                    Spacer(Modifier.height(14.dp))
                    ElderButton(text = "再来一局", onClick = { game.newRound(); selectedIndices = emptySet(); refresh() }, minHeight = 44.dp)
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
private fun currentPlayLabel(play: PaiJiuPlay): String = PaiJiuRules.playLabel(play)

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

/**
 * 电脑座位：头像+名字+牌背+名次，以及该家赢得的墩。
 * 墩：每墩竖向 4 张（墩主牌在最上；出牌明牌、垫牌背面），多墩横向排开。
 * 本轮出的牌/垫牌不在此显示（显示在台布金圈附近各家方位）。
 */
@Composable
private fun OpponentSeat(
    p: Int,
    name: String,
    avatar: Int,
    game: PaiJiuGame,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SeatAvatar(avatar)
            Spacer(Modifier.width(4.dp))
            Text("$name ${game.hands[p].size}张", fontSize = 14.sp, color = Color(0xFFE8F0E4))
            // 曹操：墩牌显示在"曹操6张"字样右侧（同排）
            if (p == 2 && game.trickCount[p] > 0) {
                Spacer(Modifier.width(8.dp))
                Text("${game.trickCount[p]}墩", fontSize = 14.sp, color = Color(0xFFFFE082))
            }
        }
        if (game.isFinished(p)) {
            Text("第${game.rankOf(p)}名", fontSize = 14.sp, color = Color(0xFFF6C453))
        }
        Spacer(Modifier.height(4.dp))
        if (p == 2) {
            // 曹操：手牌行（下方显示本轮出的牌）
            Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                repeat(game.hands[p].size) { PaiJiuTileBack(Modifier.size(15.dp, 22.dp)) }
            }
            // 本轮出的牌：靠近手牌下方（唯一显示处）
            if (game.lastPlayed[p].isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    game.lastPlayed[p].forEach { t -> PaiJiuTileFace(t, Modifier.size(20.dp, 32.dp)) }
                }
            } else if (game.lastPassed[p]) {
                Spacer(Modifier.height(3.dp))
                PaiJiuTileBack(Modifier.size(16.dp, 26.dp))
            }
        } else {
            // 刘备/孙权：牌背 + 下方墩
            Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                repeat(game.hands[p].size) { PaiJiuTileBack(Modifier.size(15.dp, 22.dp)) }
            }
            if (game.trickCount[p] > 0) {
                Spacer(Modifier.height(6.dp))
                Text("${game.trickCount[p]}墩", fontSize = 14.sp, color = Color(0xFFFFE082))
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    game.tricks[p].forEach { trick ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            trick.forEach { entry ->
                                if (entry.isPass) {
                                    PaiJiuTileBack(Modifier.size(13.dp, 20.dp))
                                } else {
                                    val isOwner = entry.player == p
                                    Row(
                                        modifier = if (isOwner) Modifier.border(1.dp, Color(0xFFFFE082), RoundedCornerShape(3.dp)) else Modifier,
                                    ) {
                                        entry.play?.tiles.orEmpty().forEach { t -> PaiJiuTileFace(t, Modifier.size(13.dp, 20.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 本轮出的牌：显示在台布金圈附近各家方位（出牌明牌大牌 / 垫牌背面） */
@Composable
private fun ZonePlay(game: PaiJiuGame, p: Int, modifier: Modifier = Modifier) {
    if (game.lastPlayed[p].isNotEmpty()) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            game.lastPlayed[p].forEach { t -> PaiJiuTileFace(t, Modifier.size(30.dp, 48.dp)) }
        }
    } else if (game.lastPassed[p]) {
        // 垫牌：背面显示在该家方位
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(1) { PaiJiuTileBack(Modifier.size(24.dp, 38.dp)) }
        }
    }
}

/** 玩家手牌 + 操作按钮 */
@Composable
private fun PlayerHandArea(
    game: PaiJiuGame,
    selectedIndices: Set<Int>,
    onSelect: (Int) -> Unit,
    onPlay: () -> Unit,
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
                // 领出：初始/选中都显示「出牌」
                ElderButton(text = "出牌", onClick = onPlay, minHeight = 44.dp)
            } else {
                // 接牌阶段：按钮文字随选中牌大小变化
                // 选中且能管上 → 「管上」；否则 → 「垫牌」
                val cur = game.currentPlay
                val sorted = PaiJiuRules.sortHand(game.hands[0])
                val selected = selectedIndices.sorted().mapNotNull { sorted.getOrNull(it) }
                val canGuan = cur != null && selected.size == cur.count &&
                    PaiJiuRules.classify(selected)?.let { PaiJiuRules.canBeat(it, cur) } == true
                if (selectedIndices.isNotEmpty() && canGuan) {
                    ElderButton(text = "管上", onClick = onPlay, minHeight = 44.dp)
                } else {
                    ElderButton(text = "垫牌", onClick = onPass, minHeight = 44.dp,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 手牌行：手牌居中，右侧显示墩（多墩横向、每墩竖向4张）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            // 手牌（居中，多选；排序见 PaiJiuRules.sortHand）
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                val sorted = PaiJiuRules.sortHand(game.hands[0])
                sorted.forEachIndexed { idx, t ->
                    val isSelected = idx in selectedIndices
                    val canUse = game.current == 0 && !game.gameOver && (
                        game.isLeadPhase || PaiJiuRules.legalPlays(game.hands[0], game.currentPlay!!).any { it.tiles.contains(t) }
                        )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .then(if (isSelected) Modifier.offset(y = (-14).dp) else Modifier)
                            .then(
                                if (isSelected) Modifier.border(4.dp, Color(0xFFF6C453), RoundedCornerShape(8.dp))
                                else if (canUse) Modifier.border(2.dp, Color(0xFF8BC34A), RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable(enabled = game.current == 0 && !game.gameOver) { onSelect(idx) },
                    ) {
                        PaiJiuTileFace(t, Modifier.size(PaiJiuTileWidth, PaiJiuTileHeight))
                    }
                    // 选中牌下方加小三角提示
                    if (isSelected) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(2.dp))
                            Text("▲", fontSize = 12.sp, color = Color(0xFFF6C453))
                        }
                    }
                }
            }
            // 手牌右侧：墩（多墩横向排开；每墩竖向4张，墩主最上，明牌/垫牌背面）
            if (game.trickCount[0] > 0) {
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${game.trickCount[0]}墩", fontSize = 15.sp, color = Color(0xFFFFE082))
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                        game.tricks[0].forEach { trick ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                trick.forEach { entry ->
                                    if (entry.isPass) {
                                        PaiJiuTileBack(Modifier.size(14.dp, 22.dp))
                                    } else {
                                        val isOwner = entry.player == 0
                                        Row(
                                            modifier = if (isOwner) Modifier.border(1.dp, Color(0xFFFFE082), RoundedCornerShape(3.dp)) else Modifier,
                                        ) {
                                            entry.play?.tiles.orEmpty().forEach { t -> PaiJiuTileFace(t, Modifier.size(14.dp, 22.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
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
