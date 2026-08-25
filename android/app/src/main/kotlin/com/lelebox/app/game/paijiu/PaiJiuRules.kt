package com.lelebox.app.game.paijiu

import kotlin.random.Random

/**
 * 牌九（打天九）出牌牌型与大小规则（v2：三门/副子）。
 *
 * 出牌数：每一圈每个玩家的出牌数必须相同。
 * 牌型：
 * - 单张：1 张
 * - 对子：相同名字 2 张（猴六+猴三 = 对猴，也是对子）
 * - 副子 2 张：天九（天+杂九）/ 地八（地+杂八）/ 人七（人+杂七）/ 鹅五（鹅+杂五）
 * - 副子 3 张类型 A：2 天门 + 1 对应杂（2天+杂九 = 三天九…）
 * - 副子 3 张类型 B：1 天门 + 2 对应杂（天+2杂九 = 三天九…）
 * - 副子 4 张：2 天门 + 2 对应杂（2天+2杂九 = 四天九…）
 *
 * 大小：同门比大小；副子同名内按 天九>地八>人七>鹅五；3 张两种类型互不比；不同张数互不比。
 */
enum class PlayKind { SINGLE, PAIR, FU_2, FU_3A, FU_3B, FU_4 }

/** 副子名称（天九/地八/人七/鹅五） */
enum class FuName(val label: String, val order: Int) {
    TIAN_JIU("天九", 1),
    DI_BA("地八", 2),
    REN_QI("人七", 3),
    E_WU("鹅五", 4),
}

/** 一手出的牌 */
data class PaiJiuPlay(
    val tiles: List<PaiJiuTile>,
    val kind: PlayKind,
    val fuName: FuName? = null,
) {
    /** 出牌数（一圈内各家必须相同） */
    val count: Int get() = tiles.size

    /** 是否为对猴 */
    val isDuiHou: Boolean
        get() = kind == PlayKind.PAIR && PaiJiuTiles.isDuiHou(tiles[0], tiles[1])

    /** 是否为对子（含对猴） */
    val isPair: Boolean get() = kind == PlayKind.PAIR

    companion object {
        fun single(t: PaiJiuTile) = PaiJiuPlay(listOf(t), PlayKind.SINGLE)
    }
}

object PaiJiuRules {

    /** 分析一手牌属于什么牌型；非法组合返回 null */
    fun classify(tiles: List<PaiJiuTile>): PaiJiuPlay? {
        val n = tiles.size
        return when (n) {
            1 -> PaiJiuPlay(tiles, PlayKind.SINGLE)
            2 -> classifyPairOrFu2(tiles)
            3 -> classifyFu3(tiles)
            4 -> classifyFu4(tiles)
            else -> null
        }
    }

    /** 2 张：对子（同名 或 对猴）或 副子2 */
    private fun classifyPairOrFu2(tiles: List<PaiJiuTile>): PaiJiuPlay? {
        val (a, b) = tiles
        // 对子：同名（对猴也算同名？不——猴六+猴三是不同名但对子。对子=同名2张 或 对猴）
        if (a.name == b.name) return PaiJiuPlay(tiles, PlayKind.PAIR)
        if (PaiJiuTiles.isDuiHou(a, b)) return PaiJiuPlay(tiles, PlayKind.PAIR)
        // 副子2：天九/地八/人七/鹅五
        val fu = fu2Name(a, b) ?: return null
        return PaiJiuPlay(tiles, PlayKind.FU_2, fu)
    }

    /** 副子2：天+杂九 / 地+杂八 / 人+杂七 / 鹅+杂五 */
    private fun fu2Name(a: PaiJiuTile, b: PaiJiuTile): FuName? {
        val tian = listOf(TileName.TIAN, TileName.DI, TileName.REN, TileName.E)
        val za = listOf(TileName.ZA_JIU, TileName.ZA_BA, TileName.ZA_QI, TileName.ZA_WU)
        for (i in 0..3) {
            val (t, z) = tian[i] to za[i]
            val names = setOf(a.name, b.name)
            if (names == setOf(t, z)) {
                return FuName.entries[i]
            }
        }
        return null
    }

    /** 3 张：类型 A（2 天门 + 1 对应杂）或 类型 B（1 天门 + 2 对应杂） */
    private fun classifyFu3(tiles: List<PaiJiuTile>): PaiJiuPlay? {
        val tianNames = listOf(TileName.TIAN, TileName.DI, TileName.REN, TileName.E)
        val zaNames = listOf(TileName.ZA_JIU, TileName.ZA_BA, TileName.ZA_QI, TileName.ZA_WU)
        for (i in 0..3) {
            val t = tianNames[i]; val z = zaNames[i]
            val tCnt = tiles.count { it.name == t }
            val zCnt = tiles.count { it.name == z }
            if (tCnt + zCnt != 3) continue
            if (tCnt == 2 && zCnt == 1) return PaiJiuPlay(tiles, PlayKind.FU_3A, FuName.entries[i])
            if (tCnt == 1 && zCnt == 2) return PaiJiuPlay(tiles, PlayKind.FU_3B, FuName.entries[i])
        }
        return null
    }

