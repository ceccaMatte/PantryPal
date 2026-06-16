package com.example.pantrypal.domain.matching

import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodCategorySuggestion
import javax.inject.Inject

class FoodCategoryMatcher @Inject constructor(
    private val textNormalizer: TextNormalizer
) {
    fun match(
        query: String,
        sources: List<FoodCategoryMatchSource>,
        limit: Int = 8
    ): List<FoodCategorySuggestion> {
        val normalizedQuery = textNormalizer.normalizeFoodText(query)
        if (normalizedQuery.isBlank()) {
            return sources
                .sortedWith(compareByDescending<FoodCategoryMatchSource> { it.category.lastUsedAt }
                    .thenBy { it.category.name })
                .take(limit)
                .map {
                    FoodCategorySuggestion(
                        categoryId = it.category.id,
                        name = it.category.name,
                        score = 10,
                        reason = "recent"
                    )
                }
        }

        return sources
            .mapNotNull { source -> source.bestSuggestionFor(normalizedQuery) }
            .sortedWith(compareByDescending<FoodCategorySuggestion> { it.score }.thenBy { it.name })
            .take(limit)
    }

    fun shouldShowCreateNew(query: String, sources: List<FoodCategoryMatchSource>): Boolean {
        val normalizedQuery = textNormalizer.normalizeFoodText(query)
        if (normalizedQuery.isBlank()) return false
        return sources.none { source ->
            source.category.normalizedName == normalizedQuery ||
                source.aliases.any { it.normalizedAlias == normalizedQuery }
        }
    }

    private fun FoodCategoryMatchSource.bestSuggestionFor(query: String): FoodCategorySuggestion? {
        val categoryName = category.normalizedName
        val aliasNames = aliases.map { it.normalizedAlias }

        val nameScore = scoreText(categoryName, query, isAlias = false)
        val aliasScore = aliasNames.maxOfOrNull { scoreText(it, query, isAlias = true) } ?: 0
        val score = maxOf(nameScore, aliasScore)
        if (score <= 0) return null

        val reason = when (score) {
            in 100..Int.MAX_VALUE -> "exact"
            in 80..99 -> "alias"
            in 60..79 -> "starts_with"
            in 40..59 -> "contains"
            else -> "tokens"
        }

        return FoodCategorySuggestion(
            categoryId = category.id,
            name = category.name,
            score = score,
            reason = reason
        )
    }

    private fun scoreText(candidate: String, query: String, isAlias: Boolean): Int {
        if (candidate == query) return if (isAlias) 90 else 100
        if (candidate.startsWith(query)) return if (isAlias) 65 else 75
        if (candidate.contains(query)) return if (isAlias) 45 else 55

        val queryTokens = query.split(" ").filter { it.isNotBlank() }.toSet()
        val candidateTokens = candidate.split(" ").filter { it.isNotBlank() }.toSet()
        val common = queryTokens.intersect(candidateTokens).size
        if (common == 0) return 0

        return 20 + common * 5
    }
}
