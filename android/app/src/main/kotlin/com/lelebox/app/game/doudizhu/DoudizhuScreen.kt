package com.lelebox.app.game.doudizhu

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

/** 斗地主入口：选难度 → 横屏对局（沉浸全屏，界面内自带紧凑返回/帮助） */
@Composable
fun DoudizhuScreen(
    onBack: () -> Unit = {},
    onHelp: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var level by remember { mutableStateOf<DoudizhuLevel?>(null) }
    when (val lv = level) {
        null -> LevelSelect(onStart = { level = it }, onBack = onBack, modifier = modifier)
        else -> GameTable(lv, onBack = onBack, onHelp = onHelp, onBackToLevels = { level = null }, modifier = modifier)
    }
}

@Composable
private fun LevelSelect(
    onStart: (DoudizhuLevel) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ElderButton(text = "←", onClick = onBack, minHeight = 44.dp, modifier = Modifier.width(72.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("🃏", fontSize = 44.sp)
        Spacer(Modifier.height(8.dp))
        Text("斗地主", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "叫地主后先出完牌就赢。选个电脑水平吧。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DoudizhuLevel.entries.forEach { lv ->
                ElderButton(
                    text = lv.label,
                    onClick = { onStart(lv) },
                    modifier = Modifier.weight(1f),
                    minHeight = 56.dp,
                )
            }
        }
    }
}

