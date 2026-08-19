package com.lelebox.app.game.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGameTest {

    private fun isValidSolution(b: IntArray): Boolean {
        for (r in 0 until 9) {
            val row = (0 until 9).map { b[r * 9 + it] }
            if (row.toSet().size != 9 || row.any { it !in 1..9 }) return false
        }
        for (c in 0 until 9) {
            val col = (0 until 9).map { b[it * 9 + c] }
            if (col.toSet().size != 9) return false
        }
        for (br in 0..2) for (bc in 0..2) {
            val box = (0 until 9).map { b[(br * 3 + it / 3) * 9 + bc * 3 + it % 3] }
            if (box.toSet().size != 9) return false
        }
        return true
    }

    @Test
    fun `generated puzzle has unique solution and correct clue count`() {
        for (level in SudokuLevel.entries) {
            val (puzzle, solved) = Sudoku.generatePuzzle(level)
            assertEquals(level.clues, puzzle.count { it != 0 })
            assertEquals(1, Sudoku.countSolutions(puzzle, 2))
            assertTrue(isValidSolution(solved))
            // 谜题与终盘一致（挖掉的格均为 0）
            puzzle.forEachIndexed { i, v -> if (v != 0) assertEquals(v, solved[i]) }
        }
    }

    @Test
    fun `solve returns a valid solution`() {
        val (puzzle, _) = Sudoku.generatePuzzle(SudokuLevel.EASY)
        val solved = Sudoku.solve(puzzle)
        assertNotNull(solved)
        assertTrue(isValidSolution(solved!!))
    }

    @Test
    fun `empty board counts many solutions`() {
        val empty = IntArray(81)
        // limit=2 时最多计到 2
        assertEquals(2, Sudoku.countSolutions(empty, 2))
    }

    @Test
    fun `solved board counts exactly one solution`() {
        val solved = Sudoku.generateSolved()
        assertEquals(1, Sudoku.countSolutions(solved, 2))
    }
}
