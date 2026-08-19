package com.lelebox.app.game.memory

/** 记忆翻牌：级别定义与牌面生成（docs/01：卡面用大号动物头像，无计时无惩罚） */
enum class MemoryLevel(val label: String, val pairs: Int, val columns: Int) {
    EASY("简单（6 对）", 6, 4),
    HARD("困难（8 对）", 8, 4),
}

/** 生成一副洗好的牌：每只动物出现两次 */
fun buildMemoryBoard(level: MemoryLevel): List<MemoryAnimal> {
    val faces = MEMORY_ANIMALS.shuffled().take(level.pairs)
    return (faces + faces).shuffled()
}
