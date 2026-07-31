# GentleInk

**Wholesome reading on every page.**

GentleInk is a profanity-filtering e-reader system for parents and readers with religious convictions who want to enjoy books without expletives — without the false positives that plague simple word lists (`bass`, `assassin`, `classic` stay intact; `ass` as a donkey is preserved).

Named to avoid confusion with the 2015 *Clean Reader* app and the *CleanRead* Calibre plugin.

## What’s included

| Component | Path | Purpose |
|---|---|---|
| **Shared filter core** | [`core/`](core/) | Dictionary + context rules + substitution lexicon + golden tests |
| **Android APK** | [`android/`](android/) | Sideload on Android e-readers (Boox, Onyx, Xteink S4, etc.) |
| **Calibre plugin** | [`calibre-plugin/`](calibre-plugin/) | Pre-clean EPUB/AZW3 before sending to any device |
| **KOReader plugin** | [`koreader-plugin/`](koreader-plugin/) | On-device preview/filter on KOReader devices |

## Filter modes

- **Substitute** (default): `hell` → `heck`, `shit` → `poop`, profane `ass` → `butt`
- **Mask**: `hell` → `****`
- **Remove**: deletes matched tokens

## Profiles

- **Family** — mild substitutions (`heck`, `poop`, `fudge`)
- **Religious Strict** — stronger neutral replacements (`goodness`, `darn`, `person`)

## Download

**Latest release:** [v0.1.0 on GitHub Releases](https://github.com/drewnicorn-git/gentleink/releases/tag/v0.1.0)

| Asset | For |
|---|---|
| `GentleInk-0.1.0.apk` | Android e-readers (Boox, Onyx, Xteink S4, etc.) |
| `gentleink-calibre-0.1.0.zip` | Calibre desktop plugin |
| `gentleink-koreader-0.1.0.zip` | KOReader plugin |

See [docs/INSTALL.md](docs/INSTALL.md) for install steps.

## Release packaging

```powershell
.\scripts\package-release.ps1 -Version 0.1.0
```

Produces `dist/gentleink-calibre-*.zip`, `dist/gentleink-koreader-*.zip`, and (with Android SDK) `dist/GentleInk-*.apk`.

See [docs/INSTALL.md](docs/INSTALL.md) for end-user install instructions.

## Distribution status

| Phase | Status | Deliverable |
|---|---|---|
| 1 — EPUB reader + live filter | Done | Android library, reader, settings, onboarding |
| 2 — Release signing + packaging | Done | `keystore.properties.example`, `package-release.ps1`, GitHub Actions |
| 3 — Calibre batch clean | Done | `gentleink-calibre-*.zip` with backup + batch action |
| 4 — KOReader batch + revert | Done | `gentleink-koreader-*.zip` with clean/revert menu |
| 5 — F-Droid metadata | Done | `metadata/f-droid.yml` (submit when repo is public) |

**Build APK locally:** Open `android/` in Android Studio → Run, or `./gradlew assembleRelease` with `keystore.properties` configured.

## Quick start — filter core tests

```bash
cd core
node --test tests/filter.test.js
node src/cli.js "What the hell! Move your ass."
```

## Quick start — Android APK

Requirements: Android SDK, JDK 17.

```bash
cd android
./gradlew assembleDebug
```

Install the APK from `android/app/build/outputs/apk/debug/app-debug.apk` on your Android e-reader (enable “Install unknown apps” if sideloading).

## Quick start — Calibre plugin

1. Zip the `calibre-plugin/` folder contents as `gentleink-calibre.zip`.
2. In Calibre: **Preferences → Plugins → Load plugin from file**.
3. Restart Calibre and enable **GentleInk Language Filter** from the toolbar.

See [`docs/device-compatibility.md`](docs/device-compatibility.md) for DRM and device notes.

## Architecture

Three-tier hybrid filter (dictionary alone is not enough):

1. **Tier 1** — unambiguous profanity with word-boundary + compound allowlist
2. **Tier 2** — context rules for ambiguous words (`ass`, `hell`, `damn`, `bitch`, `cock`, `suck`, `crap`)
3. **Tier 3** (planned) — optional on-device TFLite for edge cases

## License

MIT — use and adapt freely. Word lists in `core/data/` contain explicit terms for filtering purposes only.
