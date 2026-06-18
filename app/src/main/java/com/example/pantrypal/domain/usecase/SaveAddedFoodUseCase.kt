package com.example.pantrypal.domain.usecase

import com.example.pantrypal.core.image.ImageStorage
import com.example.pantrypal.core.image.NoOpImageStorage
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.SaveAddedFoodCommand
import com.example.pantrypal.domain.model.SaveAddedFoodResult
import com.example.pantrypal.domain.model.SaveAddedFoodValidationError
import javax.inject.Inject

class SaveAddedFoodUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val imageStorage: ImageStorage = NoOpImageStorage
) {
    suspend operator fun invoke(command: SaveAddedFoodCommand): SaveAddedFoodResult {
        val validLots = command.lots.filter { it.quantity > 0 }
        val errors = buildSet {
            val selection = command.categorySelection
            if (selection == null || selection is AddFoodCategorySelection.New && selection.name.isBlank()) {
                add(SaveAddedFoodValidationError.CATEGORY_REQUIRED)
            }
            if (validLots.isEmpty()) add(SaveAddedFoodValidationError.LOTS_REQUIRED)
            if (validLots.any { it.expirationDate == null }) add(SaveAddedFoodValidationError.DATE_REQUIRED)
            if (command.lots.any { it.quantity < 0 }) add(SaveAddedFoodValidationError.QUANTITY_INVALID)
        }
        if (errors.isNotEmpty()) return SaveAddedFoodResult.ValidationError(errors)

        return try {
            val categoryId = pantryRepository.saveAddedFood(
                categorySelection = requireNotNull(command.categorySelection),
                lots = validLots,
                barcodeProductDraft = command.barcodeProductDraft
            )
            runCatching { maybeSaveCategoryImage(categoryId, command.barcodeProductDraft?.imageUrl) }
            SaveAddedFoodResult.Success(categoryId)
        } catch (_: Exception) {
            SaveAddedFoodResult.StorageError
        }
    }

    private suspend fun maybeSaveCategoryImage(categoryId: Long, imageUrl: String?) {
        val url = imageUrl?.takeIf { it.isNotBlank() } ?: return
        val category = pantryRepository.getFoodCategory(categoryId) ?: return
        if (!category.imageUri.isNullOrBlank()) return
        val localImageUri = imageStorage.saveCategoryImageFromUrl(categoryId, url) ?: return
        pantryRepository.updateFoodCategoryImageIfEmpty(categoryId, localImageUri)
    }
}
