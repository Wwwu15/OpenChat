package com.example.aiassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiassistant.ui.theme.AiColors
import com.example.aiassistant.ui.theme.AiSpacing

@Composable
fun AiScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AiColors.Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(AiColors.TopChrome)
        ) {
            topBar()
        }
        Box(modifier = Modifier.weight(1f).background(AiColors.Background)) {
            content()
        }
        Box(modifier = Modifier.background(AiColors.BottomChrome)) {
            bottomBar()
        }
    }
}

@Composable
fun AiStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(horizontal = AiSpacing.Xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("9:41", color = AiColors.TextSecondary.copy(alpha = 0.72f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("5G  100%", color = AiColors.TextSecondary.copy(alpha = 0.72f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
