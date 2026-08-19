package com.lelebox.app.game.sudoku

import kotlin.random.Random

/** 数独难度：clues = 预填格数（越少越难） */
enum class SudokuLevel(val label: String, val clues: Int) {
    EASY("简单", 45),
    MEDIUM("中等", 36),
    HARD("困难", 28),
}

/**
 * 数独生成/求解（参考 sgtpuzzles 算法思想，独立实现）。
 * 谜题生成保证唯一解；零网络、零依赖。
 */
object Sudoku {
    const val N = 9

    private fun valid(board: IntArray, pos: Int, v: Int): Boolean {
        val r = pos / N
        val c = pos % N
        for (k in 0 until N) {
            if (board[r * N + k] == v) return false
            if (board[k * N + c] == v) return false
        }
        val br = r / 3 * 3
        val bc = c / 3 * 3
        for (i in br until br + 3) {
            for (j in bc until bc + 3) {
                if (board[i * N + j] == v) return false
            }
        }
        return true
    }

    /** 回溯生成完整终盘 */
    fun generateSolved(rnd: Random = Random(System.currentTimeMillis())): IntArray {
        val board = IntArray(N * N)
        fun fill(pos: Int): Boolean {
            if (pos == N * N) return true
            for (v in (1..9).shuffled(rnd)) {
                if (valid(board, pos, v)) {
                    board[pos] = v
                    if (fill(pos + 1)) return true
                    board[pos] = 0
                }
            }
            return false
        }
        check(fill(0)) { "sudoku generation failed" }
        return board
    }

    /** 求解；返回一个解或 null（无解） */
    fun solve(board: IntArray): IntArray? {
        val b = board.copyOf()
        fun fill(pos: Int): Boolean {
            if (pos == N * N) return true
            if (b[pos] != 0) return fill(pos + 1)
            for (v in 1..9) {
                if (valid(b, pos, v)) {
                    b[pos] = v
                    if (fill(pos + 1)) return true
                    b[pos] = 0
                }
            }
            return false
        }
        return if (fill(0)) b else null
    }

    /** 解计数（最多统计到 limit） */
    fun countSolutions(board: IntArray, limit: Int = 2): Int {
        var count = 0
        val b = board.copyOf()
        fun fill(pos: Int) {
            if (count >= limit) return
            if (pos == N * N) {
                count++
                return
            }
            if (b[pos] != 0) {
                fill(pos + 1)
                return
            }
            for (v in 1..9) {
                if (valid(b, pos, v)) {
                    b[pos] = v
                    fill(pos + 1)
                    b[pos] = 0
                    if (count >= limit) return
                }
            }
        }
        fill(0)
        return count
    }

    /** 生成谜题：挖空并保证唯一解。返回 (谜题, 终盘) */
    fun generatePuzzle(
        level: SudokuLevel,
        rnd: Random = Random(System.currentTimeMillis()),
    ): Pair<IntArray, IntArray> {
        val solved = generateSolved(rnd)
        val puzzle = solved.copyOf()
        val target = N * N - level.clues
        var removed = 0
        for (pos in (0 until N * N).shuffled(rnd)) {
            if (removed >= target) break
            val backup = puzzle[pos]
            puzzle[pos] = 0
            if (countSolutions(puzzle, 2) == 1) {
                removed++
            } else {
                puzzle[pos] = backup
            }
        }
        return puzzle to solved
    }
}
