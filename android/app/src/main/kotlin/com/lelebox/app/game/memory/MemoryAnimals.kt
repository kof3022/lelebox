package com.lelebox.app.game.memory

import com.lelebox.app.R

/**
 * 卡面动物（8 种常见动物，风格统一；即梦生成，见 docs/10）。
 * iconRes 指向 drawable-xxhdpi/xxxhdpi 的压缩 PNG。
 */
data class MemoryAnimal(
    val id: String,
    val emoji: String,
    val name: String,
    val iconRes: Int? = null,
)

val MEMORY_ANIMALS = listOf(
    MemoryAnimal("cat", "🐱", "小猫", R.drawable.ic_animal_cat),
    MemoryAnimal("dog", "🐶", "小狗", R.drawable.ic_animal_dog),
    MemoryAnimal("rabbit", "🐰", "兔子", R.drawable.ic_animal_rabbit),
    MemoryAnimal("panda", "🐼", "熊猫", R.drawable.ic_animal_panda),
    MemoryAnimal("frog", "🐸", "青蛙", R.drawable.ic_animal_frog),
    MemoryAnimal("monkey", "🐵", "猴子", R.drawable.ic_animal_monkey),
    MemoryAnimal("bear", "🐻", "小熊", R.drawable.ic_animal_bear),
    MemoryAnimal("fox", "🦊", "狐狸", R.drawable.ic_animal_fox),
)
