param(
    [switch]$Sign,
    [switch]$Upload,
    [string]$SshHost = "",
    [string]$SshPath = "/var/www/ericlostie-launcher/public/installer/",
    [string]$CertThumbprint = "",
    [string]$Channel = "win",
    [switch]$AllowStableOnPre
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$TimestampUrl   = "http://time.certum.pl"
$SigntoolExe    = "C:\Program Files (x86)\Windows Kits\10\bin\10.0.26100.0\x64\signtool.exe"

$DesktopRoot = Split-Path $PSScriptRoot -Parent
$ProjectFile = Join-Path $DesktopRoot "LostieLauncher\LostieLauncher.csproj"
$PublishDir  = Join-Path $DesktopRoot "publish"
$ReleasesDir = Join-Path $DesktopRoot "releases"
$IconFile    = Join-Path $DesktopRoot "LostieLauncher\Assets\app.ico"

$Version = "$(dotnet msbuild $ProjectFile -getProperty:Version)".Trim()

if ($LASTEXITCODE -ne 0 -or $Version -notmatch '^\d+\.\d+\.\d+(-pre\.\d+)?$') {
    Write-Error "Version '$Version' from $ProjectFile must be X.Y.Z or X.Y.Z-pre.N."
    exit 1
}

$IsPreRelease = $Version.Contains("-")
if ($Channel -eq "win" -and $IsPreRelease) {
    Write-Error "Version $Version is a pre-release. Pack it with -Channel pre, or clear <VersionSuffix>."
    exit 1
}
if ($Channel -eq "pre" -and -not $IsPreRelease -and -not $AllowStableOnPre) {
    Write-Error "Version $Version is stable. Set <VersionSuffix>pre.N</VersionSuffix>, or pass -AllowStableOnPre to promote it."
    exit 1
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  LostieLauncher — Velopack Release Build" -ForegroundColor Cyan
Write-Host "  Version : $Version" -ForegroundColor Cyan
Write-Host "  Channel : $Channel" -ForegroundColor Cyan
if ($Sign) {
    Write-Host "  Signing : Enabled (Certum smart card)" -ForegroundColor Cyan
    Write-Host "  Thumbprint: $CertThumbprint" -ForegroundColor Cyan
}
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

if ($Sign) {
    if (-not $CertThumbprint) {
        Write-Error "-CertThumbprint is required when using -Sign."
        exit 1
    }
    if (-not (Test-Path $SigntoolExe)) {
        Write-Error "signtool.exe not found at: $SigntoolExe`nInstall Windows SDK or update the path in this script."
        exit 1
    }
}

Write-Host "[1/3] Cleaning previous output..." -ForegroundColor Yellow
Remove-Item $PublishDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "[2/3] Publishing (win-x64, self-contained)..." -ForegroundColor Yellow
dotnet publish $ProjectFile `
    --configuration Release `
    --runtime win-x64 `
    --self-contained true `
    --output $PublishDir `
    -p:PublishSingleFile=false `
    -p:PublishTrimmed=false

if ($LASTEXITCODE -ne 0) { Write-Error "dotnet publish failed."; exit 1 }

Write-Host "[3/3] Packaging with vpk..." -ForegroundColor Yellow

$vpkArgs = @(
    "pack"
    "--packId",      "LostieLauncher"
    "--packVersion", $Version
    "--packDir",     $PublishDir
    "--mainExe",     "LostieLauncher.exe"
    "--packTitle",   "Lostie Launcher"
    "--icon",        $IconFile
    "--outputDir",   $ReleasesDir
    "--channel",     $Channel
)

if ($Sign) {
    Write-Host "Registrando clave de tarjeta inteligente..." -ForegroundColor Yellow
    certutil -repairstore -user MY $CertThumbprint "C:\Users\Jagoba\keyinfo.inf"
    if ($LASTEXITCODE -ne 0) { Write-Error "certutil -repairstore falló."; exit 1 }

    $vpkArgs += @(
        "--signTemplate", "`"$SigntoolExe`" sign /sha1 $CertThumbprint /tr $TimestampUrl /td sha256 /fd sha256 {{file}}"
    )
}

& vpk @vpkArgs

if ($LASTEXITCODE -ne 0) { Write-Error "vpk pack failed."; exit 1 }

Write-Host ""
Write-Host "✅ Package ready in: $ReleasesDir" -ForegroundColor Green
Write-Host ""
Get-ChildItem $ReleasesDir | Format-Table Name, Length -AutoSize

if ($Upload) {
    if (-not $SshHost) { Write-Error "-SshHost is required when using -Upload."; exit 1 }

    Write-Host "Uploading to ${SshHost}:${SshPath} ..." -ForegroundColor Yellow
    scp -r "$ReleasesDir\*" "${SshHost}:${SshPath}"

    if ($LASTEXITCODE -ne 0) { Write-Error "Upload via SCP failed."; exit 1 }

    Write-Host "✅ Upload complete." -ForegroundColor Green
}