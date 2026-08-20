package com.lelebox.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 全局字号三档（docs/03 §1）：小 / 标准 / 大 */
enum class FontScale(val label: String, val factor: Float) {
    SMALL("小", 0.85f),
    STANDARD("标准", 1f),
    LARGE("大", 1.25f),
}

/** 安全解析存档里的档位（旧版本曾用 XLARGE 等，需兜底） */
fun parseFontScale(saved: String?): FontScale =
    FontScale.entries.firstOrNull { it.name == saved } ?: FontScale.STANDARD

val LocalFontScale = staticCompositionLocalOf { FontScale.STANDARD }

// ===== 乐龄游戏盒 · Editorial Luxury 暖调色板（2025 重新设计） =====
// 依据 taste-skill（redesign-existing-projects / high-end-visual-design）：
// 单一暖灰家族、去饱和点缀色、奶油底 + 鼠尾草绿主色 + 暖棕强调。

// 基底与表面
val WarmCream = Color(0xFFFBF7F0)          // 背景：暖奶油
val WarmCreamDeep = Color(0xFFF4EDE1)      // 背景渐变深一档
val CardSurface = Color(0xFFFFFFFF)        // 卡片表面
val CardTint = Color(0xFFF3EDE2)           // 卡片浅色面（图标容器/次要按钮）
val OutlineWarm = Color(0xFFE4DBCB)        // 暖色细线（hairline）

// 文字
val Espresso = Color(0xFF2E2A25)           // 主文字：深咖啡
val WarmGray = Color(0xFF6E675E)           // 次级文字：暖灰
val WarmGrayLight = Color(0xFF9A9185)      // 弱化文字

// 主色与强调（单主色 + 单强调）
val ElderGreen = Color(0xFF3D6B52)         // 主色：鼠尾草绿（去饱和）
val ElderGreenDeep = Color(0xFF2E5440)     // 主色按压/深档
val ElderGreenSoft = Color(0xFFDCE9E0)     // 主色浅面（tonal 容器）
val ElderOrange = Color(0xFFA9713B)        // 强调：暖棕（设置等次要 CTA）
val ElderOrangeSoft = Color(0xFFF0E2D2)

// 游戏身份色（去饱和、与暖底和谐；docs/03 §3）
val Game2048 = Color(0xFF3E8E7E)           // 青绿
val GameSudoku = Color(0xFF4A6FA5)         // 雾蓝
val GameMemory = Color(0xFF7A5C9E)         // 灰紫
val GameGomoku = Color(0xFF6E8B74)         // 鼠尾草灰绿（五子棋）
val GameLink = Color(0xFFC4623C)           // 陶土（连连看）

// 功能色
val SuccessSoft = Color(0xFFDCE9E0)
val ErrorSoft = Color(0xFFF6DDD6)

private val LightColors = lightColorScheme(
    primary = ElderGreen,
    onPrimary = Color.White,
    primaryContainer = ElderGreenSoft,
    onPrimaryContainer = ElderGreenDeep,
    secondary = ElderOrange,
    onSecondary = Color.White,
    secondaryContainer = ElderOrangeSoft,
    onSecondaryContainer = Color(0xFF5C3A18),
    background = WarmCream,
    onBackground = Espresso,
    surface = CardSurface,
    onSurface = Espresso,
    surfaceVariant = CardTint,
    onSurfaceVariant = WarmGray,
    outline = OutlineWarm,
    outlineVariant = OutlineWarm,
    error = Color(0xFFB3261E),
    errorContainer = ErrorSoft,
)

@Composable
fun ElderTheme(
    fontScale: FontScale,
    content: @Composable () -> Unit,
) {
    val colors = LightColors
    val f = fontScale.factor
    val base = Typography()

    fun scale(size: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f, height: Float = 1.35f): TextStyle =
        TextStyle(
            fontSize = (size * f).sp,
            fontWeight = weight,
            letterSpacing = tracking.sp,
            lineHeight = ((size * f) * height).sp,
        )

    val scaled = base.copy(
        displaySmall = scale(36, FontWeight.SemiBold, -0.5f),
        headlineMedium = scale(30, FontWeight.SemiBold, -0.3f),
        headlineSmall = scale(26, FontWeight.SemiBold, -0.2f),
        titleLarge = scale(26, FontWeight.SemiBold, -0.2f),
        titleMedium = scale(22, FontWeight.Medium, 0f),
        titleSmall = scale(20, FontWeight.Medium, 0.2f),
        bodyLarge = scale(20, FontWeight.Normal, 0f, 1.45f),
        bodyMedium = scale(20, FontWeight.Normal, 0f, 1.45f),
        labelLarge = scale(22, FontWeight.SemiBold),
        labelMedium = scale(18, FontWeight.Medium, 0.4f),
    )
    CompositionLocalProvider(LocalFontScale provides fontScale) {
        MaterialTheme(colorScheme = colors, typography = scaled, content = content)
    }
}
