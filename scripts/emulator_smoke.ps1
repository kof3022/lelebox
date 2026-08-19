$ErrorActionPreference = "Continue"
$adb = "C:\Users\prcki\Android\Sdk\platform-tools\adb.exe"
$shots = "C:\Users\prcki\Documents\DeepseekHarness\flycat\乐龄游戏盒\screenshots"

function Get-Texts {
    & $adb shell uiautomator dump /sdcard/u.xml 2>&1 | Out-Null
    & $adb pull /sdcard/u.xml "$env:TEMP\u.xml" 2>&1 | Out-Null
    return [System.IO.File]::ReadAllText("$env:TEMP\u.xml", [System.Text.Encoding]::UTF8)
}
function Wait-Home {
    for ($i = 0; $i -lt 12; $i++) {
        $x = Get-Texts
        if ($x -match "欢迎回来") { return $true }
        Start-Sleep -Seconds 3
    }
    return $false
}
function Tap-CenterOfText($xml, $pattern) {
    $m = [regex]::Match($xml, "text=`"$pattern`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"")
    if (-not $m.Success) { return $false }
    $x = ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2
    $y = ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2
    & $adb shell input tap $x $y 2>&1 | Out-Null
    return $true
}

# 1) solitaire：右上一张卡（重试至进入）
& $adb shell am start -n com.lelebox.app/.MainActivity 2>&1 | Out-Null
if (Wait-Home) {
    $entered = $false
    for ($try = 1; $try -le 3 -and -not $entered; $try++) {
        & $adb shell input tap 797 734 2>&1 | Out-Null
        Start-Sleep -Seconds 8
        $u = Get-Texts
        if ($u -match "纸牌接龙" -and $u -match "帮助") { $entered = $true }
    }
    if ($entered) {
        $u = Get-Texts
        if ($u -match "怎么玩") { Tap-CenterOfText $u "知道了" | Out-Null; Start-Sleep -Seconds 4 }
        & $adb shell screencap -p /sdcard/g.png 2>&1 | Out-Null
        & $adb pull /sdcard/g.png "$shots\game_solitaire.png" 2>&1 | Out-Null
        Write-Host "solitaire: $((Get-Item "$shots\game_solitaire.png").Length)"
    } else { Write-Host "solitaire: ENTER FAILED" }
    & $adb shell am force-stop com.lelebox.app 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}

# 2) memory：右下卡片 + 点「简单」进棋盘
& $adb shell am start -n com.lelebox.app/.MainActivity 2>&1 | Out-Null
if (Wait-Home) {
    $entered = $false
    for ($try = 1; $try -le 3 -and -not $entered; $try++) {
        & $adb shell input tap 797 1243 2>&1 | Out-Null
        Start-Sleep -Seconds 8
        $u = Get-Texts
        if ($u -match "记忆翻牌" -and $u -match "帮助") { $entered = $true }
    }
    if ($entered) {
        $u = Get-Texts
        if ($u -match "怎么玩") { Tap-CenterOfText $u "知道了" | Out-Null; Start-Sleep -Seconds 4 }
        $u = Get-Texts
        Tap-CenterOfText $u "简单" | Out-Null
        Start-Sleep -Seconds 5
        & $adb shell screencap -p /sdcard/g.png 2>&1 | Out-Null
        & $adb pull /sdcard/g.png "$shots\game_memory.png" 2>&1 | Out-Null
        Write-Host "memory: $((Get-Item "$shots\game_memory.png").Length)"
    } else { Write-Host "memory: ENTER FAILED" }
    & $adb shell am force-stop com.lelebox.app 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}

# 3) sudoku：左下一张卡 + 点「简单」进棋盘
& $adb shell am start -n com.lelebox.app/.MainActivity 2>&1 | Out-Null
if (Wait-Home) {
    $entered = $false
    for ($try = 1; $try -le 3 -and -not $entered; $try++) {
        & $adb shell input tap 283 1243 2>&1 | Out-Null
        Start-Sleep -Seconds 8
        $u = Get-Texts
        if ($u -match "数独" -and $u -match "帮助") { $entered = $true }
    }
    if ($entered) {
        $u = Get-Texts
        if ($u -match "怎么玩") { Tap-CenterOfText $u "知道了" | Out-Null; Start-Sleep -Seconds 4 }
        $u = Get-Texts
        Tap-CenterOfText $u "简单" | Out-Null
        Start-Sleep -Seconds 5
        & $adb shell screencap -p /sdcard/g.png 2>&1 | Out-Null
        & $adb pull /sdcard/g.png "$shots\game_sudoku.png" 2>&1 | Out-Null
        Write-Host "sudoku: $((Get-Item "$shots\game_sudoku.png").Length)"
    } else { Write-Host "sudoku: ENTER FAILED" }
    & $adb shell am force-stop com.lelebox.app 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}
