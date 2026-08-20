package com.lelebox.app.game.doudizhu

import kotlin.random.Random

/** 扑克牌：rank 3..15=3..2，16=小王，17=大王；suit 0..3 花色，4=王 */
data class Card(val rank: Int, val suit: Int)

object Doudizhu {
    fun rankText(r: Int) = when (r) {
        11 -> "J"; 12 -> "Q"; 13 -> "K"; 14 -> "A"; 15 -> "2"; 16 -> "小王"; 17 -> "大王"; else -> "$r"
    }
    fun suitText(s: Int) = when (s) { 0 -> "♠"; 1 -> "♥"; 2 -> "♦"; 3 -> "♣"; else -> "" }
    fun isRed(c: Card) = c.suit == 1 || c.suit == 2 || c.rank >= 16
    fun cardText(c: Card) = if (c.rank >= 16) rankText(c.rank) else rankText(c.rank) + suitText(c.suit)

    fun newDeck(): List<Card> {
        val list = mutableListOf<Card>()
        for (r in 3..15) for (s in 0..3) list.add(Card(r, s))
        list.add(Card(16, 4)); list.add(Card(17, 4))
        return list.shuffled(Random(System.currentTimeMillis()))
    }
}

enum class ComboType { SINGLE, PAIR, TRIPLE, TRIPLE_ONE, TRIPLE_TWO, STRAIGHT, PAIR_STRAIGHT, PLANE, PLANE_WING, FOUR_TWO, BOMB, ROCKET }

data class Combo(val type: ComboType, val mainRank: Int, val length: Int, val cards: List<Card>)

/** 牌型判定 */
fun parseCombo(cards: List<Card>): Combo? {
    if (cards.isEmpty()) return null
    val n = cards.size
    val sorted = cards.sortedBy { it.rank }
    val ranks = sorted.map { it.rank }
    val counts = ranks.groupingBy { it }.eachCount()
    if (n == 2 && ranks.contains(16) && ranks.contains(17)) return Combo(ComboType.ROCKET, 17, 2, sorted)
    if (n == 4 && counts.values.all { it == 4 }) return Combo(ComboType.BOMB, ranks[0], 4, sorted)
    if (n == 1) return Combo(ComboType.SINGLE, ranks[0], 1, sorted)
    if (n == 2 && counts.values.all { it == 2 }) return Combo(ComboType.PAIR, ranks[0], 2, sorted)
    if (n == 3 && counts.values.all { it == 3 }) return Combo(ComboType.TRIPLE, ranks[0], 3, sorted)
    if (n == 4) {
        counts.entries.firstOrNull { it.value == 3 }?.let { return Combo(ComboType.TRIPLE_ONE, it.key, 4, sorted) }
    }
    if (n == 5) {
        val triple = counts.entries.firstOrNull { it.value == 3 }
        val pair = counts.entries.firstOrNull { it.value == 2 }
        if (triple != null && pair != null) return Combo(ComboType.TRIPLE_TWO, triple.key, 5, sorted)
    }
    if (n >= 5 && counts.values.all { it == 1 } && ranks.all { it < 15 } && isConsecutive(ranks)) {
        return Combo(ComboType.STRAIGHT, ranks.last(), n, sorted)
    }
    if (n >= 6 && n % 2 == 0 && counts.values.all { it == 2 }) {
        val rks = counts.keys.sorted()
        if (rks.all { it < 15 } && isConsecutive(rks)) return Combo(ComboType.PAIR_STRAIGHT, rks.last(), rks.size, sorted)
    }
    val triples = counts.filter { it.value == 3 }.keys.sorted()
    if (triples.size >= 2 && isConsecutive(triples) && triples.all { it < 15 }) {
        val wings = n - triples.size * 3
        if (wings == 0) return Combo(ComboType.PLANE, triples.last(), triples.size, sorted)
        if (wings == triples.size) return Combo(ComboType.PLANE_WING, triples.last(), triples.size, sorted)
    }
    if (n == 6) {
        counts.entries.firstOrNull { it.value == 4 }?.let { return Combo(ComboType.FOUR_TWO, it.key, 6, sorted) }
    }
    return null
}

private fun isConsecutive(ranks: List<Int>): Boolean {
    for (i in 1 until ranks.size) if (ranks[i] != ranks[i - 1] + 1) return false
    return true
}

