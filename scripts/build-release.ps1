<#
.SYNOPSIS
    Builds and packages EricLostieLauncher for release using Velopack.

.DESCRIPTION
    1. Reads the version from the .csproj
    2. Publishes a self-contained win-x64 build
    3. Packages it with `vpk pack`
    4. Optionally uploads the releases/ folder to the server via SCP

.PARAMETER Upload
    If set, uploads the output to the configured remote server.

.PARAMETER SshHost
    SSH host for upload (e.g. user@yourserver.com). Required when -Upload is set.

.PARAMETER SshPath
    Remote path on the server (e.g. /var/www/ericlostie-launcher/public/installer/).
    Required when -Upload is set.

.EXAMPLE
    .\scripts\build-release.ps1
    .\scripts\build-release.ps1 -Upload -SshHost "user@jagoba.dev" -SshPath "/var/www/installer/"
#>

param(
    [switch]$Upload,
    [string]$SshHost = "",
    [string]$SshPath = "/var/www/ericlostie-launcher/public/installer/"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Paths ─────────────────────────────────────────────────────────────────────
$RepoRoot    = Split-Path $PSScriptRoot -Parent
$ProjectFile = Join-Path $RepoRoot "EricLostieLauncher\EricLostieLauncher.csproj"
$PublishDir  = Join-Path $RepoRoot "publish"
$ReleasesDir = Join-Path $RepoRoot "releases"

# ── Read version from .csproj ─────────────────────────────────────────────────
[xml]$csproj = Get-Content $ProjectFile
$Version = $csproj.Project.PropertyGroup.Version | Where-Object { $_ } | Select-Object -First 1

# Velopack requires 3-part semver (major.minor.patch) — strip the 4th segment if present
$Version = ($Version -split "\.")[0..2] -join "."

if (-not $Version) {
    Write-Error "Could not read <Version> from $ProjectFile"
    exit 1
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  EricLostieLauncher — Velopack Release Build" -ForegroundColor Cyan
Write-Host "  Version : $Version" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ── Clean ─────────────────────────────────────────────────────────────────────
Write-Host "[1/3] Cleaning previous output..." -ForegroundColor Yellow
Remove-Item $PublishDir  -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $ReleasesDir -Recurse -Force -ErrorAction SilentlyContinue

# ── Publish ───────────────────────────────────────────────────────────────────
Write-Host "[2/3] Publishing (win-x64, self-contained)..." -ForegroundColor Yellow
dotnet publish $ProjectFile `
    --configuration Release `
    --runtime win-x64 `
    --self-contained true `
    --output $PublishDir `
    -p:PublishSingleFile=false `
    -p:PublishTrimmed=false

if ($LASTEXITCODE -ne 0) { Write-Error "dotnet publish failed."; exit 1 }

# ── Pack with vpk ─────────────────────────────────────────────────────────────
Write-Host "[3/3] Packaging with vpk..." -ForegroundColor Yellow
vpk pack `
    --packId      EricLostieLauncher `
    --packVersion $Version `
    --packDir     $PublishDir `
    --mainExe     EricLostieLauncher.exe `
    --packTitle   "EricLostie Launcher" `
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
