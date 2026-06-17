package com.example.pantrypal.feature.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.domain.matching.FoodCategoryMatcher
import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.RecipeAvailability
import com.example.pantrypal.domain.model.RecipeAvailabilityStatus
import com.example.pantrypal.domain.model.RecipeDetail
import com.example.pantrypal.domain.model.RecipeDetailResult
import com.example.pantrypal.domain.model.RecipeIngredientAvailabilityItem
import com.example.pantrypal.domain.model.RecipeIngredientData
import com.example.pantrypal.domain.model.ReplaceRecipeIngredientLinksCommand
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.usecase.GetRecipeAvailabilityUseCase
import com.example.pantrypal.domain.usecase.ReplaceRecipeIngredientLinksUseCase
import com.example.pantrypal.domain.usecase.ToggleFavoriteRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val pantryRepository: PantryRepository,
    private val getRecipeAvailabilityUseCase: GetRecipeAvailabilityUseCase,
    private val replaceRecipeIngredientLinksUseCase: ReplaceRecipeIngredientLinksUseCase,
    private val toggleFavoriteRecipeUseCase: ToggleFavoriteRecipeUseCase,
    private val foodCategoryMatcher: FoodCategoryMatcher,
    private val textNormalizer: TextNormalizer
) : ViewModel() {
    private val externalId: String = savedStateHandle["recipeId"] ?: ""

    private val _uiState = MutableStateFlow(RecipeDetailUiState(externalId = externalId, isLoading = true))
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecipeDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var currentRecipe: RecipeDetail? = null
    private var currentAvailability: RecipeAvailability? = null
    private val forcePresentKeys = mutableSetOf<String>()
    private val forceMissingKeys = mutableSetOf<String>()
    private val selectedCategoryByIngredientKey = mutableMapOf<String, Long>()

    init {
        viewModelScope.launch { loadRecipe() }
    }

    fun onEvent(event: RecipeDetailEvent) {
        viewModelScope.launch {
            when (event) {
                RecipeDetailEvent.OnBackClick -> _effects.send(RecipeDetailEffect.NavigateBack)
                RecipeDetailEvent.OnFavoriteClick -> toggleFavorite()
                RecipeDetailEvent.OnShareClick -> shareShoppingList()
                RecipeDetailEvent.OnSummaryToggleClick -> _uiState.update {
                    it.copy(isSummaryExpanded = !it.isSummaryExpanded)
                }
                is RecipeDetailEvent.OnIngredientClick -> toggleIngredientExpansion(event.ingredientKey)
                RecipeDetailEvent.OnDismissIngredientSheet -> _uiState.update {
                    it.copy(linkSheetIngredientKey = null, linkSheetQuery = "", linkSheetCategories = emptyList(), linkSheetSelectedCategoryIds = emptySet())
                }
                is RecipeDetailEvent.OnAvailableCategoryClick -> selectAvailableCategory(event.ingredientKey, event.categoryId)
                is RecipeDetailEvent.OnManageIngredientLinksClick -> openLinkSheet(event.ingredientKey)
                is RecipeDetailEvent.OnLinkSheetQueryChange -> {
                    _uiState.update { it.copy(linkSheetQuery = event.value) }
                    refreshLinkSheetCategories()
                }
                is RecipeDetailEvent.OnLinkSheetCategoryToggle -> toggleLinkSheetCategory(event.categoryId)
                RecipeDetailEvent.OnLinkSheetCreateCategoryClick -> createLinkSheetCategory()
                RecipeDetailEvent.OnLinkSheetSaveClick -> saveLinkSheet()
                RecipeDetailEvent.OnMoveSelectedToBuyClick -> overrideSelected(present = false)
                RecipeDetailEvent.OnMarkSelectedInPantryClick -> overrideSelected(present = true)
            }
        }
    }

    private suspend fun loadRecipe() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, configMissing = false) }
        when (val result = recipeRepository.getRecipeDetailResult(externalId)) {
            is RecipeDetailResult.Success -> {
                currentRecipe = result.recipe
                refreshAvailability()
            }
            RecipeDetailResult.Empty -> _uiState.update {
                it.copy(isLoading = false, errorMessage = "Dettaglio ricetta non disponibile")
            }
            RecipeDetailResult.ConfigMissing -> _uiState.update {
                it.copy(isLoading = false, configMissing = true, errorMessage = null)
            }
            RecipeDetailResult.QuotaExceeded -> _uiState.update {
                it.copy(isLoading = false, errorMessage = "Quota Spoonacular esaurita")
            }
            RecipeDetailResult.RateLimited -> _uiState.update {
                it.copy(isLoading = false, errorMessage = "Troppe richieste a Spoonacular")
            }
            RecipeDetailResult.NetworkError -> _uiState.update {
                it.copy(isLoading = false, errorMessage = "Connessione non disponibile")
            }
            RecipeDetailResult.InvalidResponse -> _uiState.update {
                it.copy(isLoading = false, errorMessage = "Risposta ricetta non valida")
            }
            RecipeDetailResult.GenericError -> _uiState.update {
                it.copy(isLoading = false, errorMessage = "Errore durante il caricamento ricetta")
            }
        }
    }

    private suspend fun refreshAvailability() {
        val recipe = currentRecipe ?: return
        currentAvailability = getRecipeAvailabilityUseCase(recipe)
        val isFavorite = recipeRepository.isFavorite(recipe.externalId)
        _uiState.value = recipe.toUi(
            availability = requireNotNull(currentAvailability),
            isFavorite = isFavorite,
            forcePresentKeys = forcePresentKeys,
            forceMissingKeys = forceMissingKeys,
            selectedCategoryByIngredientKey = selectedCategoryByIngredientKey,
            previousState = _uiState.value
        )
    }

    private suspend fun toggleFavorite() {
        val recipe = currentRecipe ?: return
        val isFavorite = toggleFavoriteRecipeUseCase(recipe)
        _uiState.update { it.copy(isFavorite = isFavorite) }
    }

    private suspend fun toggleIngredientExpansion(key: String) {
        val ingredient = (_uiState.value.presentIngredients + _uiState.value.missingIngredients)
            .firstOrNull { it.key == key } ?: return
        if (_uiState.value.expandedIngredientKey == key) {
            _uiState.update { it.copy(expandedIngredientKey = null) }
            return
        }
        _uiState.update {
            it.copy(
                expandedIngredientKey = ingredient.key,
                linkSheetIngredientKey = null,
                linkSheetQuery = "",
                linkSheetCategories = emptyList()
            )
        }
    }

    private fun selectAvailableCategory(ingredientKey: String, categoryId: Long) {
        selectedCategoryByIngredientKey[ingredientKey] = categoryId
        currentRecipe?.let { recipe ->
            _uiState.value = recipe.toUi(
                availability = currentAvailability ?: RecipeAvailability(emptyList()),
                isFavorite = _uiState.value.isFavorite,
                forcePresentKeys = forcePresentKeys,
                forceMissingKeys = forceMissingKeys,
                selectedCategoryByIngredientKey = selectedCategoryByIngredientKey,
                previousState = _uiState.value
            )
        }
    }

    private suspend fun openLinkSheet(key: String) {
        val ingredient = (_uiState.value.presentIngredients + _uiState.value.missingIngredients)
            .firstOrNull { it.key == key } ?: return
        _uiState.update {
            it.copy(
                linkSheetIngredientKey = ingredient.key,
                linkSheetQuery = "",
                linkSheetSelectedCategoryIds = ingredient.linkedCategoryIds(),
                linkSheetCategories = emptyList(),
                canCreateLinkSheetCategory = false
            )
        }
        refreshLinkSheetCategories()
    }

    private suspend fun refreshLinkSheetCategories() {
        val state = _uiState.value
        val ingredient = currentLinkSheetIngredient() ?: return
        val query = state.linkSheetQuery.ifBlank { ingredient.name }
        val categories = pantryRepository.searchFoodCategories(query, limit = 100)
        val sources = pantryRepository.getFoodCategoryMatchSources(query, limit = 100)
        val shouldCreate = foodCategoryMatcher.shouldShowCreateNew(query, sources)
        _uiState.update {
            it.copy(
                linkSheetCategories = categories.map { category ->
                    RecipeLinkCategoryUi(
                        categoryId = category.id,
                        label = category.name,
                        selected = category.id in state.linkSheetSelectedCategoryIds
                    )
                },
                canCreateLinkSheetCategory = shouldCreate && query.isNotBlank()
            )
        }
    }

    private fun toggleLinkSheetCategory(categoryId: Long) {
        _uiState.update { state ->
            val selected = state.linkSheetSelectedCategoryIds.toMutableSet()
            if (!selected.add(categoryId)) selected.remove(categoryId)
            state.copy(
                linkSheetSelectedCategoryIds = selected,
                linkSheetCategories = state.linkSheetCategories.map {
                    if (it.categoryId == categoryId) it.copy(selected = it.categoryId in selected) else it
                }
            )
        }
    }

    private suspend fun createLinkSheetCategory() {
        val ingredient = currentLinkSheetIngredient() ?: return
        val name = _uiState.value.linkSheetQuery.ifBlank { ingredient.name }.trim()
        if (name.isBlank()) return
        val categoryId = createCategory(name)
        _uiState.update { it.copy(linkSheetSelectedCategoryIds = it.linkSheetSelectedCategoryIds + categoryId) }
        refreshLinkSheetCategories()
    }

    private suspend fun saveLinkSheet() {
        val ingredient = currentLinkSheetIngredient() ?: return
        replaceRecipeIngredientLinksUseCase(
            ReplaceRecipeIngredientLinksCommand(
                aliasOriginal = ingredient.name,
                normalizedAlias = ingredient.normalizedName,
                externalIngredientId = ingredient.externalIngredientId,
                categoryIds = _uiState.value.linkSheetSelectedCategoryIds
            )
        )
        _uiState.update {
            it.copy(linkSheetIngredientKey = null, linkSheetQuery = "", linkSheetCategories = emptyList(), linkSheetSelectedCategoryIds = emptySet())
        }
        refreshAvailability()
    }

    private suspend fun createCategory(name: String): Long {
        val normalizedName = textNormalizer.normalizeFoodText(name)
        pantryRepository.findCategoryByNormalizedName(normalizedName)?.let { return it.id }
        return pantryRepository.createFoodCategory(
            CreateFoodCategoryInput(
                name = name,
                normalizedName = normalizedName,
                defaultStorageLocation = StorageLocation.FRIDGE,
                defaultPerishability = PerishabilityType.FRESH
            )
        )
    }

    private fun overrideSelected(present: Boolean) {
        val selected = currentExpandedIngredient() ?: return
        if (present) {
            forcePresentKeys += selected.key
            forceMissingKeys -= selected.key
        } else {
            forceMissingKeys += selected.key
            forcePresentKeys -= selected.key
        }
        currentRecipe?.let { recipe ->
            _uiState.value = recipe.toUi(
                availability = currentAvailability ?: RecipeAvailability(emptyList()),
                isFavorite = _uiState.value.isFavorite,
                forcePresentKeys = forcePresentKeys,
                forceMissingKeys = forceMissingKeys,
                selectedCategoryByIngredientKey = selectedCategoryByIngredientKey,
                previousState = _uiState.value
            )
        }
    }

    private fun currentExpandedIngredient(): RecipeIngredientUi? {
        val key = _uiState.value.expandedIngredientKey ?: return null
        return (_uiState.value.presentIngredients + _uiState.value.missingIngredients)
            .firstOrNull { it.key == key }
    }

    private fun currentLinkSheetIngredient(): RecipeIngredientUi? {
        val key = _uiState.value.linkSheetIngredientKey ?: return null
        return (_uiState.value.presentIngredients + _uiState.value.missingIngredients)
            .firstOrNull { it.key == key }
    }

    private suspend fun shareShoppingList() {
        val state = _uiState.value
        val text = buildString {
            appendLine("Lista per ${state.title}")
            appendLine()
            appendLine("Hai gia:")
            if (state.presentIngredients.isEmpty()) {
                appendLine("- Nessun ingrediente")
            } else {
                state.presentIngredients.forEach { appendLine("- ${it.shareLabel()}") }
            }
            appendLine()
            appendLine("Da comprare:")
            if (state.missingIngredients.isEmpty()) {
                appendLine("- Nulla")
            } else {
                state.missingIngredients.forEach { appendLine("- ${it.name}") }
            }
            state.sourceUrl?.takeIf { it.isNotBlank() }?.let {
                appendLine()
                appendLine("Ricetta: $it")
            }
        }
        _effects.send(RecipeDetailEffect.ShareShoppingList(text))
    }
}

