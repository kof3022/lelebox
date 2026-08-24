package com.lelebox.app.game.mahjong

import kotlin.random.Random

/** Mahjong game: 0=player, 1..3=AI. GuoBiao rules. */
class MahjongGame(seed: Long = System.currentTimeMillis()) {
    val rng = Random(seed)

    /** 4 hands (0=player) */
    val hands = Array(4) { mutableListOf<Tile>() }
    /** exposed melds per player (chow/pung/kong) */
    val exposed = Array(4) { mutableListOf<Meld>() }
    /** concealed flag per player */
    val concealed = BooleanArray(4) { true }
    /** discard history per player (shown on table) */
    val discards = Array(4) { mutableListOf<Tile>() }
    /** 4 tile walls in front of each player */
    val walls = Array(4) { mutableListOf<Tile>() }
    /** current seat */
    var current = 0
    /** last discarded tile */
    var lastDiscard: Tile? = null
    /** who discarded last */
    var lastDiscardPlayer = -1
    /** player has drawn (14 tiles in hand) */
    var hasDrawn = false
    /** after pung/chow the player discards directly without drawing */
    var mustDiscard = false
    /** 响应阶段：某家出牌后，其余三家按序声明和/杠/碰；全部放弃后才轮到下家摸牌 */
    var respondStage = false
    /** all walls exhausted */
    var exhausted = false
    /** dice values (1..6 each) */
    var dice1 = 0
    var dice2 = 0
    /** which wall to draw from next (0=player front, clockwise 1..3) */
    var currentWall = 0

    var winner = -1
    var winScheme: WinScheme? = null
    var winFans: List<FanResult> = emptyList()
    var winSelfDraw = false
    var winByTile: Tile? = null

    init { newRound() }

    fun newRound() {
        val tiles = Tile.fullSet().shuffled(rng).toMutableList()
        for (p in 0..3) {
            hands[p].clear()
            exposed[p].clear()
            concealed[p] = true
            discards[p].clear()
            walls[p].clear()
            repeat(13) { hands[p].add(tiles.removeAt(0)) }
        }
        // Remaining 84 tiles -> 4 walls of 21 in front of each player
        repeat(84) { walls[it / 21].add(tiles.removeAt(0)) }
        // Roll two dice; sum decides starting wall
        dice1 = rng.nextInt(1, 7)
        dice2 = rng.nextInt(1, 7)
        currentWall = (dice1 + dice2) % 4
        current = 0
        lastDiscard = null
        lastDiscardPlayer = -1
        hasDrawn = false
        mustDiscard = false
        respondStage = false
        exhausted = false
        winner = -1
        winScheme = null
        winFans = emptyList()
        winSelfDraw = false
        winByTile = null
        draw(0) // dealer draws one, hand = 14
    }

    /** Draw from current wall; if empty try next wall clockwise; all empty -> exhausted */
    fun draw(p: Int): Boolean {
        for (offset in 0..3) {
            val w = (currentWall + offset) % 4
            if (walls[w].isNotEmpty()) {
                hands[p].add(walls[w].removeAt(walls[w].lastIndex))
                currentWall = (w + 1) % 4
                if (p == 0) hasDrawn = true
                return true
            }
        }
        exhausted = true
        return false
    }

    /** Discard a tile; record into history. 出牌后进入响应阶段（三家按序声明和/杠/碰） */
    fun discard(p: Int, tile: Tile): Boolean {
        if (!hands[p].remove(tile)) return false
        lastDiscard = tile
        lastDiscardPlayer = p
        discards[p].add(tile)
        if (p == 0) hasDrawn = false
        if (p == 0) mustDiscard = false
        current = (p + 1) % 4
        respondStage = true
        return true
    }

    /** 当前响应者放弃声明：移到下一家；三家都放弃后响应结束，出牌者的下家摸牌 */
    fun passRespond() {
        if (winner >= 0 || !respondStage) return
        current = (current + 1) % 4
        if (current == lastDiscardPlayer) {
            respondStage = false
            lastDiscard = null
            current = (lastDiscardPlayer + 1) % 4
        }
    }

