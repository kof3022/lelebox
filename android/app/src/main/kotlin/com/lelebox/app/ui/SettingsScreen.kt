package com.lelebox.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** 设置页：字号三档 / 高对比度 / 清进度 / 关于 */
@Composable
fun SettingsScreen(
    fontScale: FontScale,
    onFontScale: (FontScale) -> Unit,
    highContrast: Boolean,
    onHighContrast: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("文字大小", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "高对比度（黑底白字）",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = highContrast, onCheckedChange = onHighContrast)
        }

        ElderButton(
            text = "清空所有游戏进度",
            onClick = { showClearDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )

        Spacer(Modifier.weight(1f))

        Text(
            "乐龄游戏盒 v0.1.0-M0\n完全离线 · 无广告 · 永久免费 · 不收集任何数据",
            style = MaterialTheme.typography.bodyMedium,
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
        )
    }
}

private fun clearGameProgress(context: Context) {
    context.getSharedPreferences("game_saves", Context.MODE_PRIVATE).edit().clear().apply()
}
