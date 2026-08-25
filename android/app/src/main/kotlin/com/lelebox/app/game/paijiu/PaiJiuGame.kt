package com.lelebox.app.game.paijiu

import kotlin.random.Random

/**
 * 牌九（打天九）对局状态机（v2：三门/副子/墩/得牌/结牌/扇/结算）。
 *
 * 流程：
 * 1. 发牌：每人 8 张；玩家(0) 当庄先领出。
 * 2. 领出：首家出 单张/对子/副子(2/3/4张)；之后各家按逆时针依次行动。
 * 3. 跟牌：出与首家相同数量、同类型且更大的牌（管上），或垫牌（同数量、不能更大、背面）。
 * 4. 一圈结束：每人本轮至多行动一次——除最后出牌者外其他参与者都垫牌
 *    → 最后出牌者赢墩（墩数 = 每人出的牌数），领出下一轮。
 * 5. 对猴：首家出对猴 → 其他家不能管上只能垫；且若其他家都还没墩，对猴算 4 墩，否则 2 墩。
 * 6. 得牌：手牌出完且最后赢墩 → 得牌；按最后出的牌判 大对结/猴结/小对结。
 * 7. 扇：打满 8 墩时按前六/前七/全部判定 六扇/七扇/八扇。
 * 8. 结算：得牌以外三家按保不输墩数（普通4/大对结猴结6/小对结5）核算，输家补得牌。
 */
class PaiJiuGame(seed: Long = System.currentTimeMillis()) {
    val rng = Random(seed)

    /** 四家手牌（0=玩家，1..3=电脑） */
    val hands = Array(4) { mutableListOf<PaiJiuTile>() }

    /** 当前行动者 */
    var current = 0

    /** 本轮领出者 */
    var leader = 0

    /** 桌面上要接的牌（null = 领出阶段） */
    var currentPlay: PaiJiuPlay? = null

    /** 出 currentPlay 的玩家 */
    var currentPlayPlayer = -1

    /** 本轮已行动者（出牌或垫牌），每人至多一次 */
    private val acted = BooleanArray(4)

    /** 各家垫牌数（显示用；垫牌背面，不明牌） */
    val passCount = IntArray(4)

    /** 一轮中的单个动作：出牌（isPass=false）或垫牌（isPass=true，背面） */
    data class RoundEntry(val player: Int, val play: PaiJiuPlay?, val isPass: Boolean)

    /** 各家赢得的墩：每墩 = 一轮的完整动作（墩主牌在最上） */
    val tricks = Array(4) { mutableListOf<List<RoundEntry>>() }

    /** 当前轮全部动作（按顺序） */
    private val roundEntries = mutableListOf<RoundEntry>()

    /** 各家本轮出的牌（显示在台布金圈附近各家方位） */
    val lastPlayed = Array(4) { mutableListOf<PaiJiuTile>() }

    /** 各家本轮是否垫牌（背面显示） */
    val lastPassed = BooleanArray(4)

    /** 各家累计墩数（每墩的"张数/4"累加：1张=1墩、2张=2墩…） */
    val trickCount = IntArray(4)

    /** 结牌顺序（先出完的在前；得牌者最后） */
    val finished = mutableListOf<Int>()

    /** 得牌玩家（手牌出完且最后赢墩），-1=未定 */
    var dePai = -1

    /** 结牌类型（得牌者最后出的牌） */
    var jieType = JieType.NONE

    /** 对猴是否算 4 墩（首家出对猴且其他家无墩） */
    var duiHouFour = false

    /** 是否对局结束 */
    var gameOver = false

    /** 是否打满 8 墩（扇判定前提） */
    val totalTricks: Int get() = (0..3).sumOf { trickCount[it] }

    /** 完局结算结果（得牌玩家赢墩数；-1=未结算） */
    var settlement = -1

    enum class JieType { NONE, DA_DUI, HOU, XIAO_DUI }

    init { newRound() }

    fun newRound() {
        val dealt = PaiJiuRules.deal(rng)
        for (p in 0..3) {
            hands[p].clear()
            hands[p].addAll(dealt[p])
        }
        current = 0
        leader = 0
        currentPlay = null
        currentPlayPlayer = -1
        acted.fill(false)
        passCount.fill(0)
        tricks.forEach { it.clear() }
        roundEntries.clear()
        lastPlayed.forEach { it.clear() }
        lastPassed.fill(false)
        trickCount.fill(0)
        finished.clear()
        dePai = -1
        jieType = JieType.NONE
        duiHouFour = false
        gameOver = false
        settlement = -1
    }

