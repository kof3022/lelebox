package com.lelebox.app.game.spot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lelebox.app.R

/** 一处差异（0..1000 归一坐标）+ 说明 */
data class SpotDiff(val rect: Rect, val desc: String)

/** 一个关卡场景：名称 + 差异表 + 绘制函数（variant=true 为变体图）；bgRes=即梦底图（null 则程序化绘制） */
data class SpotSceneDef(
    val name: String,
    val diffs: List<SpotDiff>,
    val draw: DrawScope.(variant: Boolean) -> Unit = {},
    val bgRes: Int? = null,
)

/**
 * 找不同：3 难度 × 3 关 = 9 关（参考开源多关卡找不同如 joe-brothers/differ 的关卡结构；
 * 场景为程序化绘制，差异坐标确定可单测；后续可换即梦底图，差异表不变）。
 */
enum class SpotLevel(val label: String, val diffCount: Int, val scenes: List<SpotSceneDef>) {
    EASY("简单", 5, listOf(SCENE_GARDEN, SCENE_LIVING, SCENE_ORCHARD)),
    MEDIUM("中等", 6, listOf(SCENE_PARK, SCENE_SEASIDE, SCENE_FARM)),
    HARD("困难", 7, listOf(SCENE_SNOW, SCENE_STREET, SCENE_NIGHT)),
}

class SpotGame(val scene: SpotSceneDef) {
    val sceneName = scene.name
    val diffs = scene.diffs
    val found = mutableSetOf<Int>()
    var misses = 0
        private set
    var over = false
        private set

    fun hint(): Int = diffs.indices.firstOrNull { it !in found } ?: -1

    fun restart() {
        found.clear()
        misses = 0
        over = false
    }

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

private fun DrawScope.ellipse(c: Color, cx: Float, cy: Float, rx: Float, ry: Float) {
    val s = SceneCtx(this)
    drawOval(c, topLeft = Offset(s.x(cx - rx), s.y(cy - ry)), size = androidx.compose.ui.geometry.Size(s.r(rx * 2), s.r(ry * 2)))
}

private fun DrawScope.roundedRect(c: Color, lx: Float, ty: Float, rx: Float, by: Float, radius: Float = 30f) {
    val s = SceneCtx(this)
    drawRoundRect(
        c,
        topLeft = Offset(s.x(lx), s.y(ty)),
        size = androidx.compose.ui.geometry.Size(s.r(rx - lx), s.r(by - ty)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s.r(radius), s.r(radius)),
    )
}

private fun DrawScope.rect(c: Color, lx: Float, ty: Float, rx: Float, by: Float) {
    val s = SceneCtx(this)
    drawRect(c, topLeft = Offset(s.x(lx), s.y(ty)), size = androidx.compose.ui.geometry.Size(s.r(rx - lx), s.r(by - ty)))
}

private fun DrawScope.line(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 8f) {
    val s = SceneCtx(this)
    drawLine(c, Offset(s.x(x1), s.y(y1)), Offset(s.x(x2), s.y(y2)), strokeWidth = s.r(w))
}

/** 底部阴影（椭圆），给物体落地感 */
private fun DrawScope.groundShadow(cx: Float, cy: Float, rx: Float, ry: Float = rx * 0.3f) {
    ellipse(Color(0x33000000), cx, cy, rx, ry)
}

/** 天空 + 草地，带渐变与远处山丘层次 */
private fun DrawScope.skyAndGrass(sky: Color, grass: Color) {
    val s = SceneCtx(this)
    // 天空渐变
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(sky, Color.White.copy(alpha = 0.55f)),
            startY = s.y(0f), endY = s.y(720f),
        ),
        topLeft = Offset(s.x(0f), s.y(0f)),
        size = androidx.compose.ui.geometry.Size(s.r(1000f), s.r(720f)),
    )
    // 草地渐变
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(grass, grass.copy(alpha = 0.7f)),
            startY = s.y(720f), endY = s.y(1000f),
        ),
        topLeft = Offset(s.x(0f), s.y(720f)),
        size = androidx.compose.ui.geometry.Size(s.r(1000f), s.r(280f)),
    )
    // 远山剪影
    val hill = grass.copy(alpha = 0.45f)
    ellipse(hill, 160f, 730f, 340f, 120f)
    ellipse(hill, 820f, 740f, 300f, 110f)
    ellipse(hill, 500f, 750f, 260f, 100f)
}

