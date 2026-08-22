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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lelebox.app.R
import com.lelebox.app.audio.Sfx
import com.lelebox.app.ui.ElderButton
import kotlinx.coroutines.delay

/** Mahjong: landscape fullscreen 4-player table. Clean minimal UI. */
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

    fun checkActions() {
        val d = game.lastDiscard
        canPungNow = d != null && game.canPung(0)
        canKongNow = d != null && game.canKong(0)
        canWinNow = d != null && Mahjong.findWins(game.hands[0].toList() + d).isNotEmpty()
    }

    fun onDiscard(t: Tile) {
        if (!game.hasDrawn || game.winner >= 0) return
        game.discard(0, t)
        Sfx.click(context)
        refresh()
    }

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

    LaunchedEffect(tick) {
        while (game.winner < 0 && !game.exhausted && game.current != 0) {
            delay(600)
            game.aiTurn(game.current)
            refresh()
        }
    }
    LaunchedEffect(tick) { if (game.winner >= 0) showFanDialog = true }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF2C4A3B))) {
        Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            // Minimal top: only tiny back button (no help, no extra)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ElderButton(text = "←", onClick = onBack, minHeight = 36.dp, modifier = Modifier.width(48.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    when {
                        game.winner >= 0 -> "和牌！"
                        game.exhausted -> "流局"
                        game.current == 0 -> "轮到你"
                        else -> "电脑摸打中…"
                    },
                    fontSize = 14.sp,
                    color = Color(0xFFE8F0E4),
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(2.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // ---- Top seat 2: wall + discards + hand back ----
                Column(modifier = Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    WallDisplay(count = game.walls[2].size)
                    OpponentDiscardRow(game.discards[2])
                    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                        repeat(minOf(game.hands[2].size, 12)) { TileBack(Modifier.size(14.dp, 20.dp)) }
                    }
                    Text("电脑2", fontSize = 10.sp, color = Color(0xFFE8F0E4))
                }
                // ---- Left seat 3: wall + discards + vertical hand ----
                Column(modifier = Modifier.align(Alignment.CenterStart), horizontalAlignment = Alignment.CenterHorizontally) {
                    WallDisplay(count = game.walls[3].size)
                    Text("电脑3", fontSize = 10.sp, color = Color(0xFFE8F0E4))
                    Column(verticalArrangement = Arrangement.spacedBy((-3).dp)) {
                        repeat(minOf(game.hands[3].size, 5)) { TileBack(Modifier.size(20.dp, 14.dp)) }
                    }
                    OpponentDiscardRow(game.discards[3])
                }
                // ---- Right seat 1: wall + discards + vertical hand ----
                Column(modifier = Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                    WallDisplay(count = game.walls[1].size)
                    Text("电脑1", fontSize = 10.sp, color = Color(0xFFE8F0E4))
                    Column(verticalArrangement = Arrangement.spacedBy((-3).dp)) {
                        repeat(minOf(game.hands[1].size, 5)) { TileBack(Modifier.size(20.dp, 14.dp)) }
                    }
                    OpponentDiscardRow(game.discards[1])
                }
                // ---- Center: dice + last discard ----
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Dice
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DieFace(game.dice1)
                        DieFace(game.dice2)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("从第${game.currentWall + 1}面墙起抓", fontSize = 12.sp, color = Color(0xFFFFE082))
                    Spacer(Modifier.height(6.dp))
                    if (game.lastDiscard != null) {
                        TileFace(game.lastDiscard!!, Modifier.size(44.dp, 62.dp))
                    } else if (game.hasDrawn) {
                        Text("请打出一张牌", fontSize = 15.sp, color = Color(0xFFFFE082))
                    }
                }
                // ---- Player wall (bottom center) ----
                WallDisplay(count = game.walls[0].size, modifier = Modifier.align(Alignment.BottomCenter))
            }

            // Action buttons (only available ones; no hint/help)
            key(tick) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (game.winner < 0 && !game.exhausted && game.current == 0) {
                        if (!game.hasDrawn) {
                            ElderButton(text = "摸牌", onClick = ::onDraw, modifier = Modifier.weight(1f), minHeight = 44.dp)
                        } else if (game.canWin(0)) {
                            ElderButton(text = "和牌", onClick = ::onSelfWin, modifier = Modifier.weight(1f), minHeight = 44.dp, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White))
                        }
                    }
                    if (canPungNow && game.winner < 0) ElderButton(text = "碰", onClick = ::onPung, modifier = Modifier.weight(1f), minHeight = 44.dp)
                    if (canKongNow && game.winner < 0) ElderButton(text = "杠", onClick = ::onKong, modifier = Modifier.weight(1f), minHeight = 44.dp)
                    if (canWinNow && game.winner < 0) ElderButton(text = "和", onClick = ::onWinByDiscard, modifier = Modifier.weight(1f), minHeight = 44.dp, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White))
                    if (game.canConcealedKong(0) && game.winner < 0) ElderButton(text = "暗杠", onClick = ::onConcealedKong, modifier = Modifier.weight(1f), minHeight = 44.dp)
                }
            }
            Spacer(Modifier.height(2.dp))

            // Player hand: centered; newly drawn tile on the right with a gap
            key(tick) {
                val hand = game.hands[0].sortedBy { it.groupKey() }
                val drawn = if (game.hasDrawn) hand.last() else null
                val playable = if (drawn != null) hand.filter { it != drawn } else hand
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(56.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // played tiles (centered, small)
                    playable.forEach { t ->
                        TileFace(t, Modifier.size(32.dp, 44.dp).clickable { onDiscard(t) })
                    }
                    // gap + newly drawn tile (right side, slightly raised)
                    if (drawn != null) {
                        Spacer(Modifier.width(18.dp))
                        TileFace(
                            drawn,
                            Modifier.size(32.dp, 46.dp).offset(y = (-6).dp).clickable { onDiscard(drawn) },
                        )
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
                    Text(if (game.winner == 0) "🎉 你胡了！" else "😊 电脑${game.winner} 胡了", fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    if (game.winner == 0) {
                        game.winFans.forEach { f -> Text("${f.name}  ${f.fan}番", fontSize = 15.sp) }
                        Spacer(Modifier.height(6.dp))
                        Text("共 ${FanCalculator.total(game.winFans)} 番", fontSize = 20.sp, color = Color(0xFFB23A3A))
                    } else {
                        Text("电脑和牌，继续加油！", fontSize = 15.sp)
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

/** Tile wall stack (backs) with remaining count */
@Composable
private fun WallDisplay(count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
            repeat(3) { TileBack(Modifier.size(18.dp, 24.dp)) }
        }
        Spacer(Modifier.width(4.dp))
        Text("$count", fontSize = 11.sp, color = Color(0xFFFFE082))
    }
}

/** Opponent discard history row (up to 8 recent) */
@Composable
private fun OpponentDiscardRow(discards: List<Tile>) {
    if (discards.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        discards.takeLast(8).forEach { t -> TileFace(t, Modifier.size(14.dp, 20.dp)) }
    }
}

/** Dice face */
@Composable
private fun DieFace(value: Int) {
    Surface(shape = RoundedCornerShape(4.dp), color = Color.White) {
        Box(Modifier.size(22.dp).padding(2.dp), contentAlignment = Alignment.Center) {
            Text("$value", fontSize = 13.sp, color = Color(0xFF2E2A25))
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
