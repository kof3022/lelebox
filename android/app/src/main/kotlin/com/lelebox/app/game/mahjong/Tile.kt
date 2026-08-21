package com.lelebox.app.game.mahjong

/** Mahjong tile: suit 0=wan 1=tiao 2=tong 3=wind 4=dragon; rank 1..9 (wind 1E 2S 3W 4N, dragon 1Zhong 2Fa 3Bai) */
data class Tile(val suit: Int, val rank: Int) {
    val isWind get() = suit == 3
    val isDragon get() = suit == 4
    val isHonor get() = isWind || isDragon

    fun label(): String = when (suit) {
        0 -> "$rank wan"
        1 -> "$rank tiao"
        2 -> "$rank tong"
        3 -> when (rank) { 1 -> "E"; 2 -> "S"; 3 -> "W"; 4 -> "N"; else -> "?" }
        else -> when (rank) { 1 -> "Z"; 2 -> "F"; 3 -> "B"; else -> "?" }
    }

    fun groupKey(): Int = suit * 10 + rank

    override fun toString(): String = label()

    companion object {
        /** All 136 tiles (4 of each) */
        fun fullSet(): List<Tile> {
            val list = mutableListOf<Tile>()
            for (s in 0..2) for (r in 1..9) repeat(4) { list.add(Tile(s, r)) }
            for (r in 1..4) repeat(4) { list.add(Tile(3, r)) } // winds
            for (r in 1..3) repeat(4) { list.add(Tile(4, r)) } // dragons
            return list
        }
    }
}
