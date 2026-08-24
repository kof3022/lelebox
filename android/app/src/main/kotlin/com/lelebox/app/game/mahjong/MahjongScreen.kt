package com.lelebox.app.game.mahjong

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.rotate
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

/** Mahjong: landscape fullscreen 4-player table. v0.5.4: auto-draw, Three Kingdoms seats. */
@Composable
fun MahjongScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val game = remember { MahjongGame() }
    var tick by remember { mutableIntStateOf(0) }
    var canPungNow by remember { mutableStateOf(false) }
    var canKongNow by remember { mutableStateOf(false) }
    var canWinNow by remember { mutableStateOf(false) }

    // Landscape + immersive fullscreen
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

    /** AI seat display names (Three Kingdoms): 1=孙权(right), 2=曹操(top), 3=刘备(left) */
    fun seatName(p: Int): String = when (p) { 1 -> "孙权"; 2 -> "曹操"; 3 -> "刘备"; else -> "你" }

    /** 每轮重算可操作项；无有效弃牌或已摸牌时清零（避免残留旧按钮导致碰牌不触发/误显示） */
    fun checkActions() {
        val d = game.lastDiscard ?: run {
            canPungNow = false; canKongNow = false; canWinNow = false
            return
        }
        if (game.hasDrawn || game.current != 0 || game.winner >= 0) {
            canPungNow = false; canKongNow = false; canWinNow = false
            return
        }
        canPungNow = game.canPung(0)
        canKongNow = game.canKong(0)
        canWinNow = Mahjong.findWins(game.hands[0].toList() + d).isNotEmpty()
    }

    fun onDiscard(t: Tile) {
        if (game.winner >= 0) return
        // 正常出牌需已摸牌；碰/吃后（mustDiscard）无需摸牌直接弃
        if (!game.hasDrawn && !game.mustDiscard) return
        game.discard(0, t)
        Sfx.click(context)
        refresh()
    }

    /** 放弃碰/杠/和：响应阶段放弃响应（交给下一家）；非响应阶段手动摸牌 */
    fun onDraw() {
        if (game.hasDrawn || game.winner >= 0 || game.exhausted) return
        if (game.respondStage && game.lastDiscard != null) {
            game.passRespond()
            refresh()
            return
        }
        game.draw(0)
        if (game.canWin(0)) Sfx.success(context)
        refresh()
    }

    fun onSelfWin() {
        if (game.selfWin(0)) { Sfx.success(context); refresh() }
    }

    fun onPung() { if (game.doPung(0)) { Sfx.click(context); refresh() } }
    fun onKong() { if (game.doKong(0)) { Sfx.click(context); refresh() } }
    fun onConcealedKong() { if (game.canConcealedKong(0) && game.doConcealedKong(0)) { Sfx.click(context); refresh() } }

    fun onWinByDiscard() {
        val d = game.lastDiscard ?: return
        if (game.winByDiscard(0, d)) { Sfx.success(context); refresh() }
    }

    // AI turn loop (seat 1..3 play while it is not the player's turn)
    // AI 打牌音效：弃牌/碰/杠播放点击音；AI 和牌播放成功音
    LaunchedEffect(tick) {
        while (game.winner < 0 && !game.exhausted && game.current != 0) {
            delay(600)
            val beforeDiscards = game.discards.sumOf { it.size }
            val beforeExposed = game.exposed.sumOf { it.size }
            game.aiTurn(game.current)
            if (game.winner >= 0) {
                Sfx.success(context)
            } else if (game.discards.sumOf { it.size } > beforeDiscards ||
                game.exposed.sumOf { it.size } > beforeExposed) {
                Sfx.click(context)
            }
            refresh()
        }
    }

    // Auto draw for the player (no draw button in normal play). If the player can
    // pung/kong/win on the last discard, NO timeout: buttons stay until they decide
    // (tap the action or 「摸牌」 to decline). Otherwise auto-draw quickly.
    // 响应阶段：不能响应时自动 passRespond（交给下一家）；三家都放弃后下家摸牌
    // After a pung/chow the player discards directly (mustDiscard) — no draw.
    LaunchedEffect(tick) {
        if (game.winner < 0 && !game.exhausted && game.current == 0 && !game.hasDrawn && !game.mustDiscard) {
            val d = game.lastDiscard
            val pending = d != null && (game.canPung(0) || game.canKong(0) ||
                Mahjong.findWins(game.hands[0].toList() + d).isNotEmpty())
            if (game.respondStage && d != null) {
                if (!pending) { game.passRespond(); refresh() }
            } else if (!pending) {
                delay(300)
                if (game.winner < 0 && !game.exhausted && game.current == 0 && !game.hasDrawn && !game.mustDiscard) {
                    game.draw(0)
                    if (game.canWin(0)) Sfx.success(context)
                    refresh()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF2C4A3B))) {
        // 牌桌背景（即梦生成，已压缩 drawable-xxhdpi/mj_table.jpg）
        Image(
            painter = painterResource(R.drawable.mj_table),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            // Table area: key(tick) forces full recomposition on every state change
            // (discard rows are built from mutable lists that Compose otherwise skips)
            key(tick) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // 玩家可对某家的弃牌碰/杠/和 → 高亮那家的最新弃牌（和=红，碰/杠=金）
                val pendingAction = game.winner < 0 && !game.exhausted && game.current == 0 &&
                    !game.hasDrawn && game.lastDiscard != null && (canPungNow || canKongNow || canWinNow)
                val hlColor = if (canWinNow) Color(0xFFC62828) else Color(0xFFF6C453)
                // Back button (top-left): arrow centered in the button
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
                // ---- Center: two-layer tile wall at the table image's gold-ring center ----
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 132.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Two-layer tile wall (schematic) + remaining count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy((-6).dp)) {
                            repeat(2) { TileBack(Modifier.size(24.dp, 32.dp)) }
                        }
                        Spacer(Modifier.width(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy((-6).dp)) {
                            repeat(2) { TileBack(Modifier.size(24.dp, 32.dp)) }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("${game.walls.sumOf { it.size }}", fontSize = 18.sp, color = Color(0xFFFFE082))
                    }
                    Spacer(Modifier.height(6.dp))
                    // 提示消息紧贴牌墙下方：显示"正在摸打的人"。
                    // 响应阶段（三家依次声明和/杠/碰）固定显示弃牌者，避免轮番闪动；
                    // 玩家弃牌后显示下家（孙权，即将摸打）；非响应阶段显示当前行动者。
                    // 向上偏移贴近牌墙，避开中圈金圈线。
                    if (game.winner < 0 && !game.exhausted && game.current != 0) {
                        val actor = if (game.respondStage) {
                            if (game.lastDiscardPlayer == 0) (game.lastDiscardPlayer + 1) % 4 else game.lastDiscardPlayer
                        } else game.current
                        Text(
                            "${seatName(actor)} 摸打中…",
                            fontSize = 13.sp,
                            color = Color(0xFFE8F0E4),
                            modifier = Modifier.offset(y = (-9).dp),
                        )
                    }
                }
                // ---- Top seat 2 (曹操): avatar+name fixed at top, hand backs, discards multi-row ----
                Column(modifier = Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeatAvatar(R.drawable.avatar_caocao)
                        Spacer(Modifier.width(4.dp))
                        Text("曹操 ${game.hands[2].size}张", fontSize = 12.sp, color = Color(0xFFE8F0E4))
                    }
                    if (game.winner == 2) {
                        Text("🎉 和牌！", fontSize = 15.sp, color = Color(0xFFF6C453))
                    }
                    // 和牌时翻开手牌
                    if (game.winner >= 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            game.hands[2].sortedBy { it.groupKey() }.forEach { t -> TileFace(t, Modifier.size(12.dp, 17.dp)) }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                            repeat(game.hands[2].size) { TileBack(Modifier.size(14.dp, 20.dp)) }
                        }
                    }
                    // 曹操：弃牌居中不变；明刻从弃牌第一排第8张右侧固定开始，多明刻向下排
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OpponentDiscardRows(
                            game.discards[2],
                            modifier = Modifier.align(Alignment.TopCenter),
                            highlight = pendingAction && game.lastDiscardPlayer == 2,
                            highlightColor = hlColor,
                        )
                        MeldGroup(game.exposed[2], modifier = Modifier.align(Alignment.TopStart).padding(start = 465.dp))
                    }
                }
                // ---- Left seat 3 (刘备): top-anchored (avatar never moves), discards multi-row ----
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeatAvatar(R.drawable.avatar_liubei)
                        Spacer(Modifier.width(4.dp))
                        Text("刘备 ${game.hands[3].size}张", fontSize = 12.sp, color = Color(0xFFE8F0E4))
                    }
                    if (game.winner == 3) {
                        Text("🎉 和牌！", fontSize = 15.sp, color = Color(0xFFF6C453))
                    }
                    // 和牌时翻开手牌
                    if (game.winner >= 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            game.hands[3].sortedBy { it.groupKey() }.forEach { t -> TileFace(t, Modifier.size(12.dp, 17.dp)) }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                            repeat(game.hands[3].size) { TileBack(Modifier.size(14.dp, 20.dp)) }
                        }
                    }
                    // 刘备：弃牌不变；明刻从弃牌第一排第8张右侧开始，多明刻向下排
                    Row(verticalAlignment = Alignment.Top) {
                        OpponentDiscardRows(game.discards[3], alignment = Alignment.Start, highlight = pendingAction && game.lastDiscardPlayer == 3, highlightColor = hlColor)
                        Spacer(Modifier.width(6.dp))
                        MeldGroup(game.exposed[3])
                    }
                }
                // ---- Right seat 1 (孙权): top-anchored (avatar never moves), discards multi-row ----
                Column(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 60.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeatAvatar(R.drawable.avatar_sunquan)
                        Spacer(Modifier.width(4.dp))
                        Text("孙权 ${game.hands[1].size}张", fontSize = 12.sp, color = Color(0xFFE8F0E4))
                    }
                    if (game.winner == 1) {
                        Text("🎉 和牌！", fontSize = 15.sp, color = Color(0xFFF6C453))
                    }
                    // 和牌时翻开手牌
                    if (game.winner >= 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            game.hands[1].sortedBy { it.groupKey() }.forEach { t -> TileFace(t, Modifier.size(12.dp, 17.dp)) }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                            repeat(game.hands[1].size) { TileBack(Modifier.size(14.dp, 20.dp)) }
                        }
                    }
                    // 孙权：弃牌不变；明刻从弃牌第一排第8张左侧开始，多明刻向下排
                    Row(verticalAlignment = Alignment.Top) {
                        MeldGroup(game.exposed[1])
                        Spacer(Modifier.width(6.dp))
                        OpponentDiscardRows(game.discards[1], alignment = Alignment.End, highlight = pendingAction && game.lastDiscardPlayer == 1, highlightColor = hlColor)
                    }
                }
            }
            } // key(tick)

            // Action buttons (only when actions exist; wall is top-anchored so it never shifts)
            key(tick) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (game.winner < 0 && !game.exhausted && game.current == 0) {
                        // 碰/杠/和（吃弃牌）只在有可操作的弃牌时显示；碰/吃后 lastDiscard 已清 → 消失
                        val actOnDiscard = game.lastDiscard != null && !game.hasDrawn
                        if (actOnDiscard && canPungNow) ElderButton(text = "碰", onClick = ::onPung, modifier = Modifier.weight(1f), minHeight = 44.dp)
                        if (actOnDiscard && canKongNow) ElderButton(text = "杠", onClick = ::onKong, modifier = Modifier.weight(1f), minHeight = 44.dp)
                        if (actOnDiscard && canWinNow) ElderButton(
                            text = "和", onClick = ::onWinByDiscard, modifier = Modifier.weight(1f), minHeight = 44.dp,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White),
                        )
                        // 有可操作项时无超时等待；「摸牌」= 放弃操作直接摸牌
                        if (actOnDiscard && (canPungNow || canKongNow || canWinNow)) {
                            ElderButton(text = "摸牌", onClick = ::onDraw, modifier = Modifier.weight(1f), minHeight = 44.dp)
                        }
                        if (game.hasDrawn && game.canWin(0)) {
                            ElderButton(
                                text = "和牌", onClick = ::onSelfWin, modifier = Modifier.weight(1f), minHeight = 44.dp,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White),
                            )
                        }
                        if (game.hasDrawn && game.canConcealedKong(0)) ElderButton(text = "暗杠", onClick = ::onConcealedKong, modifier = Modifier.weight(1f), minHeight = 44.dp)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))

            // Player's own discards (centered, 8/row) + melds to their right (fixed, 向上排)
            key(tick) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OpponentDiscardRows(
                        game.discards[0],
                        perRow = 8,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                    MeldGroup(game.exposed[0], modifier = Modifier.align(Alignment.BottomStart).padding(start = 465.dp))
                }
            }
            Spacer(Modifier.height(2.dp))

            // Player hand: all tiles sorted; the just-drawn tile stands out — RAISED
            // (taller tile + ▼ below, gold border fits the tile exactly).
            key(tick) {
                val drawn = if (game.hasDrawn && game.hands[0].isNotEmpty()) game.hands[0].last() else null
                val hand = game.hands[0].sortedBy { it.groupKey() }
                var marked = false
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(64.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    hand.forEach { t ->
                        val isDrawn = drawn != null && !marked && t == drawn
                        if (isDrawn) marked = true
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = if (isDrawn) {
                                    Modifier.border(3.dp, Color(0xFFF6C453), RoundedCornerShape(4.dp))
                                } else Modifier,
                            ) {
                                TileFace(
                                    t,
                                    if (isDrawn) {
                                        Modifier.size(32.dp, 46.dp).clickable { onDiscard(t) }
                                    } else {
                                        Modifier.size(32.dp, 44.dp).clickable { onDiscard(t) }
                                    },
                                )
                            }
                            if (isDrawn) {
                                Text("▼", fontSize = 11.sp, color = Color(0xFFF6C453))
                            }
                        }
                    }
                }
            }
        }

        // 和牌面板：桌面中央（不遮住整桌），显示和家提示/自摸/番数/按钮；无论谁和牌都结算番数
        if (game.winner >= 0) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xEEF7F1E6),
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (game.winner == 0) "🎉 你胡了！" else "😊 ${seatName(game.winner)} 胡了", fontSize = 20.sp)
                    if (game.winSelfDraw) {
                        Spacer(Modifier.height(4.dp))
                        Text("自摸！", fontSize = 14.sp, color = Color(0xFFC62828))
                    }
                    Spacer(Modifier.height(8.dp))
                    // 无论谁和牌都显示番数明细与合计
                    game.winFans.forEach { f -> Text("${f.name}  ${f.fan}番", fontSize = 14.sp) }
                    Spacer(Modifier.height(4.dp))
                    Text("共 ${FanCalculator.total(game.winFans)} 番", fontSize = 18.sp, color = Color(0xFFB23A3A))
                    Spacer(Modifier.height(14.dp))
                    ElderButton(text = "再来一局", onClick = { game.newRound(); refresh() }, minHeight = 44.dp)
                    Spacer(Modifier.height(6.dp))
                    ElderButton(text = "退出", onClick = onBack, minHeight = 44.dp,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface))
                }
            }
        }

        // 流局面板：桌面中央显示流局提示与按钮
        if (game.exhausted && game.winner < 0) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xEEF7F1E6),
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😊", fontSize = 40.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("流局", fontSize = 22.sp, color = Color(0xFF2E2A25))
                    Spacer(Modifier.height(6.dp))
                    Text("牌摸完了，没人胡牌。再来一局吧！", fontSize = 13.sp, color = Color(0xFF6E6A63))
                    Spacer(Modifier.height(14.dp))
                    ElderButton(text = "再来一局", onClick = { game.newRound(); refresh() }, minHeight = 44.dp)
                    Spacer(Modifier.height(6.dp))
                    ElderButton(text = "退出", onClick = onBack, minHeight = 44.dp,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface))
                }
            }
        }
    }

    LaunchedEffect(tick) {
        checkActions()
    }
}

