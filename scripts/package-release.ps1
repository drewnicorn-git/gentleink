param(
    [string]$Version = "0.1.0"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Dist = Join-Path $Root "dist"
New-Item -ItemType Directory -Force -Path $Dist | Out-Null

Write-Host "=== GentleInk Release Packager v$Version ===" -ForegroundColor Cyan

# Step 1: Golden tests
Write-Host "`n[1/5] Running golden tests..." -ForegroundColor Yellow
Push-Location (Join-Path $Root "core")
node --test tests/filter.test.js
if ($LASTEXITCODE -ne 0) { throw "Golden tests failed" }
Pop-Location
Write-Host "  PASS: 35 golden tests" -ForegroundColor Green

# Step 2: Sync Android assets
Write-Host "`n[2/5] Syncing filter assets to Android..." -ForegroundColor Yellow
$AssetsDir = Join-Path $Root "android\app\src\main\assets"
New-Item -ItemType Directory -Force -Path $AssetsDir | Out-Null
Copy-Item (Join-Path $Root "core\data\allowlist.json") $AssetsDir -Force
Copy-Item (Join-Path $Root "core\data\tier1-unambiguous.json") $AssetsDir -Force
Copy-Item (Join-Path $Root "core\data\context-rules.json") $AssetsDir -Force
Copy-Item (Join-Path $Root "core\data\substitutions.json") $AssetsDir -Force
Write-Host "  PASS: 4 JSON assets synced" -ForegroundColor Green

# Step 3: Build Android APK (if SDK available)
Write-Host "`n[3/5] Building Android APK..." -ForegroundColor Yellow
$ApkBuilt = $false
$AndroidDir = Join-Path $Root "android"
$LocalProps = Join-Path $AndroidDir "local.properties"
if (-not (Test-Path $LocalProps)) {
    $SdkPaths = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:ANDROID_HOME",
        "C:\Android\Sdk"
    ) | Where-Object { $_ -and (Test-Path $_) }
    if ($SdkPaths) {
        "sdk.dir=$($SdkPaths[0].Replace('\','\\'))" | Set-Content $LocalProps -Encoding UTF8
    }
}

$Gradlew = Join-Path $AndroidDir "gradlew.bat"
if ((Test-Path $LocalProps) -and (Test-Path $Gradlew)) {
    Push-Location $AndroidDir
    & $Gradlew assembleRelease --no-daemon 2>&1
    Pop-Location
    $ReleaseApk = Join-Path $AndroidDir "app\build\outputs\apk\release\app-release.apk"
    $UnsignedApk = Join-Path $AndroidDir "app\build\outputs\apk\release\app-release-unsigned.apk"
    if (Test-Path $ReleaseApk) {
        Copy-Item $ReleaseApk (Join-Path $Dist "GentleInk-$Version.apk") -Force
        $ApkBuilt = $true
        Write-Host "  PASS: Signed release APK" -ForegroundColor Green
    } elseif (Test-Path $UnsignedApk) {
        Copy-Item $UnsignedApk (Join-Path $Dist "GentleInk-$Version-unsigned.apk") -Force
        $ApkBuilt = $true
        Write-Host "  WARN: Unsigned APK (create keystore.properties for signing)" -ForegroundColor DarkYellow
    } else {
        Write-Host "  SKIP: Gradle build did not produce APK" -ForegroundColor DarkYellow
    }
} else {
    Write-Host "  SKIP: Android SDK or gradlew not found" -ForegroundColor DarkYellow
    Write-Host "         Install Android Studio, open android/, sync Gradle, then re-run." -ForegroundColor DarkGray
}

# Step 4: Package Calibre + KOReader plugins
Write-Host "`n[4/5] Packaging Calibre and KOReader plugins..." -ForegroundColor Yellow
$CalibreZip = Join-Path $Dist "gentleink-calibre-$Version.zip"
$KoreaderZip = Join-Path $Dist "gentleink-koreader-$Version.zip"

$CalibreStaging = Join-Path $env:TEMP "gentleink-calibre-staging"
$KoreaderStaging = Join-Path $env:TEMP "gentleink-koreader-staging"
Remove-Item $CalibreStaging, $KoreaderStaging -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $CalibreStaging | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $CalibreStaging "gentleink\core_data") | Out-Null
New-Item -ItemType Directory -Force -Path $KoreaderStaging | Out-Null

Copy-Item (Join-Path $Root "calibre-plugin\gentleink\*") (Join-Path $CalibreStaging "gentleink") -Recurse -Force
Copy-Item (Join-Path $Root "core\data\*.json") (Join-Path $CalibreStaging "gentleink\core_data") -Force
Copy-Item (Join-Path $Root "koreader-plugin\gentleink.koplugin") (Join-Path $KoreaderStaging "gentleink.koplugin") -Recurse -Force

if (Test-Path $CalibreZip) { Remove-Item $CalibreZip -Force }
if (Test-Path $KoreaderZip) { Remove-Item $KoreaderZip -Force }
Compress-Archive -Path "$CalibreStaging\gentleink" -DestinationPath $CalibreZip -Force
Compress-Archive -Path "$KoreaderStaging\*" -DestinationPath $KoreaderZip -Force
Write-Host "  PASS: Calibre plugin -> gentleink-calibre-$Version.zip" -ForegroundColor Green
Write-Host "  PASS: KOReader plugin -> gentleink-koreader-$Version.zip" -ForegroundColor Green

# Step 5: Checksums
Write-Host "`n[5/5] Generating checksums..." -ForegroundColor Yellow
$ChecksumFile = Join-Path $Dist "SHA256SUMS.txt"
$Lines = Get-ChildItem $Dist -File | Where-Object { $_.Name -ne "SHA256SUMS.txt" } | ForEach-Object {
    $hash = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower()
    "$hash  $($_.Name)"
}
$Lines | Set-Content $ChecksumFile -Encoding UTF8
Write-Host "  PASS: SHA256SUMS.txt" -ForegroundColor Green

Write-Host "`n=== Release packaging complete ===" -ForegroundColor Cyan
Get-ChildItem $Dist | Format-Table Name, Length -AutoSize
if (-not $ApkBuilt) {
    Write-Host "Note: Build APK locally with Android Studio for the full release." -ForegroundColor DarkYellow
}