    /** 是否处于领出阶段 */
    val isLeadPhase: Boolean get() = currentPlay == null

    /** 是否已出完 */
    fun isFinished(p: Int): Boolean = p in finished

    /**
     * 出牌：领出（单张/对子/副子）或管上（同数量同类型更大）。
     * @return 是否成功
     */
    fun play(p: Int, tiles: List<PaiJiuTile>): Boolean {
        if (gameOver || isFinished(p) || p != current) return false
        if (acted[p]) return false
        val play = PaiJiuRules.classify(tiles) ?: return false
        if (isLeadPhase) {
            // 领出：任何合法牌型
        } else {
            val cur = currentPlay ?: return false
            // 对猴特殊：首家出对猴，其他家不能管上
            if (cur.isDuiHou) return false
            if (!PaiJiuRules.canBeat(play, cur)) return false
        }
        // 牌必须都在手
        val hand = hands[p]
        val remaining = hand.toMutableList()
        for (t in tiles) {
            val idx = remaining.indexOf(t)
            if (idx < 0) return false
            remaining.removeAt(idx)
        }
        hand.clear(); hand.addAll(remaining)
        currentPlay = play
        currentPlayPlayer = p
        acted[p] = true
        lastPlayed[p].clear()
        lastPlayed[p].addAll(tiles)
        lastPassed[p] = false
        roundEntries.add(RoundEntry(p, play, isPass = false))
        checkFinish(p)
        advance()
        return true
    }

    /**
     * 垫牌：随意出相同数量的牌（不能比当前最大更大），背面显示。
     * 垫牌不要求构成合法牌型（可以任意 2 张），但若恰好构成同类型牌型且能管上则拒绝。
     */
    fun pass(p: Int, tiles: List<PaiJiuTile>): Boolean {
        if (gameOver || isFinished(p) || p != current) return false
        if (isLeadPhase) return false // 领出者必须出牌
        if (acted[p]) return false
        val cur = currentPlay ?: return false
        if (tiles.size != cur.count) return false // 垫牌数量必须相同
        // 垫牌不能比当前最大更大（若构成同类型牌型且能管上 → 拒绝）
        PaiJiuRules.classify(tiles)?.let { play ->
            if (play.kind == cur.kind && PaiJiuRules.canBeat(play, cur)) return false
        }
        // 牌必须在手
        val hand = hands[p]
        val remaining = hand.toMutableList()
        for (t in tiles) {
            val idx = remaining.indexOf(t)
            if (idx < 0) return false
            remaining.removeAt(idx)
        }
        hand.clear(); hand.addAll(remaining)
        acted[p] = true
        passCount[p]++
        lastPlayed[p].clear()
        lastPassed[p] = true // 垫牌背面
        roundEntries.add(RoundEntry(p, null, isPass = true))
        checkFinish(p)
        advance()
        return true
    }

    private fun advance() {
        if (gameOver) return
        val next = nextUnactedAfter(current)
        if (next == -1) {
            endRound()
        } else {
            current = next
        }
    }

    private fun nextUnactedAfter(from: Int): Int {
        for (i in 1..3) {
            val p = (from + i) % 4
            if (!isFinished(p) && !acted[p]) return p
        }
        return -1
    }

    /** 一圈结束：最后出牌者赢墩，领出下一轮 */
    private fun endRound() {
        val roundWinner = currentPlayPlayer
        val winPlay = currentPlay
        // 墩数 = 每人出的牌数（1张=1墩、2张=2墩…）
        val trickValue = winPlay?.count ?: 1
        // 对猴特殊：首家出对猴且其他家都还没墩 → 算 4 墩
        var value = trickValue
        if (winPlay != null && winPlay.isDuiHou && isLeadRoundFirst(roundWinner)) {
            val othersHaveTrick = (0..3).any { it != roundWinner && trickCount[it] > 0 }
            if (!othersHaveTrick) {
                value = 4
                duiHouFour = true
            }
        }
        // 赢墩者收走本轮动作
        if (roundWinner in 0..3 && roundEntries.isNotEmpty()) {
            val winnerEntry = roundEntries.firstOrNull { it.player == roundWinner }
            val ordered = mutableListOf<RoundEntry>()
            winnerEntry?.let { ordered.add(it) }
            roundEntries.filter { it.player != roundWinner }.forEach { ordered.add(it) }
            tricks[roundWinner].add(ordered)
            trickCount[roundWinner] += value
        }
        roundEntries.clear()
        lastPlayed.forEach { it.clear() }
        lastPassed.fill(false)
        acted.fill(false)
        currentPlay = null
        currentPlayPlayer = -1

        // 得牌检查：手牌出完且最后赢墩
        if (roundWinner in 0..3 && isFinished(roundWinner) && dePai < 0) {
            dePai = roundWinner
            jieType = judgeJieType(roundWinner, winPlay)
        }

        // 打满 8 墩 → 结束
        if (totalTricks >= 8 && dePai >= 0) {
            gameOver = true
            settlement = settle()
            return
        }
        // 得牌者若已出完（刚出完最后一手），找下一个未结牌者领出
        var next = roundWinner
        if (next < 0 || isFinished(next)) {
            next = nextUnfinishedFrom(next)
            if (next == -1) {
                gameOver = true
                if (dePai < 0) { dePai = roundWinner; jieType = judgeJieType(roundWinner, winPlay) }
                settlement = settle()
                return
            }
        }
        leader = next
        current = next
    }