/** 太阳：光晕 + 圆盘 + 光芒 */
private fun DrawScope.sun(cx: Float, cy: Float, rad: Float, color: Color, rays: Int) {
    circle(color.copy(alpha = 0.30f), cx, cy, rad * 1.45f)
    circle(color, cx, cy, rad)
    for (i in 0 until rays) {
        val a = Math.toRadians((i * 360.0 / rays)).toFloat()
        val dx = Math.cos(a.toDouble()).toFloat()
        val dy = Math.sin(a.toDouble()).toFloat()
        line(Color(0xFFF6C453), cx + dx * rad * 1.2f, cy + dy * rad * 1.2f, cx + dx * rad * 1.8f, cy + dy * rad * 1.8f, 10f)
    }
}

/** 云：底座 + 多层圆 */
private fun DrawScope.cloud(cx: Float, cy: Float, scale: Float, color: Color) {
    ellipse(color, cx, cy + 14f * scale, 78f * scale, 26f * scale)
    circle(color, cx - 46f * scale, cy + 4f * scale, 32f * scale)
    circle(color, cx + 46f * scale, cy + 2f * scale, 34f * scale)
    circle(color, cx, cy - 18f * scale, 38f * scale)
}

/** 树：树干 + 树冠多层圆 + 阴影 + 高光 */
private fun DrawScope.tree(tx: Float, ty: Float, crownColor: Color, withApples: Boolean, appleColor: Color = Color(0xFFD94F3D)) {
    groundShadow(tx, ty + 165f, 135f)
    rect(Color(0xFF8A5A2B), tx - 26f, ty, tx + 26f, ty + 150f)
    circle(crownColor, tx, ty - 12f, 112f)
    circle(crownColor, tx - 82f, ty + 42f, 70f)
    circle(crownColor, tx + 86f, ty + 38f, 72f)
    circle(crownColor.copy(alpha = 0.35f), tx - 40f, ty - 70f, 55f) // 高光
    if (withApples) {
        circle(appleColor, tx, ty - 20f, 16f)
        circle(appleColor, tx + 52f, ty + 30f, 15f)
        circle(appleColor, tx - 58f, ty + 26f, 15f)
    }
}

/** 鸟：身体 + 翅膀 */
private fun DrawScope.bird(bx: Float, by: Float, color: Color) {
    circle(color, bx, by, 15f)
    circle(color, bx + 18f, by - 6f, 9f)
    line(color, bx - 8f, by - 12f, bx + 10f, by - 20f, 4f)
    line(color, bx + 12f, by - 18f, bx + 26f, by - 12f, 4f)
}

/** 花：茎 + 花瓣 + 花蕊 */
private fun DrawScope.flower(fx: Float, fy: Float, petal: Color) {
    line(Color(0xFF6A994E), fx, fy, fx, fy + 20f, 5f)
    for (i in 0 until 5) {
        val a = Math.toRadians((i * 72.0)).toFloat()
        circle(petal, fx + Math.cos(a.toDouble()).toFloat() * 16f, fy + Math.sin(a.toDouble()).toFloat() * 16f, 13f)
    }
    circle(Color(0xFFF7C948), fx, fy, 12f)
}

private fun DrawScope.butterfly(fx: Float, fy: Float, color: Color) {
    circle(color, fx - 14f, fy, 13f)
    circle(color, fx + 14f, fy, 13f)
    circle(color, fx, fy - 14f, 10f)
    circle(color, fx, fy + 14f, 10f)
    line(Color(0xFF5A3A22), fx, fy - 20f, fx, fy + 20f, 3f)
}

/** 苹果串：叠在底图树上（底图树不结果，苹果为差异元素） */
private fun DrawScope.appleCluster(tx: Float, ty: Float, color: Color = Color(0xFFD94F3D)) {
    circle(color, tx - 12f, ty - 6f, 16f)
    circle(color, tx + 42f, ty + 6f, 15f)
    circle(color, tx - 70f, ty + 4f, 15f)
    circle(color, tx + 10f, ty + 46f, 13f)
}

