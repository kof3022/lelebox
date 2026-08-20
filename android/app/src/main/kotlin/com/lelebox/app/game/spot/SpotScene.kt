package com.lelebox.app.game.spot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** 一处差异（0..1000 归一坐标）+ 说明 */
data class SpotDiff(val rect: Rect, val desc: String)

enum class SpotLevel(val label: String, val diffCount: Int) {
    EASY("简单（5 处）", 5),
    HARD("困难（7 处）", 7),
}

/**
 * 找不同游戏：两幅场景图（原图/变体），差异区域确定（由绘制代码保证），
 * 点击差异区域即找到。两幅图完全离线程序化绘制（后续可替换为即梦底图，差异表不变）。
 */
class SpotGame(val level: SpotLevel) {
    val sceneName = if (level == SpotLevel.EASY) "花园" else "公园"
    val diffs: List<SpotDiff> =
        (if (level == SpotLevel.EASY) GARDEN_DIFFS else PARK_DIFFS).take(level.diffCount)
    val found = mutableSetOf<Int>()
    var misses = 0
        private set
    var over = false
        private set

    /** 提示：返回一个未找到差异的索引，-1 表示全找到 */
    fun hint(): Int = diffs.indices.firstOrNull { it !in found } ?: -1

    /** 重新开始一局（同场景） */
    fun restart() {
        found.clear()
        misses = 0
        over = false
    }

    /** nx,ny ∈ 0..1000；命中未找差异返回 true */
    fun checkTap(nx: Float, ny: Float): Boolean {
        if (over) return false
        val pos = Offset(nx, ny)
        for (i in diffs.indices) {
            if (i !in found && diffs[i].rect.contains(pos)) {
                found.add(i)
                if (found.size == diffs.size) over = true
                return true
            }
        }
        misses++
        return false
    }
}

/** 场景统一坐标系 0..1000 */
internal class SceneCtx(scope: DrawScope) {
    val k = scope.size.minDimension / 1000f
    val ox = (scope.size.width - 1000f * k) / 2f
    val oy = (scope.size.height - 1000f * k) / 2f
    fun x(v: Float) = ox + v * k
    fun y(v: Float) = oy + v * k
    fun r(v: Float) = v * k
}

private fun DrawScope.circle(c: Color, cx: Float, cy: Float, rad: Float, stroke: Float? = null) {
    val s = SceneCtx(this)
    if (stroke == null) drawCircle(c, s.r(rad), Offset(s.x(cx), s.y(cy)))
    else drawCircle(c, s.r(rad), Offset(s.x(cx), s.y(cy)), style = Stroke(width = s.r(stroke)))
}

private fun DrawScope.rect(c: Color, lx: Float, ty: Float, rx: Float, by: Float) {
    val s = SceneCtx(this)
    drawRect(
        c,
        topLeft = Offset(s.x(lx), s.y(ty)),
        size = androidx.compose.ui.geometry.Size(s.r(rx - lx), s.r(by - ty)),
    )
}

private fun DrawScope.line(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 8f) {
    val s = SceneCtx(this)
    drawLine(c, Offset(s.x(x1), s.y(y1)), Offset(s.x(x2), s.y(y2)), strokeWidth = s.r(w))
}

/** 背景：天空 + 草地 */
private fun DrawScope.skyAndGrass(sky: Color, grass: Color) {
    rect(sky, 0f, 0f, 1000f, 720f)
    rect(grass, 0f, 720f, 1000f, 1000f)
}

private fun DrawScope.sun(cx: Float, cy: Float, rad: Float, color: Color, rays: Int) {
    circle(color, cx, cy, rad)
    for (i in 0 until rays) {
        val a = Math.toRadians((i * 360.0 / rays)).toFloat()
        val dx = Math.cos(a.toDouble()).toFloat()
        val dy = Math.sin(a.toDouble()).toFloat()
        line(Color(0xFFF6C453), cx + dx * rad * 1.2f, cy + dy * rad * 1.2f, cx + dx * rad * 1.8f, cy + dy * rad * 1.8f, 10f)
    }
}

