package com.example.aiassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.aiassistant.R
import com.example.aiassistant.ui.theme.AiColors
import com.example.aiassistant.ui.theme.AiRadius
import com.example.aiassistant.ui.theme.AiSpacing

@Composable
fun AiComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onAttach: () -> Unit,
    isReceiving: Boolean,
    onStopReceiving: () -> Unit,
    onSend: () -> Unit,
    pendingAttachments: List<PendingAttachmentItem> = emptyList(),
    onRemovePendingAttachment: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = AiSpacing.Lg, vertical = AiSpacing.Xl)
            .background(AiColors.Surface.copy(alpha = 0.92f), RoundedCornerShape(30.dp))
            .border(1.dp, AiColors.BorderSoft, RoundedCornerShape(30.dp))
            .padding(AiSpacing.Md)
            .testTag("ai_composer_container"),
        verticalArrangement = Arrangement.spacedBy(AiSpacing.Sm)
    ) {
        if (pendingAttachments.isNotEmpty()) {
            ComposerPendingAttachments(
                attachments = pendingAttachments,
                onRemoveAttachment = onRemovePendingAttachment,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.composer_placeholder), color = AiColors.Muted) },
                minLines = 2,
                maxLines = 5,
                singleLine = false,
                leadingIcon = {
                    IconButton(
                        onClick = onAttach,
                        modifier = Modifier
                            .size(46.dp)
                            .background(AiColors.CardSurface, RoundedCornerShape(AiRadius.Pill))
                            .border(1.dp, AiColors.BorderSoft, RoundedCornerShape(AiRadius.Pill))
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add_attachment),
                            tint = AiColors.TextPrimary
                        )
                    }
                },
                trailingIcon = {
                    ComposerPrimaryActionButton(
                        isReceiving = isReceiving,
                        enabled = value.isNotBlank() || pendingAttachments.isNotEmpty(),
                        onStopReceiving = onStopReceiving,
                        onSend = onSend
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun ComposerPrimaryActionButton(
    isReceiving: Boolean,
    enabled: Boolean,
    onStopReceiving: () -> Unit,
    onSend: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(
                if (isReceiving) AiColors.Surface else AiColors.Accent.copy(alpha = if (enabled) 1f else 0.38f),
                RoundedCornerShape(AiRadius.Pill)
            )
            .border(1.dp, AiColors.BorderSoft, RoundedCornerShape(AiRadius.Pill))
            .testTag("ai_composer_primary_action"),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = if (isReceiving) onStopReceiving else onSend,
            enabled = if (isReceiving) true else enabled,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = if (isReceiving) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward,
                contentDescription = stringResource(if (isReceiving) R.string.stop_output else R.string.send),
                tint = if (isReceiving) AiColors.TextPrimary else AiColors.AccentOn
            )
        }
    }
}
