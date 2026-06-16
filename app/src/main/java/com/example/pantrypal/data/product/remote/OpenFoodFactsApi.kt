package com.example.pantrypal.data.product.remote

import com.example.pantrypal.data.product.remote.dto.OpenFoodFactsProductResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v3.6/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OpenFoodFactsProductResponseDto
}
