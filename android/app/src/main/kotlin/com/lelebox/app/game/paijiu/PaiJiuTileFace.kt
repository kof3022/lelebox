package com.lelebox.app.game.paijiu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 牌九牌面：Canvas 程序化绘制。
 * 传统样式：白底（象牙白）、窄长形（1:3 左右）、上下两格、红黑点。
 * 红点：1 和 4；黑点：2、3、5、6（骰子排列）。
 * 牌面上不显示牌名（默认），保持传统骨牌外观。
 */
@Composable
fun PaiJiuTileFace(
    tile: PaiJiuTile,
    modifier: Modifier = Modifier,
    showName: Boolean = false,
) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(6.dp))) {
        drawDominoTile(tile, showName)
    }
}

/** 牌背（统一米色，与麻将牌背风格一致） */
@Composable
fun PaiJiuTileBack(
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF8A6F4A), RoundedCornerShape(4.dp)),
    ) {
        drawRoundRect(
            color = Color(0xFFF3E9D4),
            cornerRadius = CornerRadius(size.minDimension * 0.12f),
        )
        // 中间分隔线
        drawLine(
            color = Color(0xFFD8C9A8),
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.minDimension * 0.04f,
        )
    }
}

/** 在 DrawScope 中绘制一张骨牌（供 PaiJiuTileFace 复用） */
internal fun DrawScope.drawDominoTile(tile: PaiJiuTile, showName: Boolean) {
    // 底色
    drawRoundRect(
        color = Color(0xFFFBF6EA),
        cornerRadius = CornerRadius(size.minDimension * 0.1f),
    )
    // 细边框
    drawRoundRect(
        color = Color(0xFF9A8254),
        cornerRadius = CornerRadius(size.minDimension * 0.1f),
        style = Stroke(width = size.minDimension * 0.03f),
    )
    // 上下分隔线
    val midY = size.height / 2
    drawLine(
        color = Color(0xFFD8C9A8),
        start = Offset(size.width * 0.08f, midY),
        end = Offset(size.width * 0.92f, midY),
        strokeWidth = size.minDimension * 0.035f,
    )
    // 上下两格的点
    drawPips(tile.pip1, Offset(0f, 0f), Size(size.width, midY))
    drawPips(tile.pip2, Offset(0f, midY), Size(size.width, midY))
    // 牌名（大字，老年友好）
    if (showName) {
        drawContext.canvas.nativeCanvas.drawText(
            tile.name,
            size.width / 2,
            size.height * 0.52f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(0x4E, 0x3A, 0x1E)
                textSize = size.width * 0.5f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            },
        )
    }
}

/** 在一个格子内按骰子排列绘制 n 个点（1..6）；红点：1、4 */
private fun DrawScope.drawPips(n: Int, origin: Offset, cell: Size) {
    if (n < 1 || n > 6) return
    val red = Color(0xFFC62828)
    val black = Color(0xFF263238)
    val r = cell.minDimension * 0.16f
    // 点位置（相对格子的归一化坐标 0..1）
    val positions = pipPositions(n)
    for ((fx, fy) in positions) {
        val cx = origin.x + cell.width * fx
        val cy = origin.y + cell.height * fy
        drawCircle(
            color = if (n == 1 || n == 4) red else black,
            radius = r,
            center = Offset(cx, cy),
        )
    }
}

/** 骰子点排列（归一化坐标，格内 0.25/0.5/0.75 三列三行） */
private fun pipPositions(n: Int): List<Pair<Float, Float>> = when (n) {
    1 -> listOf(0.5f to 0.5f)
    2 -> listOf(0.3f to 0.3f, 0.7f to 0.7f)
    3 -> listOf(0.3f to 0.3f, 0.5f to 0.5f, 0.7f to 0.7f)
    4 -> listOf(0.3f to 0.3f, 0.7f to 0.3f, 0.3f to 0.7f, 0.7f to 0.7f)
    5 -> listOf(0.3f to 0.3f, 0.7f to 0.3f, 0.5f to 0.5f, 0.3f to 0.7f, 0.7f to 0.7f)
    6 -> listOf(0.3f to 0.3f, 0.3f to 0.5f, 0.3f to 0.7f, 0.7f to 0.3f, 0.7f to 0.5f, 0.7f to 0.7f)
    else -> emptyList()
}

/** 便捷尺寸 */
val PaiJiuTileWidth: Dp = 56.dp
val PaiJiuTileHeight: Dp = 90.dp
