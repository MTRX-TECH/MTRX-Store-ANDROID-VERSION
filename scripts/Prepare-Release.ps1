param (
    [string]$Version = "1.0.0",
    [int]$VersionCode = 1
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Split-Path -Parent $scriptDir
Set-Location $projectDir

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "    MTRX Store - Release Packaging Tool   " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Project directory: $projectDir"
Write-Host "Target Version: $Version (VersionCode: $VersionCode)"

Write-Host "`n[1/3] Building Release APK with Gradle..." -ForegroundColor Yellow
& .\gradlew.bat :app:assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle release build failed."
    exit 1
}

$releaseApk = Join-Path $projectDir "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $releaseApk)) {
    Write-Error "Output APK not found at $releaseApk"
    exit 1
}

$outputDir = Join-Path $projectDir "release"
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$finalApkName = "MTRX-Store-v$Version.apk"
$finalApkPath = Join-Path $outputDir $finalApkName
Copy-Item -Path $releaseApk -Destination $finalApkPath -Force

$item = Get-Item $finalApkPath
$sizeBytes = $item.Length
$sizeMB = [math]::Round($sizeBytes / 1MB, 2)
$sha256 = (Get-FileHash -Path $finalApkPath -Algorithm SHA256).Hash.ToLower()

Write-Host "`n[2/3] Artifact Ready!" -ForegroundColor Green
Write-Host "File Name:   $finalApkName"
Write-Host "File Path:   $finalApkPath"
Write-Host "File Size:   $sizeBytes bytes (~$sizeMB MB)"
Write-Host "SHA-256:     $sha256"

Write-Host "`n[3/3] GitHub Release Instructions:" -ForegroundColor Cyan
Write-Host "------------------------------------------------------------"
Write-Host "1. Git Push Tag:"
Write-Host "   git tag v$Version"
Write-Host "   git push origin v$Version"
Write-Host ""
Write-Host "2. Create Release on GitHub:"
Write-Host "   URL: https://github.com/MTRX-TECH/MTRX-Store-ANDROID-VERSION/releases/new"
Write-Host "   Tag: v$Version"
Write-Host "   Title: MTRX Store v$Version"
Write-Host "   Attach file: $finalApkPath"
Write-Host ""
Write-Host "3. Update your catalog.json entry with:"
$catalogEntry = @"
{
  "packageName": "com.mtrx.store",
  "name": "MTRX Store",
  "versionName": "$Version",
  "versionCode": $VersionCode,
  "apkUrl": "https://github.com/MTRX-TECH/MTRX-Store-ANDROID-VERSION/releases/download/v$Version/$finalApkName",
  "sha256": "$sha256",
  "sizeBytes": $sizeBytes
}
"@
Write-Host $catalogEntry -ForegroundColor White
Write-Host "------------------------------------------------------------"
