package com.lelebox.app.game.paijiu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lelebox.app.R

/**
 * 牌九牌面：使用开源项目 [ammagician/GuPai](https://github.com/ammagician/GuPai)
 * 的骨牌图片（黑底暖白点/红点），替代原先的深色点阵。
 *
 * 牌面图片为 1:2.0 的方形骨牌（320x640 已统一）；调用方应使用与
 * PaiJiuTileWidth/Height 相同的长宽比尺寸，避免拉伸变形。
 */
@Composable
fun PaiJiuTileFace(
    tile: PaiJiuTile,
    modifier: Modifier = Modifier,
    showName: Boolean = false,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(tileRes(tile)),
            contentDescription = tile.name.label,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        // 牌名（大字，老年友好；默认关闭）
        if (showName) {
            Text(
                tile.name.label,
                color = Color(0xFFFFE082),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/** 牌背（深色骨牌背，与牌面同风格） */
@Composable
fun PaiJiuTileBack(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.pj_back),
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        contentScale = ContentScale.FillBounds,
    )
}

/** 牌面 → 图片资源（按点数映射，1:2.6 深色骨牌） */
fun tileRes(tile: PaiJiuTile): Int {
    val a = minOf(tile.pip1, tile.pip2)
    val b = maxOf(tile.pip1, tile.pip2)
    return when (a to b) {
        1 to 1 -> R.drawable.pj_1_1
        1 to 2 -> R.drawable.pj_1_2
        1 to 3 -> R.drawable.pj_1_3
        1 to 4 -> R.drawable.pj_1_4
        1 to 5 -> R.drawable.pj_1_5
        1 to 6 -> R.drawable.pj_1_6
        2 to 2 -> R.drawable.pj_2_2
        2 to 3 -> R.drawable.pj_2_3
        2 to 4 -> R.drawable.pj_2_4
        2 to 5 -> R.drawable.pj_2_5
        2 to 6 -> R.drawable.pj_2_6
        3 to 3 -> R.drawable.pj_3_3
        3 to 4 -> R.drawable.pj_3_4
        3 to 5 -> R.drawable.pj_3_5
        3 to 6 -> R.drawable.pj_3_6
        4 to 4 -> R.drawable.pj_4_4
        4 to 5 -> R.drawable.pj_4_5
        4 to 6 -> R.drawable.pj_4_6
        5 to 5 -> R.drawable.pj_5_5
        5 to 6 -> R.drawable.pj_5_6
        6 to 6 -> R.drawable.pj_6_6
        else -> R.drawable.pj_back
    }
}

/** 便捷尺寸（方形骨牌 1:2.0；手牌最大） */
val PaiJiuTileWidth: Dp = 52.dp
val PaiJiuTileHeight: Dp = 104.dp
