package com.lelebox.app.game.paijiu

/**
 * 牌九（打天九）AI 策略（v2：三门/副子）。
 *
 * - EASY  简单：领出最小单张；能管上就出最小；垫最小。
 * - MEDIUM 中等：优先出对子/副子；管上用刚够大的；垫保留大牌。
 * - HARD  困难：保留大牌与对子，副子/对猴运用更激进。
 */
enum class PaiJiuDifficulty { EASY, MEDIUM, HARD }

object PaiJiuAi {

    /** 领出选牌：返回要出的牌。手牌非空时必有返回值。 */
    fun chooseLead(hand: List<PaiJiuTile>, difficulty: PaiJiuDifficulty): List<PaiJiuTile> {
        require(hand.isNotEmpty()) { "领出时手牌不能为空" }
        return when (difficulty) {
            PaiJiuDifficulty.EASY -> {
                // 简单：出最小单张
                listOf(weakest(hand))
            }
            PaiJiuDifficulty.MEDIUM -> {
                // 中等：优先冲结牌（留对子最后出），其次对猴/对子、副子2，否则最小单张
                savePairForJie(hand) ?: pairLead(hand) ?: fuLead(hand) ?: listOf(weakest(hand))
            }
            PaiJiuDifficulty.HARD -> {
                // 困难：优先冲结牌，其次对猴/大对子、副子，保留小单张
                savePairForJie(hand) ?: duiHouLead(hand) ?: pairLead(hand) ?: fuLead(hand) ?: listOf(weakest(hand))
            }
        }
    }

    /** 没人压得过的强单张（各门最大）：天、杂九、老虎 */
    private val unbeatableSingles = setOf(TileName.TIAN, TileName.ZA_JIU, TileName.LAO_HU)

    /**
     * 冲结牌策略：手里有一对（含对猴），且这对之外其余牌全是没人压得过的强单张
     * （天/杂九/老虎）时，先出强单张赢墩、把对子留到最后出 → 最后出对子，
     * 结牌为 大对结（天/杂九/老虎对）/ 猴结（对猴）/ 小对结（其他对）。
     */
    private fun savePairForJie(hand: List<PaiJiuTile>): List<PaiJiuTile>? {
        // 对子成员：同名 ≥2 张，或 猴六+猴三
        val pairNames = hand.groupBy { it.name }.filterValues { it.size >= 2 }.keys
        val hasDuiHou = hand.any { it.name == TileName.HOU_LIU } && hand.any { it.name == TileName.HOU_SAN }
        if (pairNames.isEmpty() && !hasDuiHou) return null
        // 其余（非对子）牌
        val others = hand.filter { t ->
            !pairNames.contains(t.name) &&
                !(hasDuiHou && (t.name == TileName.HOU_LIU || t.name == TileName.HOU_SAN))
        }
        if (others.isEmpty()) return null
        if (others.any { it.name !in unbeatableSingles }) return null
        // 条件成立：先出最弱的强单张（留对子最后出）
        return listOf(others.maxByOrNull { it.name.order }!!)
    }

    /** 领出：对猴（若手中有且难度允许） */
    private fun duiHouLead(hand: List<PaiJiuTile>): List<PaiJiuTile>? {
        val hou6 = hand.firstOrNull { it.name == TileName.HOU_LIU } ?: return null
        val hou3 = hand.firstOrNull { it.name == TileName.HOU_SAN } ?: return null
        return listOf(hou6, hou3)
    }

    /** 领出：任意对子（含对猴） */
    private fun pairLead(hand: List<PaiJiuTile>): List<PaiJiuTile>? {
        // 对猴
        val hou6 = hand.firstOrNull { it.name == TileName.HOU_LIU }
        val hou3 = hand.firstOrNull { it.name == TileName.HOU_SAN }
        if (hou6 != null && hou3 != null) return listOf(hou6, hou3)
        // 同名对子（优先大对）
        val byName = hand.groupBy { it.name }.filterValues { it.size >= 2 }
        val best = byName.maxByOrNull { it.value[0].name.order }
        return best?.let { listOf(it.value[0], it.value[1]) }
    }

    /** 领出：副子2（天九/地八/人七/鹅五） */
    private fun fuLead(hand: List<PaiJiuTile>): List<PaiJiuTile>? {
        val tian = listOf(TileName.TIAN, TileName.DI, TileName.REN, TileName.E)
        val za = listOf(TileName.ZA_JIU, TileName.ZA_BA, TileName.ZA_QI, TileName.ZA_WU)
        for (i in 0..3) {
            val t = hand.firstOrNull { it.name == tian[i] } ?: continue
            val z = hand.firstOrNull { it.name == za[i] } ?: continue
            return listOf(t, z)
        }
        return null
    }

    /** 管上：从可管上的出法中选择 */
    fun chooseFollow(hand: List<PaiJiuTile>, current: PaiJiuPlay, difficulty: PaiJiuDifficulty): List<PaiJiuTile>? {
        val legal = PaiJiuRules.legalPlays(hand, current)
        if (legal.isEmpty()) return null
        return when (difficulty) {
            PaiJiuDifficulty.EASY -> justEnough(legal)?.tiles
            PaiJiuDifficulty.MEDIUM -> {
                // 避免浪费大牌（天/地/杂九/老虎）
                val nonBig = legal.filter { play -> play.tiles.none { it.name in bigNames } }
                (nonBig.ifEmpty { legal }).let { justEnough(it) }?.tiles
            }
            PaiJiuDifficulty.HARD -> justEnough(legal)?.tiles
        }
    }

    /** 从可管上的出法中选择"刚够大"的（能管住的最弱牌） */
    private fun justEnough(legal: List<PaiJiuPlay>): PaiJiuPlay? =
        legal.maxByOrNull { it.tiles.minOf { t -> t.name.order } }

    /** 垫牌：随意出相同数量、不能更大的牌；选价值最低 */
    fun choosePass(hand: List<PaiJiuTile>, current: PaiJiuPlay, difficulty: PaiJiuDifficulty): List<PaiJiuTile> {
        require(hand.size >= current.count) { "手牌不足垫牌" }
        return when (difficulty) {
            PaiJiuDifficulty.EASY -> hand.sortedBy { scorePass(it, hand) }.take(current.count)
            PaiJiuDifficulty.MEDIUM, PaiJiuDifficulty.HARD -> {
                // 选价值最低的 current.count 张（避开对子/大牌）
                hand.sortedBy { scorePass(it, hand) }.take(current.count)
            }
        }
    }

    private fun weakest(hand: List<PaiJiuTile>): PaiJiuTile =
        hand.maxByOrNull { it.name.order }!!

    /** 大牌（保留价值高） */
    private val bigNames = setOf(TileName.TIAN, TileName.DI, TileName.ZA_JIU, TileName.LAO_HU, TileName.HOU_LIU, TileName.HOU_SAN)

    /** 垫牌价值评分（越低越先垫）：对子成员/大牌价值高（加分保留） */
    private fun scorePass(t: PaiJiuTile, hand: List<PaiJiuTile>): Int {
        var s = t.name.order * 10
        if (hand.count { it.name == t.name } >= 2) s += 60   // 对子成员
        if (t.name in bigNames) s += 60                       // 大牌
        return s
    }
}
