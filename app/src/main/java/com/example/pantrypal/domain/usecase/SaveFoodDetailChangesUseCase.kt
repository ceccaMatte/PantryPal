package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.domain.model.FoodDetailDraft
import com.example.pantrypal.domain.model.FoodLotDraft
import com.example.pantrypal.domain.model.SaveFoodDetailChangesResult
import com.example.pantrypal.domain.model.SaveFoodDetailValidationError
import javax.inject.Inject

class SaveFoodDetailChangesUseCase @Inject constructor(
    private val pantryRepository: PantryRepository
) {
    suspend operator fun invoke(draft: FoodDetailDraft): SaveFoodDetailChangesResult {
        if (draft.name.isBlank()) {
            return SaveFoodDetailChangesResult.ValidationError(setOf(SaveFoodDetailValidationError.NAME_REQUIRED))
        }

        val normalizedDraft = draft.copy(
            name = draft.name.trim(),
            lots = mergeLotsByDate(draft.lots)
        )

        return try {
            pantryRepository.saveFoodDetailChanges(normalizedDraft)
            SaveFoodDetailChangesResult.Success
        } catch (_: Exception) {
            SaveFoodDetailChangesResult.StorageError
        }
    }

    fun mergeLotsByDate(lots: List<FoodLotDraft>): List<FoodLotDraft> =
        lots
            .filter { it.quantity > 0 }
            .groupBy { it.expirationDate }
            .map { (date, dateLots) ->
                FoodLotDraft(
                    id = dateLots.firstNotNullOfOrNull { it.id },
                    expirationDate = date,
                    quantity = dateLots.sumOf { it.quantity }
                )
            }
            .sortedBy { it.expirationDate }
}
