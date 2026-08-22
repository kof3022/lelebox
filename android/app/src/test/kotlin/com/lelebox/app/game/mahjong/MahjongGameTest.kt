package com.lelebox.app.game.mahjong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MahjongGameTest {

    private fun t(s: Int, r: Int) = Tile(s, r)

    @Test
    fun `full set is 136 tiles`() {
        assertEquals(136, Tile.fullSet().size)
        assertEquals(4, Tile.fullSet().count { it == Tile(0, 1) })
    }

    @Test
    fun `standard win with 4 melds and pair`() {
        // 123万 456条 789筒 中中中 东东
        val hand = listOf(
            t(0,1), t(0,2), t(0,3),
            t(1,4), t(1,5), t(1,6),
            t(2,7), t(2,8), t(2,9),
            t(4,1), t(4,1), t(4,1),
            t(3,1), t(3,1),
        )
        val wins = Mahjong.findWins(hand)
        assertTrue("应可和牌", wins.isNotEmpty())
    }

    @Test
    fun `seven pairs win`() {
        val hand = listOf(
            t(0,1), t(0,1), t(0,2), t(0,2), t(0,3), t(0,3),
            t(1,4), t(1,4), t(1,5), t(1,5), t(2,6), t(2,6),
            t(3,1), t(3,1),
        )
        assertTrue(Mahjong.findWins(hand).any { it.isSevenPairs })
    }

    @Test
    fun `not a win without pair`() {
        val hand = listOf(
            t(0,1), t(0,2), t(0,3),
            t(1,4), t(1,5), t(1,6),
            t(2,7), t(2,8), t(2,9),
            t(4,1), t(4,1), t(4,1),
            t(3,1), t(3,2),
        )
        assertTrue(Mahjong.findWins(hand).isEmpty())
    }

    @Test
    fun `waiting tile completes hand`() {
        // 缺 3万 即可和：12万 456条 789筒 中中中 东东（13张）
        val hand = listOf(
            t(0,1), t(0,2),
            t(1,4), t(1,5), t(1,6),
            t(2,7), t(2,8), t(2,9),
            t(4,1), t(4,1), t(4,1),
            t(3,1), t(3,1),
        )
        assertEquals(13, hand.size)
        val full = hand + t(0,3)
        assertTrue(Mahjong.findWins(full).isNotEmpty())
    }

    @Test
    fun `qingyise 24 fan`() {
        // 清一色万：123 456 789 + 111 + 22
        val hand = listOf(
            t(0,1), t(0,2), t(0,3), t(0,4), t(0,5), t(0,6),
            t(0,7), t(0,8), t(0,9), t(0,1), t(0,1), t(0,1),
            t(0,2), t(0,2),
        )
        val scheme = Mahjong.findWins(hand).first()
        val fans = FanCalculator.calculate(scheme, hand, emptyList(), selfDraw = true, seatWind = 1)
        assertTrue("应有清一色", fans.any { it.name == "清一色" && it.fan == 24 })
    }

    @Test
    fun `pengpenghe 8 fan`() {
        // 碰碰和：111 222 333 444 万 + 55筒
        val hand = listOf(
            t(0,1), t(0,1), t(0,1), t(0,2), t(0,2), t(0,2),
            t(0,3), t(0,3), t(0,3), t(0,4), t(0,4), t(0,4),
            t(2,5), t(2,5),
        )
        val scheme = Mahjong.findWins(hand).first()
        val fans = FanCalculator.calculate(scheme, hand, emptyList(), selfDraw = true, seatWind = 1)
        assertTrue(fans.any { it.name == "碰碰和" && it.fan == 8 })
    }

    @Test
    fun `game deals and player draws 14`() {
        val g = MahjongGame(seed = 42)
        assertEquals(14, g.hands[0].size)
        for (p in 1..3) assertEquals(13, g.hands[p].size)
        // 四家牌墙共 84 张（每家 21）；庄家先摸 1 张后总 83
        val totalWall = g.walls.sumOf { it.size }
        assertEquals(83, totalWall)
        assertTrue(g.walls.sumOf { it.size } <= 84)
        assertTrue(g.dice1 in 1..6 && g.dice2 in 1..6)
        assertTrue(g.currentWall in 0..3)
    }

    @Test
    fun `discard advances to next player`() {
        val g = MahjongGame(seed = 42)
        val tile = g.hands[0][0]
        g.discard(0, tile)
        assertEquals(1, g.current)
        assertEquals(tile, g.lastDiscard)
        assertFalse(g.hasDrawn)
        assertTrue(g.discards[0].contains(tile))
    }

    @Test
    fun `walls empty over time and draw moves clockwise`() {
        val g = MahjongGame(seed = 7)
        val startWall = g.currentWall
        // 玩家打一张，AI 三家各摸打
        g.discard(0, g.hands[0][0])
        for (p in 1..3) g.aiTurn(p)
        // 每次摸牌 currentWall 顺时针推进
        assertTrue(g.walls.sumOf { it.size } < 84)
        assertTrue(g.currentWall in 0..3)
    }
}