private fun RecipeIngredientUi.shareLabel(): String =
    selectedCategoryId?.let { selectedId ->
        (availableCategories + linkedCategories).firstOrNull { it.categoryId == selectedId }?.label
    } ?: name

private fun RecipeDetail.toUi(
    availability: RecipeAvailability,
    isFavorite: Boolean,
    forcePresentKeys: Set<String>,
    forceMissingKeys: Set<String>,
    selectedCategoryByIngredientKey: Map<String, Long> = emptyMap(),
    previousState: RecipeDetailUiState? = null
): RecipeDetailUiState {
    val ingredients = availability.items.mapIndexed { index, item ->
        item.toUi(index, selectedCategoryByIngredientKey)
    }.map { ingredient ->
        when {
            ingredient.key in forcePresentKeys -> ingredient.copy(isPresent = true, pantryMatchLabel = "Segnato in dispensa")
            ingredient.key in forceMissingKeys -> ingredient.copy(isPresent = false, pantryMatchLabel = null)
            else -> ingredient
        }
    }

    return RecipeDetailUiState(
        externalId = externalId,
        title = title,
        description = description.orEmpty().ifBlank { "Ricetta pronta con ingredienti selezionati." },
        sourceUrl = sourceUrl,
        readyInMinutes = preparationTimeMinutes ?: 0,
        servingsLabel = servings?.let { "$it persone" }.orEmpty(),
        isFavorite = isFavorite,
        presentIngredients = ingredients.filter { it.isPresent },
        missingIngredients = ingredients.filterNot { it.isPresent },
        isSummaryExpanded = previousState?.isSummaryExpanded ?: false,
        expandedIngredientKey = previousState?.expandedIngredientKey,
        linkSheetIngredientKey = previousState?.linkSheetIngredientKey,
        linkSheetQuery = previousState?.linkSheetQuery.orEmpty(),
        linkSheetCategories = previousState?.linkSheetCategories.orEmpty(),
        linkSheetSelectedCategoryIds = previousState?.linkSheetSelectedCategoryIds.orEmpty(),
        canCreateLinkSheetCategory = previousState?.canCreateLinkSheetCategory ?: false,
        isLoading = false,
        configMissing = false,
        errorMessage = null
    )
}

