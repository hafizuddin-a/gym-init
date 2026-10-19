package com.gyminit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

private sealed interface HomeRow {
    val key: String

    data class Month(val label: String) : HomeRow {
        override val key get() = "m-$label"
    }

    /** Display strings are computed once when the list is built, not on every scroll frame. */
    data class Done(
        val session: Session,
        val date: String,
        val exercises: String,
        val volume: String,
        val active: Boolean,
    ) : HomeRow {
        override val key get() = "s-${session.id}"
    }

    data object ProgressHeader : HomeRow {
        override val key get() = "ph"
    }

    data class Progress(val draft: Draft) : HomeRow {
        override val key get() = "p"
    }

    data object Empty : HomeRow {
        override val key get() = "e"
    }
}

/**
 * Edge lines of one row of the sessions table. The table is a lazy list, so it cannot sit inside
 * one bordered box: every row draws its own sides, a separator on top, and rounded outer corners
 * on the first and last row.
 */
private fun Modifier.tableEdges(color: Color, first: Boolean, last: Boolean): Modifier {
    val radius = 4.dp
    val shape = RoundedCornerShape(
        topStart = if (first) radius else 0.dp,
        topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp,
        bottomEnd = if (last) radius else 0.dp,
    )
    return this.clip(shape).drawWithContent {
        drawContent()
        val stroke = 1.dp.toPx()
        val half = stroke / 2
        val r = radius.toPx()
        val w = size.width
        val h = size.height
        if (first || last) {
            // rounded outline stretched past the clipped edge so only the needed corners show
            val ext = r * 4
            val top = if (first) half else -ext
            val bottom = if (last) h - half else h + ext
            drawRoundRect(
                color = color,
                topLeft = Offset(half, top),
                size = Size(w - stroke, bottom - top),
                cornerRadius = CornerRadius(r),
                style = Stroke(stroke),
            )
        } else {
            drawLine(color, Offset(half, 0f), Offset(half, h), stroke)
            drawLine(color, Offset(w - half, 0f), Offset(w - half, h), stroke)
        }
        if (!first) drawLine(color, Offset(0f, half), Offset(w, half), stroke)
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val th = LocalTheme.current
    val sessions = vm.sessions
    val progress = vm.inProgress
    var confirmNew by remember { mutableStateOf(false) }

    // Formatting dates is the expensive part, so do it once per change, not once per frame.
    val monthCount = remember(sessions) {
        val thisMonth = Fmt.monthKey(System.currentTimeMillis())
        sessions.count { Fmt.monthKey(it.timestamp) == thisMonth }
    }

    // 12 weeks, oldest first: a block is filled when that week has at least one session.
    val weeks = remember(sessions) {
        val now = System.currentTimeMillis()
        (11 downTo 0).map { w ->
            val end = now - w * WEEK_MS
            val start = end - WEEK_MS
            sessions.any { it.timestamp in start until end }
        }
    }

    // a saved session being continued (after an accidental save) lives in the in-progress section
    val activeId = if (vm.continuing) vm.editingId else null

    // longest single hold per exercise (names compare ignoring case), best two shown on Home
    val bestHolds = remember(sessions) {
        val best = HashMap<String, Int>()
        sessions.forEach { sess ->
            sess.rows.filter { it.hold }.forEach { r ->
                val sec = r.reps.toDoubleOrNull()?.toInt() ?: 0
                val key = r.name.trim().lowercase()
                if (sec > (best[key] ?: 0)) best[key] = sec
            }
        }
        best.entries.sortedByDescending { it.value }.take(2).joinToString(" · ") { "${it.key} ${Fmt.holdClock(it.value)}" }
    }

    val rows = remember(sessions, progress, activeId) {
        buildList {
            val active = sessions.firstOrNull { it.id == activeId }
            if (active != null || progress != null) add(HomeRow.ProgressHeader)
            if (active != null) {
                add(HomeRow.Done(active, Fmt.dayLabel(active.timestamp), Fmt.summary(active.rows), Fmt.volumeCell(active.volume, active.holdSeconds), true))
            }
            if (progress != null) add(HomeRow.Progress(progress))
            if (sessions.isEmpty() && progress == null) add(HomeRow.Empty)
            var lastMonth = ""
            sessions.forEach { s ->
                if (s.id == activeId) return@forEach
                val key = Fmt.monthKey(s.timestamp)
                if (key != lastMonth) {
                    lastMonth = key
                    add(HomeRow.Month(Fmt.monthLabel(s.timestamp)))
                }
                add(HomeRow.Done(s, Fmt.dayLabel(s.timestamp), Fmt.summary(s.rows), Fmt.volumeCell(s.volume, s.holdSeconds), false))
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item(key = "prompt") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Prompt("sessions", Modifier.weight(1f))
                    TermButton("[theme]", th.amber, { vm.pickerOpen = true }, filled = false)
                }
            }

            item(key = "title") {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp)) {
                    Mono("> sessions", size = 28, weight = FontWeight.ExtraBold, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    Mono("// ${sessions.size} saved · $monthCount this month", color = th.muted)
                }
            }

            item(key = "weeks") {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 16.dp)) {
                    Mono("// last 12 weeks", color = th.muted)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BlockBar(weeks, th.green)
                        Spacer(Modifier.width(8.dp))
                        Mono("${weeks.count { it }}/12", size = 14, weight = FontWeight.Bold, maxLines = 1)
                    }
                    if (bestHolds.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Mono("// best holds · $bestHolds", color = th.amber)
                    }
                }
            }

            item(key = "head") {
                Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().tableEdges(th.border, first = true, last = false)) {
                    Row(Modifier.fillMaxWidth().height(38.dp).background(th.panel)) {
                        HeadCell("date", Modifier.width(96.dp))
                        VLine()
                        HeadCell("exercises", Modifier.weight(1f))
                        VLine()
                        HeadCell("volume", Modifier.width(84.dp), end = true)
                    }
                }
            }

            itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                Box(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .tableEdges(th.border, first = false, last = index == rows.lastIndex),
                ) {
                    when (row) {
                        is HomeRow.Empty -> Box(Modifier.fillMaxWidth().padding(16.dp)) {
                            Mono("// no sessions yet — tap [+] new session", color = th.muted)
                        }

                        is HomeRow.Month -> SectionLabel("// ${row.label}", th.amber)
                        is HomeRow.ProgressHeader -> SectionLabel("// in progress", th.green)
                        is HomeRow.Progress -> SessionLine(
                            date = Fmt.dayLabel(row.draft.timestamp),
                            dateColor = th.green,
                            exercises = row.draft.rows.filter { it.name.isNotBlank() }
                                .let { if (it.isEmpty()) "(unsaved)" else Fmt.summary(it) },
                            volume = Fmt.volumeCell(row.draft.volume, row.draft.holdSeconds),
                            clickLabel = "Resume unsaved session",
                            onClick = { vm.resume() },
                        )

                        is HomeRow.Done -> SessionLine(
                            date = row.date,
                            dateColor = if (row.active) th.green else th.blue,
                            exercises = row.exercises,
                            volume = row.volume,
                            clickLabel = if (row.active) "Continue session" else null,
                            onClick = { vm.edit(row.session) },
                        )
                    }
                }
            }

        }

        TermButton(
            "[+] new session",
            th.green,
            { if (vm.hasDraft) confirmNew = true else vm.newSession() },
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            height = 56.dp,
        )
    }

    if (vm.pickerOpen) ThemePicker(vm)

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            containerColor = th.panel,
            title = { Mono("> discard unsaved session?", size = 16, weight = FontWeight.Bold) },
            text = { Mono("// or tap the in-progress row to resume it", color = th.muted) },
            confirmButton = {
                TermButton("[discard]", th.red, { confirmNew = false; vm.newSession() }, filled = false)
            },
            dismissButton = {
                TermButton("[resume]", th.green, { confirmNew = false; vm.resume() }, filled = false)
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Box(Modifier.fillMaxWidth().background(LocalTheme.current.surf).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Mono(text, color = color, size = 12, weight = FontWeight.Bold)
    }
}

