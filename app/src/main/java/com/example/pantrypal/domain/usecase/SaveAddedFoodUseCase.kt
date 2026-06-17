package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.SaveAddedFoodCommand
import com.example.pantrypal.domain.model.SaveAddedFoodResult
import com.example.pantrypal.domain.model.SaveAddedFoodValidationError
import javax.inject.Inject

class SaveAddedFoodUseCase @Inject constructor(
    private val pantryRepository: PantryRepository
) {
    suspend operator fun invoke(command: SaveAddedFoodCommand): SaveAddedFoodResult {
        val errors = buildSet {
            val selection = command.categorySelection
            if (selection == null || selection is AddFoodCategorySelection.New && selection.name.isBlank()) {
                add(SaveAddedFoodValidationError.CATEGORY_REQUIRED)
            }
            if (command.expirationDate == null) add(SaveAddedFoodValidationError.DATE_REQUIRED)
            if (command.quantity <= 0) add(SaveAddedFoodValidationError.QUANTITY_INVALID)
        }
        if (errors.isNotEmpty()) return SaveAddedFoodResult.ValidationError(errors)

        return try {
            SaveAddedFoodResult.Success(
                pantryRepository.saveAddedFood(
                    categorySelection = requireNotNull(command.categorySelection),
                    expirationDate = requireNotNull(command.expirationDate),
                    quantity = command.quantity,
                    barcodeProductDraft = command.barcodeProductDraft
                )
            )
        } catch (_: Exception) {
            SaveAddedFoodResult.StorageError
        }
    }
}
