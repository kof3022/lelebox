package com.lelebox.app.game.link

import kotlin.random.Random

/** 连连看难度 */
enum class LinkLevel(val label: String, val cols: Int, val rows: Int, val symbolCount: Int) {
    EASY("简单", 6, 6, 6),  // 6 种图案 ×6 张 = 3 对每种
    HARD("困难", 8, 6, 8),  // 8 种图案 ×6 张
}

/**
 * 连连看核心逻辑：同图案两张牌若能用 **≤2 个拐点** 的路径连通（路径只经过空格/棋盘外圈），即可消除。
 * 支持洗牌（打乱剩余牌）与提示（找一对可消）。
 */
class LinkGame(val level: LinkLevel) {

    val cols = level.cols
    val rows = level.rows
    /** 1..symbolCount；0=已消除 */
    val symbols = MutableList(cols * rows) { 0 }
    var remainingPairs = 0
        private set
    var over = false
        private set

    init {
        newBoard()
    }

    fun newBoard() {
        val total = cols * rows
        val perSymbol = total / level.symbolCount // 每种出现次数（偶数，成对）
        val list = mutableListOf<Int>()
        for (s in 1..level.symbolCount) {
            repeat(perSymbol) { list.add(s) }
        }
        list.shuffle(Random(System.currentTimeMillis()))
        symbols.clear()
        symbols.addAll(list)
        remainingPairs = total / 2
        over = false
    }

    fun get(x: Int, y: Int): Int = symbols[y * cols + x]

    fun isRemoved(idx: Int) = symbols[idx] == 0

    /** 消除一对；返回是否成功 */
    fun removePair(a: Int, b: Int): Boolean {
        if (symbols[a] == 0 || symbols[a] != symbols[b]) return false
        if (!canConnect(a % cols, a / cols, b % cols, b / cols)) return false
        symbols[a] = 0
        symbols[b] = 0
        remainingPairs--
        if (remainingPairs == 0) over = true
        return true
    }

    /** 洗牌：剩余牌打乱位置 */
    fun shuffleRemaining() {
        val remaining = symbols.filter { it != 0 }
        if (remaining.isEmpty()) return
        val shuffled = remaining.shuffled(Random(System.currentTimeMillis()))
        var i = 0
        for (idx in symbols.indices) {
            if (symbols[idx] != 0) symbols[idx] = shuffled[i++]
        }
    }

    /** 提示：找一对可消除的牌 */
    fun findHint(): Pair<Int, Int>? {
        for (a in symbols.indices) {
            if (symbols[a] == 0) continue
            for (b in a + 1 until symbols.size) {
                if (symbols[b] == symbols[a] &&
                    canConnect(a % cols, a / cols, b % cols, b / cols)
                ) {
                    return a to b
                }
            }
        }
        return null
    }

    /** ≤2 拐点连通判定（BFS，虚拟外圈可走） */
    fun canConnect(x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
        if (x1 == x2 && y1 == y2) return false
        val s1 = get(x1, y1)
        if (s1 == 0 || s1 != get(x2, y2)) return false

        val w = cols + 2
        val h = rows + 2
        val sx = x1 + 1
        val sy = y1 + 1
        val ex = x2 + 1
        val ey = y2 + 1
        val dx = intArrayOf(1, 0, -1, 0)
        val dy = intArrayOf(0, 1, 0, -1)

        data class Node(val x: Int, val y: Int, val dir: Int, val lines: Int)

        val best = HashMap<Pair<Int, Int>, Int>()
        val q = ArrayDeque<Node>()
        best[sx to sy] = 0
        for (d in 0 until 4) q.add(Node(sx, sy, d, 0))

        while (q.isNotEmpty()) {
            val cur = q.removeFirst()
            if (cur.x == ex && cur.y == ey) return true
            for (nd in 0 until 4) {
                if (nd == (cur.dir + 2) % 4) continue // 不回头
                val nx = cur.x + dx[nd]
                val ny = cur.y + dy[nd]
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                val nlines = if (nd == cur.dir) cur.lines else cur.lines + 1
                if (nlines > 2) continue
                // 可通行：外圈 / 空格 / 终点（起点已在 dist）
                val isBorder = nx == 0 || ny == 0 || nx == w - 1 || ny == h - 1
                val isEnd = nx == ex && ny == ey
                val isOpen = nx in 1..cols && ny in 1..rows && get(nx - 1, ny - 1) == 0
                if (!isBorder && !isEnd && !isOpen) continue
                val key = nx to ny
                val prev = best[key]
                if (prev == null || nlines < prev) {
                    best[key] = nlines
                    q.addLast(Node(nx, ny, nd, nlines))
                }
            }
        }
        return false
    }
}
