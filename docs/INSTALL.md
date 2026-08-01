# Installing GentleInk

GentleInk ships three components. Most e-reader users want the **Android APK**.

## Android APK (Boox, Onyx, Xteink S4, etc.)

### Requirements
- Android 8.0+ e-reader or tablet
- DRM-free EPUB files

### Install steps

1. Download `GentleInk-0.1.0.apk` from the release page.
2. Verify the SHA256 checksum against `SHA256SUMS.txt`.
3. Transfer the APK to your e-reader (USB, cloud drive, or email).
4. On the device: **Settings → Security → Install unknown apps** — enable for your file manager or browser.
5. Open the APK file and tap **Install**.
6. Launch **GentleInk**, complete onboarding, tap **+** to import an EPUB.

### First book
- Use a DRM-free EPUB (Project Gutenberg, direct publisher purchase, or Calibre-cleaned file).
- Choose **Substitute** mode in Settings for readable replacements (`heck`, `poop`, `butt`).
- Use **Family** profile for mild substitutions; **Religious Strict** for stronger neutral wording.

### Troubleshooting
| Problem | Solution |
|---|---|
| "App not installed" | Enable unknown sources; ensure enough storage |
| Book won't import | File must be `.epub` without DRM |
| Kindle purchase won't open | Use Calibre plugin first, then import cleaned EPUB |
| `bass` or `assassin` changed | Report as false positive — update planned |

---

## Calibre plugin (desktop)

1. Download **`gentleink.zip`** from the release page (not a folder-wrapped zip).
2. Calibre → **Preferences → Plugins → Load plugin from file** → select `gentleink.zip`.
3. Restart Calibre when prompted.
4. **Preferences → Toolbars & menus → The main toolbar** → add **GentleInk Clean**.
5. Select books in your library → click **GentleInk Clean** → send cleaned EPUB to your device.

Original files are backed up as `ORIGINAL_EPUB` inside each library entry.

### Calibre install error: "must contain a top-level __init__.py"

Older release zips (`gentleink-calibre-0.1.0.zip`) nested files inside a `gentleink/` folder, which Calibre rejects. Use **`gentleink.zip`** from v0.1.1 or later, or rebuild locally:

```powershell
.\scripts\package-release.ps1 -Version 0.1.1
```

Then load `dist\gentleink.zip` in Calibre.

---

## KOReader plugin (Kobo, jailbroken Kindle)

1. Download `gentleink-koreader-0.1.0.zip` and extract.
2. Copy `gentleink.koplugin/` to your device's KOReader `plugins/` folder.
3. Restart KOReader.
4. Open a book → **GentleInk filter → Clean current book**.

A backup of the original EPUB is kept in `gentleink_cache/` on the device.

---

## Privacy

All filtering runs on your device. GentleInk does not connect to the internet or collect data.

---

## Building the APK yourself

```bash
# Generate signing key (one time)
keytool -genkey -v -keystore android/keystore/gentleink-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias gentleink

# Copy and fill in android/keystore.properties from keystore.properties.example
cd android && ./gradlew assembleRelease
```

Or run the all-in-one packager:

```powershell
.\scripts\package-release.ps1 -Version 0.1.0
```
