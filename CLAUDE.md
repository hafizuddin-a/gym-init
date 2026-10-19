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
- An unsaved new session is the "draft": Home lists it under `// in progress` (tap to resume), it is persisted in prefs so it survives the app being killed, and `[+] new session` asks before discarding it. New sessions start blank (no pre-fill from the last session). Opening a saved session while a draft exists parks the draft (`parked` in AppViewModel); `[resume]` next to `[del]` in edit mode means "continue this saved session" (for an accidental early save): `AppViewModel.continuing` switches the screen to the normal workout layout (stopwatch/timer, no `[del]`), edits are autosaved to the `continued` pref so they survive the app dying, and they are committed to the session on `[save]` or when leaving for another session. On Home the continued session is listed under `// in progress` (green, removed from its month group) together with any parked unsaved session; tapping the unsaved row goes to that session instead. Edits to saved sessions are not persisted.
- Home is a `LazyColumn` (HomeScreen.kt; the weeks bar and best holds sit above the table so they stay visible with a long history): each table row draws its own edges via `tableEdges` since a lazy list can't sit in one bordered box, and row text is precomputed in `HomeRow.Done`. With 3000 sessions opening Home dropped from a 4.9 s freeze to 0.35 s (debug build, emulator). `Fmt` caches `SimpleDateFormat` per thread.
- Isometric holds: a row has `LiftRow.hold` (reps column means seconds, tap the `reps`/`sec` label to switch). Holds are excluded from kg volume and shown as a separate `// holds` total; Home shows `plank 38s`, `// best holds`, and the volume cell shows `38s` when a session has no kg. Pausing the stopwatch (total elapsed, laps ignored) or pausing/finishing the timer (goal minus remaining) shows `[add Ns to exercises]`, which calls `AppViewModel.addHold`: fills the first blank row (or appends), focuses its name field, and resets the stopwatch/timer. One hold per row, no sets. Design: the 'Isometric holds' boards in the design artifact. Exercise names are trimmed and lowercased on save. The reps/sec label is a 28dp-tall tap target (below the 44dp guideline).
- Not verified yet: the timer-end beep/vibration, light themes on a device, API 26-30 devices.

## Design rules to keep

- Everything is monospace (JetBrains Mono), lowercase, `[bracket]` buttons, `// comment` captions, `you@gym $ cmd` prompt line.
- All colours come from `LocalTheme` (Theme.kt); never hardcode colours in screens.
- Touch targets >= 44dp. Muted text must stay readable on every theme.

## Known gaps / ideas

- Timer dies if Android kills the app (no foreground service or notification).
- No export/backup of sessions; no per-set history (one row = one exercise with one weight and reps).
- No tests yet.
