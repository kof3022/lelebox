package com.lelebox.app.game.g2048

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Game2048Test {

    private fun boardOf(vararg values: Int): Game2048 {
        val g = Game2048()
        g.addTiles = false // 纯逻辑测试：禁止随机补牌，保证确定性
        values.forEachIndexed { i, v -> g.board[i] = v }
        return g
    }

    @Test
    fun `new game starts with two tiles`() {
        val g = Game2048()
        g.newGame()
        assertEquals(2, g.board.count { it != 0 })
        assertFalse(g.over)
        assertFalse(g.won)
    }

    @Test
    fun `move left slides and merges once`() {
        val g = boardOf(
            2, 2, 4, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        assertTrue(g.move(Dir.LEFT))
        assertEquals(4, g.board[0])
        assertEquals(4, g.board[1])
        assertEquals(0, g.board[2])
        // 合并得分计入
        assertEquals(4, g.score)
    }

    @Test
    fun `move left merges run of four`() {
        val g = boardOf(
            2, 2, 2, 2,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        g.move(Dir.LEFT)
        assertEquals(4, g.board[0])
        assertEquals(4, g.board[1])
        assertEquals(0, g.board[2])
        assertEquals(0, g.board[3])
    }

    @Test
    fun `move right slides to the right side`() {
        val g = boardOf(
            2, 2, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        g.move(Dir.RIGHT)
        assertEquals(0, g.board[0])
        assertEquals(0, g.board[1])
        assertEquals(0, g.board[2])
        assertEquals(4, g.board[3])
    }

    @Test
    fun `move up merges column`() {
        val g = boardOf(
            2, 0, 0, 0,
            2, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        g.move(Dir.UP)
        assertEquals(4, g.board[0])
        assertEquals(0, g.board[4])
    }

    @Test
    fun `move down slides to bottom`() {
        val g = boardOf(
            2, 0, 0, 0,
            2, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        g.move(Dir.DOWN)
        assertEquals(0, g.board[0])
        assertEquals(0, g.board[4])
        assertEquals(4, g.board[12])
    }

    @Test
    fun `invalid direction returns false without change`() {
        val g = boardOf(
            2, 4, 2, 4,
            4, 2, 4, 2,
            2, 4, 2, 4,
            4, 2, 4, 2,
        )
        val before = g.board.copyOf()
        assertFalse(g.move(Dir.LEFT))
        assertTrue(before.contentEquals(g.board))
    }

    @Test
    fun `full board with no moves ends game`() {
        val g = boardOf(
            2, 4, 2, 4,
            4, 2, 4, 2,
            2, 4, 2, 4,
            4, 2, 4, 2,
        )
        g.move(Dir.LEFT)
        assertTrue(g.over)
    }

    @Test
    fun `reaching 2048 marks won`() {
        val g = boardOf(
            1024, 1024, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        g.move(Dir.LEFT)
        assertEquals(2048, g.board[0])
        assertTrue(g.won)
    }
}
