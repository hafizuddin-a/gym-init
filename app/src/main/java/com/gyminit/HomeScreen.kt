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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

@Composable
fun HomeScreen(vm: AppViewModel) {
    val th = LocalTheme.current
    val sessions = vm.sessions
    val now = System.currentTimeMillis()
    val thisMonth = Fmt.monthKey(now)
    val monthCount = sessions.count { Fmt.monthKey(it.timestamp) == thisMonth }

    // 12 weeks, oldest first: a block is filled when that week has at least one session.
    val weeks = (11 downTo 0).map { w ->
        val end = now - w * WEEK_MS
        val start = end - WEEK_MS
        sessions.any { it.timestamp in start until end }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Prompt("sessions", Modifier.weight(1f))
                TermButton("[theme]", th.amber, { vm.pickerOpen = true }, filled = false)
            }

            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp)) {
                Mono("> sessions", size = 28, weight = FontWeight.ExtraBold, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Mono("// ${sessions.size} saved · $monthCount this month", color = th.muted)
            }

            val shape = RoundedCornerShape(4.dp)
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .border(1.dp, th.border, shape),
            ) {
                Row(Modifier.fillMaxWidth().height(38.dp).background(th.panel)) {
                    HeadCell("date", Modifier.width(96.dp))
                    VLine()
                    HeadCell("exercises", Modifier.weight(1f))
                    VLine()
                    HeadCell("volume", Modifier.width(84.dp), end = true)
                }
                if (sessions.isEmpty()) {
                    HLine()
                    Box(Modifier.fillMaxWidth().padding(16.dp)) {
                        Mono("// no sessions yet — tap [+] new session", color = th.muted)
                    }
                }
                var lastMonth = ""
                sessions.forEach { s ->
                    val key = Fmt.monthKey(s.timestamp)
                    if (key != lastMonth) {
                        lastMonth = key
                        HLine()
                        Box(
                            Modifier.fillMaxWidth().background(th.surf).padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Mono("// ${Fmt.monthLabel(s.timestamp)}", color = th.amber, size = 12, weight = FontWeight.Bold)
                        }
                    }
                    HLine()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable(role = Role.Button) { vm.edit(s) },
                    ) {
                        Box(
                            Modifier.width(96.dp).fillMaxHeight().padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { Mono(Fmt.dayLabel(s.timestamp), color = th.blue, weight = FontWeight.Bold, maxLines = 1) }
                        VLine()
                        Box(
                            Modifier.weight(1f).fillMaxHeight().padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { Mono(Fmt.summary(s.rows), maxLines = 1) }
                        VLine()
                        Box(
                            Modifier.width(84.dp).fillMaxHeight().padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) { Mono(Fmt.kg(s.volume), weight = FontWeight.Bold, maxLines = 1) }
                    }
                }
            }

            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)) {
                Mono("// last 12 weeks", color = th.muted)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BlockBar(weeks, th.green)
                    Spacer(Modifier.width(8.dp))
                    Mono("${weeks.count { it }}/12", size = 14, weight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        TermButton(
            "[+] new session",
            th.green,
            { vm.newSession() },
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            height = 56.dp,
        )
    }

    if (vm.pickerOpen) ThemePicker(vm)
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
