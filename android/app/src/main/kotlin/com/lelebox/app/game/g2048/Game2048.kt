package com.lelebox.app.game.g2048

import kotlin.random.Random

/** 2048 方向 */
enum class Dir { LEFT, RIGHT, UP, DOWN }

/**
 * 2048 核心逻辑（参考 gabrielecirulli/2048 的 MIT 算法思想，独立实现）。
 * 规则：每次滑动，所有方块尽量移动，相邻相等合并一次；合并得分；无空格且无相邻相等则结束。
 */
class Game2048 {
    companion object {
        const val SIZE = 4
        const val WIN_VALUE = 2048
    }

    val board = IntArray(SIZE * SIZE)
    var score = 0
    var won = false
    var over = false
    private val rng = Random(System.currentTimeMillis())

    fun newGame() {
        board.fill(0)
        score = 0
        won = false
        over = false
        addRandom()
        addRandom()
    }

    private fun addRandom() {
        val empties = board.indices.filter { board[it] == 0 }
        if (empties.isEmpty()) return
        val idx = empties[rng.nextInt(empties.size)]
        board[idx] = if (rng.nextInt(10) == 0) 4 else 2
    }

    /** 单行滑动+合并（左到右方向），返回新行与得分 */
    private fun slideLine(line: IntArray): Pair<IntArray, Int> {
        val compact = line.filter { it != 0 }
        val out = mutableListOf<Int>()
        var gain = 0
        var i = 0
        while (i < compact.size) {
            if (i + 1 < compact.size && compact[i] == compact[i + 1]) {
                val v = compact[i] * 2
                out.add(v)
                gain += v
                if (v >= WIN_VALUE) won = true
                i += 2
            } else {
                out.add(compact[i])
                i += 1
            }
        }
        while (out.size < SIZE) out.add(0)
        return out.toIntArray() to gain
    }

    /** 按运动方向取行列坐标（line=行/列序号，pos=该线上第几个） */
    private fun lineCell(dir: Dir, line: Int, pos: Int): Pair<Int, Int> = when (dir) {
        Dir.LEFT -> line to pos
        Dir.RIGHT -> line to (SIZE - 1 - pos)
        Dir.UP -> pos to line
        Dir.DOWN -> (SIZE - 1 - pos) to line
    }

    /** 执行一步滑动；返回棋盘是否变化 */
    fun move(dir: Dir): Boolean {
        val before = board.copyOf()
        for (line in 0 until SIZE) {
            val extracted = IntArray(SIZE) { k ->
                val (r, c) = lineCell(dir, line, k)
                board[r * SIZE + c]
            }
            val (slid, gain) = slideLine(extracted)
            score += gain
            for (k in 0 until SIZE) {
                val (r, c) = lineCell(dir, line, k)
                board[r * SIZE + c] = slid[k]
            }
        }
        val changed = !board.contentEquals(before)
        if (changed) {
            addRandom()
            over = !hasAnyMove()
        }
        return changed
    }

    private fun hasAnyMove(): Boolean {
        for (i in board.indices) {
            if (board[i] == 0) return true
            val r = i / SIZE
            val c = i % SIZE
            if (r + 1 < SIZE && board[i] == board[(r + 1) * SIZE + c]) return true
            if (c + 1 < SIZE && board[i] == board[r * SIZE + c + 1]) return true
        }
        return false
    }
}
