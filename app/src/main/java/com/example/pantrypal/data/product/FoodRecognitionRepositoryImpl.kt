package com.example.pantrypal.data.product

import com.example.pantrypal.data.product.remote.OpenFoodFactsApi
import com.example.pantrypal.domain.model.ExternalProductResult
import com.example.pantrypal.domain.model.RecognizedBarcodeProduct
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class FoodRecognitionRepositoryImpl @Inject constructor(
    private val api: OpenFoodFactsApi
) : FoodRecognitionRepository {
    override suspend fun lookupProductByBarcode(barcode: String): ExternalProductResult =
        try {
            val response = api.getProductByBarcode(barcode)
            val product = response.product
            val resolvedBarcode = response.code ?: barcode
            val name = product?.productName?.takeIf { it.isNotBlank() }
                ?: product?.genericName?.takeIf { it.isNotBlank() }

            if (response.status == 1 && product != null && name != null) {
                ExternalProductResult.Found(
                    RecognizedBarcodeProduct(
                        barcode = resolvedBarcode,
                        productName = product.productName,
                        genericName = product.genericName,
                        brand = product.brands,
                        quantityLabel = product.quantity,
                        imageUrl = product.imageUrl,
                        rawCategoryTags = product.categoriesTags.orEmpty(),
                        rawFoodGroupTags = product.foodGroupTags.orEmpty()
                    )
                )
            } else {
                ExternalProductResult.NotFound
            }
        } catch (error: HttpException) {
            when (error.code()) {
                404 -> ExternalProductResult.NotFound
                429, 503 -> ExternalProductResult.RateLimited
                else -> ExternalProductResult.NetworkError
            }
        } catch (_: IOException) {
            ExternalProductResult.NetworkError
        } catch (_: Exception) {
            ExternalProductResult.InvalidResponse
        }
}
