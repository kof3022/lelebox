package com.lelebox.app.game.gomoku

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** 五子棋：棋子 */
const val NONE = 0
const val BLACK = 1 // 玩家（先手）
const val WHITE = 2 // 电脑

/** 难度三档 */
enum class GomokuLevel(val label: String) {
    EASY("简单"), MEDIUM("中等"), HARD("困难"),
}

/**
 * 五子棋核心逻辑：15×15 棋盘、五连判定、启发式 AI。
 * AI 思路：对每个空点按「进攻分（自己的连子）+ 防守分（对手连子）×权重」打分；
 * 难度决定权重与随机性；HARD 额外保证「己方能赢先赢、对手要赢先堵」。
 */
class GomokuGame(val size: Int = 15) {

    val board = IntArray(size * size) // 0/1/2
    var current = BLACK
    var winner = 0
    var over = false

    fun reset() {
        board.fill(NONE)
        current = BLACK
        winner = 0
        over = false
    }

    fun inBounds(x: Int, y: Int) = x in 0 until size && y in 0 until size

    fun get(x: Int, y: Int) = board[y * size + x]

    /** 玩家落子；成功返回 true */
    fun place(x: Int, y: Int): Boolean {
        if (over || !inBounds(x, y) || get(x, y) != NONE) return false
        board[y * size + x] = current
        if (hasWon(x, y, current)) {
            winner = current
            over = true
        } else if (isBoardFull()) {
            over = true // 平局
        } else {
            current = if (current == BLACK) WHITE else BLACK
        }
        return true
    }

    fun isBoardFull(): Boolean = board.all { it != NONE }

    /** 以 (x,y) 为最后落点判定是否五连 */
    fun hasWon(x: Int, y: Int, color: Int): Boolean {
        val dirs = arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, -1))
        for (d in dirs) {
            var count = 1
            // 正方向
            var nx = x + d[0]; var ny = y + d[1]
            while (inBounds(nx, ny) && get(nx, ny) == color) { count++; nx += d[0]; ny += d[1] }
            // 反方向
            nx = x - d[0]; ny = y - d[1]
            while (inBounds(nx, ny) && get(nx, ny) == color) { count++; nx -= d[0]; ny -= d[1] }
            if (count >= 5) return true
        }
        return false
    }

    /** 电脑落子，返回坐标；无空位返回 null */
    fun aiMove(level: GomokuLevel): Pair<Int, Int>? {
        val empties = (0 until size * size).filter { board[it] == NONE }
        if (empties.isEmpty()) return null
        // 第一步走中心附近
        if (board.count { it != NONE } == 0) {
            val c = size / 2
            return c to c
        }
        val opp = if (current == BLACK) WHITE else BLACK
        val candidates = empties.map { idx ->
            val x = idx % size
            val y = idx / size
            val attack = evaluatePoint(x, y, current)
            val defend = evaluatePoint(x, y, opp)
            val (aw, dw) = when (level) {
                GomokuLevel.EASY -> 1.0 to 0.7
                GomokuLevel.MEDIUM -> 1.0 to 1.0
                GomokuLevel.HARD -> 1.1 to 1.05
            }
            Triple(idx, attack * aw + defend * dw, max(attack, defend))
        }
        // HARD/MEDIUM：己方可赢必赢；对手要赢必堵
        if (level != GomokuLevel.EASY) {
            candidates.firstOrNull { it.second >= WIN_SCORE }?.let { return (it.first % size) to (it.first / size) }
            candidates.firstOrNull { it.third >= WIN_SCORE }?.let { return (it.first % size) to (it.first / size) }
        }
        val best = candidates.maxByOrNull { it.second }!!
        if (level == GomokuLevel.EASY) {
            // 简单：在较优候选里随机
            val top = candidates.sortedByDescending { it.second }.take(6)
            val pick = top[Random.nextInt(top.size)]
            return (pick.first % size) to (pick.first / size)
        }
        return (best.first % size) to (best.first / size)
    }

    private fun isWinningScore(s: Double) = s >= WIN_SCORE

    /** 单点四方向连子评分（连子长度 + 开放端） */
    private fun evaluatePoint(x: Int, y: Int, color: Int): Double {
        if (!inBounds(x, y) || get(x, y) != NONE) return 0.0
        var total = 0.0
        val dirs = arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, -1))
        for (d in dirs) {
            // 向两边数连子
            var len = 1
            var open = 0
            var nx = x + d[0]; var ny = y + d[1]
            while (inBounds(nx, ny) && get(nx, ny) == color) { len++; nx += d[0]; ny += d[1] }
            if (inBounds(nx, ny) && get(nx, ny) == NONE) open++
            nx = x - d[0]; ny = y - d[1]
            while (inBounds(nx, ny) && get(nx, ny) == color) { len++; nx -= d[0]; ny -= d[1] }
            if (inBounds(nx, ny) && get(nx, ny) == NONE) open++
            total += patternScore(len, open)
        }
        return total
    }

    companion object {
        const val WIN_SCORE = 1000000.0

        /** 连子得分表 */
        private fun patternScore(len: Int, open: Int): Double = when {
            len >= 5 -> WIN_SCORE
            len == 4 && open == 2 -> 100000.0
            len == 4 && open == 1 -> 50000.0
            len == 3 && open == 2 -> 12000.0
            len == 3 && open == 1 -> 5000.0
            len == 2 && open == 2 -> 1200.0
            len == 2 && open == 1 -> 300.0
            len == 1 && open == 2 -> 30.0
            else -> 5.0
        }
    }
}
