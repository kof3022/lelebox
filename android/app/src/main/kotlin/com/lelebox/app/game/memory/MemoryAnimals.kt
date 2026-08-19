package com.lelebox.app.game.memory

/**
 * 卡面动物（8 种常见动物，风格统一）。
 * 即梦生成 PNG 后填入 iconRes（如 R.drawable.ic_animal_cat），同名即可覆盖 emoji 显示；
 * 提示词见 docs/10。
 */
data class MemoryAnimal(
    val id: String,
    val emoji: String,
    val name: String,
    val iconRes: Int? = null,
)

val MEMORY_ANIMALS = listOf(
    MemoryAnimal("cat", "🐱", "小猫"),
    MemoryAnimal("dog", "🐶", "小狗"),
    MemoryAnimal("rabbit", "🐰", "兔子"),
    MemoryAnimal("panda", "🐼", "熊猫"),
    MemoryAnimal("frog", "🐸", "青蛙"),
    MemoryAnimal("monkey", "🐵", "猴子"),
    MemoryAnimal("bear", "🐻", "小熊"),
    MemoryAnimal("fox", "🦊", "狐狸"),
)