    fun canWin(p: Int): Boolean {
        val tiles = hands[p]
        if (tiles.size % 3 != 2) return false
        return Mahjong.findWins(tiles).isNotEmpty()
    }

    fun selfWin(p: Int): Boolean {
        if (!canWin(p)) return false
        return doWin(p, hands[p].toList(), null, selfDraw = true)
    }

    /** Chow options for player p using the given discard (only from the player above, seat 3) */
    fun chowOptions(p: Int, discard: Tile): List<List<Tile>> {
        if (discard.suit >= 3) return emptyList()
        val r = discard.rank
        val hand = hands[p].toList()
        val options = mutableListOf<List<Tile>>()
        for (start in (r - 2)..r) {
            if (start < 1 || start + 2 > 9) continue
            val need = listOf(Tile(discard.suit, start), Tile(discard.suit, start + 1), Tile(discard.suit, start + 2))
            val remaining = hand.toMutableList()
            var ok = true
            for (n in need) {
                if (n == discard) continue
                val idx = remaining.indexOf(n)
                if (idx < 0) { ok = false; break }
                remaining.removeAt(idx)
            }
            if (ok) options.add(need)
        }
        return options.distinct()
    }

    /** Player chows from the player above (seat 3) */
    fun doChow(option: List<Tile>): Boolean {
        val d = lastDiscard ?: return false
        if (lastDiscardPlayer != 3) return false
        val hand = hands[0].toMutableList()
        for (t in option) if (t != d) { if (!hand.remove(t)) return false }
        hands[0].clear(); hands[0].addAll(hand)
        hands[0].remove(d)
        exposed[0].add(Meld.Chow(option))
        // 被吃的弃牌从出牌者弃牌区移除（归入顺子）
        if (lastDiscardPlayer in 0..3) discards[lastDiscardPlayer].remove(d)
        concealed[0] = false
        lastDiscard = null
        respondStage = false
        hasDrawn = false
        mustDiscard = true // 吃后直接弃牌，不抓牌
        current = 0
        return true
    }

    fun canPung(p: Int): Boolean {
        val d = lastDiscard ?: return false
        return hands[p].count { it == d } >= 2
    }

    fun canKong(p: Int): Boolean {
        val d = lastDiscard ?: return false
        return hands[p].count { it == d } >= 3
    }

    fun doPung(p: Int): Boolean {
        val d = lastDiscard ?: return false
        if (hands[p].count { it == d } < 2) return false
        hands[p].remove(d); hands[p].remove(d)
        exposed[p].add(Meld.Pung(listOf(d, d, d)))
        // 被碰的弃牌从出牌者弃牌区移除（归入明刻，避免同一张牌显示两次 → 出现5张假象）
        if (lastDiscardPlayer in 0..3) discards[lastDiscardPlayer].remove(d)
        concealed[p] = false
        lastDiscard = null
        respondStage = false
        current = p
        hasDrawn = false
        if (p == 0) mustDiscard = true // 碰后直接弃牌，不抓牌
        return true
    }

    fun doKong(p: Int): Boolean {
        val d = lastDiscard ?: return false
        if (hands[p].count { it == d } < 3) return false
        repeat(3) { hands[p].remove(d) }
        exposed[p].add(Meld.Kong(listOf(d, d, d, d)))
        // 被杠的弃牌从出牌者弃牌区移除（归入明杠）
        if (lastDiscardPlayer in 0..3) discards[lastDiscardPlayer].remove(d)
        concealed[p] = false
        lastDiscard = null
        respondStage = false
        current = p
        hasDrawn = false
        draw(p)
        return true
    }

    fun canConcealedKong(p: Int): Boolean {
        return hands[p].groupBy { it }.values.any { it.size == 4 }
    }

