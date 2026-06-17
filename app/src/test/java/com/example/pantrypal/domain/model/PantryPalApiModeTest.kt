package com.example.pantrypal.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PantryPalApiModeTest {
    @Test
    fun missingOrInvalidModeFallsBackToMock() {
        assertEquals(PantryPalApiMode.MOCK, PantryPalApiMode.fromRaw(null))
        assertEquals(PantryPalApiMode.MOCK, PantryPalApiMode.fromRaw(""))
        assertEquals(PantryPalApiMode.MOCK, PantryPalApiMode.fromRaw("banana"))
    }

    @Test
    fun validModeParsingIsCaseInsensitive() {
        assertEquals(PantryPalApiMode.REAL, PantryPalApiMode.fromRaw("real"))
        assertEquals(PantryPalApiMode.CACHE_ONLY, PantryPalApiMode.fromRaw("cache_only"))
        assertEquals(PantryPalApiMode.MOCK, PantryPalApiMode.fromRaw("MOCK"))
    }
}
