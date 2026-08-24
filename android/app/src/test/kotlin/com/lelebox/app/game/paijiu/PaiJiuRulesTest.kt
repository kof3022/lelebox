package com.lelebox.app.game.paijiu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** M0：牌表数据模型 + 接牌判定 + 发牌 单测 */
class PaiJiuRulesTest {

    // ---------- 1. 牌表完整性 ----------

    @Test
    fun `full set has exactly 32 tiles`() {
        assertEquals(32, PaiJiuTiles.FULL_SET.size)
    }

    @Test
    fun `full set has 22 civil and 10 military`() {
        val civil = PaiJiuTiles.FULL_SET.count { it.suit == TileSuit.CIVIL }
        val military = PaiJiuTiles.FULL_SET.count { it.suit == TileSuit.MILITARY }
        assertEquals(22, civil)
        assertEquals(10, military)
    }

    @Test
    fun `civil tiles come in pairs of 2`() {
        // 11 种文牌各 2 张
        val byId = PaiJiuTiles.FULL_SET.filter { it.suit == TileSuit.CIVIL }.groupBy { it.id }
        assertEquals(11, byId.size)
        byId.forEach { (_, tiles) -> assertEquals(2, tiles.size) }
    }

    @Test
    fun `military tiles are singles`() {
        val byId = PaiJiuTiles.FULL_SET.filter { it.suit == TileSuit.MILITARY }.groupBy { it.id }
        assertEquals(10, byId.size)
        byId.forEach { (_, tiles) -> assertEquals(1, tiles.size) }
    }

    @Test
    fun `21 distinct pip combinations in full set`() {
        assertEquals(21, PaiJiuTiles.FULL_SET.map { it.id }.distinct().size)
    }

    @Test
    fun `civil rank order is strictly increasing`() {
        // 天 > 地 > 人 > 和 > 梅 > 长三 > 板凳 > 虎头 > 红头 > 高脚 > 零霖
        val civilRanks = listOf(
            PaiJiuTiles.HEAVEN.rank, PaiJiuTiles.EARTH.rank, PaiJiuTiles.MAN.rank,
            PaiJiuTiles.HARMONY.rank, PaiJiuTiles.PLUM.rank, PaiJiuTiles.LONG_THREE.rank,
            PaiJiuTiles.BENCH.rank, PaiJiuTiles.TIGER_HEAD.rank, PaiJiuTiles.RED_HEAD.rank,
            PaiJiuTiles.HIGH_LEG.rank, PaiJiuTiles.LING_LING.rank,
        )
        assertEquals(civilRanks.sortedBy { it.order }, civilRanks)
    }

    @Test
    fun `military rank order nine-eight-seven-five`() {
        assertTrue(PaiJiuTiles.MIX9_A.rank.order < PaiJiuTiles.MIX8_A.rank.order)
        assertTrue(PaiJiuTiles.MIX8_A.rank.order < PaiJiuTiles.MIX7_A.rank.order)
        assertTrue(PaiJiuTiles.MIX7_A.rank.order < PaiJiuTiles.MIX5_A.rank.order)
    }

    // ---------- 2. 至尊 ----------

    @Test
    fun `supreme pair is 1-2 plus 2-4`() {
        assertTrue(PaiJiuTiles.isSupremePair(PaiJiuTiles.SUPREME_A, PaiJiuTiles.SUPREME_B))
        assertTrue(PaiJiuTiles.SUPREME_A.isSupremeWith(PaiJiuTiles.SUPREME_B))
        // 顺序无关
        assertTrue(PaiJiuTiles.isSupremePair(PaiJiuTiles.SUPREME_B, PaiJiuTiles.SUPREME_A))
    }

    @Test
    fun `other pairs are not supreme`() {
        assertFalse(PaiJiuTiles.isSupremePair(PaiJiuTiles.HEAVEN, PaiJiuTiles.HEAVEN))
        assertFalse(PaiJiuTiles.isSupremePair(PaiJiuTiles.SUPREME_A, PaiJiuTiles.MIX9_A))
    }

    @Test
    fun `supreme components alone are mixed six and three`() {
        assertEquals(TileRank.MIXED_SIX, PaiJiuTiles.SUPREME_A.rank)
        assertEquals(TileRank.MIXED_THREE, PaiJiuTiles.SUPREME_B.rank)
        // 单出等级：杂七 > 二四 > 杂五 > 丁三
        assertTrue(PaiJiuTiles.MIX7_A.rank.order < PaiJiuTiles.SUPREME_A.rank.order)
        assertTrue(PaiJiuTiles.SUPREME_A.rank.order < PaiJiuTiles.MIX5_A.rank.order)
        assertTrue(PaiJiuTiles.MIX5_A.rank.order < PaiJiuTiles.SUPREME_B.rank.order)
    }

    // ---------- 3. 单张接牌 ----------

