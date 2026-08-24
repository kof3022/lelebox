package com.lelebox.app.game.mahjong

import org.junit.Assert.assertTrue
import org.junit.Test

/** 验证：桌上可见的同值牌（手牌+弃牌+明刻）最多 4 张（修复碰/杠重复显示弃牌的 bug） */
class MahjongFiveBugTest {

    private fun meldTiles(m: Meld): List<Tile> = when (m) {
        is Meld.Chow -> m.tiles
        is Meld.Pung -> m.tiles
        is Meld.Kong -> m.tiles
        is Meld.Pair -> m.tiles
    }

    @Test
    fun `no more than 4 of any tile across hands discards and melds`() {
        var worst = 0
        repeat(600) { seed ->
            val g = MahjongGame(seed = seed.toLong())
            repeat(45) {
                if (g.winner >= 0 || g.exhausted) return@repeat
                if (g.current == 0) {
                    if (!g.hasDrawn && !g.mustDiscard) g.draw(0)
                    if (g.hasDrawn || g.mustDiscard) {
                        val t = g.hands[0].first()
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
                    var cnt = 0
                    for (h in g.hands) cnt += h.count { it == t }
                    for (d in g.discards) cnt += d.count { it == t }
                    for (list in g.exposed) for (m in list) cnt += meldTiles(m).count { it == t }
                    if (cnt > worst) worst = cnt
                    assertTrue("seed=$seed tile=$t visible=$cnt > 4", cnt <= 4)
                }
            }
        }
        println("max visible of any tile across 600 games = $worst")
    }
}
