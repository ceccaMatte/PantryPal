package com.example.pantrypal.feature.addfood

import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.product.FoodRecognitionRepository
import com.example.pantrypal.domain.matching.FoodCategoryMatcher
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.AddFoodLotDraft
import com.example.pantrypal.domain.model.BarcodeProductDraft
import com.example.pantrypal.domain.model.BarcodeProductLink
import com.example.pantrypal.domain.model.BarcodeResolution
import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.ExternalProductResult
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodDetailData
import com.example.pantrypal.domain.model.FoodDetailDraft
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.StorageLocationFilter
import com.example.pantrypal.domain.usecase.ResolveBarcodeUseCase
import com.example.pantrypal.domain.usecase.SaveAddedFoodUseCase
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModelBarcodeTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Anti-duplication ---

    @Test
    fun `OnBarcodeDetected with valid barcode sets isProcessingBarcode true while resolving`() = runTest {
        val deferred = CompletableDeferred<ExternalProductResult>()
        val vm = buildViewModel(remoteLookup = { deferred.await() })

        vm.onScanEvent(ScanEvent.OnBarcodeDetected("1234567890123"))

        assertTrue("isProcessingBarcode should be true while use case is suspended",
            vm.scanState.value.isProcessingBarcode)

        deferred.complete(ExternalProductResult.NotFound)
    }

    @Test
    fun `second OnBarcodeDetected while processing is ignored`() = runTest {
        val callCount = AtomicInteger(0)
        val deferred = CompletableDeferred<ExternalProductResult>()
        val vm = buildViewModel(remoteLookup = {
            callCount.incrementAndGet()
            deferred.await()
        })

        vm.onScanEvent(ScanEvent.OnBarcodeDetected("1234567890123"))
        vm.onScanEvent(ScanEvent.OnBarcodeDetected("1234567890123")) // duplicate, should be ignored
        vm.onScanEvent(ScanEvent.OnBarcodeDetected("9876543210987")) // different barcode, still ignored

        assertEquals("Use case should be called exactly once", 1, callCount.get())

        deferred.complete(ExternalProductResult.NotFound)
    }

    @Test
    fun `network error resets isProcessingBarcode to false`() = runTest {
        val vm = buildViewModel(remoteLookup = { ExternalProductResult.NetworkError })

        vm.onScanEvent(ScanEvent.OnBarcodeDetected("1234567890123"))

        assertFalse("isProcessingBarcode should be false after network error",
            vm.scanState.value.isProcessingBarcode)
    }

    @Test
    fun `rate limited resets isProcessingBarcode to false`() = runTest {
        val vm = buildViewModel(remoteLookup = { ExternalProductResult.RateLimited })

        vm.onScanEvent(ScanEvent.OnBarcodeDetected("1234567890123"))

        assertFalse(vm.scanState.value.isProcessingBarcode)
    }

    @Test
    fun `not found resets isProcessingBarcode to false`() = runTest {
        val vm = buildViewModel(remoteLookup = { ExternalProductResult.NotFound })

        vm.onScanEvent(ScanEvent.OnBarcodeDetected("1234567890123"))

        assertFalse(vm.scanState.value.isProcessingBarcode)
    }

    @Test
    fun `OnRetryScanClick resets isProcessingBarcode`() = runTest {
        val deferred = CompletableDeferred<ExternalProductResult>()
        val vm = buildViewModel(remoteLookup = { deferred.await() })

        vm.onScanEvent(ScanEvent.OnBarcodeDetected("1234567890123"))
        assertTrue(vm.scanState.value.isProcessingBarcode)

        deferred.cancel()
        vm.onScanEvent(ScanEvent.OnRetryScanClick)
        assertFalse(vm.scanState.value.isProcessingBarcode)
    }

    // --- Permission ---

    @Test
    fun `OnCameraPermissionResult granted sets hasCameraPermission true`() = runTest {
        val vm = buildViewModel()

        vm.onScanEvent(ScanEvent.OnCameraPermissionResult(granted = true))

        assertTrue(vm.scanState.value.hasCameraPermission)
        assertFalse(vm.scanState.value.isRequestingPermission)
    }

    @Test
    fun `OnCameraPermissionResult denied sets hasCameraPermission false`() = runTest {
        val vm = buildViewModel()

        vm.onScanEvent(ScanEvent.OnCameraPermissionResult(granted = false))

        assertFalse(vm.scanState.value.hasCameraPermission)
        assertFalse(vm.scanState.value.isRequestingPermission)
    }

    @Test
    fun `OnRequestCameraPermissionClick sets isRequestingPermission true`() = runTest {
        val vm = buildViewModel()

        vm.onScanEvent(ScanEvent.OnRequestCameraPermissionClick)

        assertTrue(vm.scanState.value.isRequestingPermission)
    }

    // --- Helpers ---

    private fun buildViewModel(
        remoteLookup: suspend (String) -> ExternalProductResult = { ExternalProductResult.NotFound }
    ): AddFoodViewModel {
        val textNormalizer = TextNormalizer()
        val fakePantry = MinimalFakePantryRepository()
        val fakeRecognition = object : FoodRecognitionRepository {
            override suspend fun lookupProductByBarcode(barcode: String) = remoteLookup(barcode)
        }
        val resolveUseCase = ResolveBarcodeUseCase(fakePantry, fakeRecognition, FoodCategoryMatcher(textNormalizer), textNormalizer)
        return AddFoodViewModel(
            pantryRepository = fakePantry,
            foodCategoryMatcher = FoodCategoryMatcher(textNormalizer),
            saveAddedFoodUseCase = SaveAddedFoodUseCase(fakePantry),
            resolveBarcodeUseCase = resolveUseCase,
            addFoodFlowStore = AddFoodFlowStore(),
            textNormalizer = textNormalizer
        )
    }
}

