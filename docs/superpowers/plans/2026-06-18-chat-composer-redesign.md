# Chat Composer Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将主聊天页底部输入区重构为集成式 AI composer，支持顶部待发附件卡片区、左下角内置附件按钮、多行自动增高输入框，以及单一 `发送 / 停止` 主按钮。

**Architecture:** 保持现有 `ChatScreen -> ChatViewModel -> ChatRepository` 数据链路不变，把改动集中在聊天 UI 表达层与少量发送规则调整。状态逻辑优先在 `ChatViewModel` 和轻量 UI model 中收口，Compose 组件只负责渲染与交互分发，避免把发送规则散落在多个 composable 里。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、Android ViewModel、Coroutine Flow、JUnit4、Compose UI Test

---

## File Structure Map

**Modify**

- `app/src/main/java/com/example/aiassistant/ui/screens/chat/ChatViewModel.kt`
  - 允许“仅附件发送”
  - 输出更明确的 composer 派生状态
  - 保持停止生成、失败回填、附件映射等现有能力
- `app/src/main/java/com/example/aiassistant/ui/screens/chat/ChatScreen.kt`
  - 把待发附件从消息列表中移出
  - 改为在底部 composer 内展示待发附件区
  - 给 composer 传入新的 pending attachment UI model
- `app/src/main/java/com/example/aiassistant/ui/components/AiComposer.kt`
  - 从单行输入 + 双按钮改为集成式 composer
  - 增加多行输入、顶部待发附件区、单一主按钮
- `app/src/main/res/values/strings.xml`
  - 新增 composer 与附件状态文案
- `app/src/test/java/com/example/aiassistant/ui/screens/chat/ChatViewModelTest.kt`
  - 覆盖仅附件发送、失败回填、发送中状态切换

**Create**

- `app/src/main/java/com/example/aiassistant/ui/components/ComposerPendingAttachments.kt`
  - 待发附件横向卡片区
  - 图片卡片与文档卡片
  - 删除按钮与测试 tag
- `app/src/androidTest/java/com/example/aiassistant/ui/components/AiComposerTest.kt`
  - 验证单一主按钮、待发附件区显示、文档文件名显示、图片不显示文件名

---

### Task 1: 收口发送规则与 ChatViewModel 状态

**Files:**
- Modify: `app/src/main/java/com/example/aiassistant/ui/screens/chat/ChatViewModel.kt`
- Test: `app/src/test/java/com/example/aiassistant/ui/screens/chat/ChatViewModelTest.kt`

- [ ] **Step 1: 先写失败单测，覆盖“仅附件发送”和“发送失败回填附件”**

```kotlin
@Test
fun attachmentOnlySendStartsRequestAndClearsComposer() = runTest {
    val vm = ChatViewModel(
        apiProfiles = ApiProfileRepository(FakeApiKeyStorage("key"), OpenAiClient()),
        conversations = ConversationRepository(FakeConversationDao()),
        chat = ChatRepository(ImmediateAttachmentStreamApi())
    )

    vm.addAttachment(
        AttachmentPayload(
            name = "photo.jpg",
            type = AttachmentType.Image,
            dataUrl = "data:image/jpeg;base64,aaa"
        )
    )

    vm.send()
    runCurrent()

    assertTrue(vm.uiState.value.isSending)
    assertEquals("", vm.uiState.value.input)
    assertTrue(vm.uiState.value.attachments.isEmpty())
    assertEquals("user", vm.uiState.value.messages[0].role)
}

@Test
fun failedAttachmentOnlySendRestoresPendingAttachments() = runTest {
    val vm = ChatViewModel(
        apiProfiles = ApiProfileRepository(FakeApiKeyStorage("key"), OpenAiClient()),
        conversations = ConversationRepository(FakeConversationDao()),
        chat = ChatRepository(FailingAttachmentStreamApi())
    )

    val payload = AttachmentPayload(
        name = "paper.pdf",
        type = AttachmentType.Document,
        dataUrl = "data:application/pdf;base64,bbb",
        mimeType = "application/pdf"
    )
    vm.addAttachment(payload)

    vm.send()
    advanceUntilIdle()

    assertFalse(vm.uiState.value.isSending)
    assertEquals(listOf(payload), vm.uiState.value.attachments)
    assertTrue(vm.uiState.value.messages.isEmpty())
}
```