/** 能否压过上家 */
fun canBeat(prev: Combo, next: Combo): Boolean {
    if (next.type == ComboType.ROCKET) return true
    if (prev.type == ComboType.ROCKET) return false
    if (next.type == ComboType.BOMB && prev.type != ComboType.BOMB) return true
    if (prev.type == ComboType.BOMB) return next.type == ComboType.BOMB && next.mainRank > prev.mainRank
    if (next.type != prev.type) return false
    if (next.length != prev.length) return false
    return next.mainRank > prev.mainRank
}

enum class PlayResult { OK, INVALID, GAME_OVER }

/** 斗地主对局：0=玩家，1/2=AI；支持叫地主、出牌、双 AI、农民合作 */
class DoudizhuGame {

    val hands = Array(3) { mutableListOf<Card>() }
    val bottom = mutableListOf<Card>()
    var landlord = -1
    var current = 0
    var phase = 0 // 0=叫地主, 1=出牌
    var bidIndex = 0

    var lastCombo: Combo? = null
    var lastPlayer = -1
    var passCount = 0
    var winner = -1
    var over = false

    fun newDeal() {
        val deck = Doudizhu.newDeck()
        for (p in 0..2) {
            hands[p].clear()
            hands[p].addAll(deck.subList(p * 17, p * 17 + 17))
            hands[p].sortByDescending { it.rank }
        }
        bottom.clear()
        bottom.addAll(deck.subList(51, 54))
        landlord = -1
        current = 0
        phase = 0
        bidIndex = 0
        lastCombo = null
        lastPlayer = -1
        passCount = 0
        winner = -1
        over = false
    }

    fun isLandlord(p: Int) = p == landlord
    fun isFarmer(p: Int) = p != landlord

    /** 玩家叫地主；返回 true 表示已确认（false=轮到下一个叫或不叫后继续） */
    fun playerBid(call: Boolean): Boolean {
        if (phase != 0) return true
        if (call) {
            becomeLandlord(0)
            return true
        }
        // AI 依次叫
        for (p in 1..2) {
            if (aiWantsBid(p)) {
                becomeLandlord(p)
                return true
            }
        }
        newDeal() // 都不叫，重新发牌
        return false
    }

    private fun aiWantsBid(p: Int): Boolean {
        var pts = 0
        for (c in hands[p]) {
            when (c.rank) {
                15 -> pts += 2
                14 -> pts += 1
                16, 17 -> pts += 2
            }
        }
        val counts = hands[p].groupingBy { it.rank }.eachCount()
        if (counts.values.any { it == 4 }) pts += 3
        return pts >= 3
    }

    private fun becomeLandlord(p: Int) {
        landlord = p
        hands[p].addAll(bottom)
        hands[p].sortByDescending { it.rank }
        phase = 1
        current = p
        bottom.clear()
    }

    /** 玩家出牌；返回结果 */
    fun playerPlay(cards: List<Card>): PlayResult {
        if (phase != 1 || current != 0 || cards.isEmpty()) return PlayResult.INVALID
        val combo = parseCombo(cards) ?: return PlayResult.INVALID
        if (lastCombo != null && !canBeat(lastCombo!!, combo)) return PlayResult.INVALID
        hands[0].removeAll(cards)
        lastCombo = combo
        lastPlayer = 0
        passCount = 0
        if (hands[0].isEmpty()) { winner = 0; over = true; return PlayResult.GAME_OVER }
        current = next(0)
        return PlayResult.OK
    }

    fun playerPass(): PlayResult {
        if (phase != 1 || current != 0) return PlayResult.INVALID
        if (lastCombo == null) return PlayResult.INVALID // 自由出牌不能不出
        doPass(0)
        return PlayResult.OK
    }

    /** AI 走一步；由 UI 在轮到 AI 时调用，直到轮到玩家或终局 */
    fun aiAct(): PlayResult {
        if (phase != 1 || current == 0 || over) return PlayResult.INVALID
        val p = current
        val beat = if (lastCombo == null) {
            aiLead(p)
        } else {
            aiFollow(p)
        }
        if (beat == null) {
            doPass(p)
            return PlayResult.OK
        }
        hands[p].removeAll(beat.cards)
        lastCombo = beat
        lastPlayer = p
        passCount = 0
        if (hands[p].isEmpty()) { winner = p; over = true; return PlayResult.GAME_OVER }
        current = next(p)
        return PlayResult.OK
    }

