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

/** AI 难度分层 */
enum class DoudizhuLevel(val label: String, val hint: String) {
    EASY("简单", "AI 出牌较慢"), NORMAL("普通", "AI 中等水平"), HARD("困难", "AI 很厉害"),
}

/** 斗地主对局：0=玩家，1/2=AI；支持叫地主、出牌、双 AI、农民合作 */
class DoudizhuGame {

    var level = DoudizhuLevel.NORMAL

    val hands = Array(3) { mutableListOf<Card>() }
    val bottom = mutableListOf<Card>()
    var landlord = -1
    var current = 0
    var phase = 0 // 0=叫地主, 1=出牌
    var bidIndex = 0

    var lastCombo: Combo? = null
    var lastPlayer = -1
    var passCount = 0
    /** 三家各自出的上一手（用于出牌区展示） */
    val lastPlays = arrayOfNulls<Combo>(3)
    /** 本轮是否出过牌（true=出过，false=未出或已过） */
    val playedThisRound = BooleanArray(3)
    /** 本轮是否已"过"（不出牌） */
    val passedThisRound = BooleanArray(3)
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
        lastPlays.fill(null)
        playedThisRound.fill(false)
        passedThisRound.fill(false)
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
        // bottom 保留用于桌面底牌展示
    }

    /** 玩家出牌；返回结果 */
    fun playerPlay(cards: List<Card>): PlayResult {
        if (phase != 1 || current != 0 || cards.isEmpty()) return PlayResult.INVALID
        val combo = parseCombo(cards) ?: return PlayResult.INVALID
        if (lastCombo != null && !canBeat(lastCombo!!, combo)) return PlayResult.INVALID
        // 自由出牌（新一轮第一手）：清空上一轮展示，避免"过"残留
        if (lastCombo == null) clearTable()
        hands[0].removeAll(cards)
        lastCombo = combo
        lastPlayer = 0
        lastPlays[0] = combo
        playedThisRound[0] = true
        passedThisRound[0] = false
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
        // 自由出牌（新一轮第一手）：清空上一轮展示，避免"过"残留
        if (lastCombo == null) clearTable()
        hands[p].removeAll(beat.cards)
        lastCombo = beat
        lastPlayer = p
        lastPlays[p] = beat
        playedThisRound[p] = true
        passedThisRound[p] = false
        passCount = 0
        if (hands[p].isEmpty()) { winner = p; over = true; return PlayResult.GAME_OVER }
        current = next(p)
        return PlayResult.OK
    }

    private fun doPass(p: Int) {
        playedThisRound[p] = false
        passedThisRound[p] = true
        passCount++
        if (passCount >= 2) {
            lastCombo = null // 新一轮自由出牌
            clearTable()     // 只保留最近一轮：两家都过后清空牌面与"过"，等新的一手
            passCount = 0
        }
        current = next(p)
    }

    /** 新一轮第一手：清空桌面展示（各家出的牌与"过"标记） */
    private fun clearTable() {
        lastPlays.fill(null)
        playedThisRound.fill(false)
        passedThisRound.fill(false)
    }

    private fun next(p: Int) = (p + 1) % 3

    // ---------- AI 策略 ----------

    /** 自由出牌：先走组合牌型（顺子/连对/三带），单张留最后从小到大出；手牌少时尽快走完 */
    fun aiLead(p: Int): Combo? {
        val hand = hands[p]
        val combos = findCombos(hand)
        val normal = combos.filter { it.type != ComboType.BOMB && it.type != ComboType.ROCKET }
        if (normal.isEmpty()) return combos.minByOrNull { it.mainRank } // 只剩炸弹/火箭

        // 手牌很少（≤5）：一次尽量多出，尽快走完
        if (hand.size <= 5) {
            return normal.maxByOrNull { it.cards.size * 10 + it.mainRank }
                ?: normal.minByOrNull { it.mainRank }
        }

        // 组合价值评分：长牌型优先（一次消多张），单张最后出
        fun score(c: Combo): Int = when (c.type) {
            ComboType.STRAIGHT, ComboType.PAIR_STRAIGHT -> 1000 + c.cards.size * 20 - c.mainRank
            ComboType.PLANE, ComboType.PLANE_WING -> 900 + c.cards.size * 20 - c.mainRank
            ComboType.TRIPLE_TWO -> 800 - c.mainRank
            ComboType.TRIPLE_ONE -> 780 - c.mainRank
            ComboType.TRIPLE -> 750 - c.mainRank
            ComboType.PAIR -> 600 - c.mainRank * 2
            else -> 400 - c.mainRank * 3 // SINGLE：最后出，先出小单张
        }
        val best = normal.maxByOrNull { score(it) } ?: return null
        // 简单难度：有时不按最优（留点随机，但别太乱）
        if (level == DoudizhuLevel.EASY && Random.nextInt(10) < 3) {
            val pool = normal.sortedByDescending { score(it) }.take(3)
            if (pool.isNotEmpty()) return pool[Random.nextInt(pool.size)]
        }
        return best
    }

    /** 跟牌：小牌优先压；单张可从对子拆；保留炸弹时机；困难难度积极拆牌压制 */
    fun aiFollow(p: Int): Combo? {
        val prev = lastCombo ?: return aiLead(p)
        // 农民合作：队友出的牌不压（除非队友只剩少数牌）
        val teammate = findTeammate(p)
        if (teammate == lastPlayer && hands[teammate]!!.size > 4) return null
        val combos = findCombos(hands[p])
        val normal = combos
            .filter { it.type != ComboType.BOMB && it.type != ComboType.ROCKET }
            .filter { canBeat(prev, it) }
        // 简单难度：能压也常不出（留牌）
        if (level == DoudizhuLevel.EASY && normal.isNotEmpty() && Random.nextInt(10) < 4) return null

        // 优先用最小的牌压（省大牌）
        if (normal.isNotEmpty()) {
            val best = normal.minByOrNull { it.mainRank * 10 + it.cards.size }
            // 用 2/王 压小牌视为浪费：手牌多时可选择不出
            val wasteBig = prev.type == ComboType.SINGLE && best!!.mainRank >= 15 && handSize(p) > 8
            if (wasteBig && level != DoudizhuLevel.HARD) return null
            return best
        }

        // 拆牌：prev 是单张时，从对子/三张中拆一张压（困难难度积极拆，普通也偶尔拆）
        if (prev.type == ComboType.SINGLE) {
            val split = findSplitSingle(p, prev)
            if (split != null) {
                if (level == DoudizhuLevel.HARD) return split
                if (level == DoudizhuLevel.NORMAL && Random.nextInt(10) < 6) return split
            }
        }

        // 炸弹：对手快赢或自己快走时动用
        val oppMin = (0..2).filter { it != p && it != teammate }.minOf { hands[it]!!.size }
        val useBomb = when (level) {
            DoudizhuLevel.EASY -> false
            DoudizhuLevel.NORMAL -> oppMin <= 1 || handSize(p) <= 2
            DoudizhuLevel.HARD -> oppMin <= 3 || handSize(p) <= 4
        }
        if (useBomb) {
            val big = combos
                .filter { it.type == ComboType.BOMB || it.type == ComboType.ROCKET }
                .filter { canBeat(prev, it) }
                .minByOrNull { it.mainRank }
            if (big != null) return big
        }
        return null
    }

    private fun handSize(p: Int) = hands[p].size

    /** 拆牌：从对子/三张中拆一张压单张（拆小的、保留大的） */
    private fun findSplitSingle(p: Int, prev: Combo): Combo? {
        val byRank = hands[p].groupBy { it.rank }
        val groups = byRank.entries
            .filter { it.value.size >= 2 && it.key > prev.mainRank && it.key < 15 }
            .sortedBy { it.key }
        val g = groups.firstOrNull() ?: return null
        return Combo(ComboType.SINGLE, g.key, 1, listOf(g.value[0]))
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
