package com.example.aiassistant.ui.theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AiOverlayColorsTest {
    @Test
    fun lightPaletteOverlayMatchesSurfaceFeedbackStyle() {
        assertEquals(LightAiPalette.surface, LightAiPalette.overlayBackground)
        assertEquals(LightAiPalette.textPrimary, LightAiPalette.overlayText)
    }

    @Test
    fun darkPaletteOverlayMatchesSurfaceFeedbackStyle() {
        assertEquals(DarkAiPalette.surface, DarkAiPalette.overlayBackground)
        assertEquals(DarkAiPalette.textPrimary, DarkAiPalette.overlayText)
    }

    @Test
    fun lightAndDarkOverlayBackgroundsStillDifferThroughTheme() {
        assertNotEquals(LightAiPalette.surface, DarkAiPalette.surface)
    }
}