- [ ] **Step 2: 运行单测确认当前实现失败**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.aiassistant.ui.screens.chat.ChatViewModelTest"
```

Expected:

```text
ChatViewModelTest > attachmentOnlySendStartsRequestAndClearsComposer FAILED
ChatViewModelTest > failedAttachmentOnlySendRestoresPendingAttachments FAILED
```

- [ ] **Step 3: 最小改动实现“仅附件发送可用”与发送回填**

```kotlin
data class ChatUiState(
    val input: String = "",
    val messages: List<ChatUiMessage> = emptyList(),
    val messageAttachments: Map<Int, List<ChatUiAttachment>> = emptyMap(),
    val attachments: List<AttachmentPayload> = emptyList(),
    val isSending: Boolean = false,
    val error: ChatUiError? = null
) {
    val canSend: Boolean
        get() = input.trim().isNotEmpty() || attachments.isNotEmpty()
}

fun send() {
    val current = _uiState.value
    if (current.isSending || !current.canSend) return

    val text = current.input.trim()
    val attachments = current.attachments
    val messageText = AttachmentPrompt.build(text, attachments)
    val previewText = messageText.ifBlank { text }

    lastAttempt = PendingSend(text, attachments)
    lastStableMessages = current.messages
    lastStableAttachments = current.messageAttachments

    sendJob = viewModelScope.launch {
        _uiState.update {
            it.copy(
                input = "",
                attachments = emptyList(),
                isSending = true,
                error = null,
                messages = it.messages + ChatUiMessage("user", previewText) + ChatUiMessage("assistant", "")
            )
        }
        // 其余现有发送逻辑保持不变
    }
}
```

- [ ] **Step 4: 重新运行单测确认通过**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.aiassistant.ui.screens.chat.ChatViewModelTest"
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 5: 提交本任务**

```powershell
git add app/src/main/java/com/example/aiassistant/ui/screens/chat/ChatViewModel.kt app/src/test/java/com/example/aiassistant/ui/screens/chat/ChatViewModelTest.kt
git commit -m "feat: support attachment-first composer sending"
```

---

### Task 2: 新建待发附件卡片组件

**Files:**
- Create: `app/src/main/java/com/example/aiassistant/ui/components/ComposerPendingAttachments.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/com/example/aiassistant/ui/components/AiComposerTest.kt`

- [ ] **Step 1: 先写 Compose UI 测试，锁定附件区展示规则**

```kotlin
@RunWith(AndroidJUnit4::class)
class AiComposerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun documentAttachmentShowsFilenameAndStatus() {
        composeRule.setContent {
            AiTheme {
                PendingAttachmentStrip(
                    attachments = listOf(
                        PendingAttachmentUiModel.Document(
                            id = "doc-1",
                            fileName = "需求说明书.docx",
                            status = "已就绪"
                        )
                    ),
                    onRemove = {}
                )
            }
        }

        composeRule.onNodeWithText("需求说明书.docx").assertExists()
        composeRule.onNodeWithText("已就绪").assertExists()
    }

    @Test
    fun imageAttachmentDoesNotShowFilename() {
        composeRule.setContent {
            AiTheme {
                PendingAttachmentStrip(
                    attachments = listOf(
                        PendingAttachmentUiModel.Image(
                            id = "img-1",
                            previewDataUrl = "data:image/jpeg;base64,aaa"
                        )
                    ),
                    onRemove = {}
                )
            }
        }

        composeRule.onNodeWithTag("pending-image-card-img-1").assertExists()
        composeRule.onNodeWithTag("pending-image-filename").assertDoesNotExist()
    }
}
```

- [ ] **Step 2: 运行仪器测试，确认组件尚不存在而失败**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.aiassistant.ui.components.AiComposerTest
```

Expected:

```text
error: Unresolved reference: PendingAttachmentStrip
error: Unresolved reference: PendingAttachmentUiModel
```

- [ ] **Step 3: 实现待发附件 strip、图片卡片、文档卡片与字符串**

