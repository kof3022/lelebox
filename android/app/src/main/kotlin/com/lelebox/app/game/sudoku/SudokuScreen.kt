package com.lelebox.app.game.sudoku

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelebox.app.R
import com.lelebox.app.audio.Sfx
import com.lelebox.app.ui.ElderButton
import com.lelebox.app.ui.ElderGreen
import com.lelebox.app.ui.ErrorSoft
import com.lelebox.app.ui.GameSudoku
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

private const val SAVE_KEY = "native_sudoku_state"

/** 数独入口（L1 原生）：按钮式输入、候选数、提示、错误高亮、无计时、自动存档 */
@Composable
fun SudokuScreen(
    prefs: SharedPreferences,
    modifier: Modifier = Modifier,
) {
    var level by remember { mutableStateOf<SudokuLevel?>(null) }
    // 物理返回键：对局页 → 难度页 →（难度页交给壳退出）
    if (level != null) BackHandler { level = null }
    when (val lv = level) {
        null -> SudokuLevelSelect(onStart = { level = it }, modifier = modifier)
        else -> SudokuBoard(lv, prefs, onBackToLevels = { level = null }, modifier = modifier)
    }
}

@Composable
private fun SudokuLevelSelect(
    onStart: (SudokuLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_game_sudoku),
            contentDescription = "数独",
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("数独", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "每行、每列、每个九宫格里，1 到 9 各出现一次。慢慢想，不着急。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        SudokuLevel.entries.forEach { lv ->
            ElderButton(text = lv.label, onClick = { onStart(lv) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class GameData(val puzzle: IntArray, val solved: IntArray, val current: IntArray)

@Composable
private fun SudokuBoard(
    level: SudokuLevel,
    prefs: SharedPreferences,
    onBackToLevels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var round by remember { mutableIntStateOf(0) }
    val init = remember(level, round) {
        val saved = loadState(prefs, level)
        if (saved != null) {
            saved
        } else {
            val (p, s) = Sudoku.generatePuzzle(level)
            GameData(p, s, p.copyOf())
        }
    }
    val puzzle = init.puzzle
    val solved = init.solved
    // 关键：current 必须随 round 重置（否则「重新开始」后旧填入值残留，与新手盘错位）
    var current by remember(round) { mutableStateOf(init.current) }
    val givens = remember(puzzle) { puzzle.indices.filter { puzzle[it] != 0 }.toSet() }

    var selected by remember { mutableStateOf<Int?>(null) }
    var candidateMode by remember { mutableStateOf(false) }
    var cands by remember { mutableStateOf<Map<Int, Set<Int>>>(emptyMap()) }
    var hintCount by remember { mutableIntStateOf(0) }
    // 「填满但有错」提示只弹一次，点「继续修改」后收起
    var errorNotice by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val errors = current.indices.filter { current[it] != 0 && current[it] != solved[it] }.toSet()
    val won = current.all { it != 0 } && errors.isEmpty()
    val fullWithErrors = current.all { it != 0 } && !won

    // 完成音效
    LaunchedEffect(won) {
        if (won) Sfx.success(context)
    }

    fun save() {
        val o = JSONObject()
        o.put("level", level.name)
        val p = JSONArray()
        val c = JSONArray()
        puzzle.forEach { p.put(it) }
        current.forEach { c.put(it) }
        o.put("puzzle", p)
        o.put("current", c)
        prefs.edit().putString(SAVE_KEY, o.toString()).apply()
    }

    fun newGame() {
        prefs.edit().remove(SAVE_KEY).apply()
        selected = null
        cands = emptyMap()
        hintCount = 0
        errorNotice = false
        round++
    }

    fun onDigit(d: Int) {
        val pos = selected ?: return
        if (pos in givens) return
        if (candidateMode) {
            // 做记号模式：先清空该格已填数字，让笔记立刻可见
            if (current[pos] != 0) {
                current = current.copyOf().apply { this[pos] = 0 }
            }
            val cur = cands[pos].orEmpty()
            val next = if (d in cur) cur - d else cur + d
            cands = if (next.isEmpty()) cands - pos else cands + (pos to next)
            Sfx.click(context)
        } else {
            current = current.copyOf().apply { this[pos] = d }
            cands = cands - pos
            Sfx.click(context)
            save()
        }
    }

    fun onClear() {
        val pos = selected ?: return
        if (pos in givens) return
        if (candidateMode) {
            cands = cands - pos
        } else {
            current = current.copyOf().apply { this[pos] = 0 }
            save()
        }
    }

    fun onHint() {
        // 优先提示「当前选中的格子」的正确数字
        val sel = selected
        val wrong = if (sel != null && current[sel] != solved[sel]) {
            listOf(sel)
        } else {
            current.indices.filter { current[it] != solved[it] }
        }
        if (wrong.isEmpty()) return
        val pos = wrong[Random.nextInt(wrong.size)]
        current = current.copyOf().apply { this[pos] = solved[pos] }
        cands = cands - pos
        hintCount++
        Sfx.click(context)
        save()
    }

    // 外层 Box：让结束/提示遮罩能叠在棋盘上方（兄弟节点在壳 Column 里会被堆到屏幕外）
    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 操作区：两行大按钮，字不换行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ElderButton(
                text = "重新开始",
                onClick = ::newGame,
                modifier = Modifier.weight(1f),
                minHeight = 56.dp,
            )
            ElderButton(
                text = if (hintCount > 0) "提示($hintCount)" else "提示",
                onClick = ::onHint,
                modifier = Modifier.weight(1f),
                minHeight = 56.dp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ElderButton(
                text = if (candidateMode) "做记号：开" else "做记号：关",
                onClick = { candidateMode = !candidateMode },
                modifier = Modifier.weight(1f),
                minHeight = 56.dp,
                colors = if (candidateMode) {
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
        Text(
            "「做记号」打开后：先点一个格子，再点数字，小记号会写进格子里当笔记（再点一次可取消）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        // 九宫格（弹性占满剩余空间，避免挤压底部数字键）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
                .border(2.dp, Color(0xFF3B3B3B)),
        ) {
            Column(Modifier.fillMaxSize()) {
                for (r in 0 until 9) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        for (c in 0 until 9) {
                            val pos = r * 9 + c
                            val value = current[pos]
                            SudokuCell(
                                value = value,
                                isGiven = pos in givens,
                                isError = pos in errors,
                                isSelected = selected == pos,
                                candidates = cands[pos].orEmpty(),
                                index = pos,
                                onClick = { selected = pos },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 数字大按钮（1-9 + 清除）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (1..5).forEach { d ->
                NumPadButton("$d", Modifier.weight(1f)) { onDigit(d) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (6..9).forEach { d ->
                NumPadButton("$d", Modifier.weight(1f)) { onDigit(d) }
            }
            ElderButton(
                text = "清除",
                onClick = ::onClear,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            )
        }
    }

    if (won) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎉", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("完成！你真棒！", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            // 星级互动：提示用得越少星越多
            val stars = if (hintCount == 0) 3 else if (hintCount <= 3) 2 else 1
            Text(
                "⭐".repeat(stars) + "☆".repeat(3 - stars),
                fontSize = 40.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when (stars) {
                    3 -> "没有用提示，全部自己完成！"
                    2 -> "真棒！只用了 $hintCount 次提示"
                    else -> "完成啦！用了 $hintCount 次提示，慢慢想也完成了"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFE0B2),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "用心走的每一步都算数",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            ElderButton(text = "再来一局", onClick = ::newGame, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            ElderButton(text = "换难度", onClick = onBackToLevels, modifier = Modifier.fillMaxWidth())
        }
        }
    }

    // 填满但有错：不能直接判定结束，给用户明确的互动（继续修改 / 重来）
    if (fullWithErrors && !errorNotice) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🧐", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("有格子填错了", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(
                "别着急，红色格子就是填错的地方，改过来就能完成啦",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFE0B2),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            ElderButton(text = "继续修改", onClick = { errorNotice = true }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            ElderButton(text = "重新开始", onClick = ::newGame, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            ElderButton(
                text = "换难度",
                onClick = onBackToLevels,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
        }
    }
    }
}

@Composable
private fun NumPadButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElderButton(
        text = text,
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = GameSudoku,
            contentColor = Color.White,
        ),
    )
}

@Composable
private fun SudokuCell(
    value: Int,
    isGiven: Boolean,
    isError: Boolean,
    isSelected: Boolean,
    candidates: Set<Int>,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        isError -> ErrorSoft
        isSelected -> Color(0xFFFFF8E1)
        else -> if (isGiven) Color(0xFFF0EDE6) else Color.White
    }
    val desc = when {
        isGiven -> "第${index + 1}格，已给数字 $value"
        value != 0 -> "第${index + 1}格，填了 $value"
        else -> "第${index + 1}格，空格"
    }
    Box(
        modifier = modifier
            .semantics { contentDescription = desc }
            .background(bg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ElderGreen else Color(0xFFD8CFC2),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                "$value",
                fontSize = 22.sp,
                color = if (isGiven) Color(0xFF2E2A25) else GameSudoku,
                fontWeight = if (isGiven) FontWeight.Bold else FontWeight.Normal,
            )
        } else if (candidates.isNotEmpty()) {
            // 数字串流式排版：避免 3x3 小格裁剪，老人看得清
            Text(
                text = candidates.sorted().joinToString(" "),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = GameSudoku,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
            )
        }
    }
}

/** 让 9x9 网格按宽度自适应取高（方形） */

private fun loadState(prefs: SharedPreferences, level: SudokuLevel): GameData? {
    val raw = prefs.getString(SAVE_KEY, null) ?: return null
    return try {
        val o = JSONObject(raw)
        if (o.getString("level") != level.name) return null
        val p = o.getJSONArray("puzzle")
        val c = o.getJSONArray("current")
        val puzzle = IntArray(81) { p.getInt(it) }
        val current = IntArray(81) { c.getInt(it) }
        val solved = Sudoku.solve(puzzle) ?: return null
        GameData(puzzle, solved, current)
    } catch (e: Exception) {
        null
    }
}
