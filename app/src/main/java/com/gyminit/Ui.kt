package com.gyminit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Mono(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalTheme.current.fg,
    size: Int = 13,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontFamily = MonoFont,
        fontWeight = weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        letterSpacing = letterSpacing,
    )
}

@Composable
fun MonoRich(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = LocalTheme.current.fg,
    size: Int = 13,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontFamily = MonoFont,
        fontWeight = weight,
        maxLines = 1,
        letterSpacing = letterSpacing,
    )
}

/** `you@gym $ command` line shown at the top of every screen. */
@Composable
fun Prompt(command: String, modifier: Modifier = Modifier) {
    val th = LocalTheme.current
    MonoRich(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = th.green)) { append("you@gym") }
            append(" $ $command")
        },
        modifier = modifier,
        color = th.muted,
    )
}

@Composable
fun TermButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    val th = LocalTheme.current
    val shape = RoundedCornerShape(4.dp)
    val tint = if (enabled) color else color.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (filled) th.surf else Color.Transparent)
            .border(1.dp, tint, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Mono(label, color = tint, size = 14, weight = FontWeight.Bold, maxLines = 1)
    }
}

/** Button with a label on the left and a live value on the right. */
@Composable
fun ToolButton(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val th = LocalTheme.current
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(th.surf)
            .border(1.dp, th.blue, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Mono(label, color = th.blue, size = 14, weight = FontWeight.Bold, maxLines = 1)
        Mono(value, color = th.muted, size = 12, maxLines = 1)
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    val th = LocalTheme.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = "Back" }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Mono("<", color = th.muted, size = 18, weight = FontWeight.Bold)
    }
}

@Composable
fun VLine() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(LocalTheme.current.border))
}

@Composable
fun HLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalTheme.current.border))
}

/** Header cell of a spreadsheet-style table. */
@Composable
fun HeadCell(text: String, modifier: Modifier = Modifier, end: Boolean = false) {
    Box(
        modifier = modifier.fillMaxHeight().padding(horizontal = 10.dp),
        contentAlignment = if (end) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Mono(text, color = LocalTheme.current.muted, size = 12, weight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun TermField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    hint: String = "",
    numeric: Boolean = false,
    color: Color = LocalTheme.current.fg,
) {
    val th = LocalTheme.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.semantics { contentDescription = label },
        singleLine = true,
        textStyle = TextStyle(
            color = color,
            fontFamily = MonoFont,
            fontSize = 14.sp,
            fontWeight = if (numeric) FontWeight.Bold else FontWeight.Normal,
        ),
        cursorBrush = SolidColor(th.blue),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && hint.isNotEmpty()) {
                    Mono(hint, color = th.muted.copy(alpha = 0.6f), size = 14, maxLines = 1)
                }
                inner()
            }
        },
    )
}
