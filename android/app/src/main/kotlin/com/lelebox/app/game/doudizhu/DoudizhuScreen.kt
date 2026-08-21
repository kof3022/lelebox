package com.lelebox.app.game.doudizhu

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lelebox.app.R
import com.lelebox.app.audio.Sfx
import com.lelebox.app.ui.ElderButton
import com.lelebox.app.ui.ElderGreen
import com.lelebox.app.ui.ElderTopBar
import kotlinx.coroutines.delay

/** 斗地主入口：选难度（竖屏）→ 对局（横屏沉浸全屏） */
@Composable
fun DoudizhuScreen(
    onBack: () -> Unit = {},
    onHelp: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var level by remember { mutableStateOf<DoudizhuLevel?>(null) }

    // 方向与沉浸：难度页竖屏（显示系统栏）；对局横屏沉浸（隐藏系统栏）
    LaunchedEffect(level != null) {
        val act = activity ?: return@LaunchedEffect
        if (level == null) {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            WindowCompat.setDecorFitsSystemWindows(act.window, true)
            WindowCompat.getInsetsController(act.window, act.window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        } else {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            WindowCompat.setDecorFitsSystemWindows(act.window, false)
            val c = WindowCompat.getInsetsController(act.window, act.window.decorView)
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    // 焦点回归（对话框关闭等）后重新隐藏系统栏
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo.isWindowFocused, level != null) {
        val act = activity
        if (level != null && windowInfo.isWindowFocused && act != null) {
            WindowCompat.getInsetsController(act.window, act.window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    // 离开斗地主时恢复默认方向与系统栏
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { act ->
                WindowCompat.setDecorFitsSystemWindows(act.window, true)
                WindowCompat.getInsetsController(act.window, act.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    when (val lv = level) {
        null -> LevelSelect(onStart = { level = it }, onBack = onBack, onHelp = onHelp, modifier = modifier)
        else -> GameTable(lv, onBack = onBack, onBackToLevels = { level = null }, modifier = modifier)
    }
}

/** 从 Context 向上找到 Activity（LocalContext 可能是 ContextThemeWrapper） */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun LevelSelect(
    onStart: (DoudizhuLevel) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 头部与找不同等游戏统一：ElderTopBar（标题居中 + ← 返回 + 帮助）
        ElderTopBar(title = "斗地主", onBack = onBack, onRight = onHelp, rightText = "帮助")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // 图标 + 标题 + 说明（与找不同难度选择同一风格）
            Image(
                painter = painterResource(R.drawable.ic_game_doudizhu),
                contentDescription = "斗地主",
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text("斗地主", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                "叫地主后先出完牌就赢。\n选个电脑水平吧。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))
            // 难度按钮竖排大按钮（与找不同一致）
            DoudizhuLevel.entries.forEach { lv ->
                ElderButton(
                    text = "${lv.label}（${lv.hint}）",
                    onClick = { onStart(lv) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GameTable(
    level: DoudizhuLevel,
    onBack: () -> Unit,
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

    // AI 回合 + 托管驱动：key 含 autoPlay，开启托管立即接管出牌（无需先手动出一张）
    LaunchedEffect(tick, autoPlay) {
        while (game.phase == 1 && !game.over && (game.current != 0 || autoPlay)) {
            delay(700)
            if (game.current == 0 && autoPlay) {
                // 托管：复用 AI 策略（aiLead/aiFollow），与电脑同级智慧
                val target = if (game.lastCombo == null) game.aiLead(0) else game.aiFollow(0)
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

    fun onNewDeal() {
        game.newDeal()
        selected = emptySet()
        Sfx.click(context)
        refresh()
    }

    // 全屏牌桌：即梦牌桌背景图铺满 + 半透明深绿叠层保证文字可读 + 金色包边
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
    // 牌桌背景图（table_felt.png，即梦生成），contentScale 铺满
    Image(
        painter = painterResource(R.drawable.table_felt),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(17.dp)),
        contentScale = ContentScale.Crop,
    )
    // 半透明深绿叠层：保证牌面/文字在图片上仍清晰
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x59000000)),
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 主区：左对手 | 中央桌面 | 右对手
        key(tick) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpponentPanel(
                    name = "孔子",
                    role = playerRoleText(game, 1),
                    count = game.hands[1].size,
                    backCount = game.hands[1].size,
                    active = game.phase == 1 && !game.over && game.current == 1,
                    modifier = Modifier.width(112.dp),
                )
                // 中央桌面（牌桌已全屏，此处为透明出牌区）
                Table(
                    game = game,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                OpponentPanel(
                    name = "庄子",
                    role = playerRoleText(game, 2),
                    count = game.hands[2].size,
                    backCount = game.hands[2].size,
                    active = game.phase == 1 && !game.over && game.current == 2,
                    modifier = Modifier.width(112.dp),
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

        // 手牌：发牌后（叫地主阶段）即可看到自己的牌；出牌阶段同样展示、可点；整体横向居中
        // 注：不用 horizontalScroll——它会 clip 到容器边界，旋转角会被截断；手牌最多 20 张放得下
        if (game.phase == 0 || game.phase == 1) {
            key(tick) {
                Row(
                    modifier = Modifier.height(78.dp),
                    horizontalArrangement = Arrangement.spacedBy((-7).dp),
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
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // 出牌按钮（仅出牌阶段）：退出/重发小按钮与中间三按钮分开
        if (game.phase == 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ElderButton(text = "退出", onClick = onBack, modifier = Modifier.width(100.dp), minHeight = 48.dp)
                Spacer(Modifier.width(10.dp))
                ElderButton(text = "出牌", onClick = ::onPlay, modifier = Modifier.weight(1f), minHeight = 54.dp)
                ElderButton(text = "不出", onClick = ::onPass, modifier = Modifier.weight(1f), minHeight = 54.dp)
                ElderButton(
                    text = if (autoPlay) "托管：开" else "托管：关",
                    onClick = { autoPlay = !autoPlay },
                    modifier = Modifier.weight(1f),
                    minHeight = 54.dp,
                )
                Spacer(Modifier.width(10.dp))
                ElderButton(text = "重发", onClick = ::onNewDeal, modifier = Modifier.width(100.dp), minHeight = 48.dp)
            }
        }
    }
    // 遮罩在 Box 内最后声明 → 显示在最上层（赢=金 / 输=红横幅）；key(tick) 保证 game.over 变化时强制重组
    key(tick) {
        if (game.over) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xB3000000)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val win = game.winner == 0
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (win) Color(0xFFF0A93C) else Color(0xFFC0392B),
                    border = androidx.compose.foundation.BorderStroke(3.dp, if (win) Color(0xFFFFE082) else Color(0xFFE57373)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (win) "🎉" else "😊", fontSize = 52.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when {
                                win -> "你赢啦！"
                                game.isLandlord(game.winner) -> "地主赢了"
                                else -> "农民赢了"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (win) Color(0xFF4A3200) else Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (win) "打得真好，再来一局？" else "下次加油！",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (win) Color(0xFF6B4A00) else Color(0xFFFFE0E0),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ElderButton(text = "再来一局", onClick = ::onNewDeal, modifier = Modifier.weight(1f))
                ElderButton(text = "换难度", onClick = onBackToLevels, modifier = Modifier.weight(1f))
            }
        }
    }
    }
    }
    }

private fun playerRoleText(game: DoudizhuGame, p: Int): String = when {
    game.landlord == -1 -> "—"
    game.isLandlord(p) -> "地主"
    else -> "农民"
}

/** 中央出牌区：底牌 + 三家出的牌（孔子左上/庄子右上/你中下）；背景透明（牌桌已全屏） */
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
        // 孔子出的牌（左上）
        TablePlay(game, 1, Modifier.align(Alignment.TopStart))
        // 庄子出的牌（右上）
        TablePlay(game, 2, Modifier.align(Alignment.TopEnd))
        // 你出的牌（中下）
        TablePlay(game, 0, Modifier.align(Alignment.BottomCenter))
    }
}

/** 一家出牌区：轮到玩家→"请出牌"（优先）；出牌→放大展示牌；本轮"过"→显示"过"；其余留空 */
@Composable
private fun TablePlay(game: DoudizhuGame, p: Int, modifier: Modifier = Modifier) {
    val combo = game.lastPlays[p]
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            // 轮到玩家：你的位置优先显示"请出牌"（即使上一轮牌面还在，也提示你出牌）
            game.phase == 1 && !game.over && p == 0 && game.current == 0 -> {
                Text("请出牌", fontSize = 24.sp, color = Color(0xFFFFE082), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            combo != null -> {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    combo.cards.take(8).forEach { c -> MiniCard(c, Modifier.size(40.dp, 56.dp)) }
                    if (combo.cards.size > 8) Text("…", fontSize = 16.sp, color = Color.White)
                }
            }
            game.phase == 1 && !game.over && game.passedThisRound[p] -> {
                Text("过", fontSize = 26.sp, color = Color(0xFFB9C9BF), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

/** 对手面板：头像+身份徽章同行、名字、手牌牌背、张数、行动高亮 */
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
            // 头像 + 身份徽章同一行（节省纵向空间）
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // 即梦头像：孔子 / 庄子（圆形裁切）
                Image(
                    painter = painterResource(if (name == "孔子") R.drawable.confucius else R.drawable.zhuangzi),
                    contentDescription = name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
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
            }
            Spacer(Modifier.height(3.dp))
            Text(name, style = MaterialTheme.typography.titleSmall)
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
