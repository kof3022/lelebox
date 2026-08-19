package com.lelebox.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/** 全局字号三档（docs/03 §1） */
enum class FontScale(val label: String, val factor: Float) {
    STANDARD("标准", 1f),
    LARGE("大", 1.25f),
    XLARGE("超大", 1.6f),
}

val LocalFontScale = staticCompositionLocalOf { FontScale.STANDARD }

// 老年友好暖色系（docs/03 §3）
val WarmBackground = Color(0xFFFDF8F0)
val WarmText = Color(0xFF3B3B3B)
val ElderGreen = Color(0xFF2E7D32)
val ElderOrange = Color(0xFFE65100)

private val LightColors = lightColorScheme(
    primary = ElderGreen,
    onPrimary = Color.White,
    secondary = ElderOrange,
    background = WarmBackground,
    onBackground = WarmText,
    surface = Color.White,
    onSurface = WarmText,
    error = Color(0xFFB3261E),
)

private val HighContrastColors = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,
    secondary = Color(0xFFFFD600),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    error = Color(0xFFFFB4AB),
)

@Composable
fun ElderTheme(
    fontScale: FontScale,
    highContrast: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (highContrast) HighContrastColors else LightColors
    val base = Typography()
    val scaled = base.copy(
        displaySmall = base.displaySmall.copy(fontSize = (36 * fontScale.factor).sp),
        headlineMedium = base.headlineMedium.copy(fontSize = (30 * fontScale.factor).sp),
        titleLarge = base.titleLarge.copy(fontSize = (26 * fontScale.factor).sp),
        titleMedium = base.titleMedium.copy(fontSize = (22 * fontScale.factor).sp),
        bodyLarge = base.bodyLarge.copy(fontSize = (20 * fontScale.factor).sp),
        bodyMedium = base.bodyMedium.copy(fontSize = (20 * fontScale.factor).sp),
        labelLarge = base.labelLarge.copy(fontSize = (22 * fontScale.factor).sp),
    )
    CompositionLocalProvider(LocalFontScale provides fontScale) {
        MaterialTheme(colorScheme = colors, typography = scaled, content = content)
    }
}
