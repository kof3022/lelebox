package com.lelebox.app.game.paijiu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** M0v2：三门牌表 + 牌型分类 + 大小比较 + 发牌 单测 */
class PaiJiuRulesTest {

    // ---------- 1. 牌表与三门 ----------

    @Test
    fun `full set has exactly 32 tiles`() {
        assertEquals(32, PaiJiuTiles.FULL_SET.size)
    }

    @Test
    fun `three doors have 14-10-8 tiles`() {
        val tian = PaiJiuTiles.FULL_SET.count { it.suit == TileSuit.TIAN }
        val za = PaiJiuTiles.FULL_SET.count { it.suit == TileSuit.ZA }
        val yao = PaiJiuTiles.FULL_SET.count { it.suit == TileSuit.YAO }
        assertEquals(14, tian)
        assertEquals(10, za)
        assertEquals(8, yao)
    }

    @Test
    fun `tian door has 7 kinds x2`() {
        val byName = PaiJiuTiles.FULL_SET.filter { it.suit == TileSuit.TIAN }.groupBy { it.name }
        assertEquals(7, byName.size)
        byName.forEach { (_, tiles) -> assertEquals(2, tiles.size) }
    }

    @Test
    fun `yao door has 4 kinds x2`() {
        val byName = PaiJiuTiles.FULL_SET.filter { it.suit == TileSuit.YAO }.groupBy { it.name }
        assertEquals(4, byName.size)
        byName.forEach { (_, tiles) -> assertEquals(2, tiles.size) }
    }

    @Test
    fun `za door has 10 singles with pairs of same name`() {
        val byName = PaiJiuTiles.FULL_SET.filter { it.suit == TileSuit.ZA }.groupBy { it.name }
        assertEquals(6, byName.size)
        assertEquals(2, byName[TileName.ZA_JIU]!!.size)
        assertEquals(2, byName[TileName.ZA_BA]!!.size)
        assertEquals(2, byName[TileName.ZA_QI]!!.size)
        assertEquals(1, byName[TileName.HOU_LIU]!!.size)
        assertEquals(2, byName[TileName.ZA_WU]!!.size)
        assertEquals(1, byName[TileName.HOU_SAN]!!.size)
    }

    @Test
    fun `door order correct`() {
        // 天门：天>地>人>鹅>麻五>长三>板凳
        val tianOrder = listOf(TileName.TIAN, TileName.DI, TileName.REN, TileName.E, TileName.MA_WU, TileName.CHANG_SAN, TileName.BAN_DENG)
        assertEquals(tianOrder.map { it.order }, tianOrder.map { it.order }.sorted())
        // 杂门：杂九>杂八>杂七>猴六>杂五>猴三
        val zaOrder = listOf(TileName.ZA_JIU, TileName.ZA_BA, TileName.ZA_QI, TileName.HOU_LIU, TileName.ZA_WU, TileName.HOU_SAN)
        assertEquals(zaOrder.map { it.order }, zaOrder.map { it.order }.sorted())
        // 幺门：老虎>四六>大锤>小锤
        val yaoOrder = listOf(TileName.LAO_HU, TileName.SI_LIU, TileName.DA_CHUI, TileName.XIAO_CHUI)
        assertEquals(yaoOrder.map { it.order }, yaoOrder.map { it.order }.sorted())
    }

    // ---------- 2. 对猴 ----------

    @Test
    fun `dui hou is hou6 plus hou3`() {
        assertTrue(PaiJiuTiles.isDuiHou(PaiJiuTiles.HOU_LIU, PaiJiuTiles.HOU_SAN))
        assertTrue(PaiJiuTiles.isDuiHou(PaiJiuTiles.HOU_SAN, PaiJiuTiles.HOU_LIU))
        assertFalse(PaiJiuTiles.isDuiHou(PaiJiuTiles.HOU_LIU, PaiJiuTiles.HOU_LIU))
    }

    // ---------- 3. 牌型分类 ----------