```kotlin
sealed interface PendingAttachmentUiModel {
    val id: String

    data class Image(
        override val id: String,
        val previewDataUrl: String
    ) : PendingAttachmentUiModel

    data class Document(
        override val id: String,
        val fileName: String,
        val status: String
    ) : PendingAttachmentUiModel
}

@Composable
fun PendingAttachmentStrip(
    attachments: List<PendingAttachmentUiModel>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth().testTag("pending-attachment-strip"),
        horizontalArrangement = Arrangement.spacedBy(AiSpacing.Md)
    ) {
        items(attachments, key = { it.id }) { item ->
            when (item) {
                is PendingAttachmentUiModel.Image -> PendingImageCard(item, onRemove)
                is PendingAttachmentUiModel.Document -> PendingDocumentCard(item, onRemove)
            }
        }
    }
}

@Composable
private fun PendingDocumentCard(
    item: PendingAttachmentUiModel.Document,
    onRemove: (String) -> Unit
) {
    Box {
        AiCard(modifier = Modifier.width(244.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AiSpacing.Md)) {
                Icon(Icons.Rounded.Description, contentDescription = null, tint = AiColors.Accent)
                Column(Modifier.weight(1f)) {
                    Text(item.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.status, color = AiColors.Muted, fontSize = 13.sp)
                }
            }
        }
        IconButton(
            onClick = { onRemove(item.id) },
            modifier = Modifier.align(Alignment.TopEnd).testTag("pending-remove-${item.id}")
        ) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.remove_attachment))
        }
    }
}
```

```xml
<string name="attachment_ready">已就绪</string>
<string name="attachment_parsing">解析中...</string>
<string name="attachment_upload_failed">上传失败</string>
```

- [ ] **Step 4: 重新运行仪器测试确认通过**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.aiassistant.ui.components.AiComposerTest
```

Expected:

```text
AiComposerTest > documentAttachmentShowsFilenameAndStatus PASSED
AiComposerTest > imageAttachmentDoesNotShowFilename PASSED
```

- [ ] **Step 5: 提交本任务**

```powershell
git add app/src/main/java/com/example/aiassistant/ui/components/ComposerPendingAttachments.kt app/src/main/res/values/strings.xml app/src/androidTest/java/com/example/aiassistant/ui/components/AiComposerTest.kt
git commit -m "feat: add pending attachment cards for composer"
```

---

### Task 3: 重写 AiComposer 为单一主按钮的集成式输入区

**Files:**
- Modify: `app/src/main/java/com/example/aiassistant/ui/components/AiComposer.kt`
- Modify: `app/src/main/java/com/example/aiassistant/ui/components/ComposerPendingAttachments.kt`
- Test: `app/src/androidTest/java/com/example/aiassistant/ui/components/AiComposerTest.kt`

- [ ] **Step 1: 先补失败测试，锁定“只有一个主按钮”和“生成中切换为停止”**

```kotlin
@Test
fun composerUsesSinglePrimaryActionButton() {
    composeRule.setContent {
        AiTheme {
            AiComposer(
                value = "你好",
                onValueChange = {},
                pendingAttachments = emptyList(),
                onAttach = {},
                isReceiving = false,
                onStopReceiving = {},
                onRemovePendingAttachment = {},
                onSend = {}
            )
        }
    }

    composeRule.onAllNodesWithTag("composer-primary-action").assertCountEquals(1)
    composeRule.onNodeWithTag("composer-stop-action").assertDoesNotExist()
}

@Test
fun composerShowsStopActionWhileReceiving() {
    composeRule.setContent {
        AiTheme {
            AiComposer(
                value = "",
                onValueChange = {},
                pendingAttachments = emptyList(),
                onAttach = {},
                isReceiving = true,
                onStopReceiving = {},
                onRemovePendingAttachment = {},
                onSend = {}
            )
        }
    }

    composeRule.onNodeWithTag("composer-primary-action").assertExists()
    composeRule.onNodeWithTag("composer-status").assertTextContains("正在生成")
}
```

- [ ] **Step 2: 运行仪器测试确认旧 composer 结构不满足要求**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.aiassistant.ui.components.AiComposerTest
```

Expected:

```text
Expected exactly '1' node but found '2'
```

