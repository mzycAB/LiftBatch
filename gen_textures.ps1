Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"

$outDir = "c:\Users\user\Desktop\aaa\liftbatch\src\main\resources\assets\liftbatch"
New-Item -ItemType Directory -Force -Path "$outDir\textures\item" | Out-Null

# Pixel maps: 16 rows x 16 chars.
# . = transparent, O = outline, S = steel, L = light steel, A = head accent,
# Y = yellow, T = teal, G = green, R = red
$wrenchBase = @(
    "....OOO..OOO....",
    "...OAAA..AAAO...",
    "...OAAA..AAAO...",
    "...OAAA..AAAO...",
    "....OAAAAAAO....",
    "....OAAAAAAO....",
    ".....OAAAAO.....",
    "......OSSO......",
    "......OSLO......",
    "......OSSO......",
    "......OSLO......",
    "......OSSO......",
    "......OSLO......",
    "......OSSO......",
    ".....OSSSSO.....",
    "................"
)

function With-Handle {
    param([string[]]$Base, [hashtable]$HandleRows)
    $rows = @($Base)
    foreach ($key in $HandleRows.Keys) {
        $rows[[int]$key] = $HandleRows[$key]
    }
    return ,$rows
}

$floorWrench = With-Handle $wrenchBase @{ 9 = "......OLLO......"; 12 = "......OLLO......" }

# speed wrench: yellow chevrons on the handle
$speedWrench = With-Handle $wrenchBase @{
    9  = "......OYYO......"
    10 = "......OSSO......"
    11 = "......OYYO......"
    12 = "......OSSO......"
}

$displayWrench = With-Handle $wrenchBase @{ 9 = "......OAAO......"; 10 = "......OAAO......"; 11 = "......OAAO......" }

$callButtonWrench = With-Handle $wrenchBase @{ 9 = "......OAAO......"; 12 = "......OAAO......" }

# unbind wrenches: identical to their bind counterparts with a red backslash drawn on top
function Add-RedSlash {
    param([string[]]$Rows)
    $result = [System.Collections.ArrayList]@()
    for ($y = 0; $y -lt 16; $y++) {
        $chars = $Rows[$y].ToCharArray()
        if ($y -ge 2 -and $y -le 13) {
            if (($y + 1) -lt 16) { $chars[$y + 1] = 'R' }
            if (($y + 2) -lt 16) { $chars[$y + 2] = 'R' }
        }
        $result.Add(-join $chars) | Out-Null
    }
    return ,$result.ToArray()
}

$displayUnbindWrench = Add-RedSlash $displayWrench

$callButtonUnbindWrench = Add-RedSlash $callButtonWrench

function Make-Texture {
    param([string]$Path, [hashtable]$Palette, [string[]]$Rows)
    $bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 16; $y++) {
        $row = $Rows[$y]
        for ($x = 0; $x -lt 16; $x++) {
            $ch = $row.Substring($x, 1)
            if ($Palette.ContainsKey($ch)) {
                $c = $Palette[$ch]
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c[0], $c[1], $c[2], $c[3]))
            }
        }
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "wrote $Path"
}

$outline = @(255, 26, 27, 32)      # dark outline
$steel   = @(255, 142, 149, 156)   # steel gray
$light   = @(255, 201, 207, 214)   # light steel highlight
$yellow  = @(255, 245, 200, 70)    # speed chevrons
$teal    = @(255, 31, 168, 160)    # display accent
$green   = @(255, 63, 191, 79)     # call button accent
$red     = @(255, 219, 68, 68)     # unbind heads
$blue    = @(255, 61, 111, 224)    # floor wrench accent
$purple  = @(255, 158, 96, 224)    # speed wrench head

$common = @{ '.' = @(0,0,0,0); 'O' = $outline; 'S' = $steel; 'L' = $light; 'Y' = $yellow; 'T' = $teal; 'G' = $green; 'R' = $red }

function Palette-With {
    param([hashtable]$Base, [array]$Accent)
    $p = @{}
    foreach ($k in $Base.Keys) { $p[$k] = $Base[$k] }
    $p['A'] = $Accent
    return $p
}

Make-Texture "$outDir\textures\item\floor_wrench.png"             (Palette-With $common $blue)   $floorWrench
Make-Texture "$outDir\textures\item\speed_wrench.png"             (Palette-With $common $purple) $speedWrench
Make-Texture "$outDir\textures\item\display_wrench.png"           (Palette-With $common $teal)   $displayWrench
Make-Texture "$outDir\textures\item\call_button_wrench.png"       (Palette-With $common $green)  $callButtonWrench
Make-Texture "$outDir\textures\item\display_unbind_wrench.png"    (Palette-With $common $red)    $displayUnbindWrench
Make-Texture "$outDir\textures\item\call_button_unbind_wrench.png" (Palette-With $common $red)   $callButtonUnbindWrench

# NOTE: icon.png is the author's custom image (c:\Users\user\Desktop\aaa\icon.png) — do not regenerate it here.
