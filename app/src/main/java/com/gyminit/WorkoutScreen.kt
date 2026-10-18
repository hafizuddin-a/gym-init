package com.gyminit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private fun cleanNumber(v: String): String = v.replace(',', '.').filter { it.isDigit() || it == '.' }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(vm: AppViewModel) {
    val th = LocalTheme.current
    var confirmDelete by remember { mutableStateOf(false) }
    val editing = vm.editingId != null
    val keyboardOpen = WindowInsets.isImeVisible

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton { vm.goHome() }
            Prompt(if (editing) "session --edit" else "session --new", Modifier.weight(1f))
            if (editing) {
                TermButton("[del]", th.red, { confirmDelete = true }, filled = false)
                Spacer(Modifier.width(8.dp))
            }
            TermButton("[save]", th.amber, { vm.save() }, filled = false)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
                Mono(
                    if (editing) "> edit session" else "> today's session",
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
                    HeadCell("weight", Modifier.width(84.dp))
                    VLine()
                    HeadCell("reps", Modifier.width(52.dp))
                    VLine()
                    Box(Modifier.width(32.dp))
                }
                vm.draft.forEachIndexed { index, row ->
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
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        VLine()
                        Row(
                            Modifier.width(84.dp).fillMaxHeight().padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TermField(
                                value = row.weight,
                                onChange = { vm.updateRow(index, row.copy(weight = cleanNumber(it))) },
                                label = "Exercise ${index + 1} weight in kilograms",
                                hint = row.hintWeight.ifEmpty { "0" },
                                numeric = true,
                                color = th.blue,
                                modifier = Modifier.weight(1f),
                            )
                            Mono("kg", color = th.muted, size = 12)
                        }
                        VLine()
                        Box(
                            Modifier.width(52.dp).fillMaxHeight().padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            TermField(
                                value = row.reps,
                                onChange = { vm.updateRow(index, row.copy(reps = cleanNumber(it))) },
                                label = "Exercise ${index + 1} reps",
                                hint = row.hintReps.ifEmpty { "0" },
                                numeric = true,
                                color = th.blue,
                                modifier = Modifier.fillMaxWidth(),
                            )
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
            val count = vm.draft.count { it.name.isNotBlank() }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)) {
                Mono("// volume", color = th.muted)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BlockBar(ratio, 16, th.green)
                    Spacer(Modifier.width(8.dp))
                    Mono("${Fmt.kg(volume)} kg", size = 14, weight = FontWeight.Bold, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))
                val bestText = if (best > 0) " · best session: ${Fmt.kg(best)} kg" else ""
                Mono("// $count exercise${if (count == 1) "" else "s"}$bestText", color = th.muted)
            }
        }

        if (!keyboardOpen) {
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
                    Fmt.clock(vm.timerRemaining),
                    { vm.goTimer() },
                    Modifier.weight(1f),
                )
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
