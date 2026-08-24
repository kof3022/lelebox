package com.lelebox.app.game.paijiu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** M1：核心对局状态机（发牌-出牌-接牌-垫牌-赢墩-领出-结牌） */
class PaiJiuGameTest {

    // ---------- 发牌与初始状态 ----------

    @Test
    fun `new round deals 8 tiles each and player leads`() {
        val g = PaiJiuGame(seed = 7)
        for (p in 0..3) assertEquals(8, g.hands[p].size)
        assertEquals(0, g.current)
        assertTrue(g.isLeadPhase)
        assertFalse(g.gameOver)
    }

    // ---------- 领出 ----------

    @Test
    fun `player leads a single`() {
        val g = PaiJiuGame(seed = 7)
        val t = g.hands[0][0]
        assertTrue(g.play(0, listOf(t)))
        assertEquals(t, g.currentPlay?.tiles?.first())
        assertEquals(0, g.currentPlayPlayer)
        assertEquals(7, g.hands[0].size)
        assertEquals(1, g.current)
    }

    @Test
    fun `player cannot lead a pair of different civil tiles`() {
        val g = PaiJiuGame(seed = 7)
        // 构造：若手牌没有不同文牌对，此测试需找合法/非法输入
        val h = g.hands[0]
        val a = h[0]
        val b = h.firstOrNull { it.rank != a.rank || it.suit != a.suit } ?: h[1]
        assertFalse(g.play(0, listOf(a, b))) // 不同牌不是对子，不能领出
    }

    @Test
    fun `player leads a civil pair`() {
        val g = PaiJiuGame(seed = 7)
        val hand = g.hands[0].toList()
        val pairRank = hand.groupBy { it.rank }.entries.firstOrNull { it.value.size >= 2 }
        if (pairRank != null) {
            val a = pairRank.value[0]; val b = pairRank.value[1]
            assertTrue(g.play(0, listOf(a, b)))
            assertEquals(PlayKind.PAIR, g.currentPlay?.kind)
            assertEquals(6, g.hands[0].size)
        } else {
            // 无对子则领单张
            assertTrue(g.play(0, listOf(hand[0])))
        }
    }

    // ---------- 接牌（大食小） ----------

    @Test
    fun `next player can beat with larger single`() {
        val g = PaiJiuGame(seed = 7)
        // 玩家领出天牌（若有），否则领最小牌；构造确定性场景
        // 用受控发牌：把天/地放进固定玩家手里
        g.newRound()
        val hand1 = g.hands[1].toMutableList()
        // 玩家领出杂五（武牌），孙权(1) 若有杂九/杂八/杂七可接
        val lead = hand1.minByOrNull { it.rank.order }!!
        g.play(0, listOf(g.hands[0][0])) // 先领任意
        // 直接验证 canBeat 逻辑在 play 中的约束
        val cur = g.currentPlay!!
        val beats = PaiJiuRules.legalPlays(g.hands[1], cur)
        if (beats.isNotEmpty()) {
            assertTrue(g.play(1, beats[0].tiles))
            assertEquals(1, g.currentPlayPlayer)
        } else {
            // 接不住必须垫牌
            val t = g.hands[1][0]
            assertTrue(g.pass(1, t))
            assertTrue(g.isFinished(1) || g.hands[1].size == 7)
        }
    }

    @Test
    fun `cannot play different kind to beat`() {
        val g = PaiJiuGame(seed = 7)
        g.newRound()
        // 玩家领单张
        val lead = g.hands[0][0]
        g.play(0, listOf(lead))
        val cur = g.currentPlay!!
        if (cur.kind == PlayKind.SINGLE) {
            // 孙权试着用对子接单张 → 拒绝
            val pair = g.hands[1].groupBy { it.rank }.entries.firstOrNull { it.value.size >= 2 }
            if (pair != null) {
                assertFalse(g.play(1, listOf(pair.value[0], pair.value[1])))
            }
        }
    }

    @Test
    fun `civil and military cannot beat each other`() {
        val g = PaiJiuGame(seed = 7)
        // 构造：玩家出武牌（杂五），孙权有文牌（天）也不能接（文武不互接）
        // 通过直接操作 hands 构造确定性场景
        g.newRound()
        val mix5 = PaiJiuTiles.MIX5_A
        g.hands[0].clear(); g.hands[0].addAll(listOf(mix5) + PaiJiuTiles.FULL_SET.take(7))
        g.hands[1].clear(); g.hands[1].addAll(listOf(PaiJiuTiles.HEAVEN) + PaiJiuTiles.FULL_SET.drop(8).take(7))
        assertTrue(g.play(0, listOf(mix5)))
        // 孙权只有文牌天，不能接武牌杂五
        assertFalse(g.play(1, listOf(PaiJiuTiles.HEAVEN)))
        // 必须垫牌
        assertTrue(g.pass(1, PaiJiuTiles.HEAVEN))
    }

    // ---------- 一轮结束与赢墩领出 ----------

    @Test
    fun `round ends when all others pass and winner leads next round`() {
        val g = PaiJiuGame(seed = 7)
        g.newRound()
        // 玩家领单张
        val lead = g.hands[0][0]
        g.play(0, listOf(lead))
        // 三家依次垫牌
        for (p in 1..3) {
            val t = g.hands[p].minByOrNull { it.rank.order }!!
            g.pass(p, t)
        }
        // 一轮结束：玩家赢墩并领出下一轮
        assertTrue(g.isLeadPhase)
        assertEquals(0, g.current)
        assertEquals(0, g.leader)
    }