    fun doConcealedKong(p: Int): Boolean {
        val group = hands[p].groupBy { it }.values.firstOrNull { it.size == 4 } ?: return false
        val t = group[0]
        repeat(4) { hands[p].remove(t) }
        exposed[p].add(Meld.Kong(listOf(t, t, t, t)))
        concealed[p] = false
        draw(p)
        return true
    }

    private fun doWin(p: Int, tiles: List<Tile>, byTile: Tile?, selfDraw: Boolean): Boolean {
        val schemes = Mahjong.findWins(tiles)
        val scheme = schemes.firstOrNull() ?: return false
        winner = p
        winScheme = scheme
        winSelfDraw = selfDraw
        winByTile = byTile
        winFans = FanCalculator.calculate(scheme, tiles, exposed[p], selfDraw, seatWind = 1, roundWind = 1)
        return true
    }

    fun winByDiscard(p: Int, tile: Tile): Boolean {
        if (lastDiscard != tile) return false
        val tiles = hands[p].toList() + tile
        val schemes = Mahjong.findWins(tiles)
        if (schemes.isEmpty()) return false
        winner = p
        winScheme = schemes.first()
        winSelfDraw = false
        winByTile = tile
        winFans = FanCalculator.calculate(winScheme!!, tiles, exposed[p], false, seatWind = 1, roundWind = 1)
        return true
    }

    /**
     * AI 回合：
     * - 响应阶段：只声明和/杠/碰；不响应则 passRespond 交给下一家（不摸牌，弃牌留给后续家）
     * - 非响应阶段：摸牌 → 自摸/暗杠 → 弃牌（弃牌进入新一轮响应阶段）
     */
    fun aiTurn(p: Int) {
        if (winner >= 0 || exhausted) return
        val d = lastDiscard
        if (d != null && lastDiscardPlayer != p && hands[p].count { it == d } >= 1) {
            // 抢和：别人的弃牌正好能和 → 直接和
            if (winByDiscard(p, d)) return
            // 明杠/碰：手里 ≥3 张杠，≥2 张碰（完成面子，攻势更强）
            val cnt = hands[p].count { it == d }
            if (cnt >= 3 && doKong(p)) {
                val x = pickDiscard(p)
                discard(p, x)
                return
            }
            if (cnt == 2 && doPung(p)) {
                val x = pickDiscard(p)
                discard(p, x)
                return
            }
        }
        if (respondStage) { passRespond(); return }
        draw(p)
        if (canWin(p)) { selfWin(p); return }
        if (canConcealedKong(p)) doConcealedKong(p)
        val discard = pickDiscard(p)
        discard(p, discard)
    }

    /**
     * 弃牌选择：对手牌价值的启发式评分，弃最弱的一张。
     * 保留：对子/刻子（cnt²）、可直接成顺的相邻牌（±1 强于 ±2）、
     * 能放进更多种顺子的中张；优先弃：孤立牌、边张 1/9、无对字牌。
     */
    private fun pickDiscard(p: Int): Tile {
        val hand = hands[p]
        fun score(t: Tile): Int {
            val cnt = hand.count { it == t }
            var s = cnt * cnt * 60
            if (t.suit < 3) {
                val has1 = hand.any { it.suit == t.suit && it.rank == t.rank - 1 }
                val has2 = hand.any { it.suit == t.suit && it.rank == t.rank + 1 }
                if (has1) s += 15
                if (has2) s += 15
                val hasGap1 = hand.any { it.suit == t.suit && it.rank == t.rank - 2 }
                val hasGap2 = hand.any { it.suit == t.suit && it.rank == t.rank + 2 }
                if (hasGap1) s += 8
                if (hasGap2) s += 8
                // 能组成顺子的起点数（开放性）：中张价值更高
                val starts = (t.rank - 2..t.rank).count { it >= 1 && it + 2 <= 9 }
                s += starts * 10
            } else {
                s += cnt * 25
                if (cnt == 1) s += 2 // 孤张字牌价值最低
            }
            return s
        }
        return hand.minByOrNull { score(it) } ?: hand.first()
    }
}
