package com.example.aiassistant.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aiassistant.MainActivity
import com.example.aiassistant.ui.theme.AndroidAIAssistantTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiComposerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun documentPendingCardShowsFilenameAndStatus() {
        composeRule.setContent {
            AndroidAIAssistantTheme(darkTheme = false) {
                ComposerPendingAttachments(
                    attachments = listOf(
                        PendingAttachmentItem(
                            id = "doc-1",
                            type = PendingAttachmentType.Document,
                            fileName = "spec.pdf",
                            status = PendingAttachmentStatus.Ready
                        )
                    )
                )
            }
        }

        composeRule.onNodeWithText("spec.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("已就绪").assertIsDisplayed()
    }

    @Test
    fun imagePendingCardDoesNotShowFilenameText() {
        composeRule.setContent {
            AndroidAIAssistantTheme(darkTheme = false) {
                ComposerPendingAttachments(
                    attachments = listOf(
                        PendingAttachmentItem(
                            id = "img-1",
                            type = PendingAttachmentType.Image,
                            fileName = "photo.png",
                            status = PendingAttachmentStatus.UploadFailed
                        )
                    )
                )
            }
        }

        composeRule.onAllNodesWithText("photo.png").assertCountEquals(0)
        composeRule.onNodeWithTag("pending_attachment_image_thumb_img-1").assertIsDisplayed()
    }
}
