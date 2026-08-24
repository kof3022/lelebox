package com.lelebox.app.game.paijiu

import kotlin.random.Random

/**
 * 牌九（打天九）对局状态机。
 *
 * 流程（M1）：
 * 1. 发牌：每人 8 张；玩家(0) 当庄先领出（可后续改为掷骰定庄）。
 * 2. 领出：出单张 / 文对 / 至尊对；之后各家按逆时针依次行动。
 * 3. 接牌（大食小）：同类型（单张对单张、对子对对子）且更大才能接；
 *    接不住必须垫牌（弃一张，失去本轮资格）。
 * 4. 一轮结束：每人本轮至多行动一次——除最后出牌者外其他参与者都垫牌
 *    → 最后出牌者赢墩，领出下一轮。
 * 5. 结牌：手牌出完（0 张）即结牌，按出完顺序排名；3 人出完 → 对局结束。
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

    /** 各家累计垫牌数（UI 显示牌背标记，不明牌） */
    val passCount = IntArray(4)

    /**
     * 各家赢得的墩：每墩是一轮中按出牌顺序的明牌（含出牌者）。
     * 一墩 = List<Pair<Int, List<PaiJiuTile>>>（出牌者, 出的牌）。
     * 显示规则：赢墩者（墩主，即此轮最大牌的人）的牌在最上面，下面是按出牌顺序的其余牌。
     */
    val tricks = Array(4) { mutableListOf<List<Pair<Int, List<PaiJiuTile>>>>() }

    /** 当前轮打出的牌（明牌，含出牌者，按顺序） */
    private val roundPlays = mutableListOf<Pair<Int, List<PaiJiuTile>>>()

    /** 各家本轮出的牌（显示在该家附近；本轮结束后清空，归入墩） */
    val lastPlayed = Array(4) { mutableListOf<PaiJiuTile>() }

    /** 各家累计墩数（= tricks[p].size 的快捷） */
    val trickCount = IntArray(4)

    /** 结牌顺序（先出完的在前） */
    val finished = mutableListOf<Int>()

    /** 是否对局结束（3 人出完） */
    var gameOver = false

    /** 头科（最先出完者） */
    var winner = -1

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
        roundPlays.clear()
        lastPlayed.forEach { it.clear() }
        trickCount.fill(0)
        finished.clear()
        gameOver = false
        winner = -1
    }

    /** 是否处于领出阶段（桌面无牌，须出任意合法牌） */
    val isLeadPhase: Boolean get() = currentPlay == null

    /** 是否已结牌（手牌出完） */
    fun isFinished(p: Int): Boolean = p in finished

    /**
     * 出牌：领出任意合法牌 / 接牌须同类型且更大。
     * @return 是否成功
     */
    fun play(p: Int, tiles: List<PaiJiuTile>): Boolean {
        if (gameOver || isFinished(p) || p != current) return false
        if (acted[p]) return false
        val play = PaiJiuPlay(tiles)
        if (isLeadPhase) {
            if (!PaiJiuRules.isLegalLead(play)) return false
        } else {
            val cur = currentPlay ?: return false
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
        // 出牌
        hand.clear(); hand.addAll(remaining)
        currentPlay = play
        currentPlayPlayer = p
        acted[p] = true
        lastPlayed[p].clear()
        lastPlayed[p].addAll(tiles) // 显示在该家附近（大牌形式）
        roundPlays.add(p to tiles)  // 本轮明牌（含出牌者，按顺序）
        checkFinish(p)
        advance()
        return true
    }

    /**
     * 垫牌：弃一张（失去本轮资格）。垫哪张由调用方指定（AI/UI 选最小）。
     * 垫完手牌空同样算结牌。
     * @return 是否成功
     */
    fun pass(p: Int, tile: PaiJiuTile): Boolean {
        if (gameOver || isFinished(p) || p != current) return false
        if (isLeadPhase) return false // 领出者必须出牌
        if (acted[p]) return false
        if (!hands[p].remove(tile)) return false
        acted[p] = true
        passCount[p]++
        checkFinish(p)
        advance()
        return true
    }

    /** 出牌/垫牌后的统一推进：结牌判定 + 轮到下一家或结束本轮 */
    private fun advance() {
        if (gameOver) return
        val next = nextUnactedAfter(current)
        if (next == -1) {
            endRound()
        } else {
            current = next
        }
    }

    /** 找到 current 之后下一个未结牌且本轮未行动的玩家；无则 -1（本轮结束） */
    private fun nextUnactedAfter(from: Int): Int {
        for (i in 1..3) {
            val p = (from + i) % 4
            if (!isFinished(p) && !acted[p]) return p
        }
        return -1
    }

    /** 一轮结束：最后出牌者赢墩（明牌收墩），领出下一轮 */
    private fun endRound() {
        val roundWinner = currentPlayPlayer
        // 赢墩者收走本轮的明牌（墩）。
        // 显示顺序：墩主（赢墩者）的牌在最上面，下面按出牌顺序排其余牌。
        if (roundWinner in 0..3 && roundPlays.isNotEmpty()) {
            val winnerPlay = roundPlays.firstOrNull { it.first == roundWinner }
            val ordered = mutableListOf<Pair<Int, List<PaiJiuTile>>>()
            winnerPlay?.let { ordered.add(it) }
            roundPlays.filter { it.first != roundWinner }.forEach { ordered.add(it) }
            tricks[roundWinner].add(ordered)
            trickCount[roundWinner] = tricks[roundWinner].size
        }
        roundPlays.clear()
        lastPlayed.forEach { it.clear() } // 本轮出的牌已归墩，清空"附近显示"
        acted.fill(false)
        currentPlay = null
        currentPlayPlayer = -1
        // 赢墩者领出；若赢墩者已结牌（刚出完最后一手），找下一个未结牌者领出
        var next = roundWinner
        if (next < 0 || isFinished(next)) {
            next = nextUnfinishedFrom(next)
            if (next == -1) {
                gameOver = true
                winner = finished.firstOrNull() ?: -1
                return
            }
        }
        leader = next
        current = next
    }

    /** 找 from 之后第一个未结牌玩家（环形） */
    private fun nextUnfinishedFrom(from: Int): Int {
        for (i in 1..4) {
            val p = (from + i) % 4
            if (!isFinished(p)) return p
        }
        return -1
    }

    /** 结牌检查：手牌空 → 记录排名；3 人出完 → 结束 */
    private fun checkFinish(p: Int) {
        if (hands[p].isEmpty() && !isFinished(p)) {
            finished.add(p)
        }
        if (finished.size >= 3) {
            gameOver = true
            winner = finished.firstOrNull() ?: -1
        }
    }

    /** 排名：返回 (玩家, 名次)；未出完为 -1 */
    fun rankOf(p: Int): Int = finished.indexOf(p).let { if (it < 0) -1 else it + 1 }
}
