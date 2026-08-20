package com.lelebox.app.game.gomoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GomokuGameTest {

    private fun placeRow(g: GomokuGame, y: Int, x0: Int, count: Int, color: Int) {
        for (i in 0 until count) {
            g.board[y * g.size + (x0 + i)] = color
        }
    }

    @Test
    fun `reset clears board and starts with black`() {
        val g = GomokuGame()
        g.place(7, 7)
        g.reset()
        assertTrue(g.board.all { it == NONE })
        assertEquals(BLACK, g.current)
        assertFalse(g.over)
    }

    @Test
    fun `horizontal five wins`() {
        val g = GomokuGame()
        placeRow(g, 7, 3, 5, BLACK)
        assertTrue(g.hasWon(5, 7, BLACK))
        assertFalse(g.hasWon(5, 7, WHITE))
    }

    @Test
    fun `vertical five wins`() {
        val g = GomokuGame()
        placeRow(g, 3, 5, 5, WHITE)
        // 竖线：y=3..7 在 x=5
        for (i in 0 until 5) g.board[(3 + i) * g.size + 5] = WHITE
        assertTrue(g.hasWon(5, 5, WHITE))
    }

    @Test
    fun `diagonal five wins`() {
        val g = GomokuGame()
        for (i in 0 until 5) g.board[(4 + i) * g.size + (4 + i)] = BLACK
        assertTrue(g.hasWon(6, 6, BLACK))
    }

    @Test
    fun `four in a row does not win`() {
        val g = GomokuGame()
        placeRow(g, 7, 3, 4, BLACK)
        assertFalse(g.hasWon(5, 7, BLACK))
    }

    @Test
    fun `occupied cell cannot be placed`() {
        val g = GomokuGame()
        assertTrue(g.place(7, 7))
        assertFalse(g.place(7, 7))
        assertEquals(1, g.board.count { it != NONE })
    }

    @Test
    fun `place alternates turns`() {
        val g = GomokuGame()
        g.place(7, 7)
        assertEquals(WHITE, g.current)
    }

    @Test
    fun `ai move returns empty cell and hard ai takes immediate win`() {
        val g = GomokuGame()
        // 电脑（白）四连，HARD 必须补第五子获胜
        placeRow(g, 7, 4, 4, WHITE)
        g.current = WHITE
        val mv = g.aiMove(GomokuLevel.HARD)!!
        // aiMove 返回的是空位
        assertEquals(NONE, g.get(mv.first, mv.second))
        // 落子即胜
        g.place(mv.first, mv.second)
        assertTrue(g.over)
        assertEquals(WHITE, g.winner)
    }

    @Test
    fun `hard ai blocks opponent immediate win`() {
        val g = GomokuGame()
        // 玩家（黑）四连，电脑必须堵住
        placeRow(g, 7, 4, 4, BLACK)
        g.current = WHITE
        val mv = g.aiMove(GomokuLevel.HARD)!!
        // 堵点应在五连缺口处（4,7 或 8,7 之一）
        val blocked = (mv.first == 3 && mv.second == 7) || (mv.first == 8 && mv.second == 7)
        assertTrue("应堵缺口，实际 $mv", blocked)
        assertNotEquals(BLACK, g.get(mv.first, mv.second))
    }
}
