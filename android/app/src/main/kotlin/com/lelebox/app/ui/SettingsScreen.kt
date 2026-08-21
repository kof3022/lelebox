package com.lelebox.app.ui

import android.content.Context
import android.webkit.WebStorage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** 设置页：统一卡片式分组（Editorial Luxury 暖调） */
@Composable
fun SettingsScreen(
    fontScale: FontScale,
    onFontScale: (FontScale) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingCard(title = "文字大小") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FontScale.entries.forEach { scale ->
                    val selected = scale == fontScale
                    ElderButton(
                        text = scale.label,
                        onClick = { onFontScale(scale) },
                        modifier = Modifier.weight(1f),
                        colors = if (selected) {
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
                }
            }
        }

        Spacer(Modifier.weight(1f))

        SettingCard(title = "数据") {
            ElderButton(
                text = "清空所有游戏进度",
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "乐龄游戏盒 v0.5.0-m1\n完全离线 · 无广告 · 永久免费 · 不收集任何数据",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空进度", style = MaterialTheme.typography.titleLarge) },
            text = { Text("将删除所有游戏的存档，确定吗？", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                ElderButton(
                    text = "确定清空",
                    onClick = {
                        clearGameProgress(context)
                        showClearDialog = false
                    },
                )
            },
            dismissButton = {
                ElderButton(
                    text = "再想想",
                    onClick = { showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
        )
    }
}

/** 统一设置卡片：标题 + 内容（两个分组同一样式，层级清晰） */
@Composable
private fun SettingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun clearGameProgress(context: Context) {
    // 原生与 H5（经 JS 桥）存档都在 game_saves
    context.getSharedPreferences("game_saves", Context.MODE_PRIVATE).edit().clear().apply()
    // H5 游戏自带的 localStorage / WebStorage 一并清空
    try {
        WebStorage.getInstance().deleteAllData()
    } catch (_: Exception) {
        // 忽略清理失败（个别机型 WebStorage 不可用）
    }
}
