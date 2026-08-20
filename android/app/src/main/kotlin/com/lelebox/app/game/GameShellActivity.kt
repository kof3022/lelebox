package com.lelebox.app.game

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lelebox.app.game.memory.MemoryGameScreen
import com.lelebox.app.game.g2048.Game2048Screen
import com.lelebox.app.game.doudizhu.DoudizhuScreen
import com.lelebox.app.game.gomoku.GomokuScreen
import com.lelebox.app.game.link.LinkGameScreen
import com.lelebox.app.game.spot.SpotGameScreen
import com.lelebox.app.game.sudoku.SudokuScreen
import com.lelebox.app.ui.ElderButton
import com.lelebox.app.ui.ElderTheme
import com.lelebox.app.ui.ElderTopBar
import com.lelebox.app.ui.FontScale
import com.lelebox.app.ui.parseFontScale

/**
 * 游戏壳（L1/L2 共用入口）：
 * - L2：离线 WebView 加载 assets 内 H5，断网加固 + 老年 CSS 注入（按字号档位）+ 存档 JS 桥；
 * - L1：原生游戏分发（记忆翻牌 / 2048 / 数独）；
 * - 设置联动：字号三档与高对比度来自「乐龄游戏盒设置」；帮助首次自动显示，含音量调节。
 */
class GameShellActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences("game_saves", MODE_PRIVATE) }
    private val settingsPrefs by lazy { getSharedPreferences("lelebox_settings", MODE_PRIVATE) }
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameId = intent.getStringExtra(EXTRA_GAME_ID)
        if (gameId == null) {
            finish()
            return
        }
        val game = Games.byId(gameId)
        // 方向由各游戏内管理：斗地主难度页竖屏、对局横屏沉浸；其余默认竖屏
        val fontScale = parseFontScale(settingsPrefs.getString("font_scale", null))

        setContent {
            var showHelp by remember { mutableStateOf(false) }
            var soundOn by remember {
                mutableStateOf(settingsPrefs.getBoolean("sound_enabled", true))
            }

            // 首次进入自动显示帮助（大字图文）
            LaunchedEffect(Unit) {
                if (!settingsPrefs.getBoolean("help_shown_${game.id}", false)) {
                    showHelp = true
                    settingsPrefs.edit().putBoolean("help_shown_${game.id}", true).apply()
                }
            }

            // 物理返回键：直接退出（进度自动保存）
            BackHandler { finish() }

            ElderTheme(fontScale = fontScale) {
                Column(Modifier.fillMaxSize()) {
                    // 斗地主沉浸全屏：隐藏顶栏，界面内自带紧凑返回/帮助
                    if (game.id != "doudizhu") {
                        ElderTopBar(
                            title = game.title,
                            onBack = { finish() },
                            onRight = { showHelp = true },
                            rightText = "帮助",
                        )
                    }
                    when (game.kind) {
                        GameKind.WEB -> AndroidView(
                            factory = { ctx -> createWebView(ctx, game, fontScale) },
                            modifier = Modifier.fillMaxSize(),
                        )
                        GameKind.NATIVE -> when (game.id) {
                            "doudizhu" -> DoudizhuScreen(
                                onBack = { finish() },
                                onHelp = { showHelp = true },
                                modifier = Modifier.fillMaxSize(),
                            )
                            "spot" -> SpotGameScreen(
                                modifier = Modifier.fillMaxSize(),
                            )
                            "link" -> LinkGameScreen(
                                modifier = Modifier.fillMaxSize(),
                            )
                            "gomoku" -> GomokuScreen(
                                prefs = prefs,
                                modifier = Modifier.fillMaxSize(),
                            )
                            "memory" -> MemoryGameScreen(
                                prefs = prefs,
                                modifier = Modifier.fillMaxSize(),
                            )
                            "2048" -> Game2048Screen(
                                prefs = prefs,
                                modifier = Modifier.fillMaxSize(),
                            )
                            "sudoku" -> SudokuScreen(
                                prefs = prefs,
                                modifier = Modifier.fillMaxSize(),
                            )
                            else -> NativePlaceholder(game)
                        }
                    }
                }
            }

            if (showHelp) {
                AlertDialog(
                    onDismissRequest = { showHelp = false },
                    title = { Text("怎么玩「${game.title}」", style = MaterialTheme.typography.titleLarge) },
                    text = {
                        Column {
                            Text(game.help, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(18.dp))
                            Text(
                                "音效",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            ElderButton(
                                text = if (soundOn) "音效：开" else "音效：关",
                                onClick = {
                                    soundOn = !soundOn
                                    settingsPrefs.edit().putBoolean("sound_enabled", soundOn).apply()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (soundOn) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "游戏提示音开关；音量请用手机侧边音量键调节",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    confirmButton = {
                        ElderButton("知道了", onClick = { showHelp = false })
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    private fun createWebView(context: Context, game: GameEntry, fontScale: FontScale): WebView {
        val textZoom = when (fontScale) {
            FontScale.SMALL -> 115
            FontScale.STANDARD -> 130
            FontScale.LARGE -> 150
        }
        val cssFactor = when (fontScale) {
            FontScale.SMALL -> 0.9f
            FontScale.STANDARD -> 1.0f
            FontScale.LARGE -> 1.25f
        }
        return WebView(context).apply {
            webView = this
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // 离线加固：只允许 android_asset，禁止一切网络与文件外访
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.blockNetworkLoads = true
            settings.setSupportZoom(false)
            settings.textZoom = textZoom
            addJavascriptInterface(ElderBridge(prefs), "ElderBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(elderCssJs(cssFactor), null)
                }
            }
            loadUrl("file:///android_asset/${game.assetPath}")
        }
    }

    /** L1 原生游戏占位（未实现列表项） */
    @Composable
    private fun NativePlaceholder(game: GameEntry) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(game.emoji, fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "「${game.title}」正在开发中",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text("敬请期待", style = MaterialTheme.typography.bodyLarge)
        }
    }

    /** H5 存档桥：写回本机 SharedPreferences（零权限） */
    private class ElderBridge(private val prefs: SharedPreferences) {
        @JavascriptInterface
        fun saveString(key: String, value: String) {
            prefs.edit().putString("h5_$key", value).apply()
        }

        @JavascriptInterface
        fun loadString(key: String): String = prefs.getString("h5_$key", "") ?: ""
    }

    companion object {
        private const val EXTRA_GAME_ID = "game_id"

        fun start(context: Context, game: GameEntry) {
            context.startActivity(
                Intent(context, GameShellActivity::class.java).putExtra(EXTRA_GAME_ID, game.id),
            )
        }

        /** 老年样式注入（docs/03 §1-§3）：按字号档位放大字/按钮，隐藏分享等无关元素 */
        private fun elderCssJs(factor: Float): String {
            val base = (20 * factor).toInt()
            val btn = (24 * factor).toInt()
            return """
                (function(){
                  try{
                    var css = [
                      'html,body{font-size:${base}px !important;}',
                      'button{min-width:56px !important;min-height:56px !important;font-size:${btn}px !important;}',
                      'input[type=button]{min-width:56px !important;min-height:56px !important;font-size:${btn}px !important;}',
                      '.share,.sharing,.share-tip{display:none !important;}'
                    ].join('');
                    var s=document.createElement('style');
                    s.type='text/css';
                    s.appendChild(document.createTextNode(css));
                    document.head.appendChild(s);
                  }catch(e){}
                })();
            """
        }
    }
}
