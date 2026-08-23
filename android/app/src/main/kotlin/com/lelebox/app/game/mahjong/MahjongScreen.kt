package com.lelebox.app.game.mahjong

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    var showFanDialog by remember { mutableStateOf(false) }
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

    fun checkActions() {
        val d = game.lastDiscard
        val canAct = d != null && !game.hasDrawn
        canPungNow = canAct && game.canPung(0)
        canKongNow = canAct && game.canKong(0)
        canWinNow = canAct && Mahjong.findWins(game.hands[0].toList() + d).isNotEmpty()
    }

    fun onDiscard(t: Tile) {
        if (!game.hasDrawn || game.winner >= 0) return
        game.discard(0, t)
        Sfx.click(context)
        refresh()
    }

    /** 放弃碰/杠/和：手动摸牌 */
    fun onDraw() {
        if (game.hasDrawn || game.winner >= 0 || game.exhausted) return
        game.draw(0)
        if (game.canWin(0)) Sfx.success(context)
        refresh()
    }

    fun onSelfWin() {
        if (game.selfWin(0)) { Sfx.success(context); showFanDialog = true; refresh() }
    }

    fun onPung() { if (game.doPung(0)) { Sfx.click(context); refresh() } }
    fun onKong() { if (game.doKong(0)) { Sfx.click(context); refresh() } }
    fun onConcealedKong() { if (game.canConcealedKong(0) && game.doConcealedKong(0)) { Sfx.click(context); refresh() } }

    fun onWinByDiscard() {
        val d = game.lastDiscard ?: return
        if (game.winByDiscard(0, d)) { Sfx.success(context); showFanDialog = true; refresh() }
    }

    // AI turn loop (seat 1..3 play while it is not the player's turn)
    LaunchedEffect(tick) {
        while (game.winner < 0 && !game.exhausted && game.current != 0) {
            delay(600)
            game.aiTurn(game.current)
            refresh()
        }
    }

    // Auto draw for the player (no draw button in normal play). If the player can
    // pung/kong/win on the last discard, NO timeout: buttons stay until they decide
    // (tap the action or 「摸牌」 to decline). Otherwise auto-draw quickly.
    LaunchedEffect(tick) {
        if (game.winner < 0 && !game.exhausted && game.current == 0 && !game.hasDrawn) {
            val d = game.lastDiscard
            val pending = d != null && (game.canPung(0) || game.canKong(0) ||
                Mahjong.findWins(game.hands[0].toList() + d).isNotEmpty())
            if (!pending) {
                delay(300)
                if (game.winner < 0 && !game.exhausted && game.current == 0 && !game.hasDrawn) {
                    game.draw(0)
                    if (game.canWin(0)) Sfx.success(context)
                    refresh()
                }
            }
        }
    }

    LaunchedEffect(tick) { if (game.winner >= 0) showFanDialog = true }

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
                // Back button (top-left, floats over the table)
                ElderButton(
                    text = "←",
                    onClick = onBack,
                    minHeight = 36.dp,
                    modifier = Modifier.width(48.dp).align(Alignment.TopStart),
                )
                // ---- Center: two-layer tile wall (牌墙居中) + 状态 ----
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
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
                    // 固定高度状态槽：提示/再来一局 出现与否都不影响牌墙位置
                    Box(modifier = Modifier.height(60.dp), contentAlignment = Alignment.Center) {
                        when {
                            game.winner >= 0 -> {}
                            game.exhausted -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("流局", fontSize = 14.sp, color = Color(0xFFFFE082))
                                    Spacer(Modifier.height(2.dp))
                                    ElderButton(text = "再来一局", onClick = { game.newRound(); refresh() }, minHeight = 34.dp)
                                }
                            }
                            game.current != 0 -> Text("电脑摸打中…", fontSize = 13.sp, color = Color(0xFFE8F0E4))
                            else -> {}
                        }
                    }
                }
                // ---- Top seat 2 (曹操): avatar+name above, hand backs, discards below ----
                Column(modifier = Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeatAvatar(R.drawable.avatar_caocao)
                        Spacer(Modifier.width(4.dp))
                        Text("曹操 ${game.hands[2].size}张", fontSize = 12.sp, color = Color(0xFFE8F0E4))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                        repeat(minOf(game.hands[2].size, 12)) { TileBack(Modifier.size(14.dp, 20.dp)) }
                    }
                    OpponentDiscardRow(game.discards[2])
                }
                // ---- Left seat 3 (刘备): avatar+name above, vertical hand, discards below ----
                Column(modifier = Modifier.align(Alignment.CenterStart), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeatAvatar(R.drawable.avatar_liubei)
                        Spacer(Modifier.width(4.dp))
                        Text("刘备 ${game.hands[3].size}张", fontSize = 12.sp, color = Color(0xFFE8F0E4))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy((-3).dp)) {
                        repeat(minOf(game.hands[3].size, 5)) { TileBack(Modifier.size(20.dp, 14.dp)) }
                    }
                    OpponentDiscardRow(game.discards[3])
                }
                // ---- Right seat 1 (孙权): avatar+name above, vertical hand, discards below ----
                Column(modifier = Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeatAvatar(R.drawable.avatar_sunquan)
                        Spacer(Modifier.width(4.dp))
                        Text("孙权 ${game.hands[1].size}张", fontSize = 12.sp, color = Color(0xFFE8F0E4))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy((-3).dp)) {
                        repeat(minOf(game.hands[1].size, 5)) { TileBack(Modifier.size(20.dp, 14.dp)) }
                    }
                    OpponentDiscardRow(game.discards[1])
                }
            }
            } // key(tick)

            // Action buttons (draw is automatic; only win/pung/kong/ankang).
            // Fixed-height slot: buttons appearing/disappearing never move the wall.
            key(tick) {
                Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (game.winner < 0 && !game.exhausted && game.current == 0) {
                        if (canPungNow) ElderButton(text = "碰", onClick = ::onPung, modifier = Modifier.weight(1f), minHeight = 44.dp)
                        if (canKongNow) ElderButton(text = "杠", onClick = ::onKong, modifier = Modifier.weight(1f), minHeight = 44.dp)
                        if (canWinNow) ElderButton(
                            text = "和", onClick = ::onWinByDiscard, modifier = Modifier.weight(1f), minHeight = 44.dp,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White),
                        )
                        // 有可操作项时无超时等待；「摸牌」= 放弃操作直接摸牌
                        if ((canPungNow || canKongNow || canWinNow) && !game.hasDrawn) {
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
            }
            Spacer(Modifier.height(2.dp))

            // Player's own discards: fixed-height slot just above the hand (wall never shifts)
            key(tick) {
                Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.BottomCenter) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        OpponentDiscardRow(game.discards[0])
                    }
                }
            }
            Spacer(Modifier.height(2.dp))

            // Player hand: all tiles sorted together (drawn tile sorts into place,
            // winds stay grouped: 东南西北), no right-side separation
            key(tick) {
                val hand = game.hands[0].sortedBy { it.groupKey() }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(56.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    hand.forEach { t ->
                        TileFace(t, Modifier.size(32.dp, 44.dp).clickable { onDiscard(t) })
                    }
                }
            }
        }
    }

    // Win fan dialog
    if (showFanDialog && game.winner >= 0) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (game.winner == 0) "🎉 你胡了！" else "😊 ${seatName(game.winner)} 胡了", fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    if (game.winner == 0) {
                        game.winFans.forEach { f -> Text("${f.name}  ${f.fan}番", fontSize = 15.sp) }
                        Spacer(Modifier.height(6.dp))
                        Text("共 ${FanCalculator.total(game.winFans)} 番", fontSize = 20.sp, color = Color(0xFFB23A3A))
                    } else {
                        Text("${seatName(game.winner)} 和牌，继续加油！", fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    ElderButton(text = "再来一局", onClick = { game.newRound(); showFanDialog = false; refresh() }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    ElderButton(text = "退出", onClick = onBack, modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface))
                }
            }
        }
    }

    LaunchedEffect(tick) {
        if (game.current == 0 && game.lastDiscard != null && game.winner < 0) checkActions()
    }
}

