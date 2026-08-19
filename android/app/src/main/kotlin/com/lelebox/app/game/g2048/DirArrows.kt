package com.lelebox.app.game.g2048

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 极简方向箭头（自绘 ImageVector，细直线 + 实心箭头） */
object DirArrows {

    private fun build(name: String, d: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(androidx.compose.ui.graphics.Color.Black)) {
                // 以 path 指令解析
                val tokens = d.trim().split(Regex("\\s+"))
                var i = 0
                while (i < tokens.size) {
                    when (tokens[i]) {
                        "M" -> moveTo(tokens[++i].toFloat(), tokens[++i].toFloat())
                        "L" -> lineTo(tokens[++i].toFloat(), tokens[++i].toFloat())
                        "H" -> horizontalLineTo(tokens[++i].toFloat())
                        "V" -> verticalLineTo(tokens[++i].toFloat())
                        "Z" -> close()
                    }
                    i++
                }
            }
        }.build()

    val Up: ImageVector = build("ArrowUp", "M12 3 L17 9 H14 V19 H10 V9 H7 Z")
    val Down: ImageVector = build("ArrowDown", "M12 21 L7 15 H10 V5 H14 V15 H17 Z")
    val Left: ImageVector = build("ArrowLeft", "M3 12 L9 7 V10 H19 V14 H9 V17 Z")
    val Right: ImageVector = build("ArrowRight", "M21 12 L15 7 V10 H5 V14 H15 V17 Z")

    fun of(dir: Dir): ImageVector = when (dir) {
        Dir.UP -> Up
        Dir.DOWN -> Down
        Dir.LEFT -> Left
        Dir.RIGHT -> Right
    }
}
