package com.example.pantrypal.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    private val normalizer = TextNormalizer()

    @Test
    fun normalizeFoodText_removesAccentsPunctuationAndExtraSpaces() {
        val result = normalizer.normalizeFoodText("  Caffe'  d'Orzo, BIO!!  ")

        assertEquals("caffe d orzo bio", result)
    }

    @Test
    fun normalizeFoodText_keepsNumbers() {
        val result = normalizer.normalizeFoodText("Latte 2% UHT")

        assertEquals("latte 2 uht", result)
    }
}
