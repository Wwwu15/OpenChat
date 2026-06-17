package com.example.aiassistant.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AiChromeColorsTest {
    @Test
    fun lightPaletteTopAndBottomChromeMatchBackground() {
        assertEquals(LightAiPalette.background, LightAiPalette.topChrome)
        assertEquals(LightAiPalette.background, LightAiPalette.bottomChrome)
    }

    @Test
    fun darkPaletteTopAndBottomChromeMatchBackground() {
        assertEquals(DarkAiPalette.background, DarkAiPalette.topChrome)
        assertEquals(DarkAiPalette.background, DarkAiPalette.bottomChrome)
    }
}