@Composable
private fun GameTable(
    level: DoudizhuLevel,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onBackToLevels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // 进入即自动发牌（先发牌、后叫地主）
    val game = remember(level) {
        DoudizhuGame().apply {
            this.level = level
            newDeal()
        }
    }
    var tick by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Set<Card>>(emptySet()) }
    var autoPlay by remember { mutableStateOf(false) }

    fun refresh() = tick++

    // AI 回合 + 托管驱动
    LaunchedEffect(tick) {
        while (game.phase == 1 && !game.over && (game.current != 0 || autoPlay)) {
            delay(700)
            if (game.current == 0 && autoPlay) {
                // 托管：AI 代玩家出牌
                val combos = game.findCombos(game.hands[0])
                val prev = game.lastCombo
                val target = if (prev == null) {
                    combos.filter { it.type != ComboType.BOMB && it.type != ComboType.ROCKET }
                        .minByOrNull { it.mainRank * 10 + it.cards.size }
                        ?: combos.minByOrNull { it.mainRank }
                } else {
                    combos.filter { canBeat(prev, it) }.minByOrNull { it.mainRank }
                }
                if (target != null) game.playerPlay(target.cards) else game.playerPass()
                selected = emptySet()
            } else {
                game.aiAct()
            }
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

    val status = when {
        game.phase == 0 -> "谁来当地主？"
        game.over -> if (game.winner == 0) "你赢啦！" else if (game.isLandlord(game.winner)) "地主赢了" else "农民赢了"
        game.current == 0 -> if (autoPlay) "托管中…" else "该你出牌"
        else -> "电脑思考中…"
    }
    val role = when {
        game.landlord == -1 -> ""
        game.isLandlord(0) -> "你是地主"
        else -> "你是农民"
    }

    // 全屏牌桌：深绿绒布渐变铺满整屏 + 金色包边（即梦牌桌背景图后补替换此背景）
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E3B2E), Color(0xFF2C4A3B), Color(0xFF163024)),
                ),
            )
            .border(3.dp, Color(0xFFC9A24B), RoundedCornerShape(20.dp))
            .padding(8.dp),
    ) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 状态行（半透明深色条保证可读，隐藏一切无关内容）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ElderButton(
                text = "←",
                onClick = onBack,
                minHeight = 44.dp,
                modifier = Modifier.width(64.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Text(
                status,
                style = MaterialTheme.typography.titleMedium,
                color = if (game.current == 0 && !game.over && game.phase == 1) Color(0xFF8BE08B) else Color.White,
                modifier = Modifier.weight(1f),
            )
            Text(role, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD8E4DC))
            ElderButton(
                text = "帮助",
                onClick = onHelp,
                minHeight = 44.dp,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                ),
            )
        }

        // 主区：左对手 | 中央桌面 | 右对手
        key(tick) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpponentPanel(
                    name = "电脑1",
                    role = playerRoleText(game, 1),
                    count = game.hands[1].size,
                    backCount = game.hands[1].size,
                    active = game.phase == 1 && !game.over && game.current == 1,
                    modifier = Modifier.width(120.dp),
                )
                // 中央桌面（牌桌已全屏，此处为透明出牌区）
                Table(
                    game = game,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                OpponentPanel(
                    name = "电脑2",
                    role = playerRoleText(game, 2),
                    count = game.hands[2].size,
                    backCount = game.hands[2].size,
                    active = game.phase == 1 && !game.over && game.current == 2,
                    modifier = Modifier.width(120.dp),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // 叫地主阶段：底牌扣在桌上（牌背朝上，看不到），叫地主后翻开并插入地主手牌
        if (game.phase == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(3) { CardBack(Modifier.size(34.dp, 48.dp)) }
                Text(
                    "底牌已扣，叫地主后翻开",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE8F0E4),
                    modifier = Modifier.weight(1f),
                )
                ElderButton(text = "叫地主", onClick = { onBid(true) }, minHeight = 52.dp)
                ElderButton(text = "不叫", onClick = { onBid(false) }, minHeight = 52.dp)
            }
            Spacer(Modifier.height(6.dp))
        }

        // 手牌：发牌后（叫地主阶段）即可看到自己的牌；出牌阶段同样展示、可点
        if (game.phase == 0 || game.phase == 1) {
            key(tick) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .height(78.dp),
                ) {
                    game.hands[0].forEachIndexed { i, card ->
                        val angle = ((i - (game.hands[0].size - 1) / 2f) * 3f).coerceIn(-18f, 18f)
                        HandCard(
                            card = card,
                            selected = card in selected,
                            angle = angle,
                            onClick = if (game.phase == 0) {
                                {}
                            } else {
                                {
                                    selected = if (card in selected) selected - card else selected + card
                                    Sfx.click(context)
                                    refresh()
                                }
                            },
                            modifier = Modifier.offset(x = (-i * 7).dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // 出牌按钮（仅出牌阶段）
        if (game.phase == 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ElderButton(text = "提示", onClick = ::onHint, modifier = Modifier.weight(1f), minHeight = 54.dp)
                ElderButton(text = "出牌", onClick = ::onPlay, modifier = Modifier.weight(1f), minHeight = 54.dp)
                ElderButton(text = "不出", onClick = ::onPass, modifier = Modifier.weight(1f), minHeight = 54.dp)
                ElderButton(
                    text = if (autoPlay) "托管：开" else "托管：关",
                    onClick = { autoPlay = !autoPlay },
                    modifier = Modifier.weight(1f),
                    minHeight = 54.dp,
                )
                ElderButton(text = "重发", onClick = ::onNewDeal, modifier = Modifier.weight(1f), minHeight = 54.dp)
            }
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
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ElderButton(text = "再来一局", onClick = ::onNewDeal, modifier = Modifier.weight(1f))
                ElderButton(text = "换难度", onClick = onBackToLevels, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun playerRoleText(game: DoudizhuGame, p: Int): String = when {
    game.landlord == -1 -> "—"
    game.isLandlord(p) -> "地主"
    else -> "农民"
}

/** 中央出牌区：底牌 + 三家出的牌（上家左上/下家右上/你中下）；背景透明（牌桌已全屏） */
@Composable
private fun Table(game: DoudizhuGame, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        // 底牌（顶部中间；叫地主后翻开，其余阶段隐藏）
        if (game.phase == 1) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                game.bottom.forEach { c -> MiniCard(c, Modifier.size(34.dp, 48.dp)) }
            }
        }
        // 上家出的牌（左上）
        TablePlay("电脑1", game.lastPlays[1], Modifier.align(Alignment.TopStart))
        // 下家出的牌（右上）
        TablePlay("电脑2", game.lastPlays[2], Modifier.align(Alignment.TopEnd))
        // 你出的牌（中下）
        TablePlay("你", game.lastPlays[0], Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TablePlay(label: String, combo: Combo?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color(0xFFE8F0E4))
        if (combo == null) {
            Text("—", fontSize = 14.sp, color = Color(0xFFB9C9BF))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                combo.cards.take(12).forEach { c -> MiniCard(c, Modifier.size(30.dp, 42.dp)) }
                if (combo.cards.size > 12) Text("…", fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

/** 对手面板：头像占位 + 身份徽章 + 手牌牌背 + 张数 + 行动高亮 */
@Composable
private fun OpponentPanel(
    name: String,
    role: String,
    count: Int,
    backCount: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (active) Color(0xE6E5EFE3) else Color(0xD9F3EDE2),
        border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, if (active) ElderGreen else Color(0xFFE4DBCB)),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 头像占位（即梦头像后替换）
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD8CFC2)),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (name == "电脑1") "👴" else "👵", fontSize = 20.sp)
            }
            Spacer(Modifier.height(3.dp))
            Text(name, style = MaterialTheme.typography.titleSmall)
            // 身份徽章
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (role == "地主") Color(0xFFF0A93C) else Color(0xFF8FB58F),
            ) {
                Text(
                    role,
                    fontSize = 11.sp,
                    color = if (role == "地主") Color(0xFF4A3200) else Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
            Spacer(Modifier.height(3.dp))
            // 对手手牌牌背（层叠一行，张数变化实时更新）
            if (backCount > 0) {
                Box(Modifier.width(88.dp).height(15.dp)) {
                    repeat(minOf(backCount, 14)) { i ->
                        CardBack(
                            Modifier
                                .size(10.dp, 15.dp)
                                .align(Alignment.CenterStart)
                                .offset(x = (i * 5.5f).dp),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
            }
            Text("剩余 $count 张", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 牌背：蓝色底 + 浅色内框（老花友好，高对比） */
@Composable
private fun CardBack(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF2E5F9E))
            .border(1.dp, Color(0xFF9EC1E8), shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(7.dp, 10.dp)
                .border(1.dp, Color(0xFF9EC1E8), RoundedCornerShape(1.dp)),
        )
    }
}

@Composable
private fun HandCard(card: Card, selected: Boolean, angle: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(40.dp, 68.dp)
            .graphicsLayer {
                rotationZ = angle
                translationY = if (selected) -16.dp.toPx() else 0f
            }
            .clip(shape)
            .background(Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) ElderGreen else Color(0xFFC9BEAB), shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${Doudizhu.cardText(card)}${if (selected) "，已选中" else ""}" },
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            Doudizhu.rankText(card.rank),
            fontSize = 13.sp,
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
            fontSize = 9.sp,
            maxLines = 1,
            color = if (Doudizhu.isRed(card)) Color(0xFFC62828) else Color(0xFF2E2A25),
        )
    }
}
