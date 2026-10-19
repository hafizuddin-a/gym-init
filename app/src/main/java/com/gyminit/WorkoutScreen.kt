package com.gyminit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Digits with at most one '.' and two decimals (weights); digits only for reps. */
private fun cleanNumber(v: String, decimal: Boolean): String {
    val s = v.replace(',', '.').filter { it.isDigit() || (decimal && it == '.') }
    val dot = s.indexOf('.')
    if (dot < 0) return s
    val whole = s.substring(0, dot)
    val frac = s.substring(dot + 1).replace(".", "").take(2)
    return "$whole.$frac"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(vm: AppViewModel) {
    val th = LocalTheme.current
    var confirmDelete by remember { mutableStateOf(false) }
    val editing = vm.editingId != null
    // a saved session being continued gets the normal workout layout (tools, no delete)
    val continued = editing && vm.continuing
    val plainEdit = editing && !continued
    val keyboardOpen = WindowInsets.isImeVisible

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton { vm.goHome() }
            Prompt(if (continued) "session --resume" else if (editing) "session --edit" else "session --new", Modifier.weight(1f))
            TermButton("[save]", th.amber, { vm.save() }, filled = false)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
                Mono(
                    if (continued) "> current session" else if (editing) "> edit session" else "> today's session",
                    size = 26,
                    weight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Mono("// ${Fmt.longDate(vm.sessionTs)}", color = th.muted)
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
                    HeadCell("#", Modifier.width(32.dp))
                    VLine()
                    HeadCell("exercise", Modifier.weight(1f))
                    VLine()
                    HeadCell("weight", Modifier.width(72.dp), pad = 8.dp)
                    VLine()
                    HeadCell("reps/sec", Modifier.width(80.dp), size = 11, pad = 8.dp)
                    VLine()
                    Box(Modifier.width(32.dp))
                }
                vm.draft.forEachIndexed { index, row ->
                    val focus = remember(row.id) { FocusRequester() }
                    val keyboard = LocalSoftwareKeyboardController.current
                    // a row added from the stopwatch/timer gets the cursor in its name field
                    LaunchedEffect(vm.focusRowId) {
                        if (vm.focusRowId == row.id) {
                            delay(80)
                            try {
                                focus.requestFocus()
                                keyboard?.show()
                            } catch (_: Exception) {
                            }
                            vm.clearFocusRequest()
                        }
                    }
                    HLine()
                    Row(Modifier.fillMaxWidth().height(48.dp)) {
                        Box(
                            Modifier.width(32.dp).fillMaxHeight().background(th.panel),
                            contentAlignment = Alignment.Center,
                        ) { Mono("${index + 1}", color = th.muted, size = 12) }
                        VLine()
                        Box(
                            Modifier.weight(1f).fillMaxHeight().padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            TermField(
                                value = row.name,
                                onChange = { vm.updateRow(index, row.copy(name = it)) },
                                label = "Exercise ${index + 1} name",
                                hint = "exercise",
                                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            )
                        }
                        VLine()
                        Row(
                            Modifier.width(72.dp).fillMaxHeight().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TermField(
                                value = row.weight,
                                onChange = { vm.updateRow(index, row.copy(weight = cleanNumber(it, decimal = true))) },
                                label = "Exercise ${index + 1} weight in kilograms",
                                hint = row.hintWeight.ifEmpty { "0" },
                                numeric = true,
                                color = th.blue,
                                modifier = Modifier.weight(1f),
                            )
                            Mono("kg", color = th.muted, size = 12)
                        }
                        VLine()
                        Row(
                            Modifier.width(80.dp).fillMaxHeight().padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val tint = if (row.hold) th.amber else th.blue
                            TermField(
                                value = row.reps,
                                onChange = { vm.updateRow(index, row.copy(reps = cleanNumber(it, decimal = false))) },
                                label = "Exercise ${index + 1} ${if (row.hold) "seconds" else "reps"}",
                                hint = "0",
                                numeric = true,
                                integer = true,
                                color = tint,
                                modifier = Modifier.weight(1f),
                            )
                            // the unit sits on the same line as the number, like "kg"; tap it to switch
                            Box(
                                Modifier
                                    .width(36.dp)
                                    .fillMaxHeight()
                                    .clickable(
                                        onClickLabel = if (row.hold) "Switch to reps" else "Switch to seconds",
                                        role = Role.Button,
                                    ) { vm.updateRow(index, row.copy(hold = !row.hold)) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Mono(if (row.hold) "sec" else "reps", color = if (row.hold) th.amber else th.muted, size = 12, underline = true)
                            }
                        }
                        VLine()
                        Box(
                            Modifier
                                .width(32.dp)
                                .fillMaxHeight()
                                .clickable(onClickLabel = "Remove exercise ${index + 1}", role = Role.Button) {
                                    vm.removeRow(index)
                                },
                            contentAlignment = Alignment.Center,
                        ) { Mono("×", color = th.muted, size = 16) }
                    }
                }
                HLine()
                Row(Modifier.fillMaxWidth().height(48.dp)) {
                    Box(
                        Modifier.width(32.dp).fillMaxHeight().background(th.panel),
                        contentAlignment = Alignment.Center,
                    ) { Mono("${vm.draft.size + 1}", color = th.muted, size = 12) }
                    VLine()
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(role = Role.Button) { vm.addRow() }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { Mono("[+] add exercise", color = th.green, size = 13, weight = FontWeight.Bold) }
                }
            }

            val volume = vm.draftVolume
            val best = vm.bestVolume
            val ratio = if (maxOf(volume, best) > 0) (volume / maxOf(volume, best)).toFloat() else 0f
            val holds = vm.draftHoldSeconds
            val bestHold = vm.bestHoldSeconds
            val holdRatio = if (maxOf(holds, bestHold) > 0) holds / maxOf(holds, bestHold).toFloat() else 0f
            val lifts = vm.draft.count { it.name.isNotBlank() && !it.hold }
            val holdRows = vm.draft.count { it.hold && (it.name.isNotBlank() || it.reps.isNotBlank()) }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)) {
                Mono("// volume", color = th.muted)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BlockBar(ratio, 16, th.green)
                    Spacer(Modifier.width(8.dp))
                    Mono("${Fmt.kg(volume)} kg", size = 14, weight = FontWeight.Bold, maxLines = 1)
                }
                if (holds > 0) {
                    Spacer(Modifier.height(10.dp))
                    Mono("// holds", color = th.muted)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BlockBar(holdRatio, 16, th.amber)
                        Spacer(Modifier.width(8.dp))
                        Mono("$holds s", size = 14, weight = FontWeight.Bold, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(6.dp))
                val holdText = if (holdRows > 0) " · $holdRows hold${if (holdRows == 1) "" else "s"}" else ""
                Mono("// $lifts exercise${if (lifts == 1) "" else "s"}$holdText", color = th.muted)
                if (best > 0) Mono("// best session: ${Fmt.kg(best)} kg", color = th.muted)
            }
        }

        if (!keyboardOpen) {
            if (plainEdit) {
                Mono("// danger", color = th.muted, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 4.dp))
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                ) {
                    TermButton("[del]", th.red, { confirmDelete = true }, Modifier.weight(1f), height = 56.dp)
                    TermButton("[resume]", th.green, { vm.continueSession() }, Modifier.weight(1f), height = 56.dp)
                }
            } else {
                Mono("// tools", color = th.muted, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 4.dp))
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                ) {
                    ToolButton(
                        "[stopwatch]",
                        Fmt.clock(vm.swElapsed),
                        { vm.goStopwatch() },
                        Modifier.weight(1f),
                    )
                    ToolButton(
                        "[timer]",
                        Fmt.clock(vm.timerDisplayMs),
                        { vm.goTimer() },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = th.panel,
            title = { Mono("> delete session?", size = 16, weight = FontWeight.Bold) },
            text = { Mono("// this cannot be undone", color = th.muted) },
            confirmButton = {
                TermButton("[delete]", th.red, { confirmDelete = false; vm.deleteCurrent() }, filled = false)
            },
            dismissButton = {
                TermButton("[cancel]", th.muted, { confirmDelete = false }, filled = false)
            },
        )
    }
}
