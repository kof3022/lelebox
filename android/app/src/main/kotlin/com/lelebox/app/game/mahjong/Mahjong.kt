package com.lelebox.app.game.mahjong

/** （/） */
sealed class Meld {
    data class Chow(val tiles: List<Tile>) : Meld()   // （3 ）
    data class Pung(val tiles: List<Tile>) : Meld()   // （3 ）
    data class Kong(val tiles: List<Tile>) : Meld()   // （4 ）
    data class Pair(val tiles: List<Tile>) : Meld()   // （2 ）
}

/**  */
data class WinScheme(val melds: List<Meld>, val pair: Meld.Pair, val isSevenPairs: Boolean = false)

object Mahjong {
    /**
     *  14 （：4  + 1 ；）。
     * （，）。
     */
    fun findWins(tiles: List<Tile>): List<WinScheme> {
        val result = mutableListOf<WinScheme>()
        if (tiles.size % 3 != 2) return result
        // 
        if (isSevenPairs(tiles)) result.add(WinScheme(emptyList(), Meld.Pair(tiles.take(2)), isSevenPairs = true))
        // ：， 12  4 
        val byRank = tiles.groupBy { it.groupKey() }
        for ((key, group) in byRank) {
            if (group.size < 2) continue
            val remaining = tiles.toMutableList()
            removeOne(remaining, key); removeOne(remaining, key)
            val melds = mutableListOf<Meld>()
            if (splitMelds(remaining.sortedBy { it.groupKey() }, melds)) {
                result.add(WinScheme(melds.toList(), Meld.Pair(group.take(2))))
            }
        }
        return result.distinctBy { schemeKey(it) }
    }

    /**  12  4 （/）——： */
    private fun splitMelds(tiles: List<Tile>, out: MutableList<Meld>): Boolean {
        if (tiles.isEmpty()) return true
        val sorted = tiles.sortedBy { it.groupKey() }
        val first = sorted.first()
        val cnt = sorted.count { it == first }
        if (cnt >= 3) {
            out.add(Meld.Pung(sorted.filter { it == first }.take(3)))
            val rest = sorted.toMutableList()
            repeat(3) { rest.remove(first) }
            if (splitMelds(rest, out)) return true
            out.removeAt(out.size - 1)
        }
        // （）
        if (first.suit < 3 && first.rank <= 7) {
            val t2 = Tile(first.suit, first.rank + 1)
            val t3 = Tile(first.suit, first.rank + 2)
            if (sorted.count { it == t2 } >= 1 && sorted.count { it == t3 } >= 1) {
                out.add(Meld.Chow(listOf(first, t2, t3)))
                val rest = sorted.toMutableList()
                rest.remove(first); rest.remove(t2); rest.remove(t3)
                if (splitMelds(rest, out)) return true
                out.removeAt(out.size - 1)
            }
        }
        return false
    }

    private fun removeOne(list: MutableList<Tile>, key: Int) {
        val idx = list.indexOfFirst { it.groupKey() == key }
        if (idx >= 0) list.removeAt(idx)
    }

    private fun isSevenPairs(tiles: List<Tile>): Boolean {
        if (tiles.size != 14) return false
        return tiles.groupBy { it }.values.all { it.size == 2 }
    }

    private fun schemeKey(s: WinScheme): String = buildString {
        append(if (s.isSevenPairs) "7p" else "std")
        s.melds.forEach { m -> when (m) {
            is Meld.Chow -> append("c").append(m.tiles.joinToString("") { it.groupKey().toString() })
            is Meld.Pung -> append("p").append(m.tiles[0].groupKey())
            is Meld.Kong -> append("k").append(m.tiles[0].groupKey())
            else -> {}
        } }
    }
}