/** 弃牌多行显示：每行最多 perRow 张（牌不缩太小），全部显示；最新一张略大；
 *  highlight=true 时最新一张加边框（碰/杠/和的目标牌），highlightColor 可区分碰(金)/和(红) */
@Composable
private fun OpponentDiscardRows(
    discards: List<Tile>,
    perRow: Int = 8,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    highlight: Boolean = false,
    highlightColor: Color = Color(0xFFF6C453),
    modifier: Modifier = Modifier,
) {
    if (discards.isEmpty()) return
    Column(modifier = modifier, horizontalAlignment = alignment) {
        val chunks = discards.chunked(perRow)
        chunks.forEachIndexed { ci, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
                chunk.forEachIndexed { i, t ->
                    val last = ci == chunks.lastIndex && i == chunk.lastIndex
                    Box(
                        modifier = if (highlight && last) {
                            Modifier.border(3.dp, highlightColor, RoundedCornerShape(4.dp))
                        } else Modifier,
                    ) {
                        TileFace(t, Modifier.size(if (last) 18.dp else 14.dp, if (last) 26.dp else 20.dp))
                    }
                }
            }
        }
    }
}

/** 座位头像（圆形裁剪） */
@Composable
private fun SeatAvatar(res: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = modifier.clip(CircleShape).size(30.dp),
        contentScale = ContentScale.Crop,
    )
}

