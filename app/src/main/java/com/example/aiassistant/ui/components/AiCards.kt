package com.example.aiassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiassistant.ui.theme.AiColors
import com.example.aiassistant.ui.theme.AiRadius
import com.example.aiassistant.ui.theme.AiSpacing
import com.mikepenz.markdown.m3.Markdown

@Composable
fun AiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val border = BorderStroke(1.dp, AiColors.BorderSoft)
    val cardContent: @Composable () -> Unit = {
        Box(Modifier.padding(AiSpacing.Lg)) {
            content()
        }
    }
    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = AiColors.CardSurface,
            contentColor = AiColors.TextPrimary,
            border = border,
            shadowElevation = 3.dp,
            tonalElevation = 0.dp,
            content = cardContent
        )
    } else {
        Surface(
            modifier = modifier,
            onClick = onClick,
            shape = shape,
            color = AiColors.CardSurface,
            contentColor = AiColors.TextPrimary,
            border = border,
            shadowElevation = 3.dp,
            tonalElevation = 0.dp,
            content = cardContent
        )
    }
}

@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    markdownRenderingEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .background(
                    if (isUser) AiColors.MessageUser else AiColors.MessageAssistant,
                    RoundedCornerShape(22.dp)
                )
                .border(1.dp, AiColors.BorderSoft, RoundedCornerShape(22.dp))
                .padding(horizontal = AiSpacing.Lg, vertical = AiSpacing.Md)
        ) {
            if (!isUser && markdownRenderingEnabled) {
                Markdown(content = text)
            } else {
                Text(
                    text = text,
                    color = AiColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = AiSpacing.Xl, bottom = AiSpacing.Md),
        color = AiColors.TextPrimary.copy(alpha = 0.58f),
        fontSize = 21.sp
    )
}

@Composable
fun StatusPill(text: String, tone: StatusTone = StatusTone.Success) {
    val color = when (tone) {
        StatusTone.Success -> AiColors.Success
        StatusTone.Warning -> AiColors.Warn
        StatusTone.Danger -> AiColors.Danger
    }
    Text(
        text = text,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(AiRadius.Pill))
            .padding(horizontal = AiSpacing.Md, vertical = 5.dp),
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
}

enum class StatusTone { Success, Warning, Danger }

@Composable
fun FloatingTextBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(AiRadius.Pill))
            .background(
                color = AiColors.OverlayBackground,
                shape = RoundedCornerShape(AiRadius.Pill)
            )
            .padding(horizontal = AiSpacing.Xl, vertical = AiSpacing.Md)
    ) {
        Text(
            text = text,
            color = AiColors.OverlayText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    AiCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AiSpacing.Lg)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AiColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = AiColors.TextPrimary.copy(alpha = 0.5f), fontSize = 13.sp)
            }
            trailing?.invoke()
        }
    }
}
