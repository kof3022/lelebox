# 生成斗地主素材：牌桌背景 + 孔子/庄子头像（即梦 PNG → drawable-xxhdpi/xxxhdpi）
# 用法：将 table_felt.png / confucius.png / zhuangzi.png 放入「乐龄素材」目录后运行
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$srcRoot = 'C:\Users\prcki\Documents\DeepseekHarness\flycat\乐龄素材'
$outRoot = 'C:\Users\prcki\Documents\DeepseekHarness\flycat\乐龄游戏盒\android\app\src\main\res'

function Convert-Asset([string]$name, [int]$xxWidth, [int]$xxHeight, [int]$xxxWidth, [int]$xxxHeight) {
    $src = Join-Path $srcRoot "$name.png"
    if (!(Test-Path $src)) { Write-Host "SKIP: $src not found"; return }
    $img = [System.Drawing.Image]::FromFile($src)
    foreach ($entry in @(
        @{ dir = 'drawable-xxhdpi'; w = $xxWidth; h = $xxHeight },
        @{ dir = 'drawable-xxxhdpi'; w = $xxxWidth; h = $xxxHeight }
    )) {
        $outDir = Join-Path $outRoot $entry.dir
        if (!(Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
        $bmp = New-Object System.Drawing.Bitmap($entry.w, $entry.h)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.DrawImage($img, 0, 0, $entry.w, $entry.h)
        $out = Join-Path $outDir "$name.png"
        $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Host "generated $($entry.dir)/$name.png ($($entry.w)x$($entry.h))"
        $g.Dispose(); $bmp.Dispose()
    }
    $img.Dispose()
}

# 牌桌背景 2:1 横屏；xxhdpi 1600x800（2400 屏按 3x 取 800 宽即可），xxxhdpi 用原图 2048x1024 高保真
Convert-Asset 'table_felt' 1600 800 2048 1024
# 头像 1:1；对局内 36dp 圆形显示，512px(3x) / 768px(4x) 足够清晰
Convert-Asset 'confucius' 512 512 768 768
Convert-Asset 'zhuangzi' 512 512 768 768
Write-Host 'doudizhu assets done'
