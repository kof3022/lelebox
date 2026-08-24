package com.lelebox.app.game.mahjong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 聚焦验证：碰/杠后被claim的弃牌从出牌者弃牌区移除，且任意牌值可见数≤4 */
class MahjongClaimTest {

    private fun meldTiles(m: Meld): List<Tile> = when (m) {
        is Meld.Chow -> m.tiles
        is Meld.Pung -> m.tiles
        is Meld.Kong -> m.tiles
        is Meld.Pair -> m.tiles
    }

    private fun visibleCount(g: MahjongGame, t: Tile): Int {
        var c = 0
        for (h in g.hands) c += h.count { it == t }
        for (d in g.discards) c += d.count { it == t }
        for (list in g.exposed) for (m in list) c += meldTiles(m).count { it == t }
        return c
    }

    @Test
    fun `AI pung removes claimed tile from discarder row`() {
        val g = MahjongGame(seed = 1)
        val s = Tile(3, 2) // 南
        // 孙权(seat1)手里两个南；曹操(seat2)弃一张南
        g.hands[1].clear()
        g.hands[1].add(s); g.hands[1].add(s)
        repeat(11) { g.hands[1].add(Tile(0, it % 9 + 1)) }
        g.hands[2].clear()
        g.hands[2].add(s)
        repeat(12) { g.hands[2].add(Tile(1, it % 9 + 1)) }
        g.lastDiscard = s
        g.lastDiscardPlayer = 2
        g.discards[2].add(s)
        // 孙权回合：应碰
        g.current = 1
        g.aiTurn(1)
        assertEquals("孙权应碰牌(明刻1)", 1, g.exposed[1].size)
        assertEquals("曹操弃牌区不再有南", 0, g.discards[2].count { it == s })
        assertTrue("可见南≤4", visibleCount(g, s) <= 4)
    }

    @Test
    fun `no more than 4 of any tile across many pung-heavy games`() {
        var worst = 0
        repeat(2000) { seed ->
            val g = MahjongGame(seed = seed.toLong())
            repeat(50) {
                if (g.winner >= 0 || g.exhausted) return@repeat
                if (g.current == 0) {
                    if (!g.hasDrawn && !g.mustDiscard) g.draw(0)
                    if (g.hasDrawn || g.mustDiscard) {
                        val t = g.hands[0].maxByOrNull { tt -> g.hands[0].count { it == tt } } ?: g.hands[0].first()
                        g.discard(0, t)
                    }
                } else {
                    g.aiTurn(g.current)
                }
            }
            for (suit in 0..4) {
                val maxRank = when (suit) { 3 -> 4; 4 -> 3; else -> 9 }
                for (rank in 1..maxRank) {
                    val t = Tile(suit, rank)
                    val c = visibleCount(g, t)
                    if (c > worst) worst = c
                    assertTrue("seed=$seed tile=$t visible=$c > 4", c <= 4)
                }
            }
        }
        println("max visible of any tile across 2000 pung-heavy games = $worst")
    }
}
