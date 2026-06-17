package com.example.aiassistant.domain.attachments

enum class AttachmentType {
    Image,
    Text,
    Document,
    Unsupported;

    companion object {
        private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
        private val textExtensions = setOf("txt", "md", "markdown", "json", "csv", "tsv", "xml", "yaml", "yml", "kt", "java", "js", "ts", "py", "html", "css")
        private val documentExtensions = setOf("pdf", "doc", "docx")

        fun fromName(name: String): AttachmentType {
            val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            return when {
                extension in imageExtensions -> Image
                extension in textExtensions -> Text
                extension in documentExtensions -> Document
                else -> Unsupported
            }
        }
    }
}
