package com.gyminit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LiftRow(
    val id: Long,
    val name: String = "",
    val weight: String = "",
    val reps: String = "",
    val hintWeight: String = "",
    val hintReps: String = "",
    /** An isometric hold: `reps` then means seconds, and the row stays out of the kg volume. */
    val hold: Boolean = false,
)

private fun List<LiftRow>.kgVolume(): Double =
    filter { !it.hold }.sumOf { (it.weight.toDoubleOrNull() ?: 0.0) * (it.reps.toDoubleOrNull() ?: 0.0) }

private fun List<LiftRow>.holdTotal(): Int =
    filter { it.hold }.sumOf { it.reps.toDoubleOrNull()?.toInt() ?: 0 }

data class Session(val id: Long, val timestamp: Long, val rows: List<LiftRow>) {
    val volume: Double get() = rows.kgVolume()
    val holdSeconds: Int get() = rows.holdTotal()
}

/** Unsaved workout, kept so it survives leaving the screen or the app being killed. */
data class Draft(val editingId: Long?, val timestamp: Long, val rows: List<LiftRow>) {
    val volume: Double get() = rows.kgVolume()
    val holdSeconds: Int get() = rows.holdTotal()
}

/** Sessions live in one small JSON file; preferences hold the theme and rest time. */
class Store(private val context: Context) {
    private val file get() = File(context.filesDir, "sessions.json")
    private val prefs = context.getSharedPreferences("gyminit", Context.MODE_PRIVATE)

    fun load(): List<Session> = try {
        if (!file.exists()) {
            emptyList()
        } else {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val rows = o.getJSONArray("rows")
                Session(
                    id = o.getLong("id"),
                    timestamp = o.getLong("ts"),
                    rows = (0 until rows.length()).map { j ->
                        val r = rows.getJSONObject(j)
                        LiftRow(
                            id = j.toLong(),
                            name = r.getString("name"),
                            weight = r.getString("weight"),
                            reps = r.getString("reps"),
                            hold = r.optBoolean("hold", false),
                        )
                    },
                )
            }
        }
    } catch (e: Exception) {
        // unreadable file: keep a copy so the next save can't destroy the only evidence
        try {
            file.copyTo(File(context.filesDir, "sessions.corrupt.json"), overwrite = true)
        } catch (_: Exception) {
        }
        emptyList()
    }

    fun save(sessions: List<Session>) {
        val arr = JSONArray()
        sessions.forEach { s ->
            val rows = JSONArray()
            s.rows.forEach { r ->
                rows.put(
                    JSONObject()
                        .put("name", r.name)
                        .put("weight", r.weight)
                        .put("reps", r.reps)
                        .put("hold", r.hold),
                )
            }
            arr.put(JSONObject().put("id", s.id).put("ts", s.timestamp).put("rows", rows))
        }
        val tmp = File(context.filesDir, "sessions.json.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(arr.toString())
            tmp.delete()
        }
    }

    fun loadDraft(): Draft? = readDraft("draft")

    fun saveDraft(draft: Draft) = writeDraft("draft", draft)

    fun clearDraft() = prefs.edit().remove("draft").apply()

    /** A saved session being continued after an accidental save; kept apart from the unsaved new session. */
    fun loadContinued(): Draft? = readDraft("continued")

    fun saveContinued(draft: Draft) = writeDraft("continued", draft)

    fun clearContinued() = prefs.edit().remove("continued").apply()

    private fun readDraft(key: String): Draft? = try {
        prefs.getString(key, null)?.let { raw ->
            val o = JSONObject(raw)
            val rows = o.getJSONArray("rows")
            Draft(
                editingId = if (o.has("editing")) o.getLong("editing") else null,
                timestamp = o.getLong("ts"),
                rows = (0 until rows.length()).map { j ->
                    val r = rows.getJSONObject(j)
                    LiftRow(
                        id = j.toLong(),
                        name = r.getString("name"),
                        weight = r.getString("weight"),
                        reps = r.getString("reps"),
                        hintWeight = r.optString("hw"),
                        hintReps = r.optString("hr"),
                        hold = r.optBoolean("hold", false),
                    )
                },
            )
        }
    } catch (e: Exception) {
        null
    }

    private fun writeDraft(key: String, draft: Draft) {
        val rows = JSONArray()
        draft.rows.forEach { r ->
            rows.put(
                JSONObject()
                    .put("name", r.name).put("weight", r.weight).put("reps", r.reps)
                    .put("hw", r.hintWeight).put("hr", r.hintReps).put("hold", r.hold),
            )
        }
        val o = JSONObject().put("ts", draft.timestamp).put("rows", rows)
        draft.editingId?.let { o.put("editing", it) }
        prefs.edit().putString(key, o.toString()).apply()
    }

    var themeIdx: Int
        get() = prefs.getInt("theme", 0)
        set(v) = prefs.edit().putInt("theme", v).apply()

    var restSec: Int
        get() = prefs.getInt("rest", 90)
        set(v) = prefs.edit().putInt("rest", v).apply()
}

object Fmt {
    fun kg(v: Double): String = String.format(Locale.US, "%,.0f", v)

    fun clock(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
    }

    fun centis(ms: Long): String = String.format(Locale.US, ".%02d", (ms.coerceAtLeast(0) % 1000) / 10)

    fun stopwatch(ms: Long): String = clock(ms) + centis(ms)

    // SimpleDateFormat is costly to build and not thread-safe: keep one per pattern per thread.
    private val formats = object : ThreadLocal<HashMap<String, SimpleDateFormat>>() {
        override fun initialValue() = HashMap<String, SimpleDateFormat>()
    }

    private fun fmt(pattern: String, ts: Long): String {
        val cache = formats.get()!!
        val f = cache.getOrPut(pattern) { SimpleDateFormat(pattern, Locale.ENGLISH) }
        return f.format(Date(ts)).lowercase(Locale.ENGLISH)
    }

    fun dayLabel(ts: Long): String = fmt("EEE dd MMM", ts)

    fun longDate(ts: Long): String = fmt("EEEE, MMMM d yyyy", ts)

    fun monthKey(ts: Long): String = fmt("yyyy-MM", ts)

    fun monthLabel(ts: Long): String {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val year = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.YEAR)
        return fmt(if (year == thisYear) "MMMM" else "MMMM yyyy", ts)
    }

    fun summary(rows: List<LiftRow>): String {
        val names = rows.map { if (it.hold) "${it.name} ${it.reps.ifEmpty { "0" }}s" else it.name }
        return if (names.size <= 2) names.joinToString(", ") else names.take(2).joinToString(", ") + " +${names.size - 2}"
    }

    /** m:ss for a hold length. */
    fun holdClock(seconds: Int): String = String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)

    /** Volume cell: kg when there is any, otherwise the hold time, otherwise 0. */
    fun volumeCell(volume: Double, holdSeconds: Int): String =
        if (volume <= 0.0 && holdSeconds > 0) "${holdSeconds}s" else kg(volume)
}
