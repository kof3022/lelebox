package com.lelebox.app.game.mahjong

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelebox.app.audio.Sfx
import com.lelebox.app.ui.ElderButton
import com.lelebox.app.ui.ElderGreen
import kotlinx.coroutines.delay

/** Mahjong game screen: player vs 3 AI, GuoBiao rules, fan display on win */
@Composable
fun MahjongScreen(
    onBack: () -> Unit = {},
    onHelp: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val game = remember { MahjongGame() }
    var tick by remember { mutableIntStateOf(0) }
    var showFanDialog by remember { mutableStateOf(false) }
    var chowChoice by remember { mutableStateOf<List<List<Tile>>?>(null) }
    var canPungNow by remember { mutableStateOf(false) }
    var canKongNow by remember { mutableStateOf(false) }
    var canWinNow by remember { mutableStateOf(false) }
    var hintTile by remember { mutableStateOf<Tile?>(null) }

    fun refresh() { tick++ }

    fun checkActions() {
        val d = game.lastDiscard
        canPungNow = d != null && game.canPung(0)
        canKongNow = d != null && game.canKong(0)
        canWinNow = d != null && game.winByDiscard(0, d) // dry-run check? careful: mutates on success
        canWinNow = d != null && Mahjong.findWins(game.hands[0].toList() + d).isNotEmpty()
        chowChoice = if (d != null) game.chowOptions(0, d) else null
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
        if (game.selfWin(0)) {
            Sfx.success(context)
            showFanDialog = true
            refresh()
        }
    }

    fun onPung() {
        if (game.doPung(0)) { Sfx.click(context); refresh() }
    }

    fun onKong() {
        if (game.doKong(0)) { Sfx.click(context); refresh() }
    }

    fun onConcealedKong() {
        if (game.canConcealedKong(0) && game.doConcealedKong(0)) { Sfx.click(context); refresh() }
    }

    fun onHint() {
        // find a tile whose discard improves hand (simplified: highlight discardable isolated tile)
        hintTile = game.pickDiscardForPlayer()
        Sfx.click(context)
        refresh()
    }

    // AI turns run when it is their turn
    LaunchedEffect(tick) {
        while (game.winner < 0 && !game.exhausted && game.current != 0) {
            delay(700)
            game.aiTurn(game.current)
            refresh()
        }
    }

    // After player acts, let AI act until player's turn again
    LaunchedEffect(tick) {
        if (game.winner >= 0) {
            showFanDialog = true
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: back/help + status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ElderButton(text = "退出", onClick = onBack, minHeight = 44.dp, modifier = Modifier.width(80.dp))
            Text(
                if (game.winner >= 0) "和牌！" else if (game.exhausted) "流局" else if (game.current == 0) "轮到你" else "电脑思考中",
                style = MaterialTheme.typography.titleMedium,
            )
            ElderButton(text = "帮助", onClick = onHelp, minHeight = 44.dp)
        }
        Spacer(Modifier.height(8.dp))

        // Wall count + opponents
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("牌墙 ${game.wall.size} 张", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("电脑剩余牌：${game.hands[1].size} / ${game.hands[2].size} / ${game.hands[3].size}", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))

        // Opponent hand backs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (p in 1..3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                        repeat(5) { TileBack(Modifier.size(14.dp, 20.dp)) }
                    }
                    Text("电脑$p", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Player exposed melds (chow/pung/kong)
        if (game.exposed[0].isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                game.exposed[0].forEach { m ->
                    val tiles: List<Tile> = when (m) {
                        is Meld.Chow -> m.tiles
                        is Meld.Pung -> m.tiles
                        is Meld.Kong -> m.tiles
                        is Meld.Pair -> m.tiles
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        tiles.forEach { t -> TileFace(t, Modifier.size(24.dp, 34.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Center: last discard highlighted
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (game.lastDiscard != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("上家打出", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TileFace(game.lastDiscard!!, Modifier.size(44.dp, 62.dp))
                }
            } else if (game.hasDrawn) {
                Text("请打出一张牌", style = MaterialTheme.typography.titleMedium, color = ElderGreen)
            } else {
                Text("请摸牌", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Action buttons row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!game.hasDrawn && game.current == 0 && game.winner < 0 && !game.exhausted) {
                ElderButton(text = "摸牌", onClick = ::onDraw, modifier = Modifier.weight(1f), minHeight = 50.dp)
            } else if (game.hasDrawn && game.current == 0 && game.winner < 0) {
                ElderButton(text = "和牌", onClick = ::onSelfWin, modifier = Modifier.weight(1f), minHeight = 50.dp,
                    colors = if (game.canWin(0)) androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White) else androidx.compose.material3.ButtonDefaults.buttonColors())
                ElderButton(text = "提示", onClick = ::onHint, modifier = Modifier.weight(1f), minHeight = 50.dp)
            }
            if (canPungNow && game.winner < 0) ElderButton(text = "碰", onClick = ::onPung, modifier = Modifier.weight(1f), minHeight = 50.dp)
            if (canKongNow && game.winner < 0) ElderButton(text = "杠", onClick = ::onKong, modifier = Modifier.weight(1f), minHeight = 50.dp)
            if (game.canConcealedKong(0) && game.winner < 0) ElderButton(text = "暗杠", onClick = ::onConcealedKong, modifier = Modifier.weight(1f), minHeight = 50.dp)
            if (canWinNow && game.winner < 0) ElderButton(text = "和牌", onClick = { if (game.winByDiscard(0, game.lastDiscard!!)) { Sfx.success(context); showFanDialog = true; refresh() } }, modifier = Modifier.weight(1f), minHeight = 50.dp, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White))
            if (chowChoice != null && !chowChoice.isNullOrEmpty() && game.winner < 0) {
                ElderButton(text = "吃", onClick = { chowChoice!!.firstOrNull()?.let { game.doChow(it); Sfx.click(context); refresh() } }, modifier = Modifier.weight(1f), minHeight = 50.dp)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Player hand
        key(tick) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                game.hands[0].sortedBy { it.groupKey() }.forEach { t ->
                    val isHint = hintTile == t
                    TileFace(
                        t,
                        Modifier.size(36.dp, 52.dp)
                            .border(if (isHint) 3.dp else 1.dp, if (isHint) Color(0xFFF0A93C) else Color(0xFFC9BEAB), RoundedCornerShape(4.dp))
                            .clickable { onDiscard(t) },
                    )
                }
            }
        }
    }

    // Win fan dialog
    if (showFanDialog && game.winner >= 0) {
        Surface(
            modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).padding(20.dp),
            shape = RoundedCornerShape(0.dp),
            color = Color.Transparent,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (game.winner == 0) "🎉 你胡了！" else "😊 电脑${game.winner} 胡了", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(10.dp))
                        Text("和牌 ${game.winScheme?.let { if (it.isSevenPairs) "（七对）" else "（普通）" } ?: ""}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                        if (game.winner == 0) {
                            game.winFans.forEach { f ->
                                Text("${f.name}  ${f.fan}番", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("共 ${FanCalculator.total(game.winFans)} 番", style = MaterialTheme.typography.titleLarge, color = Color(0xFFB23A3A))
                        } else {
                            Text("电脑和牌，继续加油！", style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.height(24.dp))
                        ElderButton(text = "再来一局", onClick = { game.newRound(); showFanDialog = false; refresh() }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        ElderButton(text = "退出", onClick = onBack, modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface))
                    }
                }
            }
        }
    }

    LaunchedEffect(tick) {
        if (game.current == 0 && game.lastDiscard != null && game.winner < 0) checkActions()
    }
}

/** Simple tile face: white with label */
@Composable
private fun TileFace(tile: Tile, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFC9BEAB), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            tile.label(),
            fontSize = 13.sp,
            color = if (tile.suit == 4 && tile.rank == 1) Color(0xFFC62828) else Color(0xFF2E2A25),
            maxLines = 1,
        )
    }
}

/** Tile back */
@Composable
private fun TileBack(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF2E5F9E), RoundedCornerShape(3.dp))
            .border(1.dp, Color(0xFF9EC1E8), RoundedCornerShape(3.dp)),
    )
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
