package com.lelebox.app.game.doudizhu

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelebox.app.audio.Sfx
import com.lelebox.app.ui.ElderButton
import com.lelebox.app.ui.ElderGreen
import kotlinx.coroutines.delay

/** 斗地主入口 */
@Composable
fun DoudizhuScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val game = remember { DoudizhuGame() }
    var tick by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Set<Card>>(emptySet()) }

    fun refresh() = tick++

    // AI 回合驱动
    LaunchedEffect(tick) {
        while (game.phase == 1 && !game.over && game.current != 0) {
            delay(700)
            game.aiAct()
            refresh()
        }
    }

    fun onBid(call: Boolean) {
        game.playerBid(call)
        Sfx.click(context)
        refresh()
    }

    fun onPlay() {
        if (selected.isEmpty()) return
        val res = game.playerPlay(selected.toList())
        selected = emptySet()
        when (res) {
            PlayResult.OK -> Sfx.click(context)
            PlayResult.INVALID -> Sfx.fail(context)
            PlayResult.GAME_OVER -> Sfx.success(context)
        }
        refresh()
    }

    fun onPass() {
        if (game.playerPass() == PlayResult.OK) Sfx.click(context) else Sfx.fail(context)
        refresh()
    }

    fun onHint() {
        val combos = game.findCombos(game.hands[0])
        val prev = game.lastCombo
        val target = if (prev == null) {
            combos.filter { it.type != ComboType.BOMB && it.type != ComboType.ROCKET }
                .minByOrNull { it.mainRank * 10 + it.cards.size }
                ?: combos.minByOrNull { it.mainRank }
        } else {
            combos.filter { canBeat(prev, it) }.minByOrNull { it.mainRank }
        }
        if (target != null) {
            selected = target.cards.toSet()
            Sfx.click(context)
        } else {
            Sfx.fail(context)
        }
        refresh()
    }

    fun onNewDeal() {
        game.newDeal()
        selected = emptySet()
        Sfx.click(context)
        refresh()
    }

    val role = when {
        game.landlord == -1 -> ""
        game.isLandlord(0) -> "你是地主"
        else -> "你是农民"
    }

    val status = when {
        game.phase == 0 -> "谁来当地主？"
        game.over -> if (game.winner == 0) "你赢啦！" else if (game.isLandlord(game.winner)) "地主赢了" else "农民赢了"
        game.current == 0 -> "该你出牌"
        else -> "电脑思考中…"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 对手面板（左：电脑1，右：电脑2）
        key(tick) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OpponentPanel(
                    name = "电脑1",
                    role = playerRoleText(game, 1),
                    count = game.hands[1].size,
                    active = game.phase == 1 && !game.over && game.current == 1,
                    last = game.lastPlays[1],
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OpponentPanel(
                    name = "电脑2",
                    role = playerRoleText(game, 2),
                    count = game.hands[2].size,
                    active = game.phase == 1 && !game.over && game.current == 2,
                    last = game.lastPlays[2],
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(status, style = MaterialTheme.typography.titleMedium, color = if (game.current == 0 && !game.over && game.phase == 1) ElderGreen else MaterialTheme.colorScheme.onSurface)
        Text(role, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(6.dp))

        // 中央出牌区：三家各自出的牌
        key(tick) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF3EDE2),
            ) {
                Column(Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
                    PlayRow("电脑1", game.lastPlays[1])
                    PlayRow("你", game.lastPlays[0])
                    PlayRow("电脑2", game.lastPlays[2])
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // 叫地主阶段
        if (game.phase == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                game.bottom.forEach { c -> MiniCard(c, Modifier.size(44.dp, 62.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            Text("底牌，你要当地主吗？", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ElderButton(text = "叫地主", onClick = { onBid(true) }, modifier = Modifier.weight(1f))
                ElderButton(text = "不叫", onClick = { onBid(false) }, modifier = Modifier.weight(1f))
            }
        }

        // 己方手牌（全部可见、逐个可点）
        if (game.phase == 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(game.hands[0]) { _, card ->
                    HandCard(
                        card = card,
                        selected = card in selected,
                        onClick = {
                            selected = if (card in selected) selected - card else selected + card
                            Sfx.click(context)
                            refresh()
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ElderButton(text = "提示", onClick = ::onHint, modifier = Modifier.weight(1f), minHeight = 56.dp)
                ElderButton(text = "出牌", onClick = ::onPlay, modifier = Modifier.weight(1f), minHeight = 56.dp)
                ElderButton(text = "不出", onClick = ::onPass, modifier = Modifier.weight(1f), minHeight = 56.dp)
                ElderButton(text = "重发", onClick = ::onNewDeal, modifier = Modifier.weight(1f), minHeight = 56.dp)
            }
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
            Text(if (game.winner == 0) "🎉" else "😊", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    game.winner == 0 -> "你赢啦！"
                    game.isLandlord(game.winner) -> "地主赢了，下次加油！"
                    else -> "农民赢了！"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            ElderButton(text = "再来一局", onClick = ::onNewDeal, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun playerRoleText(game: DoudizhuGame, p: Int): String = when {
    game.landlord == -1 -> "—"
    game.isLandlord(p) -> "地主"
    else -> "农民"
}

/** 对手面板：名字 + 身份 + 手牌数 + 牌背堆 + 当前行动高亮 */
@Composable
private fun OpponentPanel(
    name: String,
    role: String,
    count: Int,
    active: Boolean,
    last: Combo?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (active) Color(0xFFE5EFE3) else Color(0xFFF3EDE2),
        border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, if (active) ElderGreen else Color(0xFFE4DBCB)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 牌背堆
            Box(
                modifier = Modifier
                    .size(width = 26.dp, height = 36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFB0453E)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$count", fontSize = 12.sp, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("$role · $count 张", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                last?.let { c ->
                    Text(
                        comboText(c),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB0453E),
                    )
                }
            }
        }
    }
}

private fun comboText(c: Combo): String {
    val name = when (c.type) {
        ComboType.SINGLE -> "单张"; ComboType.PAIR -> "对子"; ComboType.TRIPLE -> "三张"
        ComboType.TRIPLE_ONE -> "三带一"; ComboType.TRIPLE_TWO -> "三带二"
        ComboType.STRAIGHT -> "顺子"; ComboType.PAIR_STRAIGHT -> "连对"
        ComboType.PLANE -> "飞机"; ComboType.PLANE_WING -> "飞机带翅"
        ComboType.FOUR_TWO -> "四带二"; ComboType.BOMB -> "炸弹"; ComboType.ROCKET -> "王炸"
    }
    val seq = c.type == ComboType.STRAIGHT || c.type == ComboType.PAIR_STRAIGHT || c.type == ComboType.PLANE || c.type == ComboType.PLANE_WING
    return if (seq) name else "$name ${Doudizhu.rankText(c.mainRank)}"
}

/** 出牌区的一行：谁 + 出的牌（小牌） */
@Composable
private fun PlayRow(label: String, combo: Combo?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
        if (combo == null) {
            Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                combo.cards.take(12).forEach { c ->
                    MiniCard(c, Modifier.size(26.dp, 36.dp))
                }
                if (combo.cards.size > 12) {
                    Text("…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun HandCard(card: Card, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(44.dp, 74.dp)
            .offset(y = if (selected) (-16).dp else 0.dp)
            .clip(shape)
            .background(Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) ElderGreen else Color(0xFFC9BEAB), shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${Doudizhu.cardText(card)}${if (selected) "，已选中" else ""}" },
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            Doudizhu.rankText(card.rank),
            fontSize = 14.sp,
            color = if (Doudizhu.isRed(card)) Color(0xFFC62828) else Color(0xFF2E2A25),
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun MiniCard(card: Card, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFC9BEAB), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Doudizhu.cardText(card),
            fontSize = 10.sp,
            maxLines = 1,
            color = if (Doudizhu.isRed(card)) Color(0xFFC62828) else Color(0xFF2E2A25),
        )
    }
}
