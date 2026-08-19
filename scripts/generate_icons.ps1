# 生成乐龄游戏盒多密度启动图标（Windows PowerShell 5.1 + System.Drawing）
# 设计：深绿圆角方块 + 白色「乐」字（docs/03 暖色系主色 #2E7D32）
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$sizes = @{ mdpi = 48; hdpi = 72; xhdpi = 96; xxhdpi = 144; xxxhdpi = 192 }
$outRoot = 'C:\Users\prcki\Documents\DeepseekHarness\flycat\乐龄游戏盒\android\app\src\main\res'

function New-Icon([int]$size, [string]$path) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    # 圆角矩形背景 #2E7D32
    $radius = [int]($size * 0.18)
    $inset = [int]($size * 0.02)
    $gp = New-Object System.Drawing.Drawing2D.GraphicsPath
    $w = $size - 2 * $inset
    $h = $size - 2 * $inset
    $gp.AddArc($inset, $inset, 2 * $radius, 2 * $radius, 180, 90)
    $gp.AddArc($inset + $w - 2 * $radius, $inset, 2 * $radius, 2 * $radius, 270, 90)
    $gp.AddArc($inset + $w - 2 * $radius, $inset + $h - 2 * $radius, 2 * $radius, 2 * $radius, 0, 90)
    $gp.AddArc($inset, $inset + $h - 2 * $radius, 2 * $radius, 2 * $radius, 90, 90)
    $gp.CloseFigure()
    $bg = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 46, 125, 50))
    $g.FillPath($bg, $gp)

    # 白色「乐」字居中
    $fontSize = $size * 0.52
    $font = New-Object System.Drawing.Font('Microsoft YaHei', $fontSize, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $rect = New-Object System.Drawing.RectangleF(0, 0, $size, $size)
    $g.DrawString([char]0x4E50, $font, $brush, $rect, $sf)

    $dir = Split-Path $path -Parent
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)

    $sf.Dispose(); $brush.Dispose(); $font.Dispose(); $bg.Dispose(); $gp.Dispose()
    $g.Dispose(); $bmp.Dispose()
}

foreach ($entry in $sizes.GetEnumerator()) {
    $dir = Join-Path $outRoot "mipmap-$($entry.Key)"
    New-Icon $entry.Value (Join-Path $dir 'ic_launcher.png')
    New-Icon $entry.Value (Join-Path $dir 'ic_launcher_round.png')
    Write-Host "generated mipmap-$($entry.Key) ($($entry.Value)px)"
}
Write-Host 'icon generation done'
