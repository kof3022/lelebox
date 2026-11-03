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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    onHelp: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = androidx.compose.ui.platform.LocalContext.current.findActivity()
    val game = remember { PaiJiuGame() }
    var tick by remember { mutableIntStateOf(0) }
    // 玩家当前选中的牌（多选：对子/副子需选 2-4 张；用索引集合）
    var selectedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var autoPass by remember { mutableStateOf(false) }
    // 结算面板是否显示；false 时可回到牌桌查看最终牌面
    var showSettlement by remember { mutableStateOf(true) }
    // 玩法说明是否显示
    var showRules by remember { mutableStateOf(false) }
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

    /** 玩家垫牌：必须选中与当前牌型张数一致的牌才允许垫牌；张数不符则不执行（按钮此时已禁用） */
    fun playerPass() {
        val cur = game.currentPlay ?: return
        val hand = PaiJiuRules.sortHand(game.hands[0])
        val selected = selectedIndices.sorted().mapNotNull { hand.getOrNull(it) }
        if (selected.size != cur.count) return // 未选够张数：不得自动抽牌
        if (game.pass(0, selected)) { Sfx.click(context); selectedIndices = emptySet(); refresh() }
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

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF3E8D6))) {
        // 桌面背景（即梦生成的浅色牌九桌布）
        Image(
            painter = painterResource(R.drawable.pj_table),
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
                        color = Color(0xFF9A6A00),
                    )
                    Spacer(Modifier.height(8.dp))
                    // 玩家可接的牌提示（没有「过」，只有「垫牌」）；轻微上移避免与下方按钮重合
                    if (game.current == 0 && !game.isLeadPhase) {
                        val legal = PaiJiuRules.legalPlays(game.hands[0], game.currentPlay!!)
                        val need = game.currentPlay!!.count
                        Text(
                            if (legal.isEmpty()) "接不住：请先选中 $need 张牌，再点「垫牌」"
                            else "可以出：${legal.size} 种（垫牌需选 $need 张）",
                            fontSize = 14.sp,
                            color = Color(0xFF4E3B28),
                            modifier = Modifier.offset(y = (-10).dp),
                        )
                    }
                }
                // 本轮出的牌：曹操在座位手牌下方、刘备在名字右面、孙权在头像左面（见 OpponentSeat）
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

        // 结束面板（可点「看牌面」回到牌桌查看最终牌）
        if (game.gameOver && showSettlement) {
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
                    // 三个按钮横排，避免结算面板过高把下方按钮挤出屏幕
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ElderButton(text = "再来一局", onClick = { game.newRound(); selectedIndices = emptySet(); showSettlement = true; refresh() }, minHeight = 44.dp)
                        ElderButton(text = "看牌面", onClick = { showSettlement = false }, minHeight = 44.dp,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface))
                        ElderButton(text = "退出", onClick = onBack, minHeight = 44.dp,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface))
                    }
                }
            }
        }
        // 玩家自己的墩：固定显示（不参与手牌区布局流，不影响消息/按钮位置）。
        // 靠向手牌右侧（end 140），与右端孙权的墩牌错开，防止两者重合
        if (game.trickCount[0] > 0) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 140.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${game.trickCount[0]}墩", fontSize = 15.sp, color = Color(0xFF9A6A00))
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    game.tricks[0].forEach { trick ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            trick.forEach { entry ->
                                if (entry.isPass) {
                                    PaiJiuTileBack(Modifier.size(14.dp, 28.dp))
                                } else {
                                    val isOwner = entry.player == 0
                                    Row(modifier = if (isOwner) Modifier.border(1.dp, Color(0xFF9A6A00), RoundedCornerShape(3.dp)) else Modifier) {
                                        entry.play?.tiles.orEmpty().forEach { t -> PaiJiuTileFace(t, Modifier.size(14.dp, 28.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // 右上角：帮助按钮（打开「怎么玩」），左侧
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 80.dp)
                .width(64.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF6E8C4A))
                .clickable { onHelp() },
            contentAlignment = Alignment.Center,
        ) {
            Text("帮助", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        // 右上角：玩法按钮（常显），右侧
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .width(64.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF8C6E4A))
                .clickable { showRules = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("玩法", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        // 结算隐藏后回到牌桌：左下角显眼的「查看结算」按钮（重新打开结算）
        if (game.gameOver && !showSettlement) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
                    .width(132.dp)
                    .height(46.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFB23A3A))
                    .clickable { showSettlement = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("📊 查看结算", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        // 玩法说明对话框（新手也能看懂）
        if (showRules) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFDF8EE),
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.92f).fillMaxHeight(0.88f),
            ) {
                Box {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 18.dp).padding(end = 44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("牌九玩法说明", fontSize = 22.sp, color = Color(0xFFB23A3A), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text(PAI_JIU_RULES_TEXT, fontSize = 16.sp, lineHeight = 26.sp, color = Color(0xFF4E3B28))
                        Spacer(Modifier.height(16.dp))
                        ElderButton(text = "关闭", onClick = { showRules = false }, minHeight = 48.dp)
                    }
                    // 右上角关闭（✕）
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x2EB23A3A))
                            .clickable { showRules = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", fontSize = 20.sp, color = Color(0xFFB23A3A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** 桌面牌型文字 */
private fun currentPlayLabel(play: PaiJiuPlay): String = PaiJiuRules.playLabel(play)

/** 牌九玩法说明（安徽阜阳一带打法，新手也能看懂） */
private val PAI_JIU_RULES_TEXT = """
牌九也叫“打天九”，这里采用安徽阜阳一带流行的打法，和其他地方的规则略有不同。

◆ 牌具
一共 32 张骨牌，分三门：
· 天门：天、地、人、鹅牌、麻五、长三、板凳（各 2 张）
· 杂门：杂九、杂八、杂七、杂五、猴六、猴三（共 10 张）
· 幺门：老虎、四六、大锤、小锤（各 2 张）
同门按大小比，不同门之间不能比大小。

◆ 开局
你和孙权、曹操、刘备各拿 8 张牌，随机一家先领出。

◆ 出牌
· 领出（第一家）：可以出 1 张单牌；或 2 张对子（两张同名牌，或“猴六+猴三”的对猴）；或副子（天九、地八、人七、鹅五，可出 2 张、3 张或 4 张）。
· 跟牌：必须和上家出同样张数、同样类型的牌，还要更大，这叫“管上”。管不上就“垫牌”——随便挑同样张数的牌扣下（背面朝上）。
· 谁最后出（管上）的，这一墩就归谁，下一轮由他先出。一墩算几墩＝每人出的张数。

◆ 副子
· 天九＝天牌＋杂九
· 地八＝地牌＋杂八
· 人七＝人牌＋杂七
· 鹅五＝鹅牌＋杂五
都是副子时：天九最大，其次地八、人七、鹅五。

◆ 对猴
· 猴六＋猴三＝对猴。第一家出对猴，别人都管不上，只能垫牌。
· 第一家出对猴、且其他家都还没赢过墩时，这一墩算 4 墩。

◆ 得牌与结牌
· 谁赢下最后一墩，谁就“得牌”，是本局的赢家。
· 得牌时最后出的牌决定“结牌”：
  - 大对结（结在天、杂九、老虎的对子上）→ 基准 6 墩
  - 猴结（结在对猴上）→ 基准 6 墩
  - 小对结（结在其他对子上）→ 基准 5 墩
  - 单张结 → 基准 4 墩

◆ 输赢怎么算
得牌的人赢。其他三家按各自的墩数跟“保不输基准”比：
· 墩数不到基准 → 每少一墩输一墩；
· 墩数超过基准 → 多出来的赢回去；
· 得牌者赢的总墩数＝三家输赢加在一起。
例：你得牌（基准 4），孙权 5 墩赢 1，曹操 0 墩输 4，刘备 0 墩输 4，
你得牌就赢 4＋4－1＝7 墩。

◆ 扇（一家独大，看“出的牌数”不看墩数）
扇按一家连赢的“张数”算（对猴虽算 4 墩，但只算 2 张牌）：
· 连赢前 6 张＝六扇：另外两家（没得牌的两家）各多输 1 墩给扇主；
· 连赢前 7 张＝七扇：另外两家各多输 2 墩给扇主；
· 赢满 8 张＝八扇：三家各输 8 墩（结牌是小对则 9、大对或猴结则 10）；
· 得牌者不受扇的影响。
例：一家出对猴获 4 墩、再出天九获 2 墩，墩数是 6，但只出了 4 张牌，不算六扇。
""".trimIndent()

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
        Row(verticalAlignment = Alignment.Top) {
            // 孙权：本轮出的牌显示在头像左侧（同一行）
            if (p == 1 && game.lastPlayed[p].isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    game.lastPlayed[p].forEach { t -> PaiJiuTileFace(t, Modifier.size(22.dp, 44.dp)) }
                }
                Spacer(Modifier.width(10.dp))
            } else if (p == 1 && game.lastPassed[p]) {
                repeat(passTileCount(game)) { PaiJiuTileBack(Modifier.size(20.dp, 40.dp)) }
                Spacer(Modifier.width(10.dp))
            }
            SeatAvatar(avatar)
            Spacer(Modifier.width(4.dp))
            Text("$name ${game.hands[p].size}张", fontSize = 14.sp, color = Color(0xFF4E3B28))
            // 刘备：本轮出的牌显示在"刘备6张"右面空处
            if (p == 3 && game.lastPlayed[p].isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    game.lastPlayed[p].forEach { t -> PaiJiuTileFace(t, Modifier.size(22.dp, 44.dp)) }
                }
            } else if (p == 3 && game.lastPassed[p]) {
                Spacer(Modifier.width(10.dp))
                repeat(passTileCount(game)) { PaiJiuTileBack(Modifier.size(20.dp, 40.dp)) }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (p == 2) {
            // 曹操：Box 内 Column 固定且水平居中（手牌/出牌保持在屏幕中部曹操座位下），
            // 墩牌绝对定位在右侧，不参与布局流、不挤占手牌/出牌
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 手牌行
                    Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                        repeat(game.hands[p].size) { PaiJiuTileBack(Modifier.size(15.dp, 30.dp)) }
                    }
                    // 本轮出的牌/垫牌：靠近手牌下方（固定位置）
                    if (game.lastPlayed[p].isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            game.lastPlayed[p].forEach { t -> PaiJiuTileFace(t, Modifier.size(24.dp, 48.dp)) }
                        }
                    } else if (game.lastPassed[p]) {
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(passTileCount(game)) { PaiJiuTileBack(Modifier.size(22.dp, 44.dp)) }
                        }
                    }
                }
                // 墩牌：绝对定位在名字行右侧、顶端对齐曹操名字（top=0），多墩横向、每墩竖向排列
                if (game.trickCount[p] > 0) {
                    Column(
                        modifier = Modifier.align(Alignment.TopCenter).padding(start = 150.dp, top = 0.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text("${game.trickCount[p]}墩", fontSize = 14.sp, color = Color(0xFF9A6A00))
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            game.tricks[p].forEach { trick ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    trick.forEach { entry ->
                                        if (entry.isPass) {
                                            PaiJiuTileBack(Modifier.size(12.dp, 24.dp))
                                        } else {
                                            // 墩主出的多张牌横向并列；其他家单张各占一行
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                entry.play?.tiles.orEmpty().forEach { t -> PaiJiuTileFace(t, Modifier.size(12.dp, 24.dp)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 刘备/孙权：牌背 + 下方墩
            Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                repeat(game.hands[p].size) { PaiJiuTileBack(Modifier.size(15.dp, 30.dp)) }
            }
            if (game.trickCount[p] > 0) {
                Spacer(Modifier.height(6.dp))
                Text("${game.trickCount[p]}墩", fontSize = 14.sp, color = Color(0xFF9A6A00))
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    game.tricks[p].forEach { trick ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            trick.forEach { entry ->
                                if (entry.isPass) {
                                    PaiJiuTileBack(Modifier.size(13.dp, 26.dp))
                                } else {
                                    val isOwner = entry.player == p
                                    Row(
                                        modifier = if (isOwner) Modifier.border(1.dp, Color(0xFF9A6A00), RoundedCornerShape(3.dp)) else Modifier,
                                    ) {
                                        entry.play?.tiles.orEmpty().forEach { t -> PaiJiuTileFace(t, Modifier.size(13.dp, 26.dp)) }
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

/** 垫牌张数（垫牌数量与首家出牌数相同；无明面牌时按 1） */
private fun passTileCount(game: PaiJiuGame): Int = game.currentPlay?.count ?: 1

/** 本轮出的牌：显示在台布金圈附近各家方位（出牌明牌大牌 / 垫牌背面） */
@Composable
private fun ZonePlay(game: PaiJiuGame, p: Int, modifier: Modifier = Modifier) {
    if (game.lastPlayed[p].isNotEmpty()) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            game.lastPlayed[p].forEach { t -> PaiJiuTileFace(t, Modifier.size(24.dp, 48.dp)) }
        }
    } else if (game.lastPassed[p]) {
        // 垫牌：背面显示在该家方位（张数与垫牌数一致）
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(passTileCount(game)) { PaiJiuTileBack(Modifier.size(24.dp, 48.dp)) }
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
                    fontSize = 14.sp, color = Color(0xFF4E3B28))
            } else if (game.isLeadPhase) {
                // 领出：初始/选中都显示「出牌」
                ElderButton(text = "出牌", onClick = onPlay, minHeight = 44.dp)
            } else {
                // 接牌阶段：按钮文字随选中牌大小变化
                // 选中且能管上 → 「管上」；选中张数与当前牌型一致 → 「垫牌」；否则垫牌禁用
                val cur = game.currentPlay
                val sorted = PaiJiuRules.sortHand(game.hands[0])
                val selected = selectedIndices.sorted().mapNotNull { sorted.getOrNull(it) }
                val need = cur?.count ?: 0
                val canGuan = cur != null && selected.size == need &&
                    PaiJiuRules.classify(selected)?.let { PaiJiuRules.canBeat(it, cur) } == true
                if (selectedIndices.isNotEmpty() && canGuan) {
                    ElderButton(text = "管上", onClick = onPlay, minHeight = 44.dp)
                } else {
                    ElderButton(text = "垫牌", onClick = onPass, minHeight = 44.dp,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface),
                        enabled = selected.size == need)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
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
                            Text("▲", fontSize = 12.sp, color = Color(0xFFC88A00))
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
