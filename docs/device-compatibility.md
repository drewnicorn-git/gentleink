# Device compatibility and DRM

## Where GentleInk works

| Device / platform | Support | Notes |
|---|---|---|
| Android e-readers (Boox, Onyx, PocketBook Android, Xteink S4) | **APK sideload** | Primary target. Install `app-debug.apk` or release build. |
| KOReader (Kobo, Kindle jailbreak, Android, etc.) | **Plugin** | Copy `koreader-plugin/gentleink.koplugin/` into KOReader `plugins/`. |
| Any e-reader | **Calibre pre-process** | Clean EPUB/AZW3 locally, then transfer. |
| Kindle (native, non-Android) | **Calibre only** | No APK sideloading. Use Calibre → Send to Kindle (DRM-free files). |
| Kobo (native firmware) | **Calibre or KOReader** | Native Kobo app cannot run GentleInk APK. |
| Kindle / Kobo / Libby **DRM** books | **Limited** | Cannot filter inside Amazon/Kobo/OverDrive apps. |

## Recommended workflows for mixed libraries

### DRM-free personal EPUBs
1. Open directly in **GentleInk Android** app, or
2. Pre-clean in **Calibre**, then copy to device.

### Kindle purchases (DRM)
Amazon DRM prevents third-party apps from reading or modifying those files. Options:
- Buy DRM-free editions from publishers when available.
- Use Calibre + DeDRM (legal status varies by jurisdiction; user responsibility).
- Read unfiltered in the Kindle app for DRM titles.

### Library loans (Libby / OverDrive)
Loans are DRM-protected inside vendor apps. GentleInk cannot filter in-app. If you obtain a DRM-free copy legally, use Calibre or the Android app.

## Performance on e-ink

- Filtering runs on a background thread.
- Large EPUB imports are capped at 20,000 characters in the preview UI (batch/full-book mode planned).
- Prefer **Substitute** mode for readability vs blank gaps from **Remove** mode.
