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
)

data class Session(val id: Long, val timestamp: Long, val rows: List<LiftRow>) {
    val volume: Double
        get() = rows.sumOf { (it.weight.toDoubleOrNull() ?: 0.0) * (it.reps.toDoubleOrNull() ?: 0.0) }
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
                        )
                    },
                )
            }
        }
    } catch (e: Exception) {
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
                        .put("reps", r.reps),
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

    private fun fmt(pattern: String, ts: Long): String =
        SimpleDateFormat(pattern, Locale.ENGLISH).format(Date(ts)).lowercase(Locale.ENGLISH)

    fun dayLabel(ts: Long): String = fmt("EEE dd MMM", ts)

    fun longDate(ts: Long): String = fmt("EEEE, MMMM d yyyy", ts)

    fun monthKey(ts: Long): String = fmt("yyyy-MM", ts)

    fun monthLabel(ts: Long): String {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val year = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.YEAR)
        return fmt(if (year == thisYear) "MMMM" else "MMMM yyyy", ts)
    }

    fun summary(rows: List<LiftRow>): String {
        val names = rows.map { it.name }
        return if (names.size <= 2) names.joinToString(", ") else names.take(2).joinToString(", ") + " +${names.size - 2}"
    }
}
