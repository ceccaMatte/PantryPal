package com.example.pantrypal.data.recipe.source

private val leadingQuantityRegex = Regex("""^\s*(\d+(?:[.,]\d+)?|\d+/\d+)\s*([a-zA-Z]+)?\s+""")

fun resolveIngredientCleanName(
    nameClean: String?,
    name: String?,
    original: String?
): String? =
    nameClean?.takeIf { it.isNotBlank() }?.trim()
        ?: name?.takeIf { it.isNotBlank() }?.trim()
        ?: original?.takeIf { it.isNotBlank() }?.parseCleanNameFromOriginal()

fun ingredientDisplayAmount(
    amount: Double?,
    unit: String?,
    original: String?
): String =
    listOfNotNull(
        amount?.let { value ->
            if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().trimEnd('0').trimEnd('.')
        },
        unit?.takeIf { it.isNotBlank() }?.trim()
    ).joinToString(" ").ifBlank {
        original?.parseDisplayAmountFromOriginal().orEmpty()
    }

private fun String.parseCleanNameFromOriginal(): String {
    var cleaned = trim()
    repeat(3) {
        cleaned = cleaned.replace(leadingQuantityRegex, "")
    }
    return cleaned.trim().ifBlank { trim() }
}

private fun String.parseDisplayAmountFromOriginal(): String {
    val parts = mutableListOf<String>()
    var remaining = trim()
    repeat(2) {
        val match = leadingQuantityRegex.find(remaining) ?: return@repeat
        parts += match.value.trim()
        remaining = remaining.removeRange(match.range).trimStart()
    }
    return parts.joinToString(" ")
}
