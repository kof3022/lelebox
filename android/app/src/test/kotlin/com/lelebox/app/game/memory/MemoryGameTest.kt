package com.lelebox.app.game.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryGameTest {

    @Test
    fun `board contains exactly two of each face`() {
        for (level in MemoryLevel.entries) {
            val board = buildMemoryBoard(level)
            assertEquals(level.pairs * 2, board.size)
            val counts = board.groupingBy { it }.eachCount()
            assertEquals(level.pairs, counts.size)
            assertTrue("每张图案应恰好出现 2 次", counts.values.all { it == 2 })
        }
    }

    @Test
    fun `level definitions are consistent`() {
        assertEquals(6, MemoryLevel.EASY.pairs)
        assertEquals(8, MemoryLevel.HARD.pairs)
        assertTrue(MemoryLevel.EASY.columns >= 3)
    }
}
