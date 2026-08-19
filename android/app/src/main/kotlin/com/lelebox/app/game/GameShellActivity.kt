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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.lelebox.app.ui.ElderButton
import com.lelebox.app.ui.ElderTheme
import com.lelebox.app.ui.ElderTopBar
import com.lelebox.app.ui.FontScale

/**
 * 游戏壳（L1/L2 共用入口）：
 * - L2：离线 WebView 加载 assets 内 H5，断网加固 + 老年 CSS 注入 + 存档 JS 桥；
 * - L1：M1 前的原生占位页。
 */
class GameShellActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences("game_saves", MODE_PRIVATE) }
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameId = intent.getStringExtra(EXTRA_GAME_ID)
        if (gameId == null) {
            finish()
            return
        }
        val game = Games.byId(gameId)

        setContent {
            var showExitConfirm by remember { mutableStateOf(false) }

            // 物理返回键：一律走二次确认
            BackHandler { showExitConfirm = true }

            ElderTheme(fontScale = FontScale.STANDARD, highContrast = false) {
                Column(Modifier.fillMaxSize()) {
                    ElderTopBar(
                        title = game.title,
                        onBack = { showExitConfirm = true },
                    )
                    when (game.kind) {
                        GameKind.WEB -> AndroidView(
                            factory = { ctx -> createWebView(ctx, game) },
                            modifier = Modifier.fillMaxSize(),
                        )
                        GameKind.NATIVE -> NativePlaceholder(game)
                    }
                }
            }

            if (showExitConfirm) {
                AlertDialog(
                    onDismissRequest = { showExitConfirm = false },
                    title = { Text("要退出吗？", style = MaterialTheme.typography.titleLarge) },
                    text = { Text("进度会自动保存，下次接着玩。", style = MaterialTheme.typography.bodyLarge) },
                    confirmButton = {
                        ElderButton(
                            text = "退出",
                            onClick = {
                                showExitConfirm = false
                                finish()
                            },
                        )
                    },
                    dismissButton = {
                        ElderButton(
                            text = "继续玩",
                            onClick = { showExitConfirm = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
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

    private fun createWebView(context: Context, game: GameEntry): WebView =
        WebView(context).apply {
            webView = this
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // 离线加固：只允许 android_asset，禁止一切网络与文件外访
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.blockNetworkLoads = true
            settings.setSupportZoom(false)
            // 老年基础放大档（与注入 CSS 叠加）
            settings.textZoom = 130
            addJavascriptInterface(ElderBridge(prefs), "ElderBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(ELDER_CSS_JS, null)
                }
            }
            loadUrl("file:///android_asset/${game.assetPath}")
        }

    /** L1 原生游戏占位（M1 替换为真实实现） */
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
            Text("M1 版本上线，敬请期待", style = MaterialTheme.typography.bodyLarge)
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

        /** 老年样式注入（docs/03 §1-§3）：放大字/按钮，隐藏分享等无关元素 */
        private const val ELDER_CSS_JS = """
            (function(){
              try{
                var css = [
                  'html,body{font-size:20px !important;}',
                  'button{min-width:56px !important;min-height:56px !important;font-size:1.25rem !important;}',
                  'input[type=button]{min-width:56px !important;min-height:56px !important;font-size:1.25rem !important;}',
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