// ============ 简单（5 处） ============

private val GARDEN_DIFFS = listOf(
    SpotDiff(Rect(430f, 90f, 620f, 330f), "太阳的光芒"),
    SpotDiff(Rect(120f, 120f, 330f, 250f), "一朵云"),
    SpotDiff(Rect(150f, 760f, 290f, 900f), "左花颜色"),
    SpotDiff(Rect(760f, 140f, 900f, 260f), "一只小鸟"),
    SpotDiff(Rect(500f, 470f, 760f, 760f), "树上的苹果"),
)

/** 花园：只画差异小元素（太阳光芒数 / 云 / 左花颜色 / 小鸟 / 苹果），底图为即梦空景 */
private fun DrawScope.drawGarden(variant: Boolean) {
    sun(560f, 200f, 85f, Color(0xFFF6C453), if (variant) 5 else 8)
    if (!variant) cloud(220f, 180f, 1f, Color.White)
    if (!variant) appleCluster(620f, 440f)
    flower(220f, 790f, if (variant) Color(0xFFF2A0C0) else Color(0xFFD94F3D))
    if (!variant) bird(830f, 200f, Color(0xFF3E6B8A))
}

val SCENE_GARDEN = SpotSceneDef(
    name = "花园", diffs = GARDEN_DIFFS, draw = { drawGarden(it) }, bgRes = R.drawable.spot_garden,
)

private val LIVING_DIFFS = listOf(
    SpotDiff(Rect(700f, 200f, 940f, 420f), "电视画面"),
    SpotDiff(Rect(100f, 330f, 330f, 620f), "台灯灯光"),
    SpotDiff(Rect(380f, 700f, 600f, 900f), "沙发上的猫"),
    SpotDiff(Rect(130f, 620f, 360f, 900f), "窗台的花"),
    SpotDiff(Rect(470f, 430f, 760f, 700f), "沙发靠垫颜色"),
)

/** 客厅：只画差异小元素（电视画面色 / 台灯光晕 / 窗台花 / 靠垫色 / 猫） */
private fun DrawScope.drawLiving(variant: Boolean) {
    rect(if (variant) Color(0xFF9AA5A8) else Color(0xFF7FBFE8), 735f, 215f, 915f, 400f) // 电视画面
    if (!variant) flower(265f, 830f, Color(0xFFE85D5D)) // 窗台花
    rect(if (variant) Color(0xFF5E8C5E) else Color(0xFFC9A26B), 490f, 450f, 760f, 600f) // 靠垫
    circle(if (variant) Color(0xFFB9A98D) else Color(0xFFF6C453), 200f, 600f, 50f) // 台灯光
    if (!variant) circle(Color(0x66F6C453), 200f, 560f, 90f) // 光晕
    if (!variant) {
        circle(Color(0xFFE8A33D), 520f, 780f, 30f)
        circle(Color(0xFFE8A33D), 550f, 800f, 26f)
        line(Color(0xFFE8A33D), 495f, 760f, 500f, 740f, 4f)
        line(Color(0xFFE8A33D), 545f, 760f, 550f, 740f, 4f)
    }
}

val SCENE_LIVING = SpotSceneDef(
    name = "客厅", diffs = LIVING_DIFFS, draw = { drawLiving(it) }, bgRes = R.drawable.spot_living,
)

private val ORCHARD_DIFFS = listOf(
    SpotDiff(Rect(430f, 80f, 600f, 300f), "太阳颜色"),
    SpotDiff(Rect(200f, 420f, 430f, 610f), "苹果颜色"),
    SpotDiff(Rect(720f, 560f, 940f, 740f), "果篮的苹果"),
    SpotDiff(Rect(820f, 120f, 950f, 240f), "树梢的小鸟"),
    SpotDiff(Rect(100f, 820f, 400f, 960f), "草丛小花颜色"),
)