/** 明刻名称：碰/杠/吃 */
private fun meldLabel(m: Meld): String = when (m) {
    is Meld.Pung -> "碰"
    is Meld.Kong -> "杠"
    is Meld.Chow -> "吃"
    else -> "将"
}

/** 明刻的牌张 */
private fun meldTiles(m: Meld): List<Tile> = when (m) {
    is Meld.Chow -> m.tiles
    is Meld.Pung -> m.tiles
    is Meld.Kong -> m.tiles
    is Meld.Pair -> m.tiles
}

/** 一家的明刻组（AI 座位旁小牌显示：碰/杠/吃） */
@Composable
private fun MeldGroup(melds: List<Meld>, modifier: Modifier = Modifier) {
    if (melds.isEmpty()) return
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        melds.forEach { m ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
                meldTiles(m).forEach { t -> TileFace(t, Modifier.size(12.dp, 17.dp)) }
                Text(meldLabel(m), fontSize = 8.sp, color = Color(0xFFFFE082))
            }
        }
    }
}

/** Tile face with mahjong graphic image */
@Composable
private fun TileFace(tile: Tile, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(tileRes(tile)),
        contentDescription = tile.label(),
        modifier = modifier.clip(RoundedCornerShape(3.dp)),
        contentScale = ContentScale.Crop,
    )
}

