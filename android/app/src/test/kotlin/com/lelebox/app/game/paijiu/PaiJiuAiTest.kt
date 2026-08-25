package com.lelebox.app.game.paijiu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** M2v2：AI 策略单测 */
class PaiJiuAiTest {

    @Test
    fun `easy lead is weakest single`() {
        val hand = listOf(PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A, PaiJiuTiles.BAN_DENG, PaiJiuTiles.XIAO_CHUI)
        val lead = PaiJiuAi.chooseLead(hand, PaiJiuDifficulty.EASY)
        // 最小 = 同门内 order 最大：天门板凳(order7) vs 杂门杂九(order1) vs 幺门小锤(order4)
        // order 全局跨门，但不同门不可比。EASY 取 name.order 最大 = 板凳
        assertEquals(listOf(PaiJiuTiles.BAN_DENG), lead)
    }

    @Test
    fun `hard lead prefers dui hou`() {
        val hand = listOf(PaiJiuTiles.HOU_LIU, PaiJiuTiles.HOU_SAN, PaiJiuTiles.TIAN, PaiJiuTiles.ZA_JIU_A)
        val lead = PaiJiuAi.chooseLead(hand, PaiJiuDifficulty.HARD)
        assertEquals(2, lead.size)
        assertTrue(PaiJiuTiles.isDuiHou(lead[0], lead[1]))
    }

    @Test
    fun `medium lead prefers pair`() {
        val hand = listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN, PaiJiuTiles.DI, PaiJiuTiles.ZA_JIU_A)
        val lead = PaiJiuAi.chooseLead(hand, PaiJiuDifficulty.MEDIUM)
        assertEquals(2, lead.size)
        assertEquals(TileName.TIAN, lead[0].name)
    }

    @Test
    fun `follow picks just enough`() {
        // 对方出板凳(order7)：天(order1)、人(order3) 都能管；"刚够大"= 人
        val hand = listOf(PaiJiuTiles.TIAN, PaiJiuTiles.REN, PaiJiuTiles.ZA_JIU_A)
        val current = PaiJiuRules.classify(listOf(PaiJiuTiles.BAN_DENG))!!
        val follow = PaiJiuAi.chooseFollow(hand, current, PaiJiuDifficulty.EASY)
        assertNotNull(follow)
        assertEquals(TileName.REN, follow!![0].name)
    }

    @Test
    fun `follow null when nothing beats`() {
        val hand = listOf(PaiJiuTiles.BAN_DENG, PaiJiuTiles.XIAO_CHUI)
        val current = PaiJiuRules.classify(listOf(PaiJiuTiles.TIAN))!!
        assertNull(PaiJiuAi.chooseFollow(hand, current, PaiJiuDifficulty.EASY))
    }

    @Test
    fun `pass returns same count`() {
        val hand = listOf(PaiJiuTiles.TIAN, PaiJiuTiles.DI, PaiJiuTiles.REN, PaiJiuTiles.ZA_JIU_A)
        val current = PaiJiuRules.classify(listOf(PaiJiuTiles.BAN_DENG))!!
        val pass = PaiJiuAi.choosePass(hand, current, PaiJiuDifficulty.MEDIUM)
        assertEquals(current.count, pass.size)
        assertTrue(pass.all { it in hand })
    }

    @Test
    fun `AI driven full games terminate`() {
        for (diff in PaiJiuDifficulty.entries) {
            repeat(50) { seed ->
                val g = PaiJiuGame(seed = seed.toLong())
                var guard = 0
                while (!g.gameOver && guard < 500) {
                    val p = g.current
                    if (g.isLeadPhase) {
                        g.play(p, PaiJiuAi.chooseLead(g.hands[p], diff))
                    } else {
                        val cur = g.currentPlay!!
                        val f = PaiJiuAi.chooseFollow(g.hands[p], cur, diff)
                        if (f != null) g.play(p, f) else g.pass(p, PaiJiuAi.choosePass(g.hands[p], cur, diff))
                    }
                    guard++
                }
                assertTrue("terminate diff=$diff seed=$seed", g.gameOver)
                assertTrue(g.dePai in 0..3)
            }
        }
    }
}
