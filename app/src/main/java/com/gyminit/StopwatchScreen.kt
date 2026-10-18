package com.gyminit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StopwatchScreen(vm: AppViewModel) {
    val th = LocalTheme.current
    val elapsed = vm.swElapsed
    val laps = vm.laps.toList()
    val lapTimes = laps.mapIndexed { i, total -> total - (if (i > 0) laps[i - 1] else 0L) }
    val fastest = if (lapTimes.size >= 2) lapTimes.min() else -1L
    val slowest = if (lapTimes.size >= 2) lapTimes.max() else -1L
    val currentLap = elapsed - (laps.lastOrNull() ?: 0L)
    val status = when {
        vm.swRunning -> "running"
        elapsed > 0 -> "paused"
        else -> "ready"
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton { vm.goWorkout() }
            Prompt("stopwatch", Modifier.weight(1f))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Mono("> stopwatch", size = 26, weight = FontWeight.ExtraBold, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Mono("// $status · ${laps.size} lap${if (laps.size == 1) "" else "s"}", color = th.muted)
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
                MonoRich(
                    buildAnnotatedString {
                        append(Fmt.clock(elapsed))
                        withStyle(SpanStyle(color = th.blue, fontSize = 32.sp)) { append(Fmt.centis(elapsed)) }
                    },
                    size = 52,
                    weight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.height(12.dp))
                val minuteRatio = ((elapsed % 60_000L) / 60_000f)
                Mono(Fmt.bar(minuteRatio, 20), color = th.green, size = 14, maxLines = 1)
                Spacer(Modifier.height(12.dp))
                Mono("// lap ${laps.size + 1} · ${Fmt.stopwatch(currentLap)}", color = th.muted, maxLines = 1)
            }

            if (laps.isNotEmpty()) {
                Column(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        .fillMaxWidth()
                        .clip(shape)
                        .border(1.dp, th.border, shape),
                ) {
                    Row(Modifier.fillMaxWidth().height(38.dp).background(th.panel)) {
                        HeadCell("#", Modifier.width(40.dp))
                        VLine()
                        HeadCell("lap", Modifier.weight(1f))
                        VLine()
                        HeadCell("total", Modifier.weight(1f), end = true)
                    }
                    for (i in laps.indices.reversed()) {
                        val lapColor = when (lapTimes[i]) {
                            fastest -> th.green
                            slowest -> th.amber
                            else -> th.fg
                        }
                        HLine()
                        Row(Modifier.fillMaxWidth().height(44.dp)) {
                            Box(
                                Modifier.width(40.dp).fillMaxHeight().background(th.panel),
                                contentAlignment = Alignment.Center,
                            ) { Mono("${i + 1}", color = th.muted) }
                            VLine()
                            Box(
                                Modifier.weight(1f).fillMaxHeight().padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) { Mono(Fmt.stopwatch(lapTimes[i]), color = lapColor, weight = FontWeight.Bold, maxLines = 1) }
                            VLine()
                            Box(
                                Modifier.weight(1f).fillMaxHeight().padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) { Mono(Fmt.stopwatch(laps[i]), maxLines = 1, align = TextAlign.End) }
                        }
                    }
                }
                Mono(
                    "// green = fastest · amber = slowest",
                    color = th.muted,
                    modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 10.dp),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TermButton("[reset]", th.muted, { vm.resetStopwatch() }, Modifier.weight(1f), height = 56.dp)
            TermButton("[lap]", th.blue, { vm.lap() }, Modifier.weight(1f), height = 56.dp, enabled = vm.swRunning)
            TermButton(
                if (vm.swRunning) "[pause]" else "[start]",
                if (vm.swRunning) th.amber else th.green,
                { vm.toggleStopwatch() },
                Modifier.weight(1.3f),
                height = 56.dp,
            )
        }
    }
}
