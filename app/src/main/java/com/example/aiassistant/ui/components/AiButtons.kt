package com.example.aiassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.aiassistant.ui.theme.AiColors
import com.example.aiassistant.ui.theme.AiRadius
import com.example.aiassistant.ui.theme.AiSpacing

@Composable
fun RoundIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(56.dp),
        shape = RoundedCornerShape(AiRadius.Pill),
        color = AiColors.Surface,
        border = BorderStroke(1.dp, AiColors.BorderSoft),
        shadowElevation = 8.dp
    ) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
        ) {
            Icon(imageVector = imageVector, contentDescription = contentDescription, tint = AiColors.TextPrimary)
        }
    }
}

@Composable
fun PillActions(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .background(AiColors.Surface.copy(alpha = 0.88f), RoundedCornerShape(AiRadius.Pill)),
        horizontalArrangement = Arrangement.spacedBy(AiSpacing.Sm)
    ) {
        content()
    }
}

@Composable
fun CapsuleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = if (checked) AiColors.Success else Color.White
    val thumbOffset = if (checked) 12.dp else (-12).dp

    Box(
        modifier = modifier
            .size(height = 32.dp, width = 52.dp)
            .clip(RoundedCornerShape(AiRadius.Pill))
            .background(trackColor)
            .border(1.dp, if (checked) AiColors.Success else AiColors.BorderSoft, RoundedCornerShape(AiRadius.Pill))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .background(AiColors.Surface, RoundedCornerShape(AiRadius.Pill))
                .border(1.dp, AiColors.BorderSoft, RoundedCornerShape(AiRadius.Pill))
        )
    }
}

@Composable
fun AiPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(AiRadius.Large),
        colors = ButtonDefaults.buttonColors(containerColor = AiColors.Accent, contentColor = AiColors.AccentOn)
    ) {
        androidx.compose.material3.Text(text)
    }
}

@Composable
fun AiSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(AiRadius.Large),
        colors = ButtonDefaults.buttonColors(containerColor = AiColors.Surface.copy(alpha = 0.74f), contentColor = AiColors.TextPrimary)
    ) {
        androidx.compose.material3.Text(text)
    }
}
