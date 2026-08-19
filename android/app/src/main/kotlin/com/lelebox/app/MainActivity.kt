package com.lelebox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lelebox.app.game.GameShellActivity
import com.lelebox.app.ui.ElderTheme
import com.lelebox.app.ui.ElderTopBar
import com.lelebox.app.ui.FontScale
import com.lelebox.app.ui.parseFontScale
import com.lelebox.app.ui.HomeScreen
import com.lelebox.app.ui.SettingsScreen

/** 主界面：游戏宫格 + 设置（单一 Activity，不引入导航库） */
class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences("lelebox_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var fontScale by remember {
                mutableStateOf(parseFontScale(prefs.getString("font_scale", null)))
            }
            var highContrast by remember { mutableStateOf(prefs.getBoolean("high_contrast", false)) }
            var screen by remember { mutableStateOf(Screen.HOME) }

            ElderTheme(fontScale = fontScale, highContrast = highContrast) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        when (screen) {
                            Screen.HOME -> ElderTopBar(
                                title = "乐龄游戏盒",
                                onBack = null,
                                onRight = { screen = Screen.SETTINGS },
                                rightText = "设置",
                            )
                            Screen.SETTINGS -> ElderTopBar(
                                title = "设置",
                                onBack = { screen = Screen.HOME },
                                onRight = null,
                            )
                        }
                    },
                ) { padding ->
                    when (screen) {
                        Screen.HOME -> HomeScreen(
                            onOpenGame = { game -> GameShellActivity.start(this, game) },
                            modifier = Modifier.padding(padding),
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            fontScale = fontScale,
                            onFontScale = {
                                fontScale = it
                                prefs.edit().putString("font_scale", it.name).apply()
                            },
                            highContrast = highContrast,
                            onHighContrast = {
                                highContrast = it
                                prefs.edit().putBoolean("high_contrast", it).apply()
                            },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}

private enum class Screen { HOME, SETTINGS }
