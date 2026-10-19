package com.gyminit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PRESETS = listOf(30, 60, 90, 120, 180, 300)

@Composable
fun TimerScreen(vm: AppViewModel) {
    val th = LocalTheme.current
    val remaining = vm.timerRemaining
    val ratio = if (vm.timerTotalMs > 0) remaining / vm.timerTotalMs.toFloat() else 0f
    val percent = Math.round(ratio.coerceIn(0f, 1f) * 100)
    val status = when {
        vm.timerDone -> "done"
        vm.timerRunning -> "running"
        remaining < vm.timerTotalMs -> "paused"
        else -> "ready"
    }
    val label = Fmt.clock(vm.timerDisplayMs)

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton { vm.goWorkout() }
            Prompt("timer", Modifier.weight(1f))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Mono("> timer", size = 26, weight = FontWeight.ExtraBold, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Mono("// count down to zero", color = th.muted)
            }

            val shape = RoundedCornerShape(4.dp)
            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(th.panel)
                    .border(1.dp, th.border, shape)
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Mono(label, size = 64, weight = FontWeight.ExtraBold, letterSpacing = (-2).sp, maxLines = 1)
                Spacer(Modifier.height(12.dp))
                val barColor = if (vm.timerDone) th.red else th.blue
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BlockBar(ratio, 16, barColor)
                    Spacer(Modifier.width(8.dp))
                    Mono("$percent%", color = barColor, size = 14, maxLines = 1)
                }
                Spacer(Modifier.height(12.dp))
                Mono("// $status · goal ${Fmt.clock(vm.restSec * 1000L)}", color = th.muted, maxLines = 1)
            }

            Mono("// presets", color = th.muted, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESETS.chunked(3).forEach { chunk ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunk.forEach { sec ->
                            val on = sec == vm.restSec
                            TermButton(
                                "[${sec / 60}:${String.format("%02d", sec % 60)}]",
                                if (on) th.blue else th.fg,
                                { vm.setRest(sec) },
                                Modifier.weight(1f),
                                height = 48.dp,
                                filled = on,
                            )
                        }
                    }
                }
            }

            Mono("// adjust", color = th.muted, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TermButton("[-15s]", th.red, { vm.adjustRest(-15) }, Modifier.weight(1f), height = 48.dp, filled = false)
                TermButton("[+15s]", th.green, { vm.adjustRest(15) }, Modifier.weight(1f), height = 48.dp, filled = false)
            }
        }

        if (vm.canAddTimer) {
            val seconds = vm.timerHeldSeconds
            TermButton(
                "[add ${seconds}s to exercises]",
                th.green,
                { vm.addTimerToExercises() },
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                height = 56.dp,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TermButton("[reset]", th.muted, { vm.resetTimer() }, Modifier.weight(1f), height = 56.dp)
            TermButton(
                if (vm.timerRunning) "[pause]" else "[start]",
                if (vm.timerRunning) th.amber else th.green,
                { vm.toggleTimer() },
                Modifier.weight(2f),
                height = 56.dp,
            )
        }
    }
}
