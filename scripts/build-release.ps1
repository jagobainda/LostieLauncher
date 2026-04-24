<#
.SYNOPSIS
    Builds and packages LostieLauncher for release using Velopack.

.DESCRIPTION
    1. Reads the version from the .csproj
    2. Publishes a self-contained win-x64 build
    3. Signs the main executable with the Certum smart card certificate
    4. Packages it with `vpk pack`
    5. Optionally uploads the releases/ folder to the server via SCP

.PARAMETER Sign
    If set, signs the .exe with the Certum smart card before packaging.
    Requires the ACS ACR39U reader connected with the Certum card inserted.

.PARAMETER Upload
    If set, uploads the output to the configured remote server.

.PARAMETER SshHost
    SSH host for upload (e.g. user@yourserver.com). Required when -Upload is set.

.PARAMETER SshPath
    Remote path on the server (e.g. /var/www/ericlostie-launcher/public/installer/).
    Required when -Upload is set.

.EXAMPLE
    .\scripts\build-release.ps1
    .\scripts\build-release.ps1 -Sign
    .\scripts\build-release.ps1 -Sign -Upload -SshHost "user@jagoba.dev" -SshPath "/var/www/installer/"
#>

param(
    [switch]$Sign,
    [switch]$Upload,
    [string]$SshHost = "",
    [string]$SshPath = "/var/www/ericlostie-launcher/public/installer/"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Paths ─────────────────────────────────────────────────────────────────────
$RepoRoot    = Split-Path $PSScriptRoot -Parent
$ProjectFile = Join-Path $RepoRoot "LostieLauncher\LostieLauncher.csproj"
$PublishDir  = Join-Path $RepoRoot "publish"
$ReleasesDir = Join-Path $RepoRoot "releases"
$IconFile    = Join-Path $RepoRoot "LostieLauncher\Assets\app.ico"

# ── Read version from .csproj ─────────────────────────────────────────────────
[xml]$csproj = Get-Content $ProjectFile
$Version = $csproj.Project.PropertyGroup.Version | Where-Object { $_ } | Select-Object -First 1
$Version = ($Version -split "\.")[0..2] -join "."

if (-not $Version) {
    Write-Error "Could not read <Version> from $ProjectFile"
    exit 1
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  LostieLauncher — Velopack Release Build" -ForegroundColor Cyan
Write-Host "  Version : $Version" -ForegroundColor Cyan
if ($Sign) {
Write-Host "  Signing : Enabled (Certum smart card)" -ForegroundColor Cyan
}
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ── Clean ─────────────────────────────────────────────────────────────────────
Write-Host "[1/4] Cleaning previous output..." -ForegroundColor Yellow
Remove-Item $PublishDir -Recurse -Force -ErrorAction SilentlyContinue

# ── Publish ───────────────────────────────────────────────────────────────────
Write-Host "[2/4] Publishing (win-x64, self-contained)..." -ForegroundColor Yellow
dotnet publish $ProjectFile `
    --configuration Release `
    --runtime win-x64 `
    --self-contained true `
    --output $PublishDir `
    -p:PublishSingleFile=false `
    -p:PublishTrimmed=false

if ($LASTEXITCODE -ne 0) { Write-Error "dotnet publish failed."; exit 1 }

# ── Sign ──────────────────────────────────────────────────────────────────────
if ($Sign) {
    Write-Host "[3/4] Signing with Certum smart card..." -ForegroundColor Yellow

    # Find signtool.exe
    $signtool = Get-ChildItem "C:\Program Files (x86)\Windows Kits\10\bin" `
        -Recurse -Filter signtool.exe `
        | Where-Object { $_.FullName -match "x64" } `
        | Sort-Object FullName -Descending `
        | Select-Object -First 1 -ExpandProperty FullName

    if (-not $signtool) { Write-Error "signtool.exe not found. Install Windows SDK."; exit 1 }

    $ExeToSign = Join-Path $PublishDir "LostieLauncher.exe"

    & $signtool sign `
        /fd  SHA256 `
        /td  SHA256 `
        /tr  http://timestamp.certum.pl `
        /n   "Open Source Developer Jagoba Inda" `
        /sm `
        $ExeToSign

    if ($LASTEXITCODE -ne 0) { Write-Error "signtool signing failed. Is the smart card inserted?"; exit 1 }

    Write-Host "✅ Signed successfully." -ForegroundColor Green
} else {
    Write-Host "[3/4] Signing skipped (use -Sign to enable)." -ForegroundColor DarkGray
}

# ── Pack with vpk ─────────────────────────────────────────────────────────────
Write-Host "[4/4] Packaging with vpk..." -ForegroundColor Yellow
vpk pack `
    --packId      LostieLauncher `
    --packVersion $Version `
    --packDir     $PublishDir `
    --mainExe     LostieLauncher.exe `
    --packTitle   "Lostie Launcher" `
    --icon        $IconFile `
    --outputDir   $ReleasesDir

if ($LASTEXITCODE -ne 0) { Write-Error "vpk pack failed."; exit 1 }

Write-Host ""
Write-Host "✅ Package ready in: $ReleasesDir" -ForegroundColor Green
Write-Host ""
Get-ChildItem $ReleasesDir | Format-Table Name, Length -AutoSize

# ── Optional upload ───────────────────────────────────────────────────────────
if ($Upload) {
    if (-not $SshHost) { Write-Error "-SshHost is required when using -Upload."; exit 1 }

    Write-Host "Uploading to ${SshHost}:${SshPath} ..." -ForegroundColor Yellow
    scp -r "$ReleasesDir\*" "${SshHost}:${SshPath}"

    if ($LASTEXITCODE -ne 0) { Write-Error "Upload via SCP failed."; exit 1 }

    Write-Host "✅ Upload complete." -ForegroundColor Green
}