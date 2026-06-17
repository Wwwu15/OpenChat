package com.example.aiassistant.domain.attachments

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentPromptTest {
    @Test
    fun textAttachmentIsWrappedWithoutFilename() {
        val payload = AttachmentPayload("notes.md", AttachmentType.Text, text = "hello")

        val prompt = AttachmentPrompt.build("summarize", listOf(payload))

        assertFalse(prompt.contains("notes.md"))
        assertTrue(prompt.contains("hello"))
    }
}