private class MinimalFakePantryRepository : PantryRepository {
    override fun observePantryRows(filter: StorageLocationFilter): Flow<List<PantryRow>> = emptyFlow()
    override fun observeFoodDetail(categoryId: Long): Flow<FoodDetailData?> = emptyFlow()
    override fun observeActiveLotsWithCategories(): Flow<List<LotWithCategory>> = emptyFlow()
    override suspend fun getActiveLotsWithCategories(): List<LotWithCategory> = emptyList()
    override suspend fun getActiveLotsForCategories(categoryIds: List<Long>): List<LotWithCategory> = emptyList()
    override suspend fun getFoodCategory(categoryId: Long): FoodCategory? = null
    override suspend fun searchFoodCategories(query: String, limit: Int): List<FoodCategory> = emptyList()
    override suspend fun getFoodCategoryMatchSources(query: String, limit: Int): List<FoodCategoryMatchSource> = emptyList()
    override suspend fun findCategoryByNormalizedName(normalizedName: String): FoodCategory? = null
    override suspend fun findActiveBarcodeLink(barcode: String): BarcodeProductLink? = null
    override suspend fun createFoodCategory(input: CreateFoodCategoryInput): Long = 0
    override suspend fun saveAddedFood(
        categorySelection: AddFoodCategorySelection,
        lots: List<AddFoodLotDraft>,
        barcodeProductDraft: BarcodeProductDraft?
    ): Long = 0
    override suspend fun saveFoodDetailChanges(draft: FoodDetailDraft) = Unit
    override suspend fun upsertExpiryLot(categoryId: Long, expirationDate: LocalDate, quantityDelta: Int) = Unit
    override suspend fun decrementSingleLotCategory(categoryId: Long): Boolean = false
    override suspend fun addRecipeIngredientAlias(categoryId: Long, aliasOriginal: String, language: String?): Long = 0
    override suspend fun removeRecipeIngredientAlias(linkId: Long) = Unit
    override suspend fun deactivateBarcodeLink(barcode: String) = Unit
}
