package com.lelebox.app.game.paijiu

/**
 * 牌九（打天九）AI 策略：领出 / 接牌 / 垫牌。
 *
 * 约定：TileRank.order 越小牌越大（天=1 最大）。
 *
 * 三档难度：
 * - EASY  简单：领出最弱单张（有对子才出对子）；能接就出"刚够大"的牌；垫最弱牌。
 * - MEDIUM 中等：优先出文对；接牌避免浪费大牌（天/地/至尊）；垫价值最低牌。
 * - HARD  困难：领出保留大牌与对子、对方出天/地时评估是否值得抢、垫牺牲价值最低牌。
 */
enum class PaiJiuDifficulty { EASY, MEDIUM, HARD }

object PaiJiuAi {

    /** AI 领出选牌：返回要出的牌（单张或对子）。手牌非空时必有返回值。 */
    fun chooseLead(hand: List<PaiJiuTile>, difficulty: PaiJiuDifficulty): List<PaiJiuTile> {
        require(hand.isNotEmpty()) { "领出时手牌不能为空" }
        val civilPairs = hand.filter { it.suit == TileSuit.CIVIL }
            .groupBy { it.rank }
            .filterValues { it.size >= 2 }
            .values
            .sortedBy { it[0].rank.order } // 小到大（弱到强）
        val supreme = hasSupreme(hand)

        return when (difficulty) {
            PaiJiuDifficulty.EASY -> {
                // 简单：至尊对优先（最强），否则文对（从小到大的第一个=最弱对），否则最弱单张
                supreme?.let { return it }
                civilPairs.firstOrNull()?.let { return it }
                listOf(weakest(hand))
            }
            PaiJiuDifficulty.MEDIUM -> {
                // 中等：文对从大到小（先出大对子控局），无对出中等单张（保留天/地）
                supreme?.let { return it }
                civilPairs.lastOrNull()?.let { return it }
                val candidates = hand.filter { it.rank !in bigRanks }
                listOf(if (candidates.isNotEmpty()) weakest(candidates) else weakest(hand))
            }
            PaiJiuDifficulty.HARD -> {
                // 困难：至尊保留到最后（不轻易领出）；文对从大到小；无对出最弱单张
                civilPairs.lastOrNull()?.let { return it }
                listOf(weakest(hand))
            }
        }
    }

    /** AI 接牌：从可接出法中选择"刚够大"的牌（不浪费大牌）。无合法出法返回 null（须垫牌）。 */
    fun chooseFollow(hand: List<PaiJiuTile>, current: PaiJiuPlay, difficulty: PaiJiuDifficulty): List<PaiJiuTile>? {
        val legal = PaiJiuRules.legalPlays(hand, current)
        if (legal.isEmpty()) return null

        // 当前要接的牌等级
        val currentOrder = current.tiles.minOf { it.rank.order }

        return when (difficulty) {
            PaiJiuDifficulty.EASY -> justEnough(legal)?.tiles
            PaiJiuDifficulty.MEDIUM -> {
                // 尽量用刚够大的牌，保留天/地/至尊
                val nonBig = legal.filter { play -> play.tiles.none { it.rank in bigRanks } }
                (nonBig.ifEmpty { legal }).let { justEnough(it) }?.tiles
            }
            PaiJiuDifficulty.HARD -> {
                // 若对方出天/地等高牌单张，评估是否值得抢：能用非大牌接住且手牌充裕才接，否则垫
                val beats = justEnough(legal)!!
                if (currentOrder <= TileRank.EARTH.order && current.kind == PlayKind.SINGLE &&
                    beats.tiles.any { it.rank in bigRanks } && hand.size >= 3
                ) {
                    null // 需要浪费大牌去接，不划算
                } else {
                    beats.tiles
                }
            }
        }
    }

    /** AI 垫牌：弃一张价值最低的牌（保留对子/大牌/至尊组件）。手牌非空时必有返回值。 */
    fun choosePass(hand: List<PaiJiuTile>, difficulty: PaiJiuDifficulty): PaiJiuTile {
        require(hand.isNotEmpty()) { "垫牌时手牌不能为空" }
        return when (difficulty) {
            PaiJiuDifficulty.EASY -> weakest(hand)
            PaiJiuDifficulty.MEDIUM, PaiJiuDifficulty.HARD -> hand.minByOrNull { scorePass(it, hand) }!!
        }
    }

    /** 最弱单张（order 最大） */
    private fun weakest(hand: List<PaiJiuTile>): PaiJiuTile = hand.maxByOrNull { it.rank.order }!!

    /** 从可接出法中选择"刚够大"的（order 最大，即最弱但能接住的） */
    private fun justEnough(legal: List<PaiJiuPlay>): PaiJiuPlay? =
        legal.maxByOrNull { it.tiles.minOf { t -> t.rank.order } }

    /** 至尊对是否在手 */
    private fun hasSupreme(hand: List<PaiJiuTile>): List<PaiJiuTile>? {
        val a = hand.firstOrNull { it == PaiJiuTiles.SUPREME_A } ?: return null
        val b = hand.firstOrNull { it == PaiJiuTiles.SUPREME_B } ?: return null
        return listOf(a, b)
    }

    /** 大牌：天/地/至尊组件（保留价值高） */
    private val bigRanks = setOf(TileRank.HEAVEN, TileRank.EARTH, TileRank.SUPREME)

    /** 垫牌价值评分（越低越先弃）：对子成员/大牌/至尊组件价值高（加分保留） */
    private fun scorePass(t: PaiJiuTile, hand: List<PaiJiuTile>): Int {
        var s = t.rank.order * 10
        if (t.suit == TileSuit.CIVIL && hand.count { it.rank == t.rank } >= 2) s += 60   // 对子成员
        if (t.rank in bigRanks) s += 60                                                  // 大牌
        if (t == PaiJiuTiles.SUPREME_A || t == PaiJiuTiles.SUPREME_B) s += 80            // 至尊组件
        return s
    }
}
