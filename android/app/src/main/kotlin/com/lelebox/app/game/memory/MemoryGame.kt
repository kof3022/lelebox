package com.lelebox.app.game.memory

/** 记忆翻牌：级别定义与牌面生成（docs/01：卡面用大号象形 emoji，无计时无惩罚） */
enum class MemoryLevel(val label: String, val pairs: Int, val columns: Int) {
    EASY("简单（6 对）", 6, 4),
    HARD("困难（8 对）", 8, 4),
}

/** 大号象形卡面，老人一眼可辨 */
val MEMORY_EMOJIS = listOf(
    "🐶", "🐱", "🐰", "🦊", "🐻", "🐼",
    "🐸", "🐵", "🐧", "🦁", "🐮", "🐷",
)

/** 生成一副洗好的牌：每张图案出现两次 */
fun buildMemoryBoard(level: MemoryLevel): List<String> {
    val faces = MEMORY_EMOJIS.shuffled().take(level.pairs)
    return (faces + faces).shuffled()
}
