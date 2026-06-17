package com.example.pantrypal.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeHttpLoggingInterceptorTest {
    @Test
    fun safeMessageDoesNotIncludeQueryStringOrApiKey() {
        val message = safeHttpLogMessage(
            method = "GET",
            host = "api.spoonacular.com",
            encodedPath = "/recipes/complexSearch",
            result = "200",
            durationMs = 530
        )

        assertTrue(message.contains("GET api.spoonacular.com/recipes/complexSearch -> 200 in 530ms"))
        assertFalse(message.contains("?"))
        assertFalse(message.contains("apiKey", ignoreCase = true))
    }
}
