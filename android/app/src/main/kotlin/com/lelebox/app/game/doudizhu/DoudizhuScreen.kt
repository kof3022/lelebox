package com.lelebox.app.game.doudizhu

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
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
            delay(650)
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
        if (game.playerPass() == PlayResult.OK) {
            Sfx.click(context)
        } else {
            Sfx.fail(context)
        }
        refresh()
    }

    fun onHint() {
        // 自动选一手最小的可出组合
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

    val status = when {
        game.phase == 0 -> "谁来当地主？"
        game.over -> if (game.winner == 0) "你赢啦！" else if (game.isLandlord(game.winner)) "地主赢了" else "农民赢了"
        game.current == 0 -> if (game.lastCombo == null) "该你出牌（自由出）" else "该你出牌"
        else -> "AI 思考中…"
    }

    val lastText = game.lastCombo?.let { c ->
        val name = when (c.type) {
            ComboType.SINGLE -> "单张"; ComboType.PAIR -> "对子"; ComboType.TRIPLE -> "三张"
            ComboType.TRIPLE_ONE -> "三带一"; ComboType.TRIPLE_TWO -> "三带二"
            ComboType.STRAIGHT -> "顺子"; ComboType.PAIR_STRAIGHT -> "连对"
            ComboType.PLANE -> "飞机"; ComboType.PLANE_WING -> "飞机带翅"
            ComboType.FOUR_TWO -> "四带二"; ComboType.BOMB -> "炸弹"; ComboType.ROCKET -> "王炸"
        }
        "上家出了：$name ${if (c.type == ComboType.STRAIGHT || c.type == ComboType.PAIR_STRAIGHT || c.type == ComboType.PLANE || c.type == ComboType.PLANE_WING) "" else Doudizhu.rankText(c.mainRank)}"
    } ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // AI 与状态区
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("电脑1：${game.hands[1].size} 张", style = MaterialTheme.typography.titleMedium)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(status, style = MaterialTheme.typography.titleMedium, color = if (game.current == 0 && !game.over && game.phase == 1) ElderGreen else MaterialTheme.colorScheme.onSurface)
                if (lastText.isNotEmpty()) {
                    Text(lastText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("电脑2：${game.hands[2].size} 张", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))

        // 底牌/叫地主
        if (game.phase == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                game.bottom.forEach { c ->
                    MiniCard(c, Modifier.size(46.dp, 64.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("底牌在上面，你要当地主吗？", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ElderButton(text = "叫地主", onClick = { onBid(true) }, modifier = Modifier.weight(1f))
                ElderButton(text = "不叫", onClick = { onBid(false) }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("不叫的话电脑会决定，没人要就重新发牌", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.weight(1f))

        // 手牌区
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .height(92.dp),
        ) {
            game.hands[0].forEachIndexed { i, card ->
                HandCard(
                    card = card,
                    selected = card in selected,
                    onClick = {
                        selected = if (card in selected) selected - card else selected + card
                        Sfx.click(context)
                        refresh()
                    },
                    modifier = Modifier.offset(x = (-i * 8).dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ElderButton(text = "提示", onClick = ::onHint, modifier = Modifier.weight(1f), minHeight = 56.dp)
            ElderButton(
                text = "出牌",
                onClick = ::onPlay,
                modifier = Modifier.weight(1f),
                minHeight = 56.dp,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            ElderButton(
                text = "不出",
                onClick = ::onPass,
                modifier = Modifier.weight(1f),
                minHeight = 56.dp,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
            ElderButton(text = "重发", onClick = ::onNewDeal, modifier = Modifier.weight(1f), minHeight = 56.dp)
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

@Composable
private fun HandCard(card: Card, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(46.dp, 78.dp)
            .offset(y = if (selected) (-16).dp else 0.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFC9BEAB), shape)
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
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFC9BEAB), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Doudizhu.cardText(card),
            fontSize = 12.sp,
            color = if (Doudizhu.isRed(card)) Color(0xFFC62828) else Color(0xFF2E2A25),
        )
    }
}
