package com.example.aiassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.example.aiassistant.ui.theme.AiColors
import com.example.aiassistant.navigation.AppNavHost
import com.example.aiassistant.ui.theme.AndroidAIAssistantTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkModeEnabled by container.appPreferences.darkModeEnabled.collectAsState(initial = false)
            val markdownRenderingEnabled by container.appPreferences.markdownRenderingEnabled.collectAsState(initial = true)
            val scope = rememberCoroutineScope()
            AndroidAIAssistantTheme(darkTheme = darkModeEnabled) {
                val systemBarColor = AiColors.SystemBar
                LaunchedEffect(darkModeEnabled, systemBarColor) {
                    window.statusBarColor = systemBarColor.toArgb()
                    window.navigationBarColor = systemBarColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkModeEnabled
                        isAppearanceLightNavigationBars = !darkModeEnabled
                    }
                }
                AppNavHost(
                    container = container,
                    darkModeEnabled = darkModeEnabled,
                    markdownRenderingEnabled = markdownRenderingEnabled,
                    onToggleDarkMode = {
                        scope.launch {
                            container.appPreferences.toggleDarkModeEnabled()
                        }
                    }
                )
            }
        }
    }
}