/** 果园：只画差异小元素（太阳色 / 苹果色 / 篮中果数量 / 小鸟 / 小花色） */
private fun DrawScope.drawOrchard(variant: Boolean) {
    sun(500f, 180f, 70f, if (variant) Color(0xFFF6C453) else Color(0xFFF08A3C), 8)
    appleCluster(280f, 470f, if (variant) Color(0xFF6FAF5E) else Color(0xFFD94F3D)) // 树上苹果色
    if (variant) {
        circle(Color(0xFFD94F3D), 790f, 600f, 15f)
        circle(Color(0xFFD94F3D), 830f, 600f, 15f)
        circle(Color(0xFFD94F3D), 870f, 600f, 15f)
    } else {
        circle(Color(0xFFD94F3D), 790f, 600f, 15f)
        circle(Color(0xFFD94F3D), 830f, 600f, 15f)
        circle(Color(0xFFD94F3D), 870f, 600f, 15f)
        circle(Color(0xFFD94F3D), 810f, 585f, 15f)
        circle(Color(0xFFD94F3D), 850f, 585f, 15f)
    }
    if (!variant) bird(880f, 180f, Color(0xFF3E6B8A))
    flower(170f, 880f, if (variant) Color(0xFFF2A0C0) else Color(0xFFE85D5D))
    flower(260f, 910f, if (variant) Color(0xFFF2A0C0) else Color(0xFFE85D5D))
    flower(330f, 880f, if (variant) Color(0xFFE85D5D) else Color(0xFFF2A0C0))
}

val SCENE_ORCHARD = SpotSceneDef(
    name = "果园", diffs = ORCHARD_DIFFS, draw = { drawOrchard(it) }, bgRes = R.drawable.spot_orchard,
)

// ============ 中等（6 处） ============

private val PARK_DIFFS = listOf(
    SpotDiff(Rect(430f, 60f, 700f, 300f), "天上的风筝"),
    SpotDiff(Rect(160f, 720f, 430f, 890f), "长椅颜色"),
    SpotDiff(Rect(520f, 600f, 720f, 780f), "池塘的鸭子"),
    SpotDiff(Rect(730f, 660f, 920f, 820f), "气球"),
    SpotDiff(Rect(180f, 380f, 480f, 560f), "喷泉的水"),
    SpotDiff(Rect(30f, 500f, 155f, 900f), "栅栏的柱子"),
)

/** 公园：只画差异小元素（风筝 / 长椅色 / 鸭子 / 气球 / 喷泉水 / 栅栏柱） */
private fun DrawScope.drawPark(variant: Boolean) {
    if (!variant) {
        line(Color(0xFF7A5C3E), 560f, 320f, 560f, 560f, 4f)
        rect(Color(0xFFE85D5D), 520f, 240f, 600f, 320f)
        line(Color.White, 520f, 240f, 600f, 320f, 3f)
        line(Color.White, 600f, 240f, 520f, 320f, 3f)
    }
    if (!variant) {
        circle(Color(0xFF7FBFE8), 320f, 400f, 22f)
        circle(Color(0xFF7FBFE8), 320f, 370f, 14f)
    }
    if (variant) bird(590f, 680f, Color(0xFFF2C14E)) else {
        bird(580f, 680f, Color(0xFFF2C14E))
        bird(640f, 690f, Color(0xFFF2C14E))
    }
    rect(if (variant) Color(0xFF5E8C5E) else Color(0xFF9A6B3F), 150f, 740f, 390f, 820f)
    if (!variant) {
        line(Color(0xFF7A5C3E), 820f, 760f, 820f, 880f, 4f)
        circle(Color(0xFFE85D5D), 820f, 730f, 30f)
    }
    for (i in 0 until 5) {
        val px = 60f + i * 20f
        if (!variant || i != 2) rect(Color(0xFFC9A26B), px, 560f, px + 14f, 900f)
    }
}

val SCENE_PARK = SpotSceneDef(
    name = "公园", diffs = PARK_DIFFS, draw = { drawPark(it) }, bgRes = R.drawable.spot_park,
)

