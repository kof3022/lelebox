package com.lelebox.app.game.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkGameTest {

    private fun clearAllExcept(game: LinkGame, vararg keep: Pair<Int, Int>) {
        for (i in game.symbols.indices) game.symbols[i] = 0
        keep.forEach { (x, y) -> game.symbols[y * game.cols + x] = 1 }
    }

    @Test
    fun `new board has balanced pairs`() {
        val game = LinkGame(LinkLevel.EASY)
        assertEquals(LinkLevel.EASY.cols * LinkLevel.EASY.rows / 2, game.remainingPairs)
        val counts = game.symbols.groupingBy { it }.eachCount()
        assertEquals(LinkLevel.EASY.symbolCount, counts.size)
        assertTrue(counts.values.all { it == LinkLevel.EASY.cols * LinkLevel.EASY.rows / LinkLevel.EASY.symbolCount })
    }

    @Test
    fun `adjacent same symbol connects straight`() {
        val game = LinkGame(LinkLevel.EASY)
        clearAllExcept(game, 0 to 0, 1 to 0)
        assertTrue(game.canConnect(0, 0, 1, 0))
        assertTrue(game.canConnect(1, 0, 0, 0))
    }

    @Test
    fun `same symbol with empty path connects with one turn`() {
        val game = LinkGame(LinkLevel.EASY)
        clearAllExcept(game, 0 to 0, 2 to 1)
        assertTrue(game.canConnect(0, 0, 2, 1)) // (0,0)->(2,0)->(2,1) 或外圈
    }

    @Test
    fun `blocked straight still connects around outside with two turns`() {
        val game = LinkGame(LinkLevel.EASY)
        clearAllExcept(game, 0 to 0, 3 to 0)
        // 中间两格挡住直线
        game.symbols[0 * game.cols + 1] = 2
        game.symbols[0 * game.cols + 2] = 3
        // 外圈绕行：上→右→下，2 拐点
        assertTrue(game.canConnect(0, 0, 3, 0))
    }

    @Test
    fun `different symbols or empty cannot connect`() {
        val game = LinkGame(LinkLevel.EASY)
        clearAllExcept(game, 0 to 0, 1 to 0)
        game.symbols[0 * game.cols + 1] = 2 // B=(1,0) 改为不同图案
        assertFalse(game.canConnect(0, 0, 1, 0))
        clearAllExcept(game, 0 to 0)
        assertFalse(game.canConnect(0, 0, 1, 0)) // B 为空
    }

    @Test
    fun `same cell cannot connect`() {
        val game = LinkGame(LinkLevel.EASY)
        clearAllExcept(game, 0 to 0)
        assertFalse(game.canConnect(0, 0, 0, 0))
    }

    @Test
    fun `remove pair decrements and clears`() {
        val game = LinkGame(LinkLevel.EASY)
        clearAllExcept(game, 0 to 0, 1 to 0)
        val before = game.remainingPairs
        assertTrue(game.removePair(0, 1))
        assertEquals(before - 1, game.remainingPairs)
        assertTrue(game.isRemoved(0))
        assertTrue(game.isRemoved(1))
    }

    @Test
    fun `remove different symbols fails`() {
        val game = LinkGame(LinkLevel.EASY)
        clearAllExcept(game, 0 to 0, 1 to 0)
        game.symbols[1] = 2
        assertFalse(game.removePair(0, 1))
    }

    @Test
    fun `hint finds a connectable pair and shuffle keeps multiset`() {
        val game = LinkGame(LinkLevel.EASY)
        val before = game.symbols.toList()
        val hint = game.findHint()
        assertNotNull(hint)
        val (a, b) = hint!!
        assertTrue(game.symbols[a] == game.symbols[b] && game.symbols[a] != 0)
        assertTrue(game.canConnect(a % game.cols, a / game.cols, b % game.cols, b / game.cols))

        game.shuffleRemaining()
        val after = game.symbols.toList()
        assertEquals(before.sorted(), after.sorted())
    }

    @Test
    fun `clearing all tiles ends game`() {
        val game = LinkGame(LinkLevel.EASY)
        // 逐对消完
        var guard = 0
        while (!game.over && guard < 100) {
            val hint = game.findHint() ?: break
            game.removePair(hint.first, hint.second)
            guard++
        }
        assertTrue(game.over)
        assertTrue(game.symbols.all { it == 0 })
    }
}
