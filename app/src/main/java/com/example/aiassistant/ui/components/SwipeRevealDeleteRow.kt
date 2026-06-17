package com.example.aiassistant.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.aiassistant.ui.theme.AiColors
import kotlin.math.roundToInt

@Composable
fun SwipeRevealDeleteRow(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val revealWidthPx = with(density) { 92.dp.toPx() }
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetPx, label = "swipeRevealOffset")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AiColors.Danger.copy(alpha = 0.92f), RoundedCornerShape(22.dp))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(92.dp)
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                offsetPx = 0f
                onDelete()
            }) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = AiColors.AccentOn)
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            offsetPx = (offsetPx + dragAmount).coerceIn(-revealWidthPx, 0f)
                        },
                        onDragEnd = {
                            offsetPx = if (offsetPx <= -revealWidthPx / 2f) -revealWidthPx else 0f
                        }
                    )
                }
        ) {
            content()
        }
    }
}
