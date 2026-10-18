# gym init — notes for Claude

Android gym log, Kotlin + Jetpack Compose, terminal/spreadsheet look (inspired by init.Habits).
See README.md for screens, themes and structure.

## Status

- The code was written in a cloud sandbox that could NOT reach the Android SDK or AndroidX
  (dl.google.com blocked), so **it has never been compiled or run**.
- First job locally: `./gradlew assembleDebug`, fix any compile errors, then run it on an
  emulator/device and click through all four screens (Workout, Home, Stopwatch, Timer).
- Things worth checking on a real run:
  - Edge-to-edge insets (status bar / nav bar / keyboard) on API 35.
  - The ASCII bar glyphs (█ ░ ▓) and ✓: the bundled JetBrains Mono is the latin subset, so these fall back to a system font and may misalign.
  - Text fields in the Workout grid: focus, keyboard Next, numeric filtering.
  - Theme bottom sheet (Home → [theme]) and that every screen re-themes.
  - Timer end: beep + vibration; stopwatch/timer keep running across screens.

## Design rules to keep

- Everything is monospace (JetBrains Mono), lowercase, `[bracket]` buttons, `// comment` captions, `you@gym $ cmd` prompt line.
- All colours come from `LocalTheme` (Theme.kt); never hardcode colours in screens.
- Touch targets >= 44dp. Muted text must stay readable on every theme.

## Known gaps / ideas

- Timer dies if Android kills the app (no foreground service or notification).
- No export/backup of sessions; no per-set history (one row = one exercise with one weight and reps).
- No tests yet.
