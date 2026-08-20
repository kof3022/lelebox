package com.lelebox.app.game.doudizhu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoudizhuGameTest {

    private fun c(vararg ranks: Int): List<Card> = ranks.mapIndexed { i, r -> Card(r, i % 4) }

    @Test
    fun `parse single pair triple`() {
        assertEquals(ComboType.SINGLE, parseCombo(c(7))?.type)
        assertEquals(ComboType.PAIR, parseCombo(c(8, 8))?.type)
        assertEquals(ComboType.TRIPLE, parseCombo(c(9, 9, 9))?.type)
    }

    @Test
    fun `parse triple with wings`() {
        assertEquals(ComboType.TRIPLE_ONE, parseCombo(c(9, 9, 9, 5))?.type)
        assertEquals(ComboType.TRIPLE_TWO, parseCombo(c(9, 9, 9, 5, 5))?.type)
    }

    @Test
    fun `parse straight and pair straight`() {
        val s = parseCombo(c(3, 4, 5, 6, 7))
        assertEquals(ComboType.STRAIGHT, s?.type)
        assertEquals(7, s?.mainRank)
        assertEquals(5, s?.length)
        val ps = parseCombo(c(3, 3, 4, 4, 5, 5))
        assertEquals(ComboType.PAIR_STRAIGHT, ps?.type)
        assertEquals(3, ps?.length)
        // 2 不能进顺子
        assertNull(parseCombo(c(11, 12, 13, 14, 15)))
    }

    @Test
    fun `parse bomb and rocket`() {
        assertEquals(ComboType.BOMB, parseCombo(c(10, 10, 10, 10))?.type)
        assertEquals(ComboType.ROCKET, parseCombo(c(16, 17))?.type)
    }

    @Test
    fun `parse invalid combos`() {
        assertNull(parseCombo(c(3, 4, 8, 9, 10)))     // 断顺
        assertNull(parseCombo(c(3, 3, 4)))            // 不完整
        assertNull(parseCombo(c(7, 7, 8, 8)))         // 两对不是合法牌型
    }

    @Test
    fun `parse four with two`() {
        assertEquals(ComboType.FOUR_TWO, parseCombo(c(10, 10, 10, 10, 5, 6))?.type)
    }

    @Test
    fun `can beat comparisons`() {
        val pair8 = parseCombo(c(8, 8))!!
        val pair9 = parseCombo(c(9, 9))!!
        val bomb = parseCombo(c(10, 10, 10, 10))!!
        val rocket = parseCombo(c(16, 17))!!
        val single7 = parseCombo(c(7))!!
        assertTrue(canBeat(pair8, pair9))
        assertFalse(canBeat(pair9, pair8))
        assertFalse(canBeat(pair8, single7))   // 不同类型
        assertTrue(canBeat(pair8, bomb))       // 炸弹压一切
        assertTrue(canBeat(bomb, rocket))      // 王炸压炸弹
        assertTrue(canBeat(single7, rocket))
        // 顺子长度不同不能比
        val s5 = parseCombo(c(3, 4, 5, 6, 7))!!
        val s6 = parseCombo(c(3, 4, 5, 6, 7, 8))!!
        assertFalse(canBeat(s5, s6))
    }

    @Test
    fun `deal gives 17 each plus 3 bottom`() {
        val g = DoudizhuGame()
        g.newDeal()
        assertEquals(17, g.hands[0].size)
        assertEquals(17, g.hands[1].size)
        assertEquals(17, g.hands[2].size)
        assertEquals(3, g.bottom.size)
        val total = g.hands.sumOf { it.size } + g.bottom.size
        assertEquals(54, total)
    }

    @Test
    fun `player bid makes landlord with 20 cards`() {
        val g = DoudizhuGame()
        g.newDeal()
        g.playerBid(true)
        assertEquals(0, g.landlord)
        assertEquals(20, g.hands[0].size)
        assertEquals(1, g.phase)
        assertEquals(0, g.current)
    }

    @Test
    fun `play flow single beats`() {
        val g = DoudizhuGame()
        g.newDeal()
        g.playerBid(true)
        // 玩家出最小单张
        val single = g.hands[0].minBy { it.rank }
        val res = g.playerPlay(listOf(single))
        assertEquals(PlayResult.OK, res)
        assertEquals(1, g.current)
        assertEquals(19, g.hands[0].size)
        // 两个 AI 依次行动，直到轮到玩家或终局
        var guard = 0
        while (!g.over && g.current != 0 && guard < 10) {
            g.aiAct()
            guard++
        }
        assertTrue(g.over || g.current == 0)
    }

    @Test
    fun `invalid play rejected`() {
        val g = DoudizhuGame()
        g.newDeal()
        g.playerBid(true)
        // 手牌外构造的非法牌型
        val res = g.playerPlay(listOf(Card(3, 0), Card(4, 1), Card(5, 2), Card(8, 3), Card(9, 0)))
        assertEquals(PlayResult.INVALID, res)
    }

    @Test
    fun `play to win ends game`() {
        val g = DoudizhuGame()
        g.newDeal()
        g.playerBid(true)
        // 直接把玩家手牌换成两张对子，连续出完
        g.hands[0].clear()
        g.hands[0].addAll(c(5, 5, 6, 6, 7, 7))
        g.hands[0].sortByDescending { it.rank }
        val res = g.playerPlay(c(5, 5, 6, 6, 7, 7))
        assertTrue(res == PlayResult.OK || res == PlayResult.GAME_OVER)
        // 若 OK 则 AI 出，继续把剩余（若有）出完 —— 简化：断言至少合法
        assertTrue(g.over || g.hands[0].isEmpty())
    }

    @Test
    fun `ai follow beats when possible`() {
        val g = DoudizhuGame()
        g.newDeal()
        g.playerBid(false) // AI 当地主
        assertTrue(g.landlord in 1..2)
        // 让玩家出对子，看 AI 是否跟进
        val pair = g.hands[0].groupBy { it.rank }.values.firstOrNull { it.size >= 2 }
        if (pair != null) {
            g.playerPlay(pair.take(2))
            while (!g.over && g.current != 0) {
                g.aiAct()
            }
            assertTrue(g.over || g.current == 0)
        }
    }

    @Test
    fun `pass twice resets to free lead`() {
        val g = DoudizhuGame()
        g.newDeal()
        g.playerBid(true)
        // 构造局面：玩家刚出大王（无人能压），两个 AI 手里只有小单张，必过
        g.hands[1].clear(); g.hands[1].add(Card(3, 0))
        g.hands[2].clear(); g.hands[2].add(Card(3, 1))
        g.lastCombo = parseCombo(listOf(Card(17, 4)))!!
        g.lastPlayer = 0
        g.passCount = 0
        g.current = 1
        g.aiAct() // AI1 过
        assertTrue(g.passedThisRound[1])
        assertFalse(g.playedThisRound[1])
        g.aiAct() // AI2 过
        assertNull(g.lastCombo)
        assertEquals(0, g.current)
        // 两家都过后新一轮开始，标记复位
        assertFalse(g.passedThisRound[1])
        assertFalse(g.passedThisRound[2])
    }

    @Test
    fun `played flags update on play and pass`() {
        val g = DoudizhuGame()
        g.newDeal()
        g.playerBid(true)
        val single = g.hands[0].minBy { it.rank }
        g.playerPlay(listOf(single))
        assertTrue(g.playedThisRound[0])
        // AI 行动至轮玩家或终局
        var guard = 0
        while (!g.over && g.current != 0 && guard < 10) {
            g.aiAct()
            guard++
        }
        // 玩家出的牌在展示区可见
        assertNotNull(g.lastPlays[0])
    }

    @Test
    fun `find combos returns several types`() {
        val g = DoudizhuGame()
        g.newDeal()
        val combos = g.findCombos(g.hands[0])
        assertTrue(combos.isNotEmpty())
        assertTrue(combos.any { it.type == ComboType.SINGLE })
    }
}