private fun DrawScope.cloud(cx: Float, cy: Float, scale: Float, color: Color) {
    circle(color, cx, cy, 40f * scale)
    circle(color, cx - 42f * scale, cy + 14f * scale, 30f * scale)
    circle(color, cx + 44f * scale, cy + 12f * scale, 32f * scale)
}

private fun DrawScope.tree(tx: Float, ty: Float, crownColor: Color, withApples: Boolean) {
    rect(Color(0xFF8A5A2B), tx - 26f, ty, tx + 26f, ty + 150f)
    circle(crownColor, tx, ty, 110f)
    circle(crownColor, tx - 80f, ty + 40f, 70f)
    circle(crownColor, tx + 84f, ty + 36f, 72f)
    if (withApples) {
        circle(Color(0xFFD94F3D), tx, ty - 10f, 16f)
        circle(Color(0xFFD94F3D), tx + 50f, ty + 30f, 15f)
        circle(Color(0xFFD94F3D), tx - 55f, ty + 26f, 15f)
    }
}

private fun DrawScope.flower(fx: Float, fy: Float, petal: Color) {
    circle(Color(0xFF6A994E), fx, fy + 16f, 12f) // 茎基
    for (i in 0 until 5) {
        val a = Math.toRadians((i * 72.0)).toFloat()
        circle(petal, fx + Math.cos(a.toDouble()).toFloat() * 16f, fy + Math.sin(a.toDouble()).toFloat() * 16f, 13f)
    }
    circle(Color(0xFFF7C948), fx, fy, 12f)
}

private fun DrawScope.bird(bx: Float, by: Float, color: Color) {
    // 简单小鸟：身体圆 + 翅膀 + 头
    circle(color, bx, by, 16f)
    circle(color, bx + 20f, by - 8f, 10f)
    line(color, bx - 6f, by - 10f, bx + 10f, by - 18f, 4f)
    line(color, bx + 12f, by - 16f, bx + 26f, by - 10f, 4f)
}

private fun DrawScope.butterfly(fx: Float, fy: Float, color: Color) {
    circle(color, fx - 14f, fy, 13f)
    circle(color, fx + 14f, fy, 13f)
    circle(color, fx, fy - 14f, 10f)
    circle(color, fx, fy + 14f, 10f)
    line(Color(0xFF5A3A22), fx, fy - 20f, fx, fy + 20f, 3f)
}

/** 花园场景（6 处差异） */
private val GARDEN_DIFFS = listOf(
    SpotDiff(Rect(430f, 90f, 620f, 330f), "太阳的光芒"),
    SpotDiff(Rect(120f, 120f, 330f, 250f), "一朵云"),
    SpotDiff(Rect(150f, 760f, 290f, 900f), "左花颜色"),
    SpotDiff(Rect(760f, 140f, 900f, 260f), "一只小鸟"),
    SpotDiff(Rect(500f, 470f, 760f, 760f), "树上的苹果"),
    SpotDiff(Rect(820f, 820f, 970f, 950f), "一只蝴蝶"),
)

fun drawGarden(scope: DrawScope, variant: Boolean) {
    scope.skyAndGrass(Color(0xFFBBDEF5), Color(0xFFA8D8A0))
    // 太阳（差异1：光芒数量 8 vs 5）
    scope.sun(560f, 200f, 85f, Color(0xFFF6C453), if (variant) 5 else 8)
    // 云（差异2：变体没有云）
    if (!variant) scope.cloud(220f, 180f, 1f, Color.White)
    scope.cloud(760f, 120f, 0.8f, Color(0xFFFFFFFF))
    // 树（差异5：苹果）
    scope.tree(620f, 480f, Color(0xFF5E9E4F), withApples = !variant)
    // 花（差异3：颜色）
    scope.flower(220f, 790f, if (variant) Color(0xFFF2A0C0) else Color(0xFFD94F3D))
    scope.flower(700f, 830f, Color(0xFFE8A33D))
    scope.flower(880f, 760f, Color(0xFF8A5AC9))
    scope.flower(120f, 900f, Color(0xFFE8A33D))
    // 鸟（差异4：变体没有鸟）
    if (!variant) scope.bird(830f, 200f, Color(0xFF3E6B8A))
    // 蝴蝶（差异6：变体没有蝴蝶）
    if (!variant) scope.butterfly(890f, 880f, Color(0xFFC4623C))
}

