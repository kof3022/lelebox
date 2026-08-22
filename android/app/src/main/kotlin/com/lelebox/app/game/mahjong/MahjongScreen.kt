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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelebox.app.R
import com.lelebox.app.audio.Sfx
import com.lelebox.app.ui.ElderButton
import com.lelebox.app.ui.ElderGreen
import kotlinx.coroutines.delay

/** Mahjong: landscape 4-player table. Minimal UI, only essentials. */
@Composable
fun MahjongScreen(
    onBack: () -> Unit = {},
    onHelp: () -> Unit = {},
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
    var hintTile by remember { mutableStateOf<Tile?>(null) }

    // Landscape fullscreen
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
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
    fun onHint() { hintTile = game.pickDiscardForPlayer(); Sfx.click(context); refresh() }
    fun onWinByDiscard() {
        val d = game.lastDiscard ?: return
        if (game.winByDiscard(0, d)) { Sfx.success(context); showFanDialog = true; refresh() }
    }

    LaunchedEffect(tick) {
        while (game.winner < 0 && !game.exhausted && game.current != 0) {
            delay(700)
            game.aiTurn(game.current)
            refresh()
        }
    }
    LaunchedEffect(tick) { if (game.winner >= 0) showFanDialog = true }

    // Landscape 4-player layout
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF2C4A3B))) {
        Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            // Top thin bar: only back + minimal status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ElderButton(text = "←", onClick = onBack, minHeight = 40.dp, modifier = Modifier.width(56.dp))
                Text(
                    when {
                        game.winner >= 0 -> "和牌！"
                        game.exhausted -> "流局"
                        game.current == 0 -> "轮到你"
                        else -> "电脑摸打中…"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                ElderButton(text = "帮助", onClick = onHelp, minHeight = 40.dp)
            }
            Spacer(Modifier.height(4.dp))

            // Main area: top opponent (seat 2 across), middle row (seat 3 left, seat 1 right), bottom player
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Seat 2 (across, top) - hand back row centered
                OpponentRow(
                    name = "电脑2", count = game.hands[2].size,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                // Seat 3 (left) - vertical hand back
                OpponentCol(
                    name = "电脑3", count = game.hands[3].size,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                // Seat 1 (right) - vertical hand back
                OpponentCol(
                    name = "电脑1", count = game.hands[1].size,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
                // Center: last discard + wall count
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("牌墙 ${game.wall.size}", fontSize = 14.sp, color = Color(0xFFE8F0E4))
                    Spacer(Modifier.height(4.dp))
                    if (game.lastDiscard != null) {
                        TileFace(game.lastDiscard!!, Modifier.size(46.dp, 64.dp))
                    } else if (game.hasDrawn) {
                        Text("请打出一张牌", fontSize = 16.sp, color = Color(0xFFFFE082))
                    }
                }
                // Player exposed melds (bottom center, above hand)
                if (game.exposed[0].isNotEmpty()) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-64).dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        game.exposed[0].forEach { m ->
                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                meldTiles(m).forEach { t -> TileFace(t, Modifier.size(26.dp, 36.dp)) }
                            }
                        }
                    }
                }
            }

            // Action buttons (only what is available)
            key(tick) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (game.winner < 0 && !game.exhausted && game.current == 0) {
                        if (!game.hasDrawn) {
                            ElderButton(text = "摸牌", onClick = ::onDraw, modifier = Modifier.weight(1f), minHeight = 46.dp)
                        } else {
                            if (game.canWin(0)) ElderButton(text = "和牌", onClick = ::onSelfWin, modifier = Modifier.weight(1f), minHeight = 46.dp, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White))
                            else ElderButton(text = "提示", onClick = ::onHint, modifier = Modifier.weight(1f), minHeight = 46.dp)
                        }
                    }
                    if (canPungNow && game.winner < 0) ElderButton(text = "碰", onClick = ::onPung, modifier = Modifier.weight(1f), minHeight = 46.dp)
                    if (canKongNow && game.winner < 0) ElderButton(text = "杠", onClick = ::onKong, modifier = Modifier.weight(1f), minHeight = 46.dp)
                    if (canWinNow && game.winner < 0) ElderButton(text = "和", onClick = ::onWinByDiscard, modifier = Modifier.weight(1f), minHeight = 46.dp, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White))
                    if (game.canConcealedKong(0) && game.winner < 0) ElderButton(text = "暗杠", onClick = ::onConcealedKong, modifier = Modifier.weight(1f), minHeight = 46.dp)
                }
            }
            Spacer(Modifier.height(4.dp))

            // Player hand (image tiles, tappable)
            key(tick) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(58.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    game.hands[0].sortedBy { it.groupKey() }.forEach { t ->
                        TileFace(
                            t,
                            Modifier
                                .size(36.dp, 50.dp)
                                .border(if (hintTile == t) 3.dp else 0.dp, if (hintTile == t) Color(0xFFF0A93C) else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { onDiscard(t) },
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
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (game.winner == 0) "🎉 你胡了！" else "😊 电脑${game.winner} 胡了", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    if (game.winner == 0) {
                        game.winFans.forEach { f ->
                            Text("${f.name}  ${f.fan}番", style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("共 ${FanCalculator.total(game.winFans)} 番", style = MaterialTheme.typography.titleLarge, color = Color(0xFFB23A3A))
                    } else {
                        Text("电脑和牌，继续加油！", style = MaterialTheme.typography.bodyLarge)
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

/** Top/bottom opponent: horizontal row of tile backs */
@Composable
private fun OpponentRow(name: String, count: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
            repeat(minOf(count, 14)) { TileBack(Modifier.size(16.dp, 22.dp)) }
        }
        Spacer(Modifier.height(2.dp))
        Text(name, fontSize = 11.sp, color = Color(0xFFE8F0E4))
    }
}

/** Side opponent: vertical column of tile backs */
@Composable
private fun OpponentCol(name: String, count: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, fontSize = 11.sp, color = Color(0xFFE8F0E4))
        Spacer(Modifier.height(2.dp))
        Column(verticalArrangement = Arrangement.spacedBy((-3).dp)) {
            repeat(minOf(count, 6)) { TileBack(Modifier.size(22.dp, 16.dp)) }
        }
    }
}

private fun meldTiles(m: Meld): List<Tile> = when (m) {
    is Meld.Chow -> m.tiles
    is Meld.Pung -> m.tiles
    is Meld.Kong -> m.tiles
    is Meld.Pair -> m.tiles
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

/** Pick a discardable tile for the player (hint) */
fun MahjongGame.pickDiscardForPlayer(): Tile? {
    val hand = hands[0].sortedBy { it.groupKey() }
    return hand.maxByOrNull { t ->
        var s = hand.count { it == t } * 10
        if (t.suit < 3) {
            if (!hand.any { it.suit == t.suit && it.rank == t.rank - 1 }) s += 5
            if (!hand.any { it.suit == t.suit && it.rank == t.rank + 1 }) s += 5
        } else s += 3
        s
    }
}