/** 弃牌行：全部显示（牌多时缩小）；最新一张略大 = 刚出的牌 */
@Composable
private fun OpponentDiscardRow(discards: List<Tile>, modifier: Modifier = Modifier) {
    if (discards.isEmpty()) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
        val n = discards.size
        discards.forEachIndexed { i, t ->
            val last = i == n - 1
            val (w, h) = if (n > 10) {
                if (last) 13.dp to 18.dp else 10.dp to 14.dp
            } else {
                if (last) 18.dp to 26.dp else 14.dp to 20.dp
            }
            TileFace(t, Modifier.size(w, h))
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

/** Tile back image */
@Composable
private fun TileBack(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.mj_back),
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(3.dp)),
        contentScale = ContentScale.Crop,
    )
}

/** Map Tile to drawable resource */
fun tileRes(tile: Tile): Int = when {
    tile.suit == 0 -> when (tile.rank) { 1 -> R.drawable.mj_0_1; 2 -> R.drawable.mj_0_2; 3 -> R.drawable.mj_0_3; 4 -> R.drawable.mj_0_4; 5 -> R.drawable.mj_0_5; 6 -> R.drawable.mj_0_6; 7 -> R.drawable.mj_0_7; 8 -> R.drawable.mj_0_8; else -> R.drawable.mj_0_9 }
    tile.suit == 1 -> when (tile.rank) { 1 -> R.drawable.mj_1_1; 2 -> R.drawable.mj_1_2; 3 -> R.drawable.mj_1_3; 4 -> R.drawable.mj_1_4; 5 -> R.drawable.mj_1_5; 6 -> R.drawable.mj_1_6; 7 -> R.drawable.mj_1_7; 8 -> R.drawable.mj_1_8; else -> R.drawable.mj_1_9 }
    tile.suit == 2 -> when (tile.rank) { 1 -> R.drawable.mj_2_1; 2 -> R.drawable.mj_2_2; 3 -> R.drawable.mj_2_3; 4 -> R.drawable.mj_2_4; 5 -> R.drawable.mj_2_5; 6 -> R.drawable.mj_2_6; 7 -> R.drawable.mj_2_7; 8 -> R.drawable.mj_2_8; else -> R.drawable.mj_2_9 }
    else -> when (tile.rank) { 1 -> R.drawable.mj_3_1; 2 -> R.drawable.mj_3_2; 3 -> R.drawable.mj_3_3; 4 -> R.drawable.mj_3_4; 5 -> R.drawable.mj_3_5; 6 -> R.drawable.mj_3_6; else -> R.drawable.mj_3_7 }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