    /** 判断是否为该圈首家（领出者） */
    private fun isLeadRoundFirst(p: Int): Boolean = roundEntries.firstOrNull()?.player == p

    private fun nextUnfinishedFrom(from: Int): Int {
        for (i in 1..4) {
            val p = (from + i) % 4
            if (!isFinished(p)) return p
        }
        return -1
    }

    private fun checkFinish(p: Int) {
        if (hands[p].isEmpty() && !isFinished(p)) {
            finished.add(p)
        }
    }

    /** 结牌类型判定 */
    private fun judgeJieType(p: Int, lastPlay: PaiJiuPlay?): JieType {
        val play = lastPlay ?: return JieType.NONE
        if (!play.isPair) return JieType.NONE // 单张/副子不触发对结
        if (play.isDuiHou) return JieType.HOU
        val name = play.tiles[0].name
        return when (name) {
            TileName.TIAN, TileName.ZA_JIU, TileName.LAO_HU -> JieType.DA_DUI
            else -> JieType.XIAO_DUI
        }
    }

    /** 扇判定：六扇/七扇/八扇（需打满 8 墩） */
    fun fanOf(p: Int): String? {
        if (totalTricks != 8) return null
        // 按墩的获得顺序展开：trickCount 只累计总数，需要每墩归属——用 tricks[p] 记录
        // 简化：检查该玩家是否获得"前 N 墩"——用每墩的赢家（每墩 RoundEntry 第一项是墩主）
        val winners = trickWinners()
        if (winners.size != 8) return null
        val all8 = winners.all { it == p }
        if (all8) return "八扇"
        val first7 = winners.take(7).all { it == p }
        if (first7 && winners[7] != p) return "七扇"
        val first6 = winners.take(6).all { it == p }
        if (first6 && winners[6] != p) return "六扇"
        return null
    }

    /** 每墩赢家（按墩顺序；每墩第一项为墩主） */
    private fun trickWinners(): List<Int> {
        val result = mutableListOf<Int>()
        for (p in 0..3) {
            tricks[p].forEach { result.add(p) }
        }
        return result
    }

    /** 完局结算：得牌玩家赢墩数 */
    fun settle(): Int {
        if (dePai < 0) return 0
        // 保不输基准
        val base = when (jieType) {
            JieType.DA_DUI, JieType.HOU -> 6
            JieType.XIAO_DUI -> 5
            else -> 4
        }
        val fan = fanOf(dePai)
        // 八扇特殊
        if (fan == "八扇") {
            val per = when (jieType) {
                JieType.XIAO_DUI -> 9
                JieType.DA_DUI, JieType.HOU -> 10
                else -> 8
            }
            return per * 3
        }
        var totalWin = 0
        for (p in 0..3) {
            if (p == dePai) continue
            var diff = trickCount[p] - base
            // 六扇/七扇：得牌与扇玩家之外两家多输；扇玩家加分
            val fanPlayer = (0..3).firstOrNull { q -> q != dePai && fanOf(q) != null }
            if (fanPlayer != null && fan != null) {
                if (p != fanPlayer) diff -= when (fan) {
                    "六扇" -> 1
                    "七扇" -> 2
                    else -> 0
                }
            }
            if (diff < 0) totalWin += -diff
        }
        // 六扇/七扇扇玩家加分
        if (fan != null) {
            val fanPlayer = (0..3).firstOrNull { q -> q != dePai && fanOf(q) != null } ?: return totalWin
            totalWin += when (fan) {
                "六扇" -> 2
                "七扇" -> 4
                else -> 0
            }
        }
        return totalWin
    }

    /** 排名 */
    fun rankOf(p: Int): Int = finished.indexOf(p).let { if (it < 0) -1 else it + 1 }
}
