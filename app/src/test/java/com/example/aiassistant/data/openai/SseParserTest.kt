package com.example.aiassistant.data.openai

import org.junit.Assert.assertEquals
import org.junit.Test

class SseParserTest {
    @Test
    fun parserExtractsDataLinesAndSkipsDone() {
        val lines = listOf(
            "data: {\"choices\":[{\"delta\":{\"content\":\"Hi\"}}]}",
            "",
            "data: [DONE]"
        )

        val events = SseParser.parseLines(lines)

        assertEquals(1, events.size)
        assertEquals("{\"choices\":[{\"delta\":{\"content\":\"Hi\"}}]}", events[0])
    }
}
