package com.example.aiassistant.ui.screens.chat

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiassistant.AppContainer
import com.example.aiassistant.R
import com.example.aiassistant.domain.attachments.AttachmentPayload
import com.example.aiassistant.domain.attachments.AttachmentReader
import com.example.aiassistant.domain.attachments.AttachmentType
import com.example.aiassistant.ui.components.AiCard
import com.example.aiassistant.ui.components.AiComposer
import com.example.aiassistant.ui.components.AiScaffold
import com.example.aiassistant.ui.components.AiSecondaryButton
import com.example.aiassistant.ui.components.MessageBubble
import com.example.aiassistant.ui.components.PendingAttachmentItem
import com.example.aiassistant.ui.components.PendingAttachmentStatus
import com.example.aiassistant.ui.components.PillActions
import com.example.aiassistant.ui.components.RoundIconButton
import com.example.aiassistant.ui.components.SettingsRow
import com.example.aiassistant.ui.theme.AiColors
import com.example.aiassistant.ui.theme.AiSpacing
import com.example.aiassistant.ui.viewModelFactory
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    container: AppContainer,
    conversationId: String?,
    markdownRenderingEnabled: Boolean,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val vm: ChatViewModel = viewModel(
        key = conversationId ?: "new-chat",
        factory = viewModelFactory { ChatViewModel(container.apiProfiles, container.conversations, container.chat, conversationId) }
    )
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val attachmentReader = remember(context) { AttachmentReader(context.contentResolver) }
    var showAttachments by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraImageUri
        if (success && uri != null) {
            vm.addAttachment(AttachmentPayload("camera.jpg", AttachmentType.Image, dataUrl = attachmentReader.readImageDataUrl(uri)))
        }
        cameraImageUri = null
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val name = attachmentReader.displayName(uri)
            vm.addAttachment(AttachmentPayload(name, AttachmentType.Image, dataUrl = attachmentReader.readImageDataUrl(uri)))
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = attachmentReader.displayName(uri)
            when (AttachmentType.fromName(name)) {
                AttachmentType.Text -> vm.addAttachment(AttachmentPayload(name, AttachmentType.Text, text = attachmentReader.readText(uri)))
                AttachmentType.Image -> vm.addAttachment(AttachmentPayload(name, AttachmentType.Image, dataUrl = attachmentReader.readImageDataUrl(uri)))
                AttachmentType.Document -> vm.addAttachment(
                    AttachmentPayload(
                        name = name,
                        type = AttachmentType.Document,
                        dataUrl = attachmentReader.readDocumentDataUrl(uri, name),
                        mimeType = attachmentReader.mimeType(uri, name)
                    )
                )
                AttachmentType.Unsupported -> vm.showAttachmentError(name)
            }
        }
    }

    val pendingAttachments = remember(state.attachments) {
        state.attachments.mapIndexed { index, attachment ->
            val id = pendingAttachmentId(index, attachment)
            when (attachment.type) {
                AttachmentType.Image -> PendingAttachmentItem.Image(
                    id = id,
                    imageBitmap = attachment.dataUrl?.toBitmapOrNull()
                )
                AttachmentType.Document,
                AttachmentType.Text -> PendingAttachmentItem.Document(
                    id = id,
                    fileName = attachment.name,
                    status = PendingAttachmentStatus.Ready
                )
                AttachmentType.Unsupported -> PendingAttachmentItem.Document(
                    id = id,
                    fileName = attachment.name,
                    status = PendingAttachmentStatus.UploadFailed
                )
            }
        }
    }
    val itemCount = state.messages.size + if (state.error != null) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    AiScaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AiSpacing.Lg, vertical = AiSpacing.Md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(Icons.Rounded.Menu, stringResource(R.string.history), onOpenHistory)
                PillActions {
                    IconButton(onClick = vm::newChat) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.new_chat), tint = AiColors.TextPrimary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.api_settings), tint = AiColors.TextPrimary)
                    }
                }
            }
        },
        bottomBar = {
            AiComposer(
                value = state.input,
                onValueChange = vm::updateInput,
                onAttach = { showAttachments = true },
                isReceiving = state.isSending,
                onStopReceiving = vm::stopReceiving,
                onSend = vm::send,
                pendingAttachments = pendingAttachments,
                onRemovePendingAttachment = { id ->
                    state.attachments
                        .withIndex()
                        .firstOrNull { (index, attachment) -> pendingAttachmentId(index, attachment) == id }
                        ?.value
                        ?.let(vm::removeAttachment)
                }
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AiSpacing.Lg, vertical = AiSpacing.Md),
            verticalArrangement = Arrangement.Top
        ) {
            items(
                count = state.messages.size,
                key = { index -> "message-$index-${state.messages[index].role}" }
            ) { index ->
                val message = state.messages[index]
                val isUser = message.role == "user"
                Column(Modifier.padding(top = AiSpacing.Sm)) {
                    MessageBubble(
                        text = message.content,
                        isUser = isUser,
                        markdownRenderingEnabled = markdownRenderingEnabled
                    )
                    if (state.isSending && index == state.messages.lastIndex && message.role == "assistant") {
                        ReceivingStatus(modifier = Modifier.padding(top = AiSpacing.Xs))
                    }
                    state.messageAttachments[index].orEmpty().forEach { attachment ->
                        ImageAttachmentBubble(
                            attachment = attachment,
                            isUser = isUser,
                            modifier = Modifier.padding(top = AiSpacing.Sm)
                        )
                    }
                }
            }
            state.error?.let { error ->
                item(key = "chat-error") {
                ChatErrorCard(
                    error = error,
                    onOpenSettings = onOpenSettings,
                    onRetry = vm::retryLastSend,
                    onDismiss = vm::dismissError,
                    modifier = Modifier.padding(top = AiSpacing.Md)
                )
                }
            }
        }
    }

    if (showAttachments) {
        ModalBottomSheet(onDismissRequest = { showAttachments = false }, sheetState = sheetState) {
            Column(Modifier.padding(horizontal = AiSpacing.Xl, vertical = AiSpacing.Xxl), verticalArrangement = Arrangement.spacedBy(AiSpacing.Md)) {
                AttachmentRow(stringResource(R.string.attachment_camera), Icons.Rounded.AddAPhoto) {
                    showAttachments = false
                    val uri = createCameraImageUri(context)
                    cameraImageUri = uri
                    cameraLauncher.launch(uri)
                }
                AttachmentRow(stringResource(R.string.attachment_photo), Icons.Rounded.Image) {
                    showAttachments = false
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                AttachmentRow(stringResource(R.string.attachment_file), Icons.Rounded.AttachFile) {
                    showAttachments = false
                    filePicker.launch(
                        arrayOf(
                            "text/*",
                            "application/json",
                            "text/csv",
                            "image/*",
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceivingStatus(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(
            text = stringResource(R.string.outputting),
            modifier = Modifier.padding(horizontal = AiSpacing.Lg),
            color = AiColors.Meta,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ImageAttachmentBubble(
    attachment: ChatUiAttachment,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        AiCard(modifier = Modifier.widthIn(max = 220.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(AiSpacing.Xs)) {
                attachment.dataUrl?.let { dataUrl ->
                    remember(dataUrl) { dataUrl.toImageBitmapOrNull() }?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Text(
                    attachment.name.ifBlank { if (attachment.kind == "image") "图片附件" else "附件" },
                    color = AiColors.Meta,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChatErrorCard(
    error: ChatUiError,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AiCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AiSpacing.Md)) {
            Text(error.message, color = AiColors.Danger, modifier = Modifier.weight(1f))
            AiSecondaryButton(
                text = error.actionLabel,
                onClick = {
                    when (error.action) {
                        ChatErrorAction.OpenSettings -> onOpenSettings()
                        ChatErrorAction.Retry -> onRetry()
                        ChatErrorAction.Dismiss -> onDismiss()
                    }
                }
            )
        }
    }
}

@Composable
private fun AttachmentRow(text: String, icon: ImageVector, onClick: () -> Unit) {
    SettingsRow(title = text, subtitle = stringResource(R.string.attachment_subtitle), trailing = { Icon(icon, contentDescription = null) }, onClick = onClick)
}

private fun createCameraImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("camera-", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun pendingAttachmentId(index: Int, attachment: AttachmentPayload): String {
    return "pending-$index-${attachment.name}"
}

private fun String.toBitmapOrNull() = runCatching {
    val encoded = substringAfter("base64,", missingDelimiterValue = "")
    if (encoded.isBlank()) return@runCatching null
    val bytes = Base64.decode(encoded, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()

private fun String.toImageBitmapOrNull() = runCatching {
    toBitmapOrNull()?.asImageBitmap()
}.getOrNull()