private fun RecipeIngredientAvailabilityItem.toUi(
    index: Int,
    selectedCategoryByIngredientKey: Map<String, Long>
): RecipeIngredientUi {
    val data = ingredient
    val key = data.externalIngredientId ?: "${data.normalizedName}#$index"
    val linkedLabel = linkedCategories.firstOrNull()?.name
    val isPresent = status == RecipeAvailabilityStatus.IN_PANTRY
    val selectedCategoryId = selectedCategoryByIngredientKey[key] ?: availableCategories.firstOrNull()?.id
    return RecipeIngredientUi(
        key = key,
        name = data.cleanName,
        amountLabel = data.amountLabel(),
        pantryMatchLabel = if (isPresent) {
            availableCategories.firstOrNull { it.id == selectedCategoryId }?.name ?: linkedLabel ?: "In dispensa"
        } else {
            linkedCategories.takeIf { it.isNotEmpty() }?.joinToString(prefix = "Collegato a: ") { it.name }
        },
        isPresent = isPresent,
        externalIngredientId = data.externalIngredientId,
        normalizedName = data.normalizedName,
        replaceLinkId = matchingLinks.firstOrNull()?.id,
        linkedCategoryNames = linkedCategories.map { it.name },
        linkedCategories = linkedCategories.map {
            RecipeLinkedCategoryUi(
                categoryId = it.id,
                label = it.name,
                selected = it.id == selectedCategoryId
            )
        },
        availableCategories = availableCategories.map {
            RecipeLinkedCategoryUi(
                categoryId = it.id,
                label = it.name,
                selected = it.id == selectedCategoryId
            )
        },
        selectedCategoryId = selectedCategoryId
    )
}

private fun RecipeIngredientUi.linkedCategoryIds(): Set<Long> =
    linkedCategories.map { it.categoryId }.toSet()

private fun RecipeIngredientData.amountLabel(): String =
    displayAmount?.takeIf { it.isNotBlank() } ?:
    listOfNotNull(
        amount?.let { amount ->
            if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
        },
        unit
    ).joinToString(" ").ifBlank { "q.b." }
