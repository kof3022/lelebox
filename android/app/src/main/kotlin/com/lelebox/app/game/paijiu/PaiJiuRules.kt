package com.lelebox.app.game.paijiu

import kotlin.random.Random

/** 出牌类型：单张 / 对子（文对或至尊对） */
enum class PlayKind { SINGLE, PAIR }

/** 一手出的牌（1 张单牌 或 2 张对子） */
data class PaiJiuPlay(val tiles: List<PaiJiuTile>) {
    init { require(tiles.size == 1 || tiles.size == 2) { "出牌必须是单张或对子" } }

    val kind: PlayKind get() = if (tiles.size == 1) PlayKind.SINGLE else PlayKind.PAIR

    /** 是否为至尊对（猴王对） */
    val isSupreme: Boolean
        get() = kind == PlayKind.PAIR && PaiJiuTiles.isSupremePair(tiles[0], tiles[1])

    companion object {
        fun single(t: PaiJiuTile) = PaiJiuPlay(listOf(t))
        fun pair(a: PaiJiuTile, b: PaiJiuTile) = PaiJiuPlay(listOf(a, b))
    }
}

/**
 * 牌九（天九）规则引擎：接牌判定 + 出牌合法性。
 *
 * 核心规则「大食小」：
 * 1. 同类型才能接：文牌跟文牌、武牌跟武牌，文武不互接。
 * 2. 单张只能被单张接，对子只能被对子接。
 * 3. 文牌按等级比大小；武牌按组别（点数）比大小，同组平级后出者不能接。
 * 4. 至尊对（[1:2]+[2:4]）最大，可压天牌，只能用至尊对接。
 */
object PaiJiuRules {

    /** 判定 play 是否能接住 current（须同类型且更大） */
    fun canBeat(play: PaiJiuPlay, current: PaiJiuPlay): Boolean {
        if (play.kind != current.kind) return false // 单张/对子不互接

        return when (play.kind) {
            PlayKind.SINGLE -> canBeatSingle(play.tiles[0], current.tiles[0])
            PlayKind.PAIR -> canBeatPair(play, current)
        }
    }

    /** 单张接单张 */
    fun canBeatSingle(my: PaiJiuTile, cur: PaiJiuTile): Boolean {
        if (my.suit != cur.suit) return false // 文武不互接
        if (my.rank == cur.rank) return false // 同组/同级平，后出者不能接
        return my.rank.order < cur.rank.order
    }

    /** 对子接对子 */
    fun canBeatPair(my: PaiJiuPlay, cur: PaiJiuPlay): Boolean {
        // 至尊对最大：可压一切对子
        if (my.isSupreme) return true
        // 对方是至尊对：只有至尊对能接
        if (cur.isSupreme) return false
        // 普通对子：两张同文牌组成（文对）
        val myRank = pairRank(my) ?: return false
        val curRank = pairRank(cur) ?: return false
        if (myRank == curRank) return false
        return myRank.order < curRank.order
    }

    /** 普通对子的等级：两张相同文牌 → 返回该文牌等级；非文对返回 null */
    private fun pairRank(p: PaiJiuPlay): TileRank? {
        val (a, b) = p.tiles
        if (a.suit != TileSuit.CIVIL || b.suit != TileSuit.CIVIL) return null
        if (a.rank != b.rank) return null
        return a.rank
    }

    /** 手牌中能接住 current 的所有出法（单张/对子），用于 UI 高亮与 AI 选牌 */
    fun legalPlays(hand: List<PaiJiuTile>, current: PaiJiuPlay): List<PaiJiuPlay> {
        val result = mutableListOf<PaiJiuPlay>()
        when (current.kind) {
            PlayKind.SINGLE -> {
                for (t in hand) {
                    val play = PaiJiuPlay.single(t)
                    if (canBeat(play, current)) result.add(play)
                }
            }
            PlayKind.PAIR -> {
                // 至尊对
                val supA = hand.firstOrNull { it == PaiJiuTiles.SUPREME_A }
                val supB = hand.firstOrNull { it == PaiJiuTiles.SUPREME_B }
                if (supA != null && supB != null && canBeat(PaiJiuPlay.pair(supA, supB), current)) {
                    result.add(PaiJiuPlay.pair(supA, supB))
                }
                // 普通文对
                val byRank = hand.groupBy { it.rank }.filterKeys { it != TileRank.SUPREME }
                for ((rank, tiles) in byRank) {
                    if (tiles.size >= 2 && rank.suitIsCivil()) {
                        val play = PaiJiuPlay.pair(tiles[0], tiles[1])
                        if (canBeat(play, current)) result.add(play)
                    }
                }
            }
        }
        return result.distinct()
    }

    /** 发牌：32 张洗牌后每人 8 张（0=玩家，1..3=电脑） */
    fun deal(rng: Random = Random.Default): Array<List<PaiJiuTile>> {
        val shuffled = PaiJiuTiles.FULL_SET.shuffled(rng)
        return Array(4) { p -> shuffled.subList(p * 8, p * 8 + 8).toList() }
    }

    private fun TileRank.suitIsCivil(): Boolean = when (this) {
        TileRank.HEAVEN, TileRank.EARTH, TileRank.MAN, TileRank.HARMONY, TileRank.PLUM,
        TileRank.LONG_THREE, TileRank.BENCH, TileRank.TIGER_HEAD, TileRank.RED_HEAD,
        TileRank.HIGH_LEG, TileRank.LING_LING -> true
        else -> false
    }
}