- [ ] **Step 3: 重写 AiComposer，接入多行输入、左下角附件按钮、顶部附件区和单一主按钮**

```kotlin
@Composable
fun AiComposer(
    value: String,
    onValueChange: (String) -> Unit,
    pendingAttachments: List<PendingAttachmentUiModel>,
    onAttach: () -> Unit,
    isReceiving: Boolean,
    onStopReceiving: () -> Unit,
    onRemovePendingAttachment: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = value.trim().isNotEmpty() || pendingAttachments.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = AiSpacing.Lg, vertical = AiSpacing.Xl)
            .background(AiColors.Surface.copy(alpha = 0.96f), RoundedCornerShape(30.dp))
            .border(1.dp, AiColors.BorderSoft, RoundedCornerShape(30.dp))
            .padding(AiSpacing.Lg)
    ) {
        PendingAttachmentStrip(
            attachments = pendingAttachments,
            onRemove = onRemovePendingAttachment,
            modifier = Modifier.padding(bottom = if (pendingAttachments.isEmpty()) 0.dp else AiSpacing.Md)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp, max = 180.dp)
                .testTag("composer-input"),
            textStyle = LocalTextStyle.current.copy(color = AiColors.TextPrimary, fontSize = 16.sp),
            maxLines = Int.MAX_VALUE
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = onAttach,
                modifier = Modifier.testTag("composer-attach")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_attachment))
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isReceiving) {
                    Text(
                        text = stringResource(R.string.outputting),
                        color = AiColors.Meta,
                        modifier = Modifier.testTag("composer-status")
                    )
                }

                IconButton(
                    onClick = if (isReceiving) onStopReceiving else onSend,
                    enabled = isReceiving || canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isReceiving || canSend) AiColors.Accent else AiColors.BorderSoft,
                            RoundedCornerShape(24.dp)
                        )
                        .testTag("composer-primary-action")
                ) {
                    Icon(
                        imageVector = if (isReceiving) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = AiColors.AccentOn
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: 重新运行组件仪器测试确认通过**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.aiassistant.ui.components.AiComposerTest
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 5: 提交本任务**

```powershell
git add app/src/main/java/com/example/aiassistant/ui/components/AiComposer.kt app/src/main/java/com/example/aiassistant/ui/components/ComposerPendingAttachments.kt app/src/androidTest/java/com/example/aiassistant/ui/components/AiComposerTest.kt
git commit -m "feat: redesign chat composer interaction"
```

---

### Task 4: 接线 ChatScreen，移除旧的待发附件列表占位

**Files:**
- Modify: `app/src/main/java/com/example/aiassistant/ui/screens/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/example/aiassistant/ui/screens/chat/ChatViewModel.kt`
- Test: `app/src/test/java/com/example/aiassistant/ui/screens/chat/ChatViewModelTest.kt`

- [ ] **Step 1: 先写失败单测，锁定待发附件删除行为**

```kotlin
@Test
fun removeAttachmentRemovesOnlyMatchingPendingItem() = runTest {
    val vm = ChatViewModel(
        apiProfiles = ApiProfileRepository(FakeApiKeyStorage("key"), OpenAiClient()),
        conversations = ConversationRepository(FakeConversationDao()),
        chat = ChatRepository(BlockingStreamApi())
    )

    vm.addAttachment(AttachmentPayload("one.pdf", AttachmentType.Document, dataUrl = "data:application/pdf;base64,1"))
    vm.addAttachment(AttachmentPayload("two.jpg", AttachmentType.Image, dataUrl = "data:image/jpeg;base64,2"))

    vm.removeAttachment("one.pdf")

    assertEquals(listOf("two.jpg"), vm.uiState.value.attachments.map { it.name })
}
```

- [ ] **Step 2: 运行单测确认当前或接线中的改动会暴露问题**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.aiassistant.ui.screens.chat.ChatViewModelTest"
```

Expected:

```text
If ChatScreen mapping or ViewModel naming drifts, this test fails before UI wiring is complete.
```

- [ ] **Step 3: 在 ChatScreen 中映射 pending attachments，并移除旧的 `AttachmentChip` 消息列表展示**

