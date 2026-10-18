package com.gyminit

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { Workout, Home, Stopwatch, Timer }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private var nextId = 1L

    // ---- navigation + theme ----
    var screen by mutableStateOf(Screen.Workout)
        private set
    var themeIdx by mutableIntStateOf(store.themeIdx.coerceIn(0, THEMES.lastIndex))
        private set
    var pickerOpen by mutableStateOf(false)

    fun goHome() {
        screen = Screen.Home
    }

    fun goWorkout() {
        screen = Screen.Workout
    }

    fun goStopwatch() {
        screen = Screen.Stopwatch
    }

    fun goTimer() {
        screen = Screen.Timer
    }

    fun back() {
        screen = when (screen) {
            Screen.Stopwatch, Screen.Timer -> Screen.Workout
            Screen.Workout -> Screen.Home
            Screen.Home -> Screen.Home
        }
    }

    fun setTheme(i: Int) {
        themeIdx = i.coerceIn(0, THEMES.lastIndex)
        store.themeIdx = themeIdx
    }

    // ---- sessions ----
    var sessions by mutableStateOf(store.load().sortedByDescending { it.timestamp })
        private set
    val draft = mutableStateListOf<LiftRow>()
    var editingId by mutableStateOf<Long?>(null)
        private set
    var sessionTs by mutableLongStateOf(System.currentTimeMillis())
        private set

    val draftVolume: Double
        get() = draft.sumOf { (it.weight.toDoubleOrNull() ?: 0.0) * (it.reps.toDoubleOrNull() ?: 0.0) }
    val bestVolume: Double
        get() = sessions.maxOfOrNull { it.volume } ?: 0.0

    init {
        newSession()
    }

    private fun blankRow() = LiftRow(id = nextId++)

    /** New sessions start with last session's exercise names; its numbers show as hints. */
    fun newSession() {
        val base = sessions.firstOrNull()?.rows?.filter { it.name.isNotBlank() } ?: emptyList()
        draft.clear()
        if (base.isEmpty()) {
            repeat(3) { draft.add(blankRow()) }
        } else {
            base.forEach {
                draft.add(LiftRow(nextId++, it.name, "", "", it.weight, it.reps))
            }
        }
        editingId = null
        sessionTs = System.currentTimeMillis()
        screen = Screen.Workout
    }

    fun edit(s: Session) {
        draft.clear()
        s.rows.forEach { draft.add(it.copy(id = nextId++)) }
        editingId = s.id
        sessionTs = s.timestamp
        screen = Screen.Workout
    }

    fun addRow() {
        draft.add(blankRow())
    }

    fun updateRow(index: Int, row: LiftRow) {
        if (index in draft.indices) draft[index] = row
    }

    fun removeRow(index: Int) {
        if (index in draft.indices) draft.removeAt(index)
        if (draft.isEmpty()) draft.add(blankRow())
    }

    fun save() {
        val rows = draft.filter { it.name.isNotBlank() }.map { it.copy(hintWeight = "", hintReps = "") }
        if (rows.isNotEmpty()) {
            val id = editingId ?: System.currentTimeMillis()
            val saved = Session(id, sessionTs, rows)
            sessions = (sessions.filter { it.id != id } + saved).sortedByDescending { it.timestamp }
            store.save(sessions)
        }
        screen = Screen.Home
    }

    fun deleteCurrent() {
        val id = editingId
        if (id != null) {
            sessions = sessions.filter { it.id != id }
            store.save(sessions)
        }
        screen = Screen.Home
    }

    // ---- ticker shared by stopwatch + timer ----
    var now by mutableLongStateOf(SystemClock.elapsedRealtime())
        private set
    private var ticker: Job? = null

    private fun ensureTicker() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (swRunning || timerRunning) {
                now = SystemClock.elapsedRealtime()
                if (timerRunning && timerEnd - now <= 0) finishTimer()
                delay(33)
            }
        }
    }

    // ---- stopwatch ----
    var swRunning by mutableStateOf(false)
        private set
    private var swStart = 0L
    private var swAccum by mutableLongStateOf(0L)
    val laps = mutableStateListOf<Long>()

    val swElapsed: Long
        get() = swAccum + if (swRunning) (now - swStart).coerceAtLeast(0) else 0L

    fun toggleStopwatch() {
        val t = SystemClock.elapsedRealtime()
        if (swRunning) {
            swAccum += t - swStart
            swRunning = false
        } else {
            swStart = t
            now = t
            swRunning = true
            ensureTicker()
        }
    }

    fun lap() {
        if (swRunning) laps.add(swElapsed)
    }

    fun resetStopwatch() {
        swRunning = false
        swAccum = 0L
        laps.clear()
    }

    // ---- rest timer ----
    var restSec by mutableIntStateOf(store.restSec.coerceIn(15, 900))
        private set
    var timerRunning by mutableStateOf(false)
        private set
    var timerDone by mutableStateOf(false)
        private set
    var timerTotalMs by mutableLongStateOf(restSec * 1000L)
        private set
    private var timerBase by mutableLongStateOf(restSec * 1000L)
    private var timerEnd = 0L

    val timerRemaining: Long
        get() = if (timerRunning) (timerEnd - now).coerceAtLeast(0) else timerBase

    fun toggleTimer() {
        if (timerRunning) {
            timerBase = (timerEnd - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            timerRunning = false
        } else {
            if (timerBase <= 0) resetTimer()
            timerDone = false
            val t = SystemClock.elapsedRealtime()
            now = t
            timerEnd = t + timerBase
            timerRunning = true
            ensureTicker()
        }
    }

    fun resetTimer() {
        timerRunning = false
        timerDone = false
        timerBase = restSec * 1000L
        timerTotalMs = timerBase
    }

    fun setRest(sec: Int) {
        restSec = sec.coerceIn(15, 900)
        store.restSec = restSec
        resetTimer()
    }

    fun adjustRest(deltaSec: Int) {
        val target = (restSec + deltaSec).coerceIn(15, 900)
        if (timerRunning) {
            val shift = (target - restSec) * 1000L
            timerEnd += shift
            timerTotalMs += shift
            restSec = target
            store.restSec = target
        } else {
            setRest(target)
        }
    }

    private fun finishTimer() {
        timerRunning = false
        timerBase = 0L
        timerDone = true
        signal()
    }

    private fun signal() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 80)
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
            viewModelScope.launch {
                delay(1200)
                tone.release()
            }
        } catch (e: Exception) {
            // no audio available: vibration below still fires
        }
        try {
            vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        } catch (e: Exception) {
            // device without a vibrator
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrator(): Vibrator {
        val app = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= 31) {
            app.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
