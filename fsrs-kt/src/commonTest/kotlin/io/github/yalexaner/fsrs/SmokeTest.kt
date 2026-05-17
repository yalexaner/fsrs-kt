package io.github.yalexaner.fsrs

import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun helloReturnsLibraryName() {
        assertEquals("fsrs-kt", hello())
    }
}
