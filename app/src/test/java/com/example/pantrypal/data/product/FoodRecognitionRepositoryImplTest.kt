package com.example.pantrypal.data.product

import com.example.pantrypal.data.product.remote.OpenFoodFactsApi
import com.example.pantrypal.data.product.remote.dto.OpenFoodFactsProductDto
import com.example.pantrypal.data.product.remote.dto.OpenFoodFactsProductResponseDto
import com.example.pantrypal.domain.model.ExternalProductResult
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class FoodRecognitionRepositoryImplTest {

    // --- Helpers ---

    private fun buildRepo(
        apiImpl: suspend (String) -> OpenFoodFactsProductResponseDto
    ): FoodRecognitionRepositoryImpl {
        val api = object : OpenFoodFactsApi {
            override suspend fun getProductByBarcode(barcode: String) = apiImpl(barcode)
        }
        return FoodRecognitionRepositoryImpl(api)
    }

    private fun buildRepo(fixed: OpenFoodFactsProductResponseDto) = buildRepo { fixed }

    private fun makeHttpException(code: Int): HttpException {
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = "{}".toResponseBody(mediaType)
        val rawResponse = Response.Builder()
            .code(code)
            .message("HTTP $code")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://world.openfoodfacts.org/").build())
            .body(body)
            .build()
        val errorResponse = retrofit2.Response.error<OpenFoodFactsProductResponseDto>(
            "{}".toResponseBody(mediaType),
            rawResponse
        )
        return HttpException(errorResponse)
    }

    // --- Display name fallback chain ---

    @Test
    fun `product_name standard is used as display name`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            code = "1234567890123",
            status = JsonPrimitive("success"),
            product = OpenFoodFactsProductDto(productName = "Latte Parmalat")
        ))
        val result = repo.lookupProductByBarcode("1234567890123")
        assertTrue(result is ExternalProductResult.Found)
        assertEquals("Latte Parmalat", (result as ExternalProductResult.Found).product.productName)
    }

    @Test
    fun `product_name_it used when product_name absent`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            code = "1234567890123",
            status = JsonPrimitive("success"),
            product = OpenFoodFactsProductDto(productNameIt = "Wasa Cracker IT")
        ))
        val result = repo.lookupProductByBarcode("1234567890123")
        assertTrue(result is ExternalProductResult.Found)
        assertEquals("Wasa Cracker IT", (result as ExternalProductResult.Found).product.productName)
    }

    @Test
    fun `product_name_en used when product_name and product_name_it absent`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            code = "1234567890123",
            status = JsonPrimitive("success"),
            product = OpenFoodFactsProductDto(productNameEn = "Wasa Cracker EN")
        ))
        val result = repo.lookupProductByBarcode("1234567890123")
        assertTrue(result is ExternalProductResult.Found)
        assertEquals("Wasa Cracker EN", (result as ExternalProductResult.Found).product.productName)
    }

    @Test
    fun `brands and quantity joined as display name when all name fields absent`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            code = "1234567890123",
            status = JsonPrimitive("success"),
            product = OpenFoodFactsProductDto(brands = "Parmalat", quantity = "1L")
        ))
        val result = repo.lookupProductByBarcode("1234567890123")
        assertTrue(result is ExternalProductResult.Found)
        assertEquals("Parmalat 1L", (result as ExternalProductResult.Found).product.productName)
    }

    @Test
    fun `product with no name fields falls back to Prodotto barcode`() = runTest {
        val barcode = "1234567890123"
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            code = barcode,
            status = JsonPrimitive("success"),
            product = OpenFoodFactsProductDto()
        ))
        val result = repo.lookupProductByBarcode(barcode)
        assertTrue(result is ExternalProductResult.Found)
        assertEquals("Prodotto $barcode", (result as ExternalProductResult.Found).product.productName)
    }

    // --- Status field variations (String vs Int) ---

    @Test
    fun `status String success is accepted as found`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            status = JsonPrimitive("success"),
            product = OpenFoodFactsProductDto(productName = "Test")
        ))
        assertTrue(repo.lookupProductByBarcode("1234567890123") is ExternalProductResult.Found)
    }

    @Test
    fun `status Int 1 is accepted as found`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            status = JsonPrimitive(1),
            product = OpenFoodFactsProductDto(productName = "Test")
        ))
        assertTrue(repo.lookupProductByBarcode("1234567890123") is ExternalProductResult.Found)
    }

    @Test
    fun `status String failure with product returns NotFound`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            status = JsonPrimitive("failure"),
            product = OpenFoodFactsProductDto(productName = "Test")
        ))
        assertEquals(ExternalProductResult.NotFound, repo.lookupProductByBarcode("1234567890123"))
    }

    @Test
    fun `status Int 0 with product returns NotFound`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            status = JsonPrimitive(0),
            product = OpenFoodFactsProductDto(productName = "Test")
        ))
        assertEquals(ExternalProductResult.NotFound, repo.lookupProductByBarcode("1234567890123"))
    }

    @Test
    fun `null status with product present returns Found`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(
            status = null,
            product = OpenFoodFactsProductDto(productName = "Test")
        ))
        assertTrue(repo.lookupProductByBarcode("1234567890123") is ExternalProductResult.Found)
    }

    @Test
    fun `null status with null product returns NotFound`() = runTest {
        val repo = buildRepo(OpenFoodFactsProductResponseDto(status = null, product = null))
        assertEquals(ExternalProductResult.NotFound, repo.lookupProductByBarcode("1234567890123"))
    }

    // --- JSON with unknown fields ---

    @Test
    fun `DTO with unknown JSON fields parses without failure`() {
        val json = Json { ignoreUnknownKeys = true }
        val raw = """
            {
              "code": "1234567890123",
              "status": "success",
              "unknown_field_xyz": true,
              "product": {
                "product_name": "Test Product",
                "future_server_field": 99
              }
            }
        """.trimIndent()
        val dto = json.decodeFromString<OpenFoodFactsProductResponseDto>(raw)
        assertEquals("1234567890123", dto.code)
        assertEquals("Test Product", dto.product?.productName)
    }

    @Test
    fun `status as String in JSON does not cause SerializationException`() {
        val json = Json { ignoreUnknownKeys = true }
        val raw = """{"code":"123","status":"success","product":{"product_name":"P"}}"""
        val dto = json.decodeFromString<OpenFoodFactsProductResponseDto>(raw)
        assertTrue((dto.status as? JsonPrimitive)?.isString == true)
    }

    @Test
    fun `status as Int in JSON does not cause SerializationException`() {
        val json = Json { ignoreUnknownKeys = true }
        val raw = """{"code":"123","status":1,"product":{"product_name":"P"}}"""
        val dto = json.decodeFromString<OpenFoodFactsProductResponseDto>(raw)
        assertEquals(1, (dto.status as? JsonPrimitive)?.intOrNull)
    }

    // --- HTTP error codes ---

    @Test
    fun `HTTP 404 returns NotFound`() = runTest {
        val repo = buildRepo { throw makeHttpException(404) }
        assertEquals(ExternalProductResult.NotFound, repo.lookupProductByBarcode("1234567890123"))
    }

    @Test
    fun `HTTP 429 returns RateLimited`() = runTest {
        val repo = buildRepo { throw makeHttpException(429) }
        assertEquals(ExternalProductResult.RateLimited, repo.lookupProductByBarcode("1234567890123"))
    }

    @Test
    fun `HTTP 503 returns RateLimited`() = runTest {
        val repo = buildRepo { throw makeHttpException(503) }
        assertEquals(ExternalProductResult.RateLimited, repo.lookupProductByBarcode("1234567890123"))
    }

    @Test
    fun `IOException returns NetworkError`() = runTest {
        val repo = buildRepo { throw IOException("No connectivity") }
        assertEquals(ExternalProductResult.NetworkError, repo.lookupProductByBarcode("1234567890123"))
    }

    @Test
    fun `generic exception returns InvalidResponse`() = runTest {
        val repo = buildRepo { throw RuntimeException("unexpected parse error") }
        assertEquals(ExternalProductResult.InvalidResponse, repo.lookupProductByBarcode("1234567890123"))
    }
}
