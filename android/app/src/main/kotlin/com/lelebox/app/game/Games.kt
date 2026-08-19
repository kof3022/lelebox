package com.lelebox.app.game

import androidx.compose.ui.graphics.Color

/** 游戏接入层：L1 原生（Compose） / L2 离线 H5（WebView） */
enum class GameKind { NATIVE, WEB }

/** 游戏注册表条目 */
data class GameEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: GameKind,
    val emoji: String,
    val accent: Color,
    /** WEB 游戏：assets 内相对路径，如 "games/2048/index.html" */
    val assetPath: String? = null,
)

object Games {
    /**
     * 第一批（M1 交付）。
     * M0 阶段：2048 先以 H5 冒烟验证 L2 管线，M1 起替换为原生 Compose 版；
     * 其余三款为原生占位（M1 实现）。
     */
    val firstBatch = listOf(
        GameEntry(
            id = "2048",
            title = "2048",
            subtitle = "合并数字，越合越大",
            kind = GameKind.WEB,
            emoji = "🔢",
            accent = Color(0xFF2E7D32),
            assetPath = "games/2048/index.html",
        ),
        GameEntry(
            id = "solitaire",
            title = "纸牌接龙",
            subtitle = "把牌按顺序收进家",
            kind = GameKind.NATIVE,
            emoji = "🃏",
            accent = Color(0xFFC62828),
        ),
        GameEntry(
            id = "sudoku",
            title = "数独",
            subtitle = "每行每列每宫不重复",
            kind = GameKind.NATIVE,
            emoji = "🧩",
            accent = Color(0xFF1565C0),
        ),
        GameEntry(
            id = "memory",
            title = "记忆翻牌",
            subtitle = "翻开两张相同的牌",
            kind = GameKind.NATIVE,
            emoji = "🎴",
            accent = Color(0xFF6A1B9A),
        ),
    )

    fun byId(id: String): GameEntry = firstBatch.first { it.id == id }
}
