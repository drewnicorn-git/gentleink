# GentleInk Release Packaging

Runs quality checks and packages all distributable artifacts into `dist/`.

## Prerequisites

- Node.js (for golden tests)
- Android SDK + JDK 17 (for APK; optional — skips if unavailable)
- PowerShell 5+

## Usage

```powershell
.\scripts\package-release.ps1
# Or with version tag:
.\scripts\package-release.ps1 -Version 0.1.0
```

## Output (`dist/`)

| File | Description |
|---|---|
| `GentleInk-0.1.0.apk` | Signed release APK (if SDK + keystore present) |
| `GentleInk-0.1.0-unsigned.apk` | Unsigned release APK (if SDK, no keystore) |
| `gentleink-calibre-0.1.0.zip` | Calibre plugin |
| `gentleink-koreader-0.1.0.zip` | KOReader plugin |
| `SHA256SUMS.txt` | Checksums for all artifacts |
