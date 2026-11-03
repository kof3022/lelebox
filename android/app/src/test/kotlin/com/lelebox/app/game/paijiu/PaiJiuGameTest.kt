package com.lelebox.app.game.paijiu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** M1v2：对局状态机（领出/管上/垫牌/墩/得牌/结牌/扇/结算） */
class PaiJiuGameTest {

    /** 强制玩家(0)先手领出（newRound 随机领出，测试需固定） */
    private fun PaiJiuGame.forcePlayerLead() {
        leader = 0
        current = 0
        currentPlay = null
        currentPlayPlayer = -1
    }

    @Test
    fun `new round deals 8 each and someone leads`() {
        val g = PaiJiuGame(seed = 7)
        for (p in 0..3) assertEquals(8, g.hands[p].size)
        assertTrue(g.current in 0..3)
        assertTrue(g.isLeadPhase)
        assertFalse(g.gameOver)
    }

    @Test
    fun `player leads a single`() {
        val g = PaiJiuGame(seed = 7)
        g.forcePlayerLead()
        val t = g.hands[0][0]
        assertTrue(g.play(0, listOf(t)))
        assertEquals(1, g.currentPlay?.count)
        assertEquals(7, g.hands[0].size)
        assertEquals(1, g.current)
    }

    @Test
    fun `player leads a pair`() {
        val g = PaiJiuGame(seed = 7)
        g.forcePlayerLead()
        val hand = g.hands[0].toList()
        val pair = hand.groupBy { it.name }.entries.firstOrNull { it.value.size >= 2 }
        if (pair != null) {
            val (a, b) = pair.value
            assertTrue(g.play(0, listOf(a, b)))
            assertEquals(2, g.currentPlay?.count)
            assertEquals(PlayKind.PAIR, g.currentPlay?.kind)
        } else {
            assertTrue(g.play(0, listOf(hand[0])))
        }
    }

