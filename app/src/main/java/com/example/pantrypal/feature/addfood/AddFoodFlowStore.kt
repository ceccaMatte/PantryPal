package com.example.pantrypal.feature.addfood

import com.example.pantrypal.domain.model.BarcodeProductDraft
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class AddFoodFlowStore @Inject constructor() {
    private var pendingPrefill: AddFoodPrefill? = null

    fun setPrefill(prefill: AddFoodPrefill) {
        pendingPrefill = prefill
    }

    fun consumePrefill(): AddFoodPrefill? =
        pendingPrefill.also { pendingPrefill = null }

    fun clear() {
        pendingPrefill = null
    }
}

data class AddFoodPrefill(
    val query: String,
    val selectedCategoryId: Long?,
    val barcodeProductDraft: BarcodeProductDraft?
)
