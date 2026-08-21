# 生成找不同 9 张场景底图（即梦 PNG → drawable-xxhdpi/xxxhdpi）
# 用法：将 spot_garden.png ... spot_night.png 放入「乐龄素材」目录后运行
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$srcRoot = 'C:\Users\prcki\Documents\DeepseekHarness\flycat\乐龄素材'
$outRoot = 'C:\Users\prcki\Documents\DeepseekHarness\flycat\乐龄游戏盒\android\app\src\main\res'

$names = @('garden','living','orchard','park','seaside','farm','snow','street','night')

foreach ($n in $names) {
    $src = Join-Path $srcRoot "spot_$n.png"
    if (!(Test-Path $src)) { Write-Host "SKIP: $src not found"; continue }
    $img = [System.Drawing.Image]::FromFile($src)
    # 竖构图 3:4 缩放到 xxhdpi(高 1440x1920 保持比例) / xxxhdpi(高 2048)
    $aspect = $img.Width / [float]$img.Height
    foreach ($entry in @(
        @{ dir = 'drawable-xxhdpi'; h = 1440 },
        @{ dir = 'drawable-xxxhdpi'; h = 1920 }
    )) {
        $h = $entry.h; $w = [int]($h * $aspect)
        $outDir = Join-Path $outRoot $entry.dir
        if (!(Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
        $bmp = New-Object System.Drawing.Bitmap($w, $h)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.DrawImage($img, 0, 0, $w, $h)
        $bmp.Save((Join-Path $outDir "spot_$n.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Host "ok $($entry.dir)/spot_$n.png ($w x $h)"
        $g.Dispose(); $bmp.Dispose()
    }
    $img.Dispose()
}
Write-Host 'spot assets done'
