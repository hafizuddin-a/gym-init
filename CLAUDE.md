# gym init — notes for Claude

Android gym log, Kotlin + Jetpack Compose, terminal/spreadsheet look (inspired by init.Habits).
See README.md for screens, themes and structure.

## Status

- Builds and runs: `./gradlew assembleDebug` is green; clicked through all four screens
  (Workout, Home, Stopwatch, Timer), the theme sheet and a running timer on a Pixel 9 emulator (API 36).
- Needs **JDK 17** (Gradle 8.10 / Kotlin 2.0 break on JDK 25, which is what Android Studio's
  bundled JBR is now). On macOS: `brew install openjdk@17`, then
  `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.
- Progress bars are drawn by `BlockBar` (Ui.kt), not █/░ glyphs: the bundled JetBrains Mono is the
  latin subset, so those glyphs fell back to a system font and rendered stippled.
- The tools row on the Workout screen hides while the keyboard is open (it overlapped the volume block).
- An unsaved new session is the "draft": Home lists it under `// in progress` (tap to resume), it is persisted in prefs so it survives the app being killed, and `[+] new session` asks before discarding it. New sessions start blank (no pre-fill from the last session). Opening a saved session while a draft exists parks the draft (`parked` in AppViewModel); `[resume]` next to `[del]` in edit mode (always shown) returns to the parked draft, or to a blank new session if there is none. Edits to saved sessions are not persisted.
- Home is a `LazyColumn` (HomeScreen.kt): each table row draws its own edges via `tableEdges` since a lazy list can't sit in one bordered box, and row text is precomputed in `HomeRow.Done`. With 3000 sessions opening Home dropped from a 4.9 s freeze to 0.35 s (debug build, emulator). `Fmt` caches `SimpleDateFormat` per thread.
- Not verified yet: the timer-end beep/vibration, light themes on a device, API 26-30 devices.

## Design rules to keep

- Everything is monospace (JetBrains Mono), lowercase, `[bracket]` buttons, `// comment` captions, `you@gym $ cmd` prompt line.
- All colours come from `LocalTheme` (Theme.kt); never hardcode colours in screens.
- Touch targets >= 44dp. Muted text must stay readable on every theme.

## Known gaps / ideas

- Timer dies if Android kills the app (no foreground service or notification).
- No export/backup of sessions; no per-set history (one row = one exercise with one weight and reps).
- No tests yet.
