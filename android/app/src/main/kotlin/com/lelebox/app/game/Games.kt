package com.lelebox.app.game

import androidx.compose.ui.graphics.Color
import com.lelebox.app.R
import com.lelebox.app.ui.Game2048
import com.lelebox.app.ui.GameDoudizhu
import com.lelebox.app.ui.GameGomoku
import com.lelebox.app.ui.GameLink
import com.lelebox.app.ui.GameMahjong
import com.lelebox.app.ui.GameMemory
import com.lelebox.app.ui.GameSpot
import com.lelebox.app.ui.GameSudoku

/** 游戏接入层：L1 原生（Compose） / L2 离线 H5（WebView，预留） */
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
    /** 矢量图标（即梦生成 PNG 后可替换为位图资源，见 docs/09） */
    val iconRes: Int? = null,
    /** WEB 游戏：assets 内相对路径，如 "games/xxx/index.html" */
    val assetPath: String? = null,
)

object Games {
    /** 第一批（M1）：2048/数独/记忆翻牌原生；M2 起新增游戏（docs/11） */
    val firstBatch = listOf(
        GameEntry(
            id = "doudizhu",
            title = "斗地主",
            subtitle = "和电脑打牌，先出完赢",
            help = "叫地主后先出完牌就赢。可以出单张、对子、三张、顺子等。点牌选中，再点「出牌」。如果你不是地主，另一位电脑农民和你是一伙的，你们谁先出完都算你赢。",
            kind = GameKind.NATIVE,
            emoji = "🃏",
            accent = GameDoudizhu,
            iconRes = R.drawable.ic_game_doudizhu,
        ),
        GameEntry(
            id = "spot",
            title = "找不同",
            subtitle = "找一找哪里不一样",
            help = "左边和右边两幅图，找一找哪里不一样，点一下就算找到。全部找齐就赢啦！",
            kind = GameKind.NATIVE,
            emoji = "🔍",
            accent = GameSpot,
            iconRes = R.drawable.ic_game_spot,
        ),
        GameEntry(
            id = "link",
            title = "连连看",
            subtitle = "消掉两张一样的牌",
            help = "点两张一样的图案，中间的路没有挡住的牌就能消掉（最多拐两个弯）。全部消完就赢啦！",
            kind = GameKind.NATIVE,
            emoji = "🍎",
            accent = GameLink,
            iconRes = R.drawable.ic_game_link,
        ),
        GameEntry(
            id = "gomoku",
            title = "五子棋",
            subtitle = "先连成五个就赢",
            help = "你先下黑棋，电脑下白棋。横、竖、斜哪边先连成五个就赢。点一下棋盘交叉点落子。",
            kind = GameKind.NATIVE,
            emoji = "⚫",
            accent = GameGomoku,
            iconRes = R.drawable.ic_game_gomoku,
        ),
        GameEntry(
            id = "2048",
            title = "2048",
            subtitle = "合并数字，越合越大",
            help = "向左、向右、向上或向下滑动，相同数字碰到一起会合并成更大的数字。也可以点下面的大方向按钮。目标：合成 2048！",
            kind = GameKind.NATIVE,
            emoji = "🔢",
            accent = Game2048,
            iconRes = R.drawable.ic_game_2048,
        ),
        GameEntry(
            id = "sudoku",
            title = "数独",
            subtitle = "每行每列每宫不重复",
            help = "先点一个格子，再点下面数字填入。每行、每列、每个九宫格里，1 到 9 各出现一次。慢慢想，不着急。",
            kind = GameKind.NATIVE,
            emoji = "🧩",
            accent = GameSudoku,
            iconRes = R.drawable.ic_game_sudoku,
        ),
        GameEntry(
            id = "memory",
            title = "记忆翻牌",
            subtitle = "翻开两张相同的牌",
            help = "点一张牌翻开，再点另一张。两张图案一样就配对成功，不一样会自己翻回去。全部配对成功就赢啦！",
            kind = GameKind.NATIVE,
            emoji = "🎴",
            accent = GameMemory,
            iconRes = R.drawable.ic_game_memory,
        ),
        GameEntry(
            id = "mahjong",
            title = "麻将",
            subtitle = "凑齐四副加一对就胡",
            help = "你和三位电脑打国标麻将。摸牌、打牌，凑成四副顺子或刻子加一对将就胡牌。可以吃、碰、杠。胡牌后看看赢多少番。",
            kind = GameKind.NATIVE,
            emoji = "🀄",
            accent = GameMahjong,
        ),
    )

    fun byId(id: String): GameEntry = firstBatch.first { it.id == id }
}
