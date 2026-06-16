package com.example.pantrypal.data.product

import com.example.pantrypal.domain.model.ExternalProductResult

interface FoodRecognitionRepository {
    suspend fun lookupProductByBarcode(barcode: String): ExternalProductResult
}