    private fun doPass(p: Int) {
        passCount++
        if (passCount >= 2) {
            lastCombo = null
            passCount = 0
        }
        current = next(p)
    }

    private fun next(p: Int) = (p + 1) % 3

    // ---------- AI 策略 ----------

    private fun aiLead(p: Int): Combo? {
        val combos = findCombos(hands[p])
        // 首选非炸弹最小组合（先出小牌），炸弹留后
        val normal = combos
            .filter { it.type != ComboType.BOMB && it.type != ComboType.ROCKET }
            .minByOrNull { it.mainRank * 10 + it.cards.size }
        if (normal != null) return normal
        return combos.minByOrNull { it.mainRank }
    }

    private fun aiFollow(p: Int): Combo? {
        val prev = lastCombo ?: return aiLead(p)
        // 农民合作：队友出的牌不压（除非队友只剩少数牌且我方有绝对把握）
        val teammate = findTeammate(p)
        if (teammate == lastPlayer && hands[teammate]!!.size > 4) return null
        val combos = findCombos(hands[p])
        // 普通牌里找最小能压过的
        val normal = combos
            .filter { it.type != ComboType.BOMB && it.type != ComboType.ROCKET }
            .filter { canBeat(prev, it) }
            .minByOrNull { it.mainRank }
        if (normal != null) return normal
        // 炸弹/火箭兜底
        val big = combos
            .filter { it.type == ComboType.BOMB || it.type == ComboType.ROCKET }
            .filter { canBeat(prev, it) }
            .minByOrNull { it.mainRank }
        return big
    }

    private fun findTeammate(p: Int): Int {
        if (landlord == -1) return -1
        for (i in 0..2) if (i != p && i != landlord) return i
        return -1
    }

    /** 枚举手里的所有合法牌型（启发式，不求完备） */
    fun findCombos(hand: List<Card>): List<Combo> {
        val result = mutableListOf<Combo>()
        val sorted = hand.sortedBy { it.rank }
        val byRank = sorted.groupBy { it.rank }
        // 单张
        sorted.forEach { c -> parseCombo(listOf(c))?.let { result.add(it) } }
        // 对/三/四
        byRank.values.forEach { group ->
            if (group.size >= 2) parseCombo(group.take(2))?.let { result.add(it) }
            if (group.size >= 3) {
                val triple = group.take(3)
                parseCombo(triple)?.let { result.add(it) }
                // 三带一/三带二
                val other = sorted.filter { it.rank != group[0].rank }
                if (other.isNotEmpty()) parseCombo(triple + listOf(other.first()))?.let { result.add(it) }
                val pairOther = byRank.values.firstOrNull { it[0].rank != group[0].rank && it.size >= 2 }
                if (pairOther != null) parseCombo(triple + pairOther.take(2))?.let { result.add(it) }
            }
            if (group.size == 4) parseCombo(group)?.let { result.add(it) }
        }
        // 顺子（贪心：从每档至少一张的连续段枚举）
        val avail = byRank.keys.filter { it < 15 }.sorted()
        if (avail.size >= 5) {
            for (start in avail.indices) {
                for (len in 5..avail.size - start) {
                    val seg = avail.subList(start, start + len)
                    if (isConsecutive(seg)) {
                        val cards = seg.map { r -> sorted.first { it.rank == r } }
                        parseCombo(cards)?.let { result.add(it) }
                    }
                }
            }
        }
        // 连对（贪心：每档至少两张）
        val pairAvail = byRank.filter { it.value.size >= 2 }.keys.filter { it < 15 }.sorted()
        if (pairAvail.size >= 3) {
            for (start in pairAvail.indices) {
                for (len in 3..pairAvail.size - start) {
                    val seg = pairAvail.subList(start, start + len)
                    if (isConsecutive(seg)) {
                        val cards = seg.flatMap { r -> byRank[r]!!.take(2) }
                        parseCombo(cards)?.let { result.add(it) }
                    }
                }
            }
        }
        // 火箭
        if (sorted.any { it.rank == 16 } && sorted.any { it.rank == 17 }) {
            result.add(Combo(ComboType.ROCKET, 17, 2, listOf(sorted.first { it.rank == 16 }, sorted.first { it.rank == 17 })))
        }
        return result.distinctBy { it.cards }
    }
}