    @Test
    fun `classify single`() {
        val play = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN))
        assertEquals(PlayKind.SINGLE, play?.kind)
        assertEquals(1, play?.count)
    }

    @Test
    fun `classify pair same name`() {
        val play = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN))
        assertEquals(PlayKind.PAIR, play?.kind)
    }

    @Test
    fun `classify pair dui hou`() {
        val play = PaiJiuRules.classify(listOf(PaiJiuTiles.HOU_LIU, PaiJiuTiles.HOU_SAN))
        assertEquals(PlayKind.PAIR, play?.kind)
        assertTrue(play?.isDuiHou == true)
    }

    @Test
    fun `classify fu2 tian jiu`() {
        val play = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A))
        assertEquals(PlayKind.FU_2, play?.kind)
        assertEquals(FuName.TIAN_JIU, play?.fuName)
    }

    @Test
    fun `classify fu2 all four`() {
        // 天九/地八/人七/鹅五
        assertEquals(FuName.TIAN_JIU, PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A))?.fuName)
        assertEquals(FuName.DI_BA, PaiJiuRules.classify(listOf(PaiJiuTiles.DI, PaiJiuTiles.ZA_BA_A))?.fuName)
        assertEquals(FuName.REN_QI, PaiJiuRules.classify(listOf(PaiJiuTiles.REN, PaiJiuTiles.ZA_QI_A))?.fuName)
        assertEquals(FuName.E_WU, PaiJiuRules.classify(listOf(PaiJiuTiles.E, PaiJiuTiles.ZA_WU_A))?.fuName)
    }

    @Test
    fun `classify fu3 typeA and typeB`() {
        // A：2天+1杂九
        val a = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A))
        assertEquals(PlayKind.FU_3A, a?.kind)
        assertEquals(FuName.TIAN_JIU, a?.fuName)
        // B：1天+2杂九
        val b = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A, PaiJiuTiles.ZA_JIU_B))
        assertEquals(PlayKind.FU_3B, b?.kind)
        assertEquals(FuName.TIAN_JIU, b?.fuName)
    }

    @Test
    fun `classify fu4`() {
        val play = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A, PaiJiuTiles.ZA_JIU_B))
        assertEquals(PlayKind.FU_4, play?.kind)
        assertEquals(FuName.TIAN_JIU, play?.fuName)
    }

    @Test
    fun `invalid combos are null`() {
        assertNull(PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.DI)))      // 不同名非副子
        assertNull(PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.DI, PaiJiuTiles.REN))) // 3张杂牌
        assertNull(PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN, PaiJiuTiles.DI, PaiJiuTiles.DI))) // 非副子4
    }

    // ---------- 4. 大小比较 ----------

    @Test
    fun `same door single beats lower`() {
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.TIAN, PaiJiuTiles.DI))
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.ZA_JIU_A, PaiJiuTiles.ZA_BA_A))
        assertTrue(PaiJiuRules.canBeatSingle(PaiJiuTiles.LAO_HU, PaiJiuTiles.SI_LIU))
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.DI, PaiJiuTiles.TIAN))
    }

    @Test
    fun `different door cannot compare`() {
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A))
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.ZA_JIU_A, PaiJiuTiles.TIAN))
        assertFalse(PaiJiuRules.canBeatSingle(PaiJiuTiles.LAO_HU, PaiJiuTiles.TIAN))
    }

    @Test
    fun `fu2 order tianjiu diba renqi ewu`() {
        val tianJiu = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A))!!
        val diBa = PaiJiuRules.classify(listOf(PaiJiuTiles.DI, PaiJiuTiles.ZA_BA_A))!!
        val renQi = PaiJiuRules.classify(listOf(PaiJiuTiles.REN, PaiJiuTiles.ZA_QI_A))!!
        val eWu = PaiJiuRules.classify(listOf(PaiJiuTiles.E, PaiJiuTiles.ZA_WU_A))!!
        assertTrue(PaiJiuRules.canBeat(tianJiu, diBa))
        assertTrue(PaiJiuRules.canBeat(diBa, renQi))
        assertTrue(PaiJiuRules.canBeat(renQi, eWu))
        assertFalse(PaiJiuRules.canBeat(diBa, tianJiu))
    }

    @Test
    fun `fu3 types cannot compare each other`() {
        val a = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A))!!
        val b = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A, PaiJiuTiles.ZA_JIU_B))!!
        assertFalse(PaiJiuRules.canBeat(a, b))
        assertFalse(PaiJiuRules.canBeat(b, a))
        // 同类型内可比
        val aDi = PaiJiuRules.classify(listOf(PaiJiuTiles.DI, PaiJiuTiles.DI, PaiJiuTiles.ZA_BA_A))!!
        assertTrue(PaiJiuRules.canBeat(a, aDi))
    }

    @Test
    fun `different counts cannot compare`() {
        val single = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN))!!
        val pair = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN))!!
        assertFalse(PaiJiuRules.canBeat(single, pair))
        assertFalse(PaiJiuRules.canBeat(pair, single))
    }

    @Test
    fun `dui hou cannot be beaten and cannot beat others`() {
        val duiHou = PaiJiuRules.classify(listOf(PaiJiuTiles.HOU_LIU, PaiJiuTiles.HOU_SAN))!!
        val tianPair = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN))!!
        // 对猴不能管其他对子
        assertFalse(PaiJiuRules.canBeat(duiHou, tianPair))
        // 其他对子不能管对猴（Game 层首家特殊，Rules 层统一 false）
        assertFalse(PaiJiuRules.canBeat(tianPair, duiHou))
    }

    // ---------- 5. legalPlays ----------

    @Test
    fun `legalPlays finds single beats`() {
        val hand = listOf(PaiJiuTiles.TIAN, PaiJiuTiles.DI, PaiJiuTiles.REN, PaiJiuTiles.ZA_JIU_A)
        val current = PaiJiuRules.classify(listOf(PaiJiuTiles.DI))!!
        val plays = PaiJiuRules.legalPlays(hand, current)
        // 能管地的单张：天（同门）。杂九不同门不能管
        assertTrue(plays.any { it.kind == PlayKind.SINGLE && it.tiles[0].name == TileName.TIAN })
        assertTrue(plays.none { it.tiles.any { t -> t.name == TileName.ZA_JIU } })
    }

    @Test
    fun `legalPlays finds fu beats`() {
        val hand = listOf(PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A, PaiJiuTiles.DI, PaiJiuTiles.ZA_BA_A, PaiJiuTiles.E, PaiJiuTiles.ZA_WU_A)
        val current = PaiJiuRules.classify(listOf(PaiJiuTiles.E, PaiJiuTiles.ZA_WU_A))!! // 鹅五
        val plays = PaiJiuRules.legalPlays(hand, current)
        // 能管鹅五的副子2：天九、地八
        assertTrue(plays.any { it.fuName == FuName.TIAN_JIU })
        assertTrue(plays.any { it.fuName == FuName.DI_BA })
        assertTrue(plays.none { it.fuName == FuName.E_WU })
    }

    // ---------- 6. 发牌 ----------

    @Test
    fun `deal gives 4 hands of 8 covering all 32 tiles`() {
        val hands = PaiJiuRules.deal(Random(42))
        assertEquals(4, hands.size)
        hands.forEach { assertEquals(8, it.size) }
        val all = hands.flatMap { it }
        assertEquals(32, all.size)
        assertEquals(PaiJiuTiles.FULL_SET.groupBy { it.id }.mapValues { it.value.size },
            all.groupBy { it.id }.mapValues { it.value.size })
    }

    // ---------- 7. 手牌排序 ----------

    @Test
    fun `sort hand puts tian za-jiu lao-hu first`() {
        val hand = listOf(PaiJiuTiles.BAN_DENG, PaiJiuTiles.LAO_HU, PaiJiuTiles.ZA_JIU_A, PaiJiuTiles.TIAN)
        val sorted = PaiJiuRules.sortHand(hand)
        // 天/杂九/老虎 优先最左（按 天>杂九>老虎 序）
        assertEquals(TileName.TIAN, sorted[0].name)
        assertEquals(TileName.ZA_JIU, sorted[1].name)
        assertEquals(TileName.LAO_HU, sorted[2].name)
        assertEquals(TileName.BAN_DENG, sorted[3].name)
    }

    @Test
    fun `sort hand keeps pairs together`() {
        val hand = listOf(PaiJiuTiles.TIAN, PaiJiuTiles.DI, PaiJiuTiles.TIAN, PaiJiuTiles.BAN_DENG)
        val sorted = PaiJiuRules.sortHand(hand)
        // 对天应在一起
        val tianIdx = sorted.indices.filter { sorted[it].name == TileName.TIAN }
        assertEquals(listOf(tianIdx[0], tianIdx[0] + 1), tianIdx)
    }

    @Test
    fun `deals differ across seeds`() {
        val h1 = PaiJiuRules.deal(Random(1))
        val h2 = PaiJiuRules.deal(Random(2))
        assertTrue(h1.flatMap { it } != h2.flatMap { it })
    }
}
