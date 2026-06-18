package com.example.aiassistant.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.aiassistant.R
import com.example.aiassistant.ui.theme.AndroidAIAssistantTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiComposerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun documentPendingCardShowsFilenameAndStatus() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            AndroidAIAssistantTheme(darkTheme = false) {
                ComposerPendingAttachments(
                    attachments = listOf(
                        PendingAttachmentItem.Document(
                            id = "doc-1",
                            fileName = "spec.pdf",
                            status = PendingAttachmentStatus.Ready
                        )
                    )
                )
            }
        }

        composeRule.onNodeWithTag("pending_attachment_document_name_doc-1")
            .assertIsDisplayed()
            .assertTextEquals("spec.pdf")
        composeRule.onNodeWithTag("pending_attachment_document_status_doc-1")
            .assertIsDisplayed()
            .assertTextEquals(context.getString(R.string.pending_attachment_ready))
    }

    @Test
    fun imagePendingCardDoesNotShowFilenameText() {
        composeRule.setContent {
            AndroidAIAssistantTheme(darkTheme = false) {
                ComposerPendingAttachments(
                    attachments = listOf(
                        PendingAttachmentItem.Image(
                            id = "img-1"
                        )
                    )
                )
            }
        }

        composeRule.onAllNodesWithText("photo.png").assertCountEquals(0)
        composeRule.onNodeWithTag("pending_attachment_image_thumb_img-1").assertIsDisplayed()
    }

    @Test
    fun removeAttachmentInvokesCallbackWithMatchingId() {
        var removedAttachmentId: String? = null

        composeRule.setContent {
            AndroidAIAssistantTheme(darkTheme = false) {
                ComposerPendingAttachments(
                    attachments = listOf(
                        PendingAttachmentItem.Document(
                            id = "doc-1",
                            fileName = "spec.pdf",
                            status = PendingAttachmentStatus.Parsing
                        )
                    ),
                    onRemoveAttachment = { removedAttachmentId = it }
                )
            }
        }

        composeRule.onNodeWithTag("pending_attachment_remove_doc-1")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals("doc-1", removedAttachmentId)
        }
    }

    @Test
    fun emptyPendingAttachmentsRendersNothing() {
        composeRule.setContent {
            AndroidAIAssistantTheme(darkTheme = false) {
                ComposerPendingAttachments(attachments = emptyList())
            }
        }

        composeRule.onAllNodesWithTag("pending_attachment_strip").assertCountEquals(0)
    }
}