private val SEASIDE_DIFFS = listOf(
    SpotDiff(Rect(430f, 70f, 630f, 280f), "太阳"),
    SpotDiff(Rect(150f, 560f, 420f, 810f), "小木船"),
    SpotDiff(Rect(700f, 380f, 930f, 620f), "遮阳伞颜色"),
    SpotDiff(Rect(110f, 840f, 300f, 960f), "海星"),
    SpotDiff(Rect(760f, 120f, 950f, 260f), "海鸥"),
    SpotDiff(Rect(560f, 740f, 800f, 880f), "浪花"),
)

/** 海边：只画差异小元素（太阳 / 小船 / 伞色 / 海星 / 海鸥 / 浪花） */
private fun DrawScope.drawSeaside(variant: Boolean) {
    sun(520f, 170f, 75f, Color(0xFFF6C453), 8)
    if (!variant) {
        rect(Color(0xFF8A5A2B), 190f, 740f, 360f, 800f)
        line(Color(0xFF5A3A22), 260f, 740f, 260f, 660f, 6f)
        rect(Color.White, 240f, 600f, 280f, 680f)
    }
    circle(if (variant) Color(0xFFF2A0C0) else Color(0xFFE85D5D), 790f, 520f, 80f) // 伞面
    if (variant) bird(830f, 180f, Color(0xFF3E6B8A)) else {
        bird(820f, 170f, Color(0xFF3E6B8A))
        bird(880f, 200f, Color(0xFF3E6B8A))
    }
    if (variant) starfish(180f, 920f) else {
        starfish(180f, 920f)
        starfish(240f, 930f)
    }
    if (variant) rect(Color.White, 600f, 800f, 660f, 820f) else {
        rect(Color.White, 600f, 800f, 660f, 820f)
        rect(Color.White, 700f, 820f, 760f, 840f)
    }
}

private fun DrawScope.starfish(sx: Float, sy: Float) {
    circle(Color(0xFFF08A3C), sx, sy, 22f)
    circle(Color(0xFFD95F2B), sx - 16f, sy + 6f, 8f)
    circle(Color(0xFFD95F2B), sx + 16f, sy + 6f, 8f)
    circle(Color(0xFFD95F2B), sx, sy + 18f, 8f)
}

val SCENE_SEASIDE = SpotSceneDef(
    name = "海边", diffs = SEASIDE_DIFFS, draw = { drawSeaside(it) }, bgRes = R.drawable.spot_seaside,
)

private val FARM_DIFFS = listOf(
    SpotDiff(Rect(680f, 520f, 900f, 820f), "拖拉机"),
    SpotDiff(Rect(200f, 300f, 430f, 620f), "稻草人帽子颜色"),
    SpotDiff(Rect(500f, 700f, 680f, 900f), "奶牛"),
    SpotDiff(Rect(100f, 780f, 500f, 950f), "庄稼行数"),
    SpotDiff(Rect(430f, 300f, 620f, 560f), "谷仓门"),
    SpotDiff(Rect(800f, 100f, 950f, 220f), "云朵"),
)

/** 农田：只画差异小元素（拖拉机 / 稻草人帽色 / 奶牛 / 庄稼行数 / 谷仓门 / 云） */
private fun DrawScope.drawFarm(variant: Boolean) {
    cloud(860f, 160f, 0.8f, if (variant) Color(0xFFE8E4DA) else Color.White)
    rect(if (variant) Color(0xFF6E4A2F) else Color(0xFF8A5A2B), 500f, 400f, 550f, 560f) // 谷仓门
    circle(if (variant) Color(0xFF5E8C5E) else Color(0xFFF08A3C), 275f, 400f, 45f) // 稻草人帽子
    if (!variant) {
        rect(Color(0xFFF3F0E8), 540f, 760f, 660f, 860f)
        rect(Color(0xFF2E2A25), 540f, 760f, 560f, 860f)
        circle(Color(0xFFF3F0E8), 590f, 740f, 30f)
    }
    for (i in 0 until 4) {
        val cx = 150f + i * 100f
        if (!variant || i < 3) rect(Color(0xFF6FAF5E), cx, 800f, cx + 30f, 950f)
    }
    if (!variant) {
        circle(Color(0xFF2E2A25), 720f, 780f, 36f)
        circle(Color(0xFF2E2A25), 840f, 780f, 36f)
        rect(Color(0xFFE85D5D), 700f, 700f, 850f, 750f)
        rect(Color(0xFF2E2A25), 840f, 720f, 900f, 750f)
    }
}