/** 公园场景（7 处差异） */
private val PARK_DIFFS = listOf(
    SpotDiff(Rect(430f, 60f, 700f, 300f), "天上的风筝"),
    SpotDiff(Rect(160f, 720f, 430f, 890f), "长椅颜色"),
    SpotDiff(Rect(520f, 600f, 720f, 780f), "池塘的鸭子"),
    SpotDiff(Rect(730f, 660f, 920f, 820f), "气球"),
    SpotDiff(Rect(180f, 380f, 480f, 560f), "喷泉的水"),
    SpotDiff(Rect(30f, 500f, 155f, 900f), "栅栏的柱子"),
    SpotDiff(Rect(60f, 80f, 320f, 200f), "太阳颜色"),
)

fun drawPark(scope: DrawScope, variant: Boolean) {
    scope.skyAndGrass(Color(0xFFD7E9F5), Color(0xFFB5D99C))
    // 太阳（差异7：颜色）
    scope.sun(170f, 130f, 70f, if (variant) Color(0xFFF6C453) else Color(0xFFF08A3C), 8)
    // 风筝（差异1：变体没有）
    if (!variant) {
        scope.line(Color(0xFF7A5C3E), 560f, 320f, 560f, 560f, 4f)
        val kc = Color(0xFFE85D5D)
        scope.rect(kc, 520f, 240f, 600f, 320f)
        scope.line(Color(0xFFFFFFFF), 520f, 240f, 600f, 320f, 3f)
        scope.line(Color(0xFFFFFFFF), 600f, 240f, 520f, 320f, 3f)
    }
    // 喷泉（差异5：水）
    scope.rect(Color(0xFF9AA5A8), 210f, 440f, 430f, 520f)
    scope.circle(Color(0xFF7FA6B8), 320f, 440f, 40f)
    if (!variant) {
        scope.circle(Color(0xFF7FBFE8), 320f, 400f, 22f)
        scope.circle(Color(0xFF7FBFE8), 320f, 370f, 14f)
    }
    // 池塘（差异3：鸭子数量）
    scope.circle(Color(0xFF6FB4D6), 620f, 690f, 70f)
    if (variant) {
        scope.bird(590f, 680f, Color(0xFFF2C14E))
    } else {
        scope.bird(580f, 680f, Color(0xFFF2C14E))
        scope.bird(640f, 690f, Color(0xFFF2C14E))
    }
    // 长椅（差异2：颜色）
    val bench = if (variant) Color(0xFF5E8C5E) else Color(0xFF9A6B3F)
    scope.rect(bench, 150f, 740f, 390f, 820f)
    scope.rect(Color(0xFF7A5230), 160f, 820f, 210f, 880f)
    scope.rect(Color(0xFF7A5230), 330f, 820f, 380f, 880f)
    // 气球（差异4：变体没有）
    if (!variant) {
        scope.line(Color(0xFF7A5C3E), 820f, 760f, 820f, 880f, 4f)
        scope.circle(Color(0xFFE85D5D), 820f, 730f, 30f)
    }
    // 栅栏（差异6：柱子数量）
    for (i in 0 until 5) {
        val px = 60f + i * 20f
        if (!variant || i != 2) scope.rect(Color(0xFFC9A26B), px, 560f, px + 14f, 900f)
    }
    scope.rect(Color(0xFFC9A26B), 40f, 700f, 150f, 716f)
    scope.rect(Color(0xFFC9A26B), 40f, 800f, 150f, 816f)
    // 树
    scope.tree(760f, 320f, Color(0xFF5E9E4F), withApples = false)
}
