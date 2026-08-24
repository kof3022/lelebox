package com.lelebox.app.game.paijiu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** M2：AI 策略单测 */
class PaiJiuAiTest {

    // ---------- 领出 ----------

    @Test
    fun `easy lead prefers supreme then pairs then smallest single`() {
        // 有至尊
        val hand = listOf(
            PaiJiuTiles.SUPREME_A, PaiJiuTiles.SUPREME_B,
            PaiJiuTiles.HEAVEN, PaiJiuTiles.MIX5_A,
        )
        val lead = PaiJiuAi.chooseLead(hand, PaiJiuDifficulty.EASY)
        assertTrue(PaiJiuPlay(lead).isSupreme)

        // 无至尊有文对
        val hand2 = listOf(PaiJiuTiles.HEAVEN, PaiJiuTiles.HEAVEN, PaiJiuTiles.MIX9_A, PaiJiuTiles.MIX5_A)
        val lead2 = PaiJiuAi.chooseLead(hand2, PaiJiuDifficulty.EASY)
        assertEquals(2, lead2.size)
        assertEquals(TileRank.HEAVEN, lead2[0].rank)

        // 无对子出最弱单张（杂五 order 最大=最弱）
        val hand3 = listOf(PaiJiuTiles.HEAVEN, PaiJiuTiles.MIX9_A, PaiJiuTiles.MIX5_A)
        val lead3 = PaiJiuAi.chooseLead(hand3, PaiJiuDifficulty.EASY)
        assertEquals(listOf(PaiJiuTiles.MIX5_A), lead3)
    }

    @Test
    fun `hard lead keeps supreme and uses biggest pair`() {
        val hand = listOf(
            PaiJiuTiles.SUPREME_A, PaiJiuTiles.SUPREME_B,
            PaiJiuTiles.MAN, PaiJiuTiles.MAN,
            PaiJiuTiles.EARTH,
        )
        // 困难：至尊保留，出大对（人牌对）
        val lead = PaiJiuAi.chooseLead(hand, PaiJiuDifficulty.HARD)
        assertEquals(2, lead.size)
        assertEquals(TileRank.MAN, lead[0].rank)
        assertTrue(!PaiJiuPlay(lead).isSupreme)
    }

    // ---------- 接牌 ----------

    @Test
    fun `easy follow picks just enough beating play`() {
        // 对方出人牌：天(order1)、地(order2) 都能接；"刚够大"= 地（最弱可接）
        val hand = listOf(PaiJiuTiles.HEAVEN, PaiJiuTiles.EARTH, PaiJiuTiles.MIX9_A)
        val current = PaiJiuPlay.single(PaiJiuTiles.MAN)
        val follow = PaiJiuAi.chooseFollow(hand, current, PaiJiuDifficulty.EASY)
        assertNotNull(follow)
        assertEquals(PaiJiuTiles.EARTH, follow!![0])
    }

    @Test
    fun `medium follow avoids wasting heaven and earth`() {
        // 对方出板凳(order7)：能用杂九(order12)? 不，杂九是武牌接不了文牌板凳
        // 可接文牌：天/地/人/和/梅/长三（order<7 的文牌）
        val hand = listOf(PaiJiuTiles.HEAVEN, PaiJiuTiles.MAN, PaiJiuTiles.HARMONY, PaiJiuTiles.MIX9_A)
        val current = PaiJiuPlay.single(PaiJiuTiles.BENCH)
        // 中等：避免用天，用"刚够大"的人/和——最弱可接非大牌 = 和(order4)
        val follow = PaiJiuAi.chooseFollow(hand, current, PaiJiuDifficulty.MEDIUM)
        assertEquals(PaiJiuTiles.HARMONY, follow!![0])
    }

    @Test
    fun `follow returns null when nothing beats`() {
        val hand = listOf(PaiJiuTiles.MIX5_A, PaiJiuTiles.SUPREME_B)
        val current = PaiJiuPlay.single(PaiJiuTiles.MIX9_A)
        val follow = PaiJiuAi.chooseFollow(hand, current, PaiJiuDifficulty.EASY)
        assertNull(follow)
    }

    @Test
    fun `hard follow declines beating heaven when not worth it`() {
        val hand = listOf(PaiJiuTiles.EARTH, PaiJiuTiles.MIX9_A, PaiJiuTiles.MIX5_A)
        val current = PaiJiuPlay.single(PaiJiuTiles.HEAVEN)
        // 困难：对方出天，用地球接不划算 → 返回 null（垫牌）
        assertNull(PaiJiuAi.chooseFollow(hand, current, PaiJiuDifficulty.HARD))
    }

    // ---------- 垫牌 ----------

    @Test
    fun `pass keeps pairs and big tiles`() {
        // 天对 + 杂五对 + 孤张杂九 → 应弃孤张杂九（保留对子与大牌）
        val hand = listOf(PaiJiuTiles.HEAVEN, PaiJiuTiles.HEAVEN, PaiJiuTiles.MIX9_A, PaiJiuTiles.MIX5_A, PaiJiuTiles.MIX5_B)
        val pass = PaiJiuAi.choosePass(hand, PaiJiuDifficulty.MEDIUM)
        assertEquals(PaiJiuTiles.MIX9_A, pass)
    }

    @Test
    fun `pass with supreme components keeps them`() {
        val hand = listOf(PaiJiuTiles.SUPREME_A, PaiJiuTiles.SUPREME_B, PaiJiuTiles.MIX9_A, PaiJiuTiles.MIX5_A)
        val pass = PaiJiuAi.choosePass(hand, PaiJiuDifficulty.HARD)
        // 至尊组件价值最高（-50），不弃它们
        assertTrue(pass != PaiJiuTiles.SUPREME_A && pass != PaiJiuTiles.SUPREME_B)
    }

    // ---------- 完整 AI 对局 ----------

    @Test
    fun `AI driven full games terminate with all difficulties`() {
        for (diff in PaiJiuDifficulty.entries) {
            repeat(100) { seed ->
                val g = PaiJiuGame(seed = seed.toLong())
                var guard = 0
                while (!g.gameOver && guard < 500) {
                    val p = g.current
                    if (p == 0) {
                        // 玩家用简单策略模拟
                        if (g.isLeadPhase) {
                            val lead = PaiJiuAi.chooseLead(g.hands[0], PaiJiuDifficulty.EASY)
                            g.play(0, lead)
                        } else {
                            val f = PaiJiuAi.chooseFollow(g.hands[0], g.currentPlay!!, PaiJiuDifficulty.EASY)
                            if (f != null) g.play(0, f) else g.pass(0, PaiJiuAi.choosePass(g.hands[0], PaiJiuDifficulty.EASY))
                        }
                    } else {
                        if (g.isLeadPhase) {
                            g.play(p, PaiJiuAi.chooseLead(g.hands[p], diff))
                        } else {
                            val f = PaiJiuAi.chooseFollow(g.hands[p], g.currentPlay!!, diff)
                            if (f != null) g.play(p, f) else g.pass(p, PaiJiuAi.choosePass(g.hands[p], diff))
                        }
                    }
                    guard++
                }
                assertTrue("terminate diff=$diff seed=$seed", g.gameOver)
                assertTrue(g.winner in 0..3)
            }
        }
    }
}