    @Test
    fun `beat then everyone passes gives beater the lead`() {
        val g = PaiJiuGame(seed = 7)
        g.newRound()
        // 玩家领出杂五（武牌最小档），孙权用杂九接
        val mix5 = PaiJiuTiles.MIX5_A
        g.hands[0].clear(); g.hands[0].addAll(listOf(mix5) + PaiJiuTiles.FULL_SET.take(7))
        g.hands[1].clear(); g.hands[1].addAll(listOf(PaiJiuTiles.MIX9_A) + PaiJiuTiles.FULL_SET.drop(8).take(7))
        g.hands[2].clear(); g.hands[2].addAll(PaiJiuTiles.FULL_SET.drop(16).take(8))
        g.hands[3].clear(); g.hands[3].addAll(PaiJiuTiles.FULL_SET.drop(24).take(8))
        assertTrue(g.play(0, listOf(mix5)))
        assertTrue(g.play(1, listOf(PaiJiuTiles.MIX9_A))) // 孙权接
        // 曹操、刘备垫牌
        g.pass(2, g.hands[2].minByOrNull { it.rank.order }!!)
        g.pass(3, g.hands[3].minByOrNull { it.rank.order }!!)
        // 孙权赢墩领出
        assertTrue(g.isLeadPhase)
        assertEquals(1, g.current)
        assertEquals(1, g.leader)
    }

    // ---------- 结牌与排名 ----------

    @Test
    fun `player finishing hand records rank and game continues`() {
        val g = PaiJiuGame(seed = 7)
        g.newRound()
        // 给玩家仅 2 张牌，模拟快速出完
        g.hands[0].clear()
        g.hands[0].addAll(listOf(PaiJiuTiles.HEAVEN, PaiJiuTiles.MIX9_A))
        // 玩家领出天牌 → 剩 1 张
        assertTrue(g.play(0, listOf(PaiJiuTiles.HEAVEN)))
        // 三家垫
        for (p in 1..3) g.pass(p, g.hands[p].minByOrNull { it.rank.order }!!)
        // 玩家领出杂九 → 出完结牌
        assertTrue(g.isLeadPhase)
        assertEquals(0, g.current)
        assertTrue(g.play(0, listOf(PaiJiuTiles.MIX9_A)))
        assertEquals(1, g.rankOf(0))
        assertTrue(g.isFinished(0))
        assertEquals(0, g.hands[0].size)
        // 对局未结束（还有 3 人）
        assertFalse(g.gameOver)
        // 轮到孙权领出（玩家已结牌被跳过）
        assertEquals(1, g.current)
    }

    @Test
    fun `game ends when 3 players finish`() {
        val g = PaiJiuGame(seed = 7)
        g.newRound()
        // 给三家各 1 张牌：玩家天牌、孙权地牌、曹操人牌（递减小）
        g.hands[0].clear(); g.hands[0].add(PaiJiuTiles.HEAVEN)
        g.hands[1].clear(); g.hands[1].add(PaiJiuTiles.EARTH)
        g.hands[2].clear(); g.hands[2].add(PaiJiuTiles.MAN)
        // 玩家领出天牌 → 出完 → 结牌 rank1
        assertTrue(g.play(0, listOf(g.hands[0][0])))
        assertEquals(1, g.rankOf(0))
        // 孙权轮：天牌大，孙权地牌接不住 → 垫牌出完 → 结牌 rank2
        assertEquals(1, g.current)
        assertFalse(g.isLeadPhase)
        assertTrue(g.pass(1, g.hands[1][0]))
        assertEquals(2, g.rankOf(1))
        // 曹操轮：接不住（人<地）→ 垫牌出完 → 3 人出完，游戏结束
        assertEquals(2, g.current)
        assertFalse(g.isLeadPhase)
        assertTrue(g.pass(2, g.hands[2][0]))
        assertEquals(3, g.rankOf(2))
        assertTrue(g.gameOver)
        assertEquals(0, g.winner) // 头科 = 玩家
        // 刘备未出完
        assertEquals(-1, g.rankOf(3))
    }

    // ---------- 完整模拟 ----------

    @Test
    fun `full random game always terminates with winner`() {
        var wins = 0
        repeat(200) { seed ->
            val g = PaiJiuGame(seed = seed.toLong())
            var guard = 0
            while (!g.gameOver && guard < 500) {
                val p = g.current
                if (g.isLeadPhase) {
                    // 领出：优先文对，否则单张
                    val hand = g.hands[p].toList()
                    val pair = hand.filter { it.suit == TileSuit.CIVIL }
                        .groupBy { it.rank }.entries.firstOrNull { it.value.size >= 2 }
                    if (pair != null) {
                        g.play(p, listOf(pair.value[0], pair.value[1]))
                    } else {
                        g.play(p, listOf(hand.minByOrNull { it.rank.order }!!))
                    }
                } else {
                    val cur = g.currentPlay!!
                    val beats = PaiJiuRules.legalPlays(g.hands[p], cur)
                    if (beats.isNotEmpty()) {
                        g.play(p, beats.minByOrNull { it.tiles.minOf { t -> t.rank.order } }!!.tiles)
                    } else {
                        g.pass(p, g.hands[p].minByOrNull { it.rank.order }!!)
                    }
                }
                guard++
            }
            assertTrue("game must terminate (seed=$seed)", g.gameOver)
            assertTrue(g.winner in 0..3)
            assertEquals(3, g.finished.size)
            wins += if (g.winner == 0) 1 else 0
        }
        println("player win rate: $wins/200")
    }
}
