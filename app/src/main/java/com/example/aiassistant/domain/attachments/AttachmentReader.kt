package com.example.aiassistant.domain.attachments

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64

class AttachmentReader(private val contentResolver: ContentResolver) {
    fun displayName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return uri.lastPathSegment ?: "attachment"
    }

    fun readText(uri: Uri, maxChars: Int = 40_000): String {
        return contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val buffer = CharArray(maxChars)
            val count = reader.read(buffer)
            if (count <= 0) "" else String(buffer, 0, count)
        }.orEmpty()
    }

    fun readImageDataUrl(uri: Uri): String {
        val mime = contentResolver.getType(uri) ?: "image/jpeg"
        return readDataUrl(uri, mime)
    }

    fun readDocumentDataUrl(uri: Uri, name: String): String {
        val mime = contentResolver.getType(uri) ?: mimeTypeFromName(name)
        return readDataUrl(uri, mime)
    }

    fun mimeType(uri: Uri, name: String): String {
        return contentResolver.getType(uri) ?: mimeTypeFromName(name)
    }

    private fun readDataUrl(uri: Uri, mime: String): String {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        return "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }

    private fun mimeTypeFromName(name: String): String {
        return when (name.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }
}