/** Tile back image: light ivory (dark backs were hard to see) with a small diamond pattern */
@Composable
private fun TileBack(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFF3E9D4))
            .border(1.dp, Color(0xFFCBB98F), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(7.dp).rotate(45f).background(Color(0xFFD9C9A3)),
        )
    }
}

/** Map Tile to drawable resource.
 *  Note: lietxia/mahjong_graphic is a RIICHI set, honor order 东南西北白发中:
 *  mj_3_5=白 mj_3_6=发 mj_3_7=中 (so dragon mapping is 中→7 发→6 白→5). */
fun tileRes(tile: Tile): Int = when {
    tile.suit == 0 -> when (tile.rank) { 1 -> R.drawable.mj_0_1; 2 -> R.drawable.mj_0_2; 3 -> R.drawable.mj_0_3; 4 -> R.drawable.mj_0_4; 5 -> R.drawable.mj_0_5; 6 -> R.drawable.mj_0_6; 7 -> R.drawable.mj_0_7; 8 -> R.drawable.mj_0_8; else -> R.drawable.mj_0_9 }
    tile.suit == 1 -> when (tile.rank) { 1 -> R.drawable.mj_1_1; 2 -> R.drawable.mj_1_2; 3 -> R.drawable.mj_1_3; 4 -> R.drawable.mj_1_4; 5 -> R.drawable.mj_1_5; 6 -> R.drawable.mj_1_6; 7 -> R.drawable.mj_1_7; 8 -> R.drawable.mj_1_8; else -> R.drawable.mj_1_9 }
    tile.suit == 2 -> when (tile.rank) { 1 -> R.drawable.mj_2_1; 2 -> R.drawable.mj_2_2; 3 -> R.drawable.mj_2_3; 4 -> R.drawable.mj_2_4; 5 -> R.drawable.mj_2_5; 6 -> R.drawable.mj_2_6; 7 -> R.drawable.mj_2_7; 8 -> R.drawable.mj_2_8; else -> R.drawable.mj_2_9 }
    tile.suit == 3 -> when (tile.rank) { 1 -> R.drawable.mj_3_1; 2 -> R.drawable.mj_3_2; 3 -> R.drawable.mj_3_3; 4 -> R.drawable.mj_3_4; else -> R.drawable.mj_3_4 }
    else -> when (tile.rank) { 1 -> R.drawable.mj_3_7; 2 -> R.drawable.mj_3_6; else -> R.drawable.mj_3_5 }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