    @Test
    fun `can beat with larger same-kind`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        // 玩家领地牌，孙权用天牌管上
        val di = PaiJiuTiles.DI
        val tian = PaiJiuTiles.TIAN
        g.hands[0].clear(); g.hands[0].addAll(listOf(di) + PaiJiuTiles.FULL_SET.take(7))
        g.hands[1].clear(); g.hands[1].addAll(listOf(tian) + PaiJiuTiles.FULL_SET.drop(8).take(7))
        g.play(0, listOf(di))
        assertTrue(g.play(1, listOf(tian)))
        assertEquals(1, g.currentPlayPlayer)
    }

    @Test
    fun `cannot beat with different door`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        val di = PaiJiuTiles.DI
        val zaJiu = PaiJiuTiles.ZA_JIU_A
        g.hands[0].clear(); g.hands[0].addAll(listOf(di) + PaiJiuTiles.FULL_SET.take(7))
        g.hands[1].clear(); g.hands[1].addAll(listOf(zaJiu) + PaiJiuTiles.FULL_SET.drop(8).take(7))
        g.play(0, listOf(di))
        assertFalse(g.play(1, listOf(zaJiu))) // 杂门不能管天门
    }

    @Test
    fun `pass requires same count and not bigger`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        val di = PaiJiuTiles.DI
        val tian = PaiJiuTiles.TIAN
        val ren = PaiJiuTiles.REN
        g.hands[0].clear(); g.hands[0].addAll(listOf(di) + PaiJiuTiles.FULL_SET.take(7))
        g.hands[1].clear(); g.hands[1].addAll(listOf(tian, ren) + PaiJiuTiles.FULL_SET.drop(8).take(6))
        g.play(0, listOf(di))
        // 垫牌不能比当前更大（天牌比地牌大 → 拒绝）
        assertFalse(g.pass(1, listOf(tian)))
        // 垫同数量、更小的牌（人牌比地牌小但不同门不能比——人牌也是天门，人<地）
        // 人牌比地牌小，可以垫
        assertTrue(g.pass(1, listOf(ren)))
        assertTrue(g.lastPassed[1])
    }

    @Test
    fun `dui hou lead cannot be beaten`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        val hou6 = PaiJiuTiles.HOU_LIU
        val hou3 = PaiJiuTiles.HOU_SAN
        val tian = PaiJiuTiles.TIAN
        g.hands[0].clear(); g.hands[0].addAll(listOf(hou6, hou3) + PaiJiuTiles.FULL_SET.take(6))
        g.hands[1].clear(); g.hands[1].addAll(listOf(tian, tian) + PaiJiuTiles.FULL_SET.drop(8).take(6))
        assertTrue(g.play(0, listOf(hou6, hou3))) // 首家出对猴
        assertTrue(g.currentPlay!!.isDuiHou)
        // 其他家不能管上
        assertFalse(g.play(1, listOf(tian, tian)))
        // 只能垫牌
        assertTrue(g.pass(1, listOf(g.hands[1][0], g.hands[1][1])))
    }

    @Test
    fun `round ends winner leads next and trick counted`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        val lead = g.hands[0][0]
        g.play(0, listOf(lead))
        // 三家垫牌（同数量、更小）
        for (p in 1..3) {
            val cur = g.currentPlay!!
            val pad = g.hands[p].filter { t -> t.name != cur.tiles[0].name && t.suit != cur.tiles[0].suit }.take(cur.count)
            val passTiles = if (pad.size == cur.count) pad else g.hands[p].take(cur.count)
            g.pass(p, passTiles)
        }
        assertTrue(g.isLeadPhase)
        assertEquals(0, g.current)
        assertEquals(1, g.trickCount[0]) // 单张一圈 = 1 墩
    }

    @Test
    fun `pair round gives 2 tricks`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        val pair = g.hands[0].groupBy { it.name }.entries.firstOrNull { it.value.size >= 2 }
        if (pair == null) return // 无对子跳过
        val (a, b) = pair.value
        g.play(0, listOf(a, b))
        // 三家垫（同数量2张）
        for (p in 1..3) {
            val cur = g.currentPlay!!
            val pad = g.hands[p].take(cur.count)
            g.pass(p, pad)
        }
        assertEquals(2, g.trickCount[0]) // 对子一圈 = 2 墩
    }

    @Test
    fun `de pai when hand empty and last trick`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        // 玩家只有 1 张牌，领出后赢墩即得牌
        g.hands[0].clear()
        g.hands[0].add(PaiJiuTiles.TIAN)
        assertTrue(g.play(0, listOf(PaiJiuTiles.TIAN)))
        // 三家垫
        for (p in 1..3) {
            val cur = g.currentPlay!!
            val pad = g.hands[p].take(cur.count)
            g.pass(p, pad)
        }
        assertEquals(0, g.dePai)
        assertEquals(1, g.finished.size)
    }

    @Test
    fun `jie type da dui for tian pair`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        // 玩家最后出对天 → 大对结
        g.hands[0].clear()
        g.hands[0].addAll(listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN))
        assertTrue(g.play(0, listOf(PaiJiuTiles.TIAN, PaiJiuTiles.TIAN)))
        for (p in 1..3) {
            val cur = g.currentPlay!!
            val pad = g.hands[p].take(cur.count)
            g.pass(p, pad)
        }
        assertEquals(PaiJiuGame.JieType.DA_DUI, g.jieType)
    }

    @Test
    fun `jie type hou for dui hou`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        g.hands[0].clear()
        g.hands[0].addAll(listOf(PaiJiuTiles.HOU_LIU, PaiJiuTiles.HOU_SAN))
        assertTrue(g.play(0, listOf(PaiJiuTiles.HOU_LIU, PaiJiuTiles.HOU_SAN)))
        for (p in 1..3) {
            val cur = g.currentPlay!!
            val pad = g.hands[p].take(cur.count)
            g.pass(p, pad)
        }
        assertEquals(PaiJiuGame.JieType.HOU, g.jieType)
    }

    @Test
    fun `settlement normal 4 base`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        // 手动构造：得牌玩家（0），其他三家各 3 墩（<4 输 1 墩）
        g.dePai = 0
        g.jieType = PaiJiuGame.JieType.NONE
        g.trickCount[0] = 5
        g.trickCount[1] = 3
        g.trickCount[2] = 3
        g.trickCount[3] = 3
        g.gameOver = true
        g.settlement = g.settle()
        // 三家各输 1 墩 → 得牌赢 3 墩
        assertEquals(3, g.settlement)
    }

    @Test
    fun `settlement da dui base 6`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        g.dePai = 0
        g.jieType = PaiJiuGame.JieType.DA_DUI
        g.trickCount[0] = 6
        g.trickCount[1] = 5
        g.trickCount[2] = 5
        g.trickCount[3] = 5
        g.gameOver = true
        g.settlement = g.settle()
        // 三家各 5 墩 <6 输 1 → 得牌赢 3
        assertEquals(3, g.settlement)
    }

    @Test
    fun `settlement zero-sum includes winners not just losses`() {
        // 用户报告：得牌(0)、孙权(1)5墩赢1、曹操(2)0墩输4、刘备(3)0墩输4 → 应赢 4+4-1=7（旧实现错算为 8）
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        g.dePai = 0
        g.jieType = PaiJiuGame.JieType.NONE
        g.trickCount[0] = 3
        g.trickCount[1] = 5 // 孙权 入一（5 墩 > 基准 4，赢 1 墩）
        g.trickCount[2] = 0
        g.trickCount[3] = 0
        g.gameOver = true
        g.settlement = g.settle()
        assertEquals(7, g.settlement)
        assertEquals(0, g.settlementDetail.sum()) // 零和
        assertEquals(1, g.settlementDetail[1])
        assertEquals(-4, g.settlementDetail[2])
        assertEquals(-4, g.settlementDetail[3])
    }

    @Test
    fun `settlement liu fan gives depai bonus and stays zero-sum`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        g.dePai = 0
        g.jieType = PaiJiuGame.JieType.NONE
        g.trickCount[0] = 6
        g.trickCount[1] = 1
        g.trickCount[2] = 1
        g.trickCount[3] = 0
        g.trickOrder.clear()
        g.trickOrder.addAll(listOf(0, 0, 0, 0, 0, 0, 1, 2)) // 连赢前 6 张
        g.gameOver = true
        g.settlement = g.settle()
        assertEquals("六扇", g.fanOf(0))
        // 三家各多输 1：1-4-1=-4、1-4-1=-4、0-4-1=-5 → 得牌 13
        assertEquals(13, g.settlement)
        assertEquals(0, g.settlementDetail.sum())
    }

    @Test
    fun `settlement qi fan gives depai bonus and stays zero-sum`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        g.dePai = 0
        g.jieType = PaiJiuGame.JieType.NONE
        g.trickCount[0] = 7
        g.trickCount[1] = 1
        g.trickCount[2] = 0
        g.trickCount[3] = 0
        g.trickOrder.clear()
        g.trickOrder.addAll(listOf(0, 0, 0, 0, 0, 0, 0, 1)) // 连赢前 7 张
        g.gameOver = true
        g.settlement = g.settle()
        assertEquals("七扇", g.fanOf(0))
        // 三家各多输 2：1-4-2=-5、-6、-6 → 得牌 17
        assertEquals(17, g.settlement)
        assertEquals(0, g.settlementDetail.sum())
    }

    @Test
    fun `settlement liu fan goes to fan player not depai`() {
        // 用户报告：你(0)2墩得牌、曹操(2)六扇、孙权(1)刘备(3)0墩
        // → 刘/孙各输5、曹操赢4、你得牌赢6（扇奖归扇主；得牌者不变）
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        g.dePai = 0
        g.jieType = PaiJiuGame.JieType.NONE
        g.trickCount[0] = 2
        g.trickCount[1] = 0
        g.trickCount[2] = 6
        g.trickCount[3] = 0
        g.trickOrder.clear()
        g.trickOrder.addAll(listOf(2, 2, 2, 2, 2, 2, 0, 0)) // 曹操连赢前 6 张
        g.gameOver = true
        g.settlement = g.settle()
        assertEquals("六扇", g.fanOf(2))
        assertEquals(0, g.settlementDetail.sum()) // 零和
        assertEquals(6, g.settlement)
        assertEquals(6, g.settlementDetail[0])  // 你得牌 6
        assertEquals(-5, g.settlementDetail[1]) // 孙权 -5
        assertEquals(4, g.settlementDetail[2])  // 曹操 +4
        assertEquals(-5, g.settlementDetail[3]) // 刘备 -5
    }

    @Test
    fun `settlement qi fan goes to fan player not depai`() {
        // 用户报告：你(0)1墩得牌、曹操(2)七扇、刘/孙0墩
        // → 刘/孙各输6、曹操赢7、你得牌赢5
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        g.dePai = 0
        g.jieType = PaiJiuGame.JieType.NONE
        g.trickCount[0] = 1
        g.trickCount[1] = 0
        g.trickCount[2] = 7
        g.trickCount[3] = 0
        g.trickOrder.clear()
        g.trickOrder.addAll(listOf(2, 2, 2, 2, 2, 2, 2, 0)) // 曹操连赢前 7 张
        g.gameOver = true
        g.settlement = g.settle()
        assertEquals("七扇", g.fanOf(2))
        assertEquals(0, g.settlementDetail.sum())
        assertEquals(5, g.settlement)
        assertEquals(5, g.settlementDetail[0])
        assertEquals(-6, g.settlementDetail[1])
        assertEquals(7, g.settlementDetail[2])
        assertEquals(-6, g.settlementDetail[3])
    }

    @Test
    fun `settlement ba fan special pays per to losers`() {
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        g.dePai = 0
        g.jieType = PaiJiuGame.JieType.NONE
        g.trickCount[0] = 8
        g.trickOrder.clear()
        g.trickOrder.addAll(List(8) { 0 }) // 赢满 8 张
        g.gameOver = true
        g.settlement = g.settle()
        assertEquals("八扇", g.fanOf(0))
        assertEquals(24, g.settlement) // 三家各输 8 → 得牌 8×3
        assertEquals(0, g.settlementDetail.sum())
        assertEquals(-8, g.settlementDetail[1])
        assertEquals(24, g.settlementDetail[0])
    }

    @Test
    fun `fan counts cards not dun count dui hou 4 dun only 2 cards`() {
        // 用户规则：扇看「出的牌数」不看墩数。
        // 孙权(1) 出对猴(2张)获4墩 + 天九(2张)获2墩 = 墩数 6，但只出了 4 张牌 → 不算六扇
        val g = PaiJiuGame(seed = 7)
                g.newRound()
        g.forcePlayerLead()
        g.dePai = 0
        g.jieType = PaiJiuGame.JieType.NONE
        g.trickCount[0] = 2
        g.trickCount[1] = 6 // 对猴4 + 天九2 = 6 墩（但只 4 张）
        g.trickCount[2] = 0
        g.trickCount[3] = 0
        g.trickOrder.clear()
        // 出牌顺序按张数：孙权 2+2=4 张，第 5 张起才是他家的
        g.trickOrder.addAll(listOf(1, 1, 1, 1, 0, 0, 2, 3))
        g.gameOver = true
        g.settlement = g.settle()
        assertNull(g.fanOf(1)) // 墩数 6 但只有 4 张 → 不算六扇
        // 无扇 → 普通基准结算：孙权 6-4=+2、你得牌赢 6、曹/刘各-4
        assertEquals(0, g.settlementDetail.sum()) // 零和
        assertEquals(6, g.settlementDetail[0])
        assertEquals(2, g.settlementDetail[1])
        assertEquals(-4, g.settlementDetail[2])
        assertEquals(-4, g.settlementDetail[3])
    }

    @Test
    fun `full random game terminates`() {
        var wins = 0
        repeat(100) { seed ->
            val g = PaiJiuGame(seed = seed.toLong())
            var guard = 0
            while (!g.gameOver && guard < 500) {
                val p = g.current
                if (g.isLeadPhase) {
                    g.play(p, PaiJiuAi.chooseLead(g.hands[p], PaiJiuDifficulty.EASY))
                } else {
                    val cur = g.currentPlay!!
                    val f = PaiJiuAi.chooseFollow(g.hands[p], cur, PaiJiuDifficulty.EASY)
                    if (f != null) g.play(p, f) else g.pass(p, PaiJiuAi.choosePass(g.hands[p], cur, PaiJiuDifficulty.EASY))
                }
                guard++
            }
            assertTrue("terminate seed=$seed", g.gameOver)
            assertTrue(g.dePai in 0..3)
            wins += if (g.dePai == 0) 1 else 0
        }
        println("player depai rate: $wins/100")
    }
}

