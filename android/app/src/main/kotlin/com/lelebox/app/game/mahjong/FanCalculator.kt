package com.lelebox.app.game.mahjong

/** Fan result */
data class FanResult(val name: String, val fan: Int)

/**
 * Fan calculator (elder-friendly subset, docs/16 sec5).
 * Input: win scheme, all 14 tiles, exposed melds, self-draw, seat/round wind.
 */
object FanCalculator {

    fun calculate(
        scheme: WinScheme,
        all: List<Tile>,
        exposedMelds: List<Meld>,
        selfDraw: Boolean,
        seatWind: Int,
        roundWind: Int = 1,
    ): List<FanResult> {
        val fans = mutableListOf<FanResult>()
        val sorted = all.sortedBy { it.groupKey() }
        val suitsUsed = sorted.map { it.suit }.toSet()
        val isHonorOnly = sorted.all { it.isHonor }
        val hasHonor = sorted.any { it.isHonor }
        val isConcealed = exposedMelds.isEmpty()
        val isSeven = scheme.isSevenPairs

        val allMelds = scheme.melds + exposedMelds
        val pungs = allMelds.filterIsInstance<Meld.Pung>().map { it.tiles[0] }
        val kongs = allMelds.filterIsInstance<Meld.Kong>().map { it.tiles[0] }
        val dragonPungs = pungs.count { it.isDragon } + kongs.count { it.isDragon }

        if (dragonPungs >= 3) fans.add(FanResult("大三元", 88))
        if (isHonorOnly) fans.add(FanResult("字一色", 64))
        if (suitsUsed.size == 1 && !isHonorOnly) fans.add(FanResult("清一色", 24))
        if (isSeven) fans.add(FanResult("七对", 24))
        if (!isSeven && allMelds.all { it is Meld.Pung || it is Meld.Kong }) fans.add(FanResult("碰碰和", 8))
        if (suitsUsed.size == 2 && hasHonor) fans.add(FanResult("混一色", 8))
        if (isConcealed && selfDraw) fans.add(FanResult("不求人", 8))
        if (dragonPungs >= 2) fans.add(FanResult("双箭刻", 8))
        if (allBeltYao(scheme, all)) fans.add(FanResult("全带幺", 4))
        val hasYao = sorted.any { it.isHonor || (it.suit < 3 && (it.rank == 1 || it.rank == 9)) }
        if (!hasYao && !isSeven) fans.add(FanResult("断幺", 2))
        if (isConcealed && !selfDraw) fans.add(FanResult("门前清", 2))
        if (dragonPungs >= 1) fans.add(FanResult("箭刻", 2))
        if (isConcealed && allMelds.all { it is Meld.Chow } && scheme.pair.tiles[0].suit < 3) fans.add(FanResult("平和", 2))
        val windPungs = pungs.filter { it.suit == 3 }
        if (windPungs.any { it.rank == seatWind }) fans.add(FanResult("门风刻", 2))
        if (windPungs.any { it.rank == roundWind }) fans.add(FanResult("圈风刻", 2))
        if (kongs.isNotEmpty() || exposedMelds.any { it is Meld.Kong }) fans.add(FanResult("明杠", 1))
        if (hasHonor && suitsUsed.size <= 2) fans.add(FanResult("缺一门", 1))
        val yaoPungs = pungs.count { it.isHonor || (it.suit < 3 && (it.rank == 1 || it.rank == 9)) }
        if (yaoPungs >= 1) fans.add(FanResult("幺九刻", 1))

        return fans.distinctBy { it.name }
    }

    fun total(fans: List<FanResult>): Int = fans.sumOf { it.fan }

    private fun allBeltYao(scheme: WinScheme, all: List<Tile>): Boolean {
        if (scheme.isSevenPairs) return false
        val sets: List<List<Tile>> = scheme.melds.map { m ->
            when (m) {
                is Meld.Chow -> m.tiles
                is Meld.Pung -> m.tiles
                is Meld.Kong -> m.tiles
                is Meld.Pair -> m.tiles
            }
        } + listOf(scheme.pair.tiles)
        return sets.all { set ->
            set.any { it.isHonor || (it.suit < 3 && (it.rank == 1 || it.rank == 9)) }
        }
    }
}
