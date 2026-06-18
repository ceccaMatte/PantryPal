package com.example.pantrypal.data.product

import android.util.Log
import com.example.pantrypal.data.product.remote.OpenFoodFactsApi
import com.example.pantrypal.data.product.remote.dto.OpenFoodFactsProductDto
import com.example.pantrypal.data.product.remote.dto.OpenFoodFactsProductResponseDto
import com.example.pantrypal.domain.model.ExternalProductResult
import com.example.pantrypal.domain.model.RecognizedBarcodeProduct
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import retrofit2.HttpException

@Singleton
class FoodRecognitionRepositoryImpl @Inject constructor(
    private val api: OpenFoodFactsApi
) : FoodRecognitionRepository {

    override suspend fun lookupProductByBarcode(barcode: String): ExternalProductResult {
        return try {
            Log.d(TAG, "OpenFoodFacts parse start: $barcode")
            val response = api.getProductByBarcode(barcode)
            val product = response.product
            val resolvedBarcode = response.code ?: barcode
            Log.d(TAG, "OpenFoodFacts DTO: code=${response.code} productNull=${product == null} status=${response.status}")

            if (product == null || !isProductFound(response)) {
                Log.d(TAG, "OpenFoodFacts result: NotFound reason=product_null_or_status_failure barcode=$barcode")
                return ExternalProductResult.NotFound
            }

            Log.d(TAG, "OpenFoodFacts product fields: productName=${product.productName}" +
                " productNameIt=${product.productNameIt}" +
                " genericName=${product.genericName}" +
                " brands=${product.brands}" +
                " quantity=${product.quantity}")

            val displayName = buildDisplayName(product, resolvedBarcode)
            Log.d(TAG, "OpenFoodFacts mapped displayName=$displayName")
            Log.d(TAG, "OpenFoodFacts result: Found barcode=$resolvedBarcode")

            ExternalProductResult.Found(
                RecognizedBarcodeProduct(
                    barcode = resolvedBarcode,
                    productName = displayName,
                    genericName = product.genericName.cleanOrNull()
                        ?: product.genericNameIt.cleanOrNull()
                        ?: product.genericNameEn.cleanOrNull(),
                    brand = product.brands.cleanOrNull(),
                    quantityLabel = product.quantity.cleanOrNull(),
                    imageUrl = product.imageFrontUrl.cleanOrNull() ?: product.imageUrl.cleanOrNull(),
                    rawCategoryTags = product.categoriesTags.orEmpty(),
                    rawFoodGroupTags = product.foodGroupTags.orEmpty()
                )
            )
        } catch (error: HttpException) {
            Log.d(TAG, "OpenFoodFacts HTTP: $barcode status=${error.code()}")
            when (error.code()) {
                404 -> ExternalProductResult.NotFound
                429, 503 -> ExternalProductResult.RateLimited
                else -> ExternalProductResult.NetworkError
            }
        } catch (_: IOException) {
            Log.d(TAG, "OpenFoodFacts result: NetworkError barcode=$barcode")
            ExternalProductResult.NetworkError
        } catch (e: Exception) {
            Log.d(TAG, "OpenFoodFacts result: InvalidResponse reason=${e.javaClass.simpleName}: ${e.message}")
            ExternalProductResult.InvalidResponse
        }
    }

    // OFA v3.6 uses status="success"/"failure" (String).
    // OFA v2 used status=1/0 (Int).
    // We accept both via JsonElement; fallback to product presence if status is absent or unrecognized.
    private fun isProductFound(response: OpenFoodFactsProductResponseDto): Boolean {
        val statusEl = response.status ?: return response.product != null
        if (statusEl !is JsonPrimitive) return response.product != null
        return if (statusEl.isString) {
            statusEl.content.lowercase() in setOf("success", "1")
        } else {
            statusEl.intOrNull == 1
        }
    }

    private fun buildDisplayName(product: OpenFoodFactsProductDto, barcode: String): String =
        product.productName.cleanOrNull()
            ?: product.productNameIt.cleanOrNull()
            ?: product.productNameEn.cleanOrNull()
            ?: product.genericName.cleanOrNull()
            ?: product.genericNameIt.cleanOrNull()
            ?: product.genericNameEn.cleanOrNull()
            ?: listOfNotNull(product.brands.cleanOrNull(), product.quantity.cleanOrNull())
                .joinToString(" ")
                .takeIf { it.isNotBlank() }
            ?: "Prodotto $barcode"

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotBlank() }

    companion object {
        private const val TAG = "PantryPalOFA"
    }
}
