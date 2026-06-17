package com.example.pantrypal.domain.usecase

import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.product.FoodRecognitionRepository
import com.example.pantrypal.domain.matching.FoodCategoryMatcher
import com.example.pantrypal.domain.model.BarcodeResolution
import com.example.pantrypal.domain.model.ExternalProductResult
import com.example.pantrypal.domain.model.FoodCategorySuggestion
import com.example.pantrypal.domain.model.RecognizedBarcodeProduct
import javax.inject.Inject

class ResolveBarcodeUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val foodRecognitionRepository: FoodRecognitionRepository,
    private val foodCategoryMatcher: FoodCategoryMatcher,
    private val textNormalizer: TextNormalizer
) {
    suspend operator fun invoke(rawBarcode: String): BarcodeResolution {
        val barcode = rawBarcode.trim()
        if (barcode.isBlank()) return BarcodeResolution.NotFound

        pantryRepository.findActiveBarcodeLink(barcode)?.let { link ->
            pantryRepository.getFoodCategory(link.categoryId)?.let { category ->
                return BarcodeResolution.KnownLocal(link = link, category = category)
            }
        }

        return when (val remote = foodRecognitionRepository.lookupProductByBarcode(barcode)) {
            is ExternalProductResult.Found -> remote.product.toRemoteResolution()
            ExternalProductResult.InvalidResponse -> BarcodeResolution.InvalidResponse
            ExternalProductResult.NetworkError -> BarcodeResolution.NetworkError
            ExternalProductResult.NotFound -> BarcodeResolution.NotFound
            ExternalProductResult.RateLimited -> BarcodeResolution.RateLimited
        }
    }

    private suspend fun RecognizedBarcodeProduct.toRemoteResolution(): BarcodeResolution {
        val suggestions = candidateTexts()
            .flatMap { candidate ->
                val sources = pantryRepository.getFoodCategoryMatchSources(candidate)
                foodCategoryMatcher.match(candidate, sources)
            }
            .groupBy { it.categoryId }
            .values
            .map { group -> group.maxBy { it.score } }
            .sortedWith(compareByDescending<FoodCategorySuggestion> { it.score }.thenBy { it.name })
            .take(8)

        return BarcodeResolution.FoundRemote(
            product = this,
            suggestions = suggestions,
            preselectedCategoryId = suggestions.firstOrNull { it.score >= 80 }?.categoryId
        )
    }

    private fun RecognizedBarcodeProduct.candidateTexts(): List<String> =
        buildList {
            genericName?.takeIf { it.isNotBlank() }?.let(::add)
            productName?.takeIf { it.isNotBlank() }?.let(::add)
            rawCategoryTags.map(::cleanTag).filterTo(this) { it.isNotBlank() }
            rawFoodGroupTags.map(::cleanTag).filterTo(this) { it.isNotBlank() }
        }
            .map { textNormalizer.normalizeFoodText(it) }
            .filter { it.isNotBlank() }
            .distinct()

    private fun cleanTag(value: String): String =
        value.substringAfter(':').replace('-', ' ')
}
