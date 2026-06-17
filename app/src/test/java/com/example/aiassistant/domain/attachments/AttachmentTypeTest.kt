package com.example.aiassistant.domain.attachments

import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentTypeTest {
    @Test
    fun textExtensionsAreSupportedAsText() {
        assertEquals(AttachmentType.Text, AttachmentType.fromName("notes.md"))
        assertEquals(AttachmentType.Text, AttachmentType.fromName("data.json"))
        assertEquals(AttachmentType.Text, AttachmentType.fromName("table.csv"))
    }

    @Test
    fun imageExtensionsAreSupportedAsImages() {
        assertEquals(AttachmentType.Image, AttachmentType.fromName("photo.png"))
        assertEquals(AttachmentType.Image, AttachmentType.fromName("photo.jpeg"))
    }

    @Test
    fun documentExtensionsAreSupportedAsDocuments() {
        assertEquals(AttachmentType.Document, AttachmentType.fromName("paper.pdf"))
        assertEquals(AttachmentType.Document, AttachmentType.fromName("paper.doc"))
        assertEquals(AttachmentType.Document, AttachmentType.fromName("paper.docx"))
    }
}
