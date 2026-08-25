package com.lelebox.app.game.paijiu

/**
 * 牌九（打天九）骨牌数据模型（v2：三门规则）。
 *
 * 牌具：32 张中国骨牌，分三门：
 * - 天门（14 张）：天、地、人、鹅牌、麻五、长三、板凳（各 2 张）
 * - 杂门（10 张）：杂九×2、杂八×2、杂七×2、猴六、杂五×2、猴三
 * - 幺门（8 张）：老虎、四六、大锤、小锤（各 2 张）
 *
 * 同门按序比大小，不同门不能比较。
 */
enum class TileSuit(val label: String) {
    TIAN("天门"), ZA("杂门"), YAO("幺门")
}

/**
 * 牌名（唯一标识每"种"牌；对子 = 同名两张，猴六+猴三也是对子）。
 * order 用于同门内排序（越小越大）。
 */
enum class TileName(val label: String, val suit: TileSuit, val order: Int) {
    // 天门（order 从大到小）
    TIAN("天", TileSuit.TIAN, 1),
    DI("地", TileSuit.TIAN, 2),
    REN("人", TileSuit.TIAN, 3),
    E("鹅牌", TileSuit.TIAN, 4),
    MA_WU("麻五", TileSuit.TIAN, 5),
    CHANG_SAN("长三", TileSuit.TIAN, 6),
    BAN_DENG("板凳", TileSuit.TIAN, 7),
    // 杂门
    ZA_JIU("杂九", TileSuit.ZA, 1),
    ZA_BA("杂八", TileSuit.ZA, 2),
    ZA_QI("杂七", TileSuit.ZA, 3),
    HOU_LIU("猴六", TileSuit.ZA, 4),
    ZA_WU("杂五", TileSuit.ZA, 5),
    HOU_SAN("猴三", TileSuit.ZA, 6),
    // 幺门
    LAO_HU("老虎", TileSuit.YAO, 1),
    SI_LIU("四六", TileSuit.YAO, 2),
    DA_CHUI("大锤", TileSuit.YAO, 3),
    XIAO_CHUI("小锤", TileSuit.YAO, 4),
}

/**
 * 一张骨牌：门 + 牌名 + 点数（骰子排列用）。
 * 同一 TileName 的所有牌在游戏中等价（大小/配对只按 TileName）。
 */
data class PaiJiuTile(
    val pip1: Int,
    val pip2: Int,
    val name: TileName,
) {
    val suit: TileSuit get() = name.suit

    /** 点数之和 */
    val pipSum: Int get() = pip1 + pip2

    /** 固定 id：点数排序后拼接，用于去重/判等 */
    val id: String
        get() = if (pip1 <= pip2) "$pip1-$pip2" else "$pip2-$pip1"

    override fun toString(): String = name.label
}

object PaiJiuTiles {
    // ---- 天门（各 2 张）----
    val TIAN = PaiJiuTile(6, 6, TileName.TIAN)
    val DI = PaiJiuTile(1, 1, TileName.DI)
    val REN = PaiJiuTile(4, 4, TileName.REN)
    val E = PaiJiuTile(1, 3, TileName.E)
    val MA_WU = PaiJiuTile(5, 5, TileName.MA_WU)
    val CHANG_SAN = PaiJiuTile(3, 3, TileName.CHANG_SAN)
    val BAN_DENG = PaiJiuTile(2, 2, TileName.BAN_DENG)
    // ---- 杂门 ----
    val ZA_JIU_A = PaiJiuTile(3, 6, TileName.ZA_JIU)
    val ZA_JIU_B = PaiJiuTile(4, 5, TileName.ZA_JIU)
    val ZA_BA_A = PaiJiuTile(2, 6, TileName.ZA_BA)
    val ZA_BA_B = PaiJiuTile(3, 5, TileName.ZA_BA)
    val ZA_QI_A = PaiJiuTile(2, 5, TileName.ZA_QI)
    val ZA_QI_B = PaiJiuTile(3, 4, TileName.ZA_QI)
    val HOU_LIU = PaiJiuTile(2, 4, TileName.HOU_LIU)
    val ZA_WU_A = PaiJiuTile(1, 4, TileName.ZA_WU)
    val ZA_WU_B = PaiJiuTile(2, 3, TileName.ZA_WU)
    val HOU_SAN = PaiJiuTile(1, 2, TileName.HOU_SAN)
    // ---- 幺门（各 2 张）----
    val LAO_HU = PaiJiuTile(5, 6, TileName.LAO_HU)
    val SI_LIU = PaiJiuTile(4, 6, TileName.SI_LIU)
    val DA_CHUI = PaiJiuTile(1, 6, TileName.DA_CHUI)
    val XIAO_CHUI = PaiJiuTile(1, 5, TileName.XIAO_CHUI)

    /** 完整 32 张牌 */
    val FULL_SET: List<PaiJiuTile> by lazy {
        val list = mutableListOf<PaiJiuTile>()
        // 天门各 2
        list.addAll(listOf(TIAN, TIAN))
        list.addAll(listOf(DI, DI))
        list.addAll(listOf(REN, REN))
        list.addAll(listOf(E, E))
        list.addAll(listOf(MA_WU, MA_WU))
        list.addAll(listOf(CHANG_SAN, CHANG_SAN))
        list.addAll(listOf(BAN_DENG, BAN_DENG))
        // 杂门
        list.addAll(listOf(ZA_JIU_A, ZA_JIU_B))
        list.addAll(listOf(ZA_BA_A, ZA_BA_B))
        list.addAll(listOf(ZA_QI_A, ZA_QI_B))
        list.add(HOU_LIU)
        list.addAll(listOf(ZA_WU_A, ZA_WU_B))
        list.add(HOU_SAN)
        // 幺门各 2
        list.addAll(listOf(LAO_HU, LAO_HU))
        list.addAll(listOf(SI_LIU, SI_LIU))
        list.addAll(listOf(DA_CHUI, DA_CHUI))
        list.addAll(listOf(XIAO_CHUI, XIAO_CHUI))
        list
    }

    /** 猴六 + 猴三 = 对猴 */
    fun isDuiHou(a: PaiJiuTile, b: PaiJiuTile): Boolean =
        (a.name == TileName.HOU_LIU && b.name == TileName.HOU_SAN) ||
            (a.name == TileName.HOU_SAN && b.name == TileName.HOU_LIU)
}
