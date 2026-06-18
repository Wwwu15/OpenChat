package com.example.aiassistant.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiassistant.R
import com.example.aiassistant.ui.theme.AiColors
import com.example.aiassistant.ui.theme.AiRadius
import com.example.aiassistant.ui.theme.AiSpacing

data class PendingAttachmentItem(
    val id: String,
    val type: PendingAttachmentType,
    val fileName: String,
    val status: PendingAttachmentStatus,
    val imageBitmap: Bitmap? = null
)

enum class PendingAttachmentType {
    Image,
    Document
}

enum class PendingAttachmentStatus {
    Ready,
    Parsing,
    UploadFailed
}

@Composable
fun ComposerPendingAttachments(
    attachments: List<PendingAttachmentItem>,
    onRemoveAttachment: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AiSpacing.Lg)
            .testTag("pending_attachment_strip"),
        horizontalArrangement = Arrangement.spacedBy(AiSpacing.Sm)
    ) {
        attachments.forEach { attachment ->
            when (attachment.type) {
                PendingAttachmentType.Image -> PendingImageAttachmentCard(
                    attachment = attachment,
                    onRemoveAttachment = onRemoveAttachment
                )
                PendingAttachmentType.Document -> PendingDocumentAttachmentCard(
                    attachment = attachment,
                    onRemoveAttachment = onRemoveAttachment
                )
            }
        }
    }
}

@Composable
private fun PendingImageAttachmentCard(
    attachment: PendingAttachmentItem,
    onRemoveAttachment: (String) -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(shape)
            .background(AiColors.CardSurface)
            .border(1.dp, AiColors.BorderSoft, shape)
            .testTag("pending_attachment_image_card_${attachment.id}")
    ) {
        if (attachment.imageBitmap != null) {
            Image(
                bitmap = attachment.imageBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pending_attachment_image_thumb_${attachment.id}")
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AiColors.SurfaceWarm)
                    .testTag("pending_attachment_image_thumb_${attachment.id}")
            )
        }

        RemoveAttachmentButton(
            attachmentId = attachment.id,
            containerColor = Color.Black.copy(alpha = 0.48f),
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            onRemoveAttachment(attachment.id)
        }
    }
}

@Composable
private fun PendingDocumentAttachmentCard(
    attachment: PendingAttachmentItem,
    onRemoveAttachment: (String) -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .width(240.dp)
            .clip(shape)
            .background(AiColors.CardSurface)
            .border(1.dp, AiColors.BorderSoft, shape)
            .padding(AiSpacing.Md)
            .testTag("pending_attachment_document_card_${attachment.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AiSpacing.Md)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AiColors.SurfaceWarm)
                .testTag("pending_attachment_document_icon_${attachment.id}")
        ) {
            Text(
                text = attachment.fileName.fileTypeLabel(),
                modifier = Modifier.align(Alignment.Center),
                color = AiColors.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.fileName,
                modifier = Modifier.testTag("pending_attachment_document_name_${attachment.id}"),
                color = AiColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = attachment.status.label(),
                modifier = Modifier.testTag("pending_attachment_document_status_${attachment.id}"),
                color = attachment.status.color(),
                fontSize = 12.sp
            )
        }

        RemoveAttachmentButton(
            attachmentId = attachment.id,
            containerColor = AiColors.SurfaceWarm,
            tint = AiColors.TextPrimary
        ) {
            onRemoveAttachment(attachment.id)
        }
    }
}

@Composable
private fun RemoveAttachmentButton(
    attachmentId: String,
    containerColor: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(30.dp)
            .background(containerColor, RoundedCornerShape(AiRadius.Pill))
            .testTag("pending_attachment_remove_$attachmentId")
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.remove_attachment),
            tint = tint
        )
    }
}

@Composable
private fun PendingAttachmentStatus.label(): String {
    return when (this) {
        PendingAttachmentStatus.Ready -> stringResource(R.string.pending_attachment_ready)
        PendingAttachmentStatus.Parsing -> stringResource(R.string.pending_attachment_parsing)
        PendingAttachmentStatus.UploadFailed -> stringResource(R.string.pending_attachment_upload_failed)
    }
}

@Composable
private fun PendingAttachmentStatus.color(): Color {
    return when (this) {
        PendingAttachmentStatus.Ready -> AiColors.Meta
        PendingAttachmentStatus.Parsing -> AiColors.Warn
        PendingAttachmentStatus.UploadFailed -> AiColors.Danger
    }
}

private fun String.fileTypeLabel(): String {
    val extension = substringAfterLast('.', "").trim()
    return extension.ifEmpty { "FILE" }.take(4).uppercase()
}