    /** 4 张：2 天门 + 2 对应杂 */
    private fun classifyFu4(tiles: List<PaiJiuTile>): PaiJiuPlay? {
        val tianNames = listOf(TileName.TIAN, TileName.DI, TileName.REN, TileName.E)
        val zaNames = listOf(TileName.ZA_JIU, TileName.ZA_BA, TileName.ZA_QI, TileName.ZA_WU)
        for (i in 0..3) {
            val t = tianNames[i]; val z = zaNames[i]
            if (tiles.count { it.name == t } == 2 && tiles.count { it.name == z } == 2) {
                return PaiJiuPlay(tiles, PlayKind.FU_4, FuName.entries[i])
            }
        }
        return null
    }

    /** 判定 play 是否能管上 current（同张数、同类型、更大） */
    fun canBeat(play: PaiJiuPlay, current: PaiJiuPlay): Boolean {
        if (play.count != current.count) return false // 出牌数必须相同
        if (play.kind != current.kind) return false // 不同类型不能互管
        return when (play.kind) {
            PlayKind.SINGLE -> canBeatSingle(play.tiles[0], current.tiles[0])
            PlayKind.PAIR -> canBeatPair(play, current)
            PlayKind.FU_2, PlayKind.FU_3A, PlayKind.FU_3B, PlayKind.FU_4 -> {
                // 副子：同名内比 FuName
                val a = play.fuName ?: return false
                val b = current.fuName ?: return false
                if (a == b) return false // 同级平
                a.order < b.order
            }
        }
    }

    /** 单张：同门内比大小；不同门不能比 */
    fun canBeatSingle(my: PaiJiuTile, cur: PaiJiuTile): Boolean {
        if (my.suit != cur.suit) return false // 不同门不能比较
        if (my.name == cur.name) return false
        return my.name.order < cur.name.order
    }

    /** 对子：对猴（首家）特殊处理在 Game 层；这里仅比较普通对子 */
    private fun canBeatPair(my: PaiJiuPlay, cur: PaiJiuPlay): Boolean {
        // 对猴规则：首家出对猴其他家不能管上；非首家出对猴不能管其他两张。
        // 这里统一返回 false（对猴的特殊处理在 Game 层：首家出对猴时其余只能垫牌）。
        if (my.isDuiHou || cur.isDuiHou) return false
        val a = my.tiles[0].name
        val b = cur.tiles[0].name
        if (a == b) return false
        // 对子只按首张牌的门+序比较？——对子由两张同名牌组成，首张即代表
        if (my.tiles[0].suit != cur.tiles[0].suit) return false
        return my.tiles[0].name.order < cur.tiles[0].name.order
    }

    /** 手牌中能管上 current 的所有出法 */
    fun legalPlays(hand: List<PaiJiuTile>, current: PaiJiuPlay): List<PaiJiuPlay> {
        val result = mutableListOf<PaiJiuPlay>()
        // 尝试从手牌中选 current.count 张组成能管上的牌型
        val indices = hand.indices.toList()
        combinations(hand, indices, current.count).forEach { combo ->
            val play = classify(combo) ?: return@forEach
            if (play.kind == current.kind && canBeat(play, current)) result.add(play)
        }
        return result.distinctBy { it.tiles.map { t -> t.name } }
    }

    /** 枚举 hand 中所有 count 张组合（用于找可管上的出法） */
    private fun combinations(hand: List<PaiJiuTile>, indices: List<Int>, count: Int): List<List<PaiJiuTile>> {
        if (count == 0) return listOf(emptyList())
        if (indices.size < count) return emptyList()
        val result = mutableListOf<List<PaiJiuTile>>()
        for (i in indices.indices) {
            val rest = indices.subList(i + 1, indices.size)
            combinations(hand, rest, count - 1).forEach { tail ->
                result.add(listOf(hand[indices[i]]) + tail)
            }
        }
        return result
    }

    /** 发牌：32 张洗牌后每人 8 张 */
    fun deal(rng: Random = Random.Default): Array<List<PaiJiuTile>> {
        val shuffled = PaiJiuTiles.FULL_SET.shuffled(rng)
        return Array(4) { p -> shuffled.subList(p * 8, p * 8 + 8).toList() }
    }

    /** 牌型中文名（UI 用） */
    fun playLabel(play: PaiJiuPlay): String = when (play.kind) {
        PlayKind.SINGLE -> play.tiles[0].name.label
        PlayKind.PAIR -> if (play.isDuiHou) "对猴" else "${play.tiles[0].name.label}对"
        PlayKind.FU_2, PlayKind.FU_3A, PlayKind.FU_3B, PlayKind.FU_4 -> play.fuName?.label ?: "副子"
    }
}