@Composable
private fun SessionLine(
    date: String,
    dateColor: Color,
    exercises: String,
    volume: String,
    clickLabel: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.width(96.dp).fillMaxHeight().padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Mono(date, color = dateColor, weight = FontWeight.Bold, maxLines = 1)
        }
        VLine()
        Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Mono(exercises, maxLines = 1)
        }
        VLine()
        Box(Modifier.width(84.dp).fillMaxHeight().padding(horizontal = 10.dp), contentAlignment = Alignment.CenterEnd) {
            Mono(volume, weight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePicker(vm: AppViewModel) {
    val th = LocalTheme.current
    ModalBottomSheet(
        onDismissRequest = { vm.pickerOpen = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = th.panel,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    ) {
        // The sheet lives in its own window; match its nav bar to the panel colour.
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.navigationBarColor = th.panel.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !th.dark
        }
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Prompt("theme set ${th.name}", Modifier.weight(1f))
                TermButton("[done]", th.muted, { vm.pickerOpen = false }, filled = false)
            }
            ThemeGroup(vm, "// dark", dark = true)
            Spacer(Modifier.height(8.dp))
            ThemeGroup(vm, "// light", dark = false)
        }
    }
}

@Composable
private fun ThemeGroup(vm: AppViewModel, title: String, dark: Boolean) {
    val th = LocalTheme.current
    Mono(title, color = th.muted, modifier = Modifier.padding(vertical = 6.dp))
    THEMES.forEachIndexed { i, t ->
        if (t.dark != dark) return@forEachIndexed
        val selected = i == vm.themeIdx
        HLine()
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(role = Role.RadioButton) { vm.setTheme(i) }
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Mono(
                if (selected) "[✓]" else "[ ]",
                color = th.green,
                size = 14,
                modifier = Modifier.width(36.dp),
            )
            Mono(
                t.name,
                modifier = Modifier.weight(1f),
                size = 14,
                weight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(t.bg, t.green, t.blue, t.amber).forEach { c ->
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c)
                            .border(1.dp, th.border, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}
