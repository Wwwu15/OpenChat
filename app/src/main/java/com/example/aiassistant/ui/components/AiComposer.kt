package com.example.aiassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = AiSpacing.Lg, vertical = AiSpacing.Xl)
            .background(AiColors.Surface.copy(alpha = 0.92f), RoundedCornerShape(30.dp))
            .padding(start = AiSpacing.Sm, end = AiSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttach) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_attachment), tint = AiColors.TextPrimary)
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.composer_placeholder), color = AiColors.Muted) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        IconButton(
            onClick = onStopReceiving,
            enabled = isReceiving,
            modifier = Modifier
                .size(46.dp)
                .background(AiColors.Surface, RoundedCornerShape(AiRadius.Pill))
                .border(1.dp, AiColors.BorderSoft, RoundedCornerShape(AiRadius.Pill))
        ) {
            Icon(
                Icons.Rounded.Stop,
                contentDescription = stringResource(R.string.stop_output),
                tint = if (isReceiving) AiColors.TextPrimary else AiColors.Muted.copy(alpha = 0.45f)
            )
        }
        Spacer(modifier = Modifier.width(AiSpacing.Sm))
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(46.dp)
                .background(AiColors.Accent, RoundedCornerShape(AiRadius.Pill))
        ) {
            Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.send), tint = AiColors.AccentOn)
        }
    }
}
