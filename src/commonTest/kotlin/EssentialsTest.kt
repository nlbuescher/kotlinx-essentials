package dev.buescher.kotlinx.essentials

import kotlin.test.Test
import kotlin.test.assertEquals

class EssentialsTest {
    @Test
    fun greetingReturnsExpectedText() {
        assertEquals("Hello from kotlinx-essentials", Essentials.greeting())
    }
}
