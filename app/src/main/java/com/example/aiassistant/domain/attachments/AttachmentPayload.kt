package com.example.aiassistant.domain.attachments

data class AttachmentPayload(
    val name: String,
    val type: AttachmentType,
    val text: String? = null,
    val dataUrl: String? = null,
    val mimeType: String? = null
)