    @Test
    fun `civil single beats lower civil single`() {
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.HEAVEN, PaiJiuTiles.EARTH))
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.EARTH, PaiJiuTiles.MAN))
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.TIGER_HEAD, PaiJiuTiles.RED_HEAD))
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.EARTH, PaiJiuTiles.HEAVEN))
    }

    @Test
    fun `civil and military do not interact`() {
        // 天牌（文）不能压杂九（武），反之亦然
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.HEAVEN, PaiJiuTiles.MIX9_A))
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.MIX9_A, PaiJiuTiles.HEAVEN))
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.LING_LING, PaiJiuTiles.MIX5_A))
    }

    @Test
    fun `same rank tie cannot beat`() {
        // 同组武牌平级（杂九 vs 杂九）后出者不能接
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.MIX9_A, PaiJiuTiles.MIX9_B))
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.MIX5_A, PaiJiuTiles.MIX5_B))
    }

    @Test
    fun `military single by group`() {
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.MIX9_A, PaiJiuTiles.MIX8_A))
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.MIX8_A, PaiJiuTiles.MIX7_A))
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.MIX7_A, PaiJiuTiles.MIX5_A))
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.SUPREME_A, PaiJiuTiles.MIX5_A)) // 二四压杂五
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.MIX5_A, PaiJiuTiles.SUPREME_A))
    }

    // ---------- 4. 对子接牌 ----------

    @Test
    fun `civil pair beats lower civil pair`() {
        val heavenPair = PaiJiuPlay.pair(PaiJiuTiles.HEAVEN, PaiJiuTiles.HEAVEN)
        val earthPair = PaiJiuPlay.pair(PaiJiuTiles.EARTH, PaiJiuTiles.EARTH)
        assertTrue(PaiJiuRules.canBeat(heavenPair, earthPair))
        assertFalse(PaiJiuRules.canBeat(earthPair, heavenPair))
    }

    @Test
    fun `supreme pair beats heaven pair`() {
        val supreme = PaiJiuPlay.pair(PaiJiuTiles.SUPREME_A, PaiJiuTiles.SUPREME_B)
        val heaven = PaiJiuPlay.pair(PaiJiuTiles.HEAVEN, PaiJiuTiles.HEAVEN)
        assertTrue(PaiJiuRules.canBeat(supreme, heaven))
        // 天牌对不能压至尊对
        assertFalse(PaiJiuRules.canBeat(heaven, supreme))
    }

    @Test
    fun `single cannot beat pair and vice versa`() {
        val single = PaiJiuPlay.single(PaiJiuTiles.HEAVEN)
        val pair = PaiJiuPlay.pair(PaiJiuTiles.EARTH, PaiJiuTiles.EARTH)
        assertFalse(PaiJiuRules.canBeat(single, pair))
        assertFalse(PaiJiuRules.canBeat(pair, single))
    }

    @Test
    fun `pair must be two same civil tiles`() {
        // 两张不同文牌不是对子，不能压对子
        val invalidPair = PaiJiuPlay.pair(PaiJiuTiles.HEAVEN, PaiJiuTiles.EARTH)
        val earthPair = PaiJiuPlay.pair(PaiJiuTiles.EARTH, PaiJiuTiles.EARTH)
        assertFalse(PaiJiuRules.canBeat(invalidPair, earthPair))
        // 两张武牌不是对子
        val militaryPair = PaiJiuPlay.pair(PaiJiuTiles.MIX9_A, PaiJiuTiles.MIX9_B)
        assertFalse(PaiJiuRules.canBeat(militaryPair, earthPair))
    }

    @Test
    fun `same civil pair tie cannot beat`() {
        val a = PaiJiuPlay.pair(PaiJiuTiles.HEAVEN, PaiJiuTiles.HEAVEN)
        val b = PaiJiuPlay.pair(PaiJiuTiles.HEAVEN, PaiJiuTiles.HEAVEN)
        assertFalse(PaiJiuRules.canBeat(a, b))
    }

    // ---------- 5. legalPlays ----------

    @Test
    fun `legalPlays finds beating singles and pairs`() {
        val hand = listOf(
            PaiJiuTiles.HEAVEN, PaiJiuTiles.EARTH, PaiJiuTiles.MAN,
            PaiJiuTiles.MIX9_A, PaiJiuTiles.MIX8_A,
        )
        val current = PaiJiuPlay.single(PaiJiuTiles.EARTH)
        val plays = PaiJiuRules.legalPlays(hand, current)
        // 能压地的文牌：天；能压地的武牌：无（文武不互接）→ 但杂九/杂八是武牌不能压地
        val singles = plays.filter { it.kind == PlayKind.SINGLE }
        assertEquals(listOf(PaiJiuTiles.HEAVEN), singles.map { it.tiles[0] })
    }

    @Test
    fun `legalPlays for pair current`() {
        val hand = listOf(
            PaiJiuTiles.HEAVEN, PaiJiuTiles.HEAVEN,
            PaiJiuTiles.SUPREME_A, PaiJiuTiles.SUPREME_B,
            PaiJiuTiles.EARTH,
        )
        val current = PaiJiuPlay.pair(PaiJiuTiles.EARTH, PaiJiuTiles.EARTH)
        val plays = PaiJiuRules.legalPlays(hand, current)
        assertTrue(plays.any { it.isSupreme })
        assertTrue(plays.any { it.tiles.all { t -> t.rank == TileRank.HEAVEN } && it.kind == PlayKind.PAIR })
        assertFalse(plays.any { it.kind == PlayKind.SINGLE }) // 单张不能接对子
    }

    // ---------- 6. 发牌 ----------

    @Test
    fun `deal gives 4 hands of 8 covering all 32 tiles`() {
        val hands = PaiJiuRules.deal(Random(42))
        assertEquals(4, hands.size)
        hands.forEach { assertEquals(8, it.size) }
        val all = hands.flatMap { it }
        assertEquals(32, all.size)
        // 按 id（点数组合）去重后仍是 21 种，且每种张数与原牌表一致
        assertEquals(PaiJiuTiles.FULL_SET.groupBy { it.id }.mapValues { it.value.size },
            all.groupBy { it.id }.mapValues { it.value.size })
    }

    @Test
    fun `deals differ across seeds`() {
        val h1 = PaiJiuRules.deal(Random(1))
        val h2 = PaiJiuRules.deal(Random(2))
        assertTrue(h1.flatMap { it } != h2.flatMap { it })
    }
}
