package com.example.pantrypal.core.util

import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextNormalizer @Inject constructor() {
    fun normalizeFoodText(value: String): String = normalize(value)

    fun normalize(value: String): String {
        val withoutAccents = Normalizer.normalize(value.trim().lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")

        return withoutAccents
            .replace("[\\u2018\\u2019`']".toRegex(), " ")
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
