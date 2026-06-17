package com.example.aiassistant.domain.attachments

object AttachmentPrompt {
    fun build(userText: String, attachments: List<AttachmentPayload>): String {
        if (attachments.isEmpty()) return userText
        val textAttachments = attachments.filter { it.type == AttachmentType.Text && !it.text.isNullOrBlank() }
        if (textAttachments.isEmpty()) return userText
        return buildString {
            appendLine(userText)
            appendLine()
            appendLine("Attached text content:")
            textAttachments.forEach { attachment ->
                appendLine("--- attachment ---")
                appendLine(attachment.text)
                appendLine("--- end attachment ---")
            }
        }
    }
}
