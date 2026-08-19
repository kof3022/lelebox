package com.lelebox.app.game

import androidx.compose.ui.graphics.Color
import com.lelebox.app.ui.Game2048
import com.lelebox.app.ui.GameMemory
import com.lelebox.app.ui.GameSolitaire
import com.lelebox.app.ui.GameSudoku

/** 游戏接入层：L1 原生（Compose） / L2 离线 H5（WebView） */
enum class GameKind { NATIVE, WEB }

/** 游戏注册表条目 */
data class GameEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val help: String,
    val kind: GameKind,
    val emoji: String,
    val accent: Color,
    /** WEB 游戏：assets 内相对路径，如 "games/solitaire/index.html" */
    val assetPath: String? = null,
)

object Games {
    /** 第一批（M1）：纸牌接龙=自研 H5，2048/数独/记忆翻牌=原生 Compose */
    val firstBatch = listOf(
        GameEntry(
            id = "2048",
            title = "2048",
            subtitle = "合并数字，越合越大",
            help = "向左、向右、向上或向下滑动，相同数字碰到一起会合并成更大的数字。也可以点下面的大方向按钮。目标：合成 2048！",
            kind = GameKind.NATIVE,
            emoji = "🔢",
            accent = Game2048,
        ),
        GameEntry(
            id = "solitaire",
            title = "纸牌接龙",
            subtitle = "把牌按顺序收进家",
            help = "点一下牌堆翻牌；点一张牌选中（亮黄边），再点目标位置放牌。红黑花色交替往下摆，同花色从小到大收进右上角四个家。全部收完就赢啦！",
            kind = GameKind.WEB,
            emoji = "🃏",
            accent = GameSolitaire,
            assetPath = "games/solitaire/index.html",
        ),
        GameEntry(
            id = "sudoku",
            title = "数独",
            subtitle = "每行每列每宫不重复",
            help = "先点一个格子，再点下面数字填入。每行、每列、每个九宫格里，1 到 9 各出现一次。慢慢想，不着急。",
            kind = GameKind.NATIVE,
            emoji = "🧩",
            accent = GameSudoku,
        ),
        GameEntry(
            id = "memory",
            title = "记忆翻牌",
            subtitle = "翻开两张相同的牌",
            help = "点一张牌翻开，再点另一张。两张图案一样就配对成功，不一样会自己翻回去。全部配对成功就赢啦！",
            kind = GameKind.NATIVE,
            emoji = "🎴",
            accent = GameMemory,
        ),
    )

    fun byId(id: String): GameEntry = firstBatch.first { it.id == id }
}
