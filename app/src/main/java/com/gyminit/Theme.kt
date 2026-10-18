package com.gyminit

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val MonoFont = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_extrabold, FontWeight.ExtraBold),
)

data class GymTheme(
    val name: String,
    val dark: Boolean,
    val bg: Color,
    val panel: Color,
    val surf: Color,
    val border: Color,
    val fg: Color,
    val muted: Color,
    val green: Color,
    val blue: Color,
    val amber: Color,
    val red: Color,
)

private fun theme(
    name: String,
    dark: Boolean,
    bg: Long,
    panel: Long,
    surf: Long,
    border: Long,
    fg: Long,
    muted: Long,
    green: Long,
    blue: Long,
    amber: Long,
    red: Long,
): GymTheme {
    fun c(hex: Long) = Color(0xFF000000 or hex)
    return GymTheme(
        name, dark, c(bg), c(panel), c(surf), c(border), c(fg), c(muted),
        c(green), c(blue), c(amber), c(red),
    )
}

val THEMES = listOf(
    theme("tokyo night", true, 0x1A1B26, 0x16161E, 0x1F2335, 0x2F334D, 0xC0CAF5, 0x8C94BB, 0x9ECE6A, 0x7AA2F7, 0xE0AF68, 0xF7768E),
    theme("dracula", true, 0x282A36, 0x21222C, 0x343746, 0x44475A, 0xF8F8F2, 0xA4ABCB, 0x50FA7B, 0xBD93F9, 0xFFB86C, 0xFF5555),
    theme("nord", true, 0x2E3440, 0x272C36, 0x3B4252, 0x4C566A, 0xECEFF4, 0xA3ADBF, 0xA3BE8C, 0x88C0D0, 0xEBCB8B, 0xD08770),
    theme("gruvbox dark", true, 0x282828, 0x1D2021, 0x3C3836, 0x504945, 0xEBDBB2, 0xA89984, 0xB8BB26, 0x83A598, 0xFABD2F, 0xFB4934),
    theme("catppuccin mocha", true, 0x1E1E2E, 0x181825, 0x313244, 0x45475A, 0xCDD6F4, 0xA6ADC8, 0xA6E3A1, 0x89B4FA, 0xF9E2AF, 0xF38BA8),
    theme("solarized light", false, 0xFDF6E3, 0xEEE8D5, 0xF5EEDB, 0xD6CFB8, 0x586E75, 0x657B83, 0x6B7A00, 0x1E78B8, 0x8F6B00, 0xB5342A),
    theme("github light", false, 0xFFFFFF, 0xF6F8FA, 0xEEF1F4, 0xD0D7DE, 0x1F2328, 0x59636E, 0x1A7F37, 0x0969DA, 0x9A6700, 0xCF222E),
    theme("catppuccin latte", false, 0xEFF1F5, 0xE6E9EF, 0xDCE0E8, 0xCCD0DA, 0x4C4F69, 0x5C5F77, 0x2F7D1F, 0x1E66F5, 0xA5600A, 0xC00A30),
)

val LocalTheme = staticCompositionLocalOf { THEMES[0] }