```kotlin
val pendingAttachments = state.attachments.mapIndexed { index, attachment ->
    when (attachment.type) {
        AttachmentType.Image -> PendingAttachmentUiModel.Image(
            id = "pending-$index-${attachment.name}",
            previewDataUrl = attachment.dataUrl.orEmpty()
        )
        AttachmentType.Document,
        AttachmentType.Text -> PendingAttachmentUiModel.Document(
            id = "pending-$index-${attachment.name}",
            fileName = attachment.name,
            status = context.getString(R.string.attachment_ready)
        )
        AttachmentType.Unsupported -> PendingAttachmentUiModel.Document(
            id = "pending-$index-${attachment.name}",
            fileName = attachment.name,
            status = context.getString(R.string.attachment_upload_failed)
        )
    }
}

AiComposer(
    value = state.input,
    onValueChange = vm::updateInput,
    pendingAttachments = pendingAttachments,
    onAttach = { showAttachments = true },
    isReceiving = state.isSending,
    onStopReceiving = vm::stopReceiving,
    onRemovePendingAttachment = { id ->
        pendingAttachments.firstOrNull { it.id == id }?.let { model ->
            val name = when (model) {
                is PendingAttachmentUiModel.Image -> state.attachments.firstOrNull { attachment ->
                    attachment.dataUrl == model.previewDataUrl
                }?.name
                is PendingAttachmentUiModel.Document -> model.fileName
            }
            if (name != null) vm.removeAttachment(name)
        }
    },
    onSend = vm::send
)
```

并删除这段旧逻辑：

```kotlin
items(
    count = state.attachments.size,
    key = { index -> "attachment-${state.attachments[index].name}" }
) { index ->
    val attachment = state.attachments[index]
    AttachmentChip(
        attachment = attachment,
        onRemove = { vm.removeAttachment(attachment.name) }
    )
}
```

- [ ] **Step 4: 运行聊天相关单测，确认接线后状态逻辑仍通过**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.aiassistant.ui.screens.chat.ChatViewModelTest"
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 5: 提交本任务**

```powershell
git add app/src/main/java/com/example/aiassistant/ui/screens/chat/ChatScreen.kt app/src/main/java/com/example/aiassistant/ui/screens/chat/ChatViewModel.kt app/src/test/java/com/example/aiassistant/ui/screens/chat/ChatViewModelTest.kt
git commit -m "feat: move pending attachments into composer"
```

---

### Task 5: 全量回归、模拟器验证与打包

**Files:**
- Verify only; no new source files required

- [ ] **Step 1: 运行全部单元测试**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 2: 运行 composer 相关仪器测试**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.aiassistant.ui.components.AiComposerTest
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 3: 在模拟器手动验证 5 个关键状态**

Manual checklist:

```text
1. 发送前空白：只有一个灰态主按钮，+ 号在输入框左下角内侧
2. 发送前可发送：输入文本后主按钮高亮
3. 发图片：缩略图卡片显示在输入框上方，图片文件名不显示
4. 发文档：文档卡片显示文件名与状态，可横向滑动，可点 X 删除
5. 生成中：发送按钮原地切换为停止按钮，composer 内显示“正在输出...”
```

- [ ] **Step 4: 构建 debug APK 供用户安装**

Run:

```powershell
.\gradlew.bat assembleDebug
Copy-Item .\app\build\outputs\apk\debug\app-debug.apk D:\Android-IDE\projects\app\OpenChat-debug.apk -Force
```

Expected:

```text
app-debug.apk generated successfully
```

- [ ] **Step 5: 提交验证与打包完成状态**

```powershell
git add app/src/main app/src/test app/src/androidTest
git commit -m "test: verify redesigned composer flow"
```

---

## Spec Coverage Check

- 单一 `发送 / 停止` 主按钮：Task 3
- 多行自动增高输入区：Task 3
- 左下角内置附件按钮：Task 3
- 顶部待发附件区：Task 2、Task 4
- 图片不显示文件名：Task 2
- 文档显示文件名与状态：Task 2
- 多附件横向滑动：Task 2
- 仅附件发送：Task 1
- 生成中状态联动：Task 1、Task 3
- 旧待发附件列表移除：Task 4
- 单测 / 仪器测试 / 模拟器验证 / APK：Task 5
