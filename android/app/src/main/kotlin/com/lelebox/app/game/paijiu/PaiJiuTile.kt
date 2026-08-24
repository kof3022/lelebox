package com.lelebox.app.game.paijiu

/**
 * 牌九（天九）骨牌数据模型。
 *
 * 牌具：32 张中国骨牌 = 文牌 11 对（22 张）+ 武牌 10 张。
 * 每张牌由两个骰子点数（1..6）组成，共 21 种点数组合。
 *
 * 等级（order 越小越大）：
 * 至尊 > 天 > 地 > 人 > 和 > 梅 > 长三 > 板凳 > 虎头 > 红头 > 高脚 > 零霖
 *      > 杂九 > 杂八 > 杂七 > 杂五
 */
enum class TileSuit { CIVIL, MILITARY }

/** 等级枚举，order 越小牌越大 */
enum class TileRank(val order: Int) {
    SUPREME(0),        // 至尊（猴王对，仅作为对子出现）
    HEAVEN(1),         // 天牌 [6:6]
    EARTH(2),          // 地牌 [1:1]
    MAN(3),            // 人牌 [4:4]
    HARMONY(4),        // 和牌 [1:3]
    PLUM(5),           // 梅牌 [5:5]
    LONG_THREE(6),     // 长三 [3:3]
    BENCH(7),          // 板凳 [2:2]
    TIGER_HEAD(8),     // 虎头/斧头 [5:6]
    RED_HEAD(9),       // 红头/屏风 [4:6]
    HIGH_LEG(10),      // 高脚 [1:6]
    LING_LING(11),     // 零霖 [1:5]
    MIXED_NINE(12),    // 杂九 [3:6][4:5]（点数 9）
    MIXED_EIGHT(13),   // 杂八 [2:6][3:5]（点数 8）
    MIXED_SEVEN(14),   // 杂七 [2:5][3:4]（点数 7）
    MIXED_SIX(15),     // 杂六（至尊组件二四 [2:4] 单出，点数 6）
    MIXED_FIVE(16),    // 杂五 [1:4][2:3]（点数 5）
    MIXED_THREE(17),   // 杂三（至尊组件丁三 [1:2] 单出，点数 3）
}

/** 一张骨牌：两个点数 + 文/武 + 名称 + 等级 */
data class PaiJiuTile(
    val pip1: Int,
    val pip2: Int,
    val suit: TileSuit,
    val name: String,
    val rank: TileRank,
) {
    /** 点数之和 */
    val pipSum: Int get() = pip1 + pip2

    /** 判定是否与另一张组成至尊对（猴王对） */
    fun isSupremeWith(other: PaiJiuTile): Boolean =
        PaiJiuTiles.isSupremePair(this, other)

    /** 固定 id：点数排序后拼接，用于去重/判等 */
    val id: String
        get() = if (pip1 <= pip2) "$pip1-$pip2" else "$pip2-$pip1"

    override fun toString(): String = name
}

object PaiJiuTiles {
    // ---- 文牌（每张成对出现，共 22 张）----
    val HEAVEN = PaiJiuTile(6, 6, TileSuit.CIVIL, "天", TileRank.HEAVEN)
    val EARTH = PaiJiuTile(1, 1, TileSuit.CIVIL, "地", TileRank.EARTH)
    val MAN = PaiJiuTile(4, 4, TileSuit.CIVIL, "人", TileRank.MAN)
    val HARMONY = PaiJiuTile(1, 3, TileSuit.CIVIL, "鹅牌", TileRank.HARMONY)
    val PLUM = PaiJiuTile(5, 5, TileSuit.CIVIL, "麻五", TileRank.PLUM)
    val LONG_THREE = PaiJiuTile(3, 3, TileSuit.CIVIL, "长三", TileRank.LONG_THREE)
    val BENCH = PaiJiuTile(2, 2, TileSuit.CIVIL, "板凳", TileRank.BENCH)
    val TIGER_HEAD = PaiJiuTile(5, 6, TileSuit.CIVIL, "老虎", TileRank.TIGER_HEAD)
    val RED_HEAD = PaiJiuTile(4, 6, TileSuit.CIVIL, "四六", TileRank.RED_HEAD)
    val HIGH_LEG = PaiJiuTile(1, 6, TileSuit.CIVIL, "大锤", TileRank.HIGH_LEG)
    val LING_LING = PaiJiuTile(1, 5, TileSuit.CIVIL, "小锤", TileRank.LING_LING)

    // ---- 武牌（每张单张，共 10 张）----
    val MIX9_A = PaiJiuTile(3, 6, TileSuit.MILITARY, "杂九", TileRank.MIXED_NINE)
    val MIX9_B = PaiJiuTile(4, 5, TileSuit.MILITARY, "杂九", TileRank.MIXED_NINE)
    val MIX8_A = PaiJiuTile(2, 6, TileSuit.MILITARY, "杂八", TileRank.MIXED_EIGHT)
    val MIX8_B = PaiJiuTile(3, 5, TileSuit.MILITARY, "杂八", TileRank.MIXED_EIGHT)
    val MIX7_A = PaiJiuTile(2, 5, TileSuit.MILITARY, "杂七", TileRank.MIXED_SEVEN)
    val MIX7_B = PaiJiuTile(3, 4, TileSuit.MILITARY, "杂七", TileRank.MIXED_SEVEN)
    val MIX5_A = PaiJiuTile(1, 4, TileSuit.MILITARY, "杂五", TileRank.MIXED_FIVE)
    val MIX5_B = PaiJiuTile(2, 3, TileSuit.MILITARY, "杂五", TileRank.MIXED_FIVE)
    /** 至尊组件：[2:4]（猴六，单出按杂六计，点数 6） */
    val SUPREME_A = PaiJiuTile(2, 4, TileSuit.MILITARY, "猴六", TileRank.MIXED_SIX)
    /** 至尊组件：[1:2]（猴三，单出按杂三计，点数 3） */
    val SUPREME_B = PaiJiuTile(1, 2, TileSuit.MILITARY, "猴三", TileRank.MIXED_THREE)

    /** 完整 32 张牌（文牌各 2 张 + 武牌各 1 张） */
    val FULL_SET: List<PaiJiuTile> by lazy {
        val list = mutableListOf<PaiJiuTile>()
        list.addAll(listOf(HEAVEN, HEAVEN))
        list.addAll(listOf(EARTH, EARTH))
        list.addAll(listOf(MAN, MAN))
        list.addAll(listOf(HARMONY, HARMONY))
        list.addAll(listOf(PLUM, PLUM))
        list.addAll(listOf(LONG_THREE, LONG_THREE))
        list.addAll(listOf(BENCH, BENCH))
        list.addAll(listOf(TIGER_HEAD, TIGER_HEAD))
        list.addAll(listOf(RED_HEAD, RED_HEAD))
        list.addAll(listOf(HIGH_LEG, HIGH_LEG))
        list.addAll(listOf(LING_LING, LING_LING))
        list.addAll(listOf(MIX9_A, MIX9_B, MIX8_A, MIX8_B, MIX7_A, MIX7_B, MIX5_A, MIX5_B, SUPREME_A, SUPREME_B))
        list
    }

    /** 判定两张牌是否组成至尊对（猴王对）：[1:2] + [2:4] */
    fun isSupremePair(a: PaiJiuTile, b: PaiJiuTile): Boolean {
        val pips = listOf(a.pip1, a.pip2, b.pip1, b.pip2).sorted()
        return pips == listOf(1, 2, 2, 4)
    }
}
