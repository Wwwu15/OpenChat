package com.example.aiassistant.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AiColorPalette(
    val systemBar: Color,
    val background: Color,
    val surface: Color,
    val cardSurface: Color,
    val surfaceWarm: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val muted: Color,
    val meta: Color,
    val border: Color,
    val borderSoft: Color,
    val accent: Color,
    val accentOn: Color,
    val messageUser: Color,
    val messageAssistant: Color,
    val success: Color,
    val warn: Color,
    val danger: Color
) {
    val topChrome: Color get() = background
    val bottomChrome: Color get() = background
}

val LightAiPalette = AiColorPalette(
    systemBar = Color(0xFFF4F6F8),
    background = Color(0xFFF4F6F8),
    surface = Color.White,
    cardSurface = Color(0xFFFCFCFD),
    surfaceWarm = Color(0xFFEEF3F7),
    textPrimary = Color(0xFF101418),
    textSecondary = Color(0xFF2F3944),
    muted = Color(0xFF7B8794),
    meta = Color(0xFF8C98A4),
    border = Color(0xFFDDE3EA),
    borderSoft = Color(0xFFEDF1F5),
    accent = Color(0xFF111820),
    accentOn = Color.White,
    messageUser = Color.White,
    messageAssistant = Color.White.copy(alpha = 0.68f),
    success = Color(0xFF22C55E),
    warn = Color(0xFFFBBF24),
    danger = Color(0xFFFB7185)
)

val DarkAiPalette = AiColorPalette(
    systemBar = Color(0xFF101418),
    background = Color(0xFF101418),
    surface = Color(0xFF20272F),
    cardSurface = Color(0xFF20272F),
    surfaceWarm = Color(0xFF161F27),
    textPrimary = Color(0xFFEAF0F6),
    textSecondary = Color(0xFFC4CED8),
    muted = Color(0xFF8D9AA7),
    meta = Color(0xFF9AA7B5),
    border = Color(0xFF303A45),
    borderSoft = Color(0xFF2A333D),
    accent = Color(0xFFEAF0F6),
    accentOn = Color(0xFF101418),
    messageUser = Color(0xFF26313B),
    messageAssistant = Color(0xFF20272F),
    success = Color(0xFF4ADE80),
    warn = Color(0xFFFACC15),
    danger = Color(0xFFFB7185)
)

val LocalAiColors = staticCompositionLocalOf { LightAiPalette }

object AiColors {
    val SystemBar: Color @Composable get() = LocalAiColors.current.systemBar
    val Background: Color @Composable get() = LocalAiColors.current.background
    val TopChrome: Color @Composable get() = LocalAiColors.current.topChrome
    val BottomChrome: Color @Composable get() = LocalAiColors.current.bottomChrome
    val Surface: Color @Composable get() = LocalAiColors.current.surface
    val CardSurface: Color @Composable get() = LocalAiColors.current.cardSurface
    val SurfaceWarm: Color @Composable get() = LocalAiColors.current.surfaceWarm
    val TextPrimary: Color @Composable get() = LocalAiColors.current.textPrimary
    val TextSecondary: Color @Composable get() = LocalAiColors.current.textSecondary
    val Muted: Color @Composable get() = LocalAiColors.current.muted
    val Meta: Color @Composable get() = LocalAiColors.current.meta
    val Border: Color @Composable get() = LocalAiColors.current.border
    val BorderSoft: Color @Composable get() = LocalAiColors.current.borderSoft
    val Accent: Color @Composable get() = LocalAiColors.current.accent
    val AccentOn: Color @Composable get() = LocalAiColors.current.accentOn
    val MessageUser: Color @Composable get() = LocalAiColors.current.messageUser
    val MessageAssistant: Color @Composable get() = LocalAiColors.current.messageAssistant
    val Success: Color @Composable get() = LocalAiColors.current.success
    val Warn: Color @Composable get() = LocalAiColors.current.warn
    val Danger: Color @Composable get() = LocalAiColors.current.danger
}
