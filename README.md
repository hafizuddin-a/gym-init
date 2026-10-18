# gym init

An extremely simple Android gym log that looks like a terminal / spreadsheet.
Inspired by the monospace, editor-theme aesthetic of [init.Habits](https://inithabits.com).

```
you@gym $ sessions
> sessions
// 12 saved · 4 this month

 date        | exercises          |  volume
-------------+--------------------+--------
 // october
 thu 16 oct  | bench, squat +2    |   4,210
 mon 13 oct  | deadlift, pull-up  |   3,680
```

## Screens

| Screen | What it does |
| --- | --- |
| **Workout** (opens first) | A spreadsheet grid of `exercise / weight (kg) / reps`. `[+] add exercise`, `×` to remove a row, `[save]` to store the session. New sessions start with last session's exercise names and show its numbers as hints. |
| **Home** | Every saved session, grouped by month, with volume (Σ kg × reps). Tap one to edit or delete it. `[theme]` opens the theme picker. |
| **Stopwatch** | Start / pause / reset with laps. Fastest lap is green, slowest is amber. |
| **Timer** | Rest timer with presets (0:30 – 5:00) and ±15 s. Beeps and vibrates when it hits zero. |

The stopwatch and timer keep running while you move between screens, and the
Workout screen's `[stopwatch]` / `[timer]` buttons show their live values.

### Themes

Eight editor themes, switchable from Home → `[theme]` and applied to every screen:
tokyo night (default), dracula, nord, gruvbox dark, catppuccin mocha,
solarized light, github light, catppuccin latte.

## Build

Requires JDK 17 and the Android SDK (platform 35).

```sh
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press Run. Every push also builds a
debug APK in GitHub Actions (see the `build` workflow's artifacts).

## How it is built

- Kotlin + Jetpack Compose + Material 3 (used only for the dialog and bottom sheet).
- No database: sessions are one small JSON file in app storage, preferences hold the theme and rest time.
- Single `Activity`, one `ViewModel` holding navigation, sessions, theme, stopwatch and timer state.
- minSdk 26, targetSdk 35.

```
app/src/main/java/com/gyminit/
  MainActivity.kt      app shell, edge-to-edge, back handling
  AppViewModel.kt      all state: navigation, sessions, stopwatch, timer
  Data.kt              models, JSON storage, formatting helpers
  Theme.kt             the eight themes + JetBrains Mono
  Ui.kt                terminal-style buttons, table cells, text fields
  WorkoutScreen.kt  HomeScreen.kt  StopwatchScreen.kt  TimerScreen.kt
```

## Known limits

- The timer runs only while the app process is alive: no foreground service or notification yet, so it won't ring if Android kills the app in the background.
- Data is local only (no sync or export).
- Volume is a simple Σ weight × reps per row.

## License

MIT for the app code. JetBrains Mono is bundled under the SIL Open Font License
(see `FONT-LICENSE-OFL.txt`).