val SCENE_FARM = SpotSceneDef(
    name = "农田", diffs = FARM_DIFFS, draw = { drawFarm(it) }, bgRes = R.drawable.spot_farm,
)

// ============ 困难（7 处） ============

private val SNOW_DIFFS = listOf(
    SpotDiff(Rect(180f, 640f, 480f, 900f), "雪人"),
    SpotDiff(Rect(430f, 290f, 700f, 380f), "屋顶积雪"),
    SpotDiff(Rect(720f, 680f, 900f, 880f), "雪橇"),
    SpotDiff(Rect(60f, 80f, 260f, 220f), "星星"),
    SpotDiff(Rect(600f, 120f, 700f, 280f), "烟囱冒烟"),
    SpotDiff(Rect(720f, 430f, 950f, 660f), "树上的雪"),
    SpotDiff(Rect(300f, 80f, 420f, 200f), "月亮颜色"),
)

/** 雪景：只画差异小元素（月亮色 / 星星 / 屋顶雪 / 烟囱烟 / 树雪 / 雪橇 / 雪人） */
private fun DrawScope.drawSnow(variant: Boolean) {
    circle(if (variant) Color(0xFFE8E0C8) else Color(0xFFF6C453), 360f, 140f, 55f) // 月亮
    for (i in 0 until 3) {
        if (!variant || i < 2) circle(Color.White, 110f + i * 60f, 140f + (i % 2) * 40f, 10f) // 星星
    }
    if (!variant) rect(Color.White, 430f, 300f, 700f, 350f) // 屋顶积雪
    if (variant) rect(Color(0xFF2E2A25), 650f, 240f, 690f, 380f) else {
        rect(Color(0xFF2E2A25), 650f, 240f, 690f, 380f)
        rect(Color(0xFFB9B9B9), 640f, 180f, 700f, 260f) // 烟
        circle(Color(0xFFB9B9B9), 670f, 160f, 24f)
    }
    if (!variant) circle(Color.White, 840f, 400f, 40f) // 树上的雪
    if (!variant) {
        rect(Color(0xFFC4623C), 750f, 800f, 880f, 830f)
        rect(Color(0xFF8A5A2B), 730f, 830f, 890f, 845f)
    }
    if (!variant) {
        circle(Color.White, 330f, 740f, 80f)
        circle(Color.White, 330f, 640f, 55f)
        rect(Color(0xFF2E2A25), 320f, 670f, 340f, 680f)
        rect(Color(0xFF2E2A25), 320f, 700f, 340f, 710f)
    }
}

val SCENE_SNOW = SpotSceneDef(
    name = "雪景", diffs = SNOW_DIFFS, draw = { drawSnow(it) }, bgRes = R.drawable.spot_snow,
)

private val STREET_DIFFS = listOf(
    SpotDiff(Rect(80f, 560f, 400f, 860f), "汽车颜色"),
    SpotDiff(Rect(500f, 260f, 700f, 600f), "路灯"),
    SpotDiff(Rect(760f, 500f, 930f, 700f), "花箱"),
    SpotDiff(Rect(140f, 200f, 340f, 460f), "窗户亮灯"),
    SpotDiff(Rect(400f, 700f, 640f, 900f), "自行车"),
    SpotDiff(Rect(700f, 760f, 900f, 920f), "小猫"),
    SpotDiff(Rect(100f, 100f, 300f, 200f), "云朵"),
)

/** 街角：只画差异小元素（云 / 窗灯 / 路灯 / 花箱 / 车色 / 自行车 / 小猫） */
private fun DrawScope.drawStreet(variant: Boolean) {
    cloud(200f, 150f, 0.8f, if (variant) Color(0xFFE8E4DA) else Color.White)
    rect(if (variant) Color(0xFF6E4A2F) else Color(0xFFF6C453), 140f, 240f, 180f, 300f) // 窗灯
    rect(if (variant) Color(0xFF6E4A2F) else Color(0xFFF6C453), 480f, 220f, 520f, 280f)
    circle(if (variant) Color(0xFFB9B9B9) else Color(0xFFF6C453), 600f, 290f, 26f) // 路灯
    if (!variant) circle(Color(0x44F6C453), 600f, 290f, 55f)
    if (!variant) {
        rect(Color(0xFF8A5A2B), 780f, 560f, 920f, 620f)
        flower(800f, 540f, Color(0xFFE85D5D))
        flower(850f, 540f, Color(0xFFF2A0C0))
        flower(890f, 540f, Color(0xFFE8A33D))
    }
    val car = if (variant) Color(0xFF5E8C5E) else Color(0xFF4A6FA5)
    rect(car, 120f, 640f, 380f, 720f)
    rect(car, 180f, 600f, 320f, 660f)
    if (!variant) {
        circle(Color(0xFF2E2A25), 500f, 780f, 26f)
        circle(Color(0xFF2E2A25), 560f, 780f, 26f)
        line(Color(0xFF2E2A25), 500f, 780f, 560f, 780f, 5f)
    }
    if (!variant) {
        circle(Color(0xFFE8A33D), 800f, 840f, 26f)
        circle(Color(0xFFE8A33D), 830f, 860f, 22f)
    }
}

val SCENE_STREET = SpotSceneDef(
    name = "街角", diffs = STREET_DIFFS, draw = { drawStreet(it) }, bgRes = R.drawable.spot_street,
)

private val NIGHT_DIFFS = listOf(
    SpotDiff(Rect(300f, 60f, 420f, 180f), "月亮颜色"),
    SpotDiff(Rect(60f, 80f, 260f, 200f), "星星"),
    SpotDiff(Rect(150f, 200f, 340f, 460f), "窗户亮灯"),
    SpotDiff(Rect(500f, 220f, 700f, 560f), "路灯"),
    SpotDiff(Rect(100f, 620f, 380f, 760f), "汽车灯光"),
    SpotDiff(Rect(700f, 700f, 900f, 880f), "猫咪"),
    SpotDiff(Rect(760f, 100f, 950f, 220f), "云朵"),
)

/** 夜景：只画差异小元素（月色 / 星星 / 窗灯 / 路灯 / 车灯 / 云 / 猫） */
private fun DrawScope.drawNight(variant: Boolean) {
    circle(if (variant) Color(0xFFE8E0C8) else Color(0xFFF6C453), 360f, 120f, 55f) // 月亮
    for (i in 0 until 5) {
        if (!variant || i < 3) circle(Color.White, 90f + i * 70f, 100f + (i % 3) * 40f, 9f) // 星星
    }
    cloud(840f, 150f, 0.8f, if (variant) Color(0xFF4A5A7A) else Color(0xFF6A7A9A))
    if (variant) {
        rect(Color(0xFF6E4A2F), 160f, 240f, 200f, 300f)
        rect(Color(0xFF6E4A2F), 240f, 240f, 280f, 300f)
    } else {
        rect(Color(0xFFF6C453), 160f, 240f, 200f, 300f)
        rect(Color(0xFFF6C453), 240f, 240f, 280f, 300f)
        rect(Color(0xFFF6C453), 470f, 220f, 510f, 280f)
    }
    circle(if (variant) Color(0xFFB9B9B9) else Color(0xFFF6C453), 600f, 230f, 24f) // 路灯
    if (!variant) circle(Color(0x44F6C453), 600f, 230f, 55f)
    if (!variant) {
        circle(Color(0xFFF6C453), 390f, 670f, 10f) // 车灯
        circle(Color(0xFFF6C453), 390f, 690f, 10f)
    }
    if (!variant) {
        circle(Color(0xFFE8A33D), 800f, 800f, 24f)
        circle(Color(0xFFE8A33D), 828f, 818f, 20f)
    }
}

val SCENE_NIGHT = SpotSceneDef(
    name = "夜景", diffs = NIGHT_DIFFS, draw = { drawNight(it) }, bgRes = R.drawable.spot_night,
)
