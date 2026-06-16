# PantryPal — UI State / ViewModel Contract

> Versione 1.0  
> Scopo: definire come ogni schermata viene rappresentata nel ViewModel tramite `UiState`, `Event` ed `Effect`.

---

## 1. Principi generali

### 1.1 Pattern UDF

Ogni schermata segue un flusso unidirezionale:

```text
Composable
→ invia Event al ViewModel
→ ViewModel aggiorna UiState
→ Composable ridisegna la UI
→ ViewModel può emettere Effect one-shot
```

Schema consigliato:

```kotlin
val uiState: StateFlow<ScreenUiState>
val effects: Flow<ScreenEffect>

fun onEvent(event: ScreenEvent)
```

---

### 1.2 UiState

`UiState` rappresenta tutto ciò che serve per disegnare la schermata in un dato momento.

Contiene:

```text
dati da mostrare
loading state
empty state
errori inline
sheet attivo
stato draft locale
selezioni temporanee
```

Non contiene:

```text
NavController
Context Android
DAO
Repository
oggetti Room Entity direttamente, se non trasformati in modelli UI
```

---

### 1.3 Event

`Event` rappresenta un’azione dell’utente.

Esempi:

```text
OnSaveClick
OnBackClick
OnCategorySelected
OnQuantityIncrement
OnFavoriteClick
OnShareClick
```

La UI non chiama direttamente use case o repository.

---

### 1.4 Effect

`Effect` rappresenta eventi one-shot.

Esempi:

```text
NavigateBack
NavigateTo(route)
ShowSnackbar(message)
OpenShareSheet(text)
RequestNotificationPermission
OpenDatePicker
```

Gli effect non devono restare dentro `UiState`, perché non sono stato persistente della schermata.

---

## 2. Errori, loading e Material Design

### 2.1 Errori di validazione form

Per errori legati a campi compilati male si usano componenti standard Material:

```text
TextField isError
supportingText
messaggi inline vicino al campo
```

Esempi:

```text
categoria non selezionata
data scadenza mancante
quantità non valida
nome categoria vuoto
```

Non si usano snackbar per errori che l’utente deve correggere in un campo specifico.

---

### 2.2 Errori generici

Per errori non associati a un campo specifico si usa snackbar Material.

Esempi:

```text
errore di rete
errore salvataggio
permesso negato
API non disponibile
```

---

### 2.3 Loading

Per MVP si usa loading semplice:

```text
CircularProgressIndicator
LinearProgressIndicator dove più adatto
```

Non sono richiesti skeleton custom.

---

### 2.4 Empty state

Gli stati vuoti sono rappresentati nel `UiState` con flag o sealed state dedicati.

Esempi:

```text
dispensa vuota
nessun alimento in scadenza
nessun preferito
nessuna ricetta suggerita
offline
```

---

## 3. Gestione bottom sheet / modal

I bottom sheet sono gestiti dalla schermata che li possiede.

Non esiste uno stato globale dei modal.

Esempio:

```kotlin
data class AddFoodUiState(
    val activeSheet: AddFoodSheet? = null
)

sealed interface AddFoodSheet {
    data object AddChoice : AddFoodSheet
    data class ProductRecognized(...) : AddFoodSheet
    data object AddExpiryLot : AddFoodSheet
}
```

Regola:

```text
ogni ViewModel gestisce i propri sheet nel proprio UiState
```

---

## 4. Home

### 4.1 Responsabilità

La Home mostra:

```text
saluto utente
totale confezioni
conteggi Frigo / Freezer / Dispensa
alimenti in scadenza
ricette suggerite
```

La overview locale e le ricette suggerite sono caricate separatamente.

---

### 4.2 HomeUiState

```kotlin
data class HomeUiState(
    val isOverviewLoading: Boolean = true,
    val isSuggestedRecipesLoading: Boolean = false,

    val username: String? = null,

    val totalPackages: Int = 0,
    val fridgePackages: Int = 0,
    val freezerPackages: Int = 0,
    val pantryPackages: Int = 0,

    val expiringFoods: List<HomeExpiringFoodUi> = emptyList(),

    val suggestedRecipes: List<RecipeCardUi> = emptyList(),
    val suggestedRecipesState: SuggestedRecipesState = SuggestedRecipesState.Idle,

    val errorMessage: String? = null
)
```

---

### 4.3 HomeExpiringFoodUi

```kotlin
data class HomeExpiringFoodUi(
    val categoryId: Long,
    val name: String,
    val expiringQuantity: Int,
    val nearestExpirationDate: LocalDate,
    val storageLocation: StorageLocation
)
```

Regola:

```text
se più lotti dello stesso alimento sono in scadenza,
si mostra una sola chip con expiringQuantity totale
```

Esempio:

```text
Latte ×3
```

---

### 4.4 SuggestedRecipesState

```kotlin
sealed interface SuggestedRecipesState {
    data object Idle : SuggestedRecipesState
    data object Loading : SuggestedRecipesState
    data object EmptyPantry : SuggestedRecipesState
    data object Offline : SuggestedRecipesState
    data object Error : SuggestedRecipesState
    data object Success : SuggestedRecipesState
}
```

---

### 4.5 HomeEvent

```kotlin
sealed interface HomeEvent {
    data object OnRetrySuggestedRecipesClick : HomeEvent
    data class OnExpiringFoodClick(val categoryId: Long) : HomeEvent
    data class OnStorageStatClick(val filter: StorageLocationFilter) : HomeEvent
    data class OnRecipeClick(val recipeId: String) : HomeEvent
    data object OnFabClick : HomeEvent
}
```

---

### 4.6 HomeEffect

```kotlin
sealed interface HomeEffect {
    data class NavigateToPantry(
        val filter: StorageLocationFilter,
        val focusedExpiringCategoryId: Long? = null
    ) : HomeEffect

    data class NavigateToRecipeDetail(val recipeId: String) : HomeEffect
    data object OpenAddChoiceSheet : HomeEffect
    data class ShowSnackbar(val message: String) : HomeEffect
}
```

---

## 5. Dispensa

### 5.1 Responsabilità

La Dispensa mostra:

```text
filtro luogo persistente
sezione In scadenza
lista alimenti presenti
stepper rapido quando non ambiguo
```

---

### 5.2 PantryUiState

```kotlin
data class PantryUiState(
    val isLoading: Boolean = true,
    val selectedFilter: StorageLocationFilter = StorageLocationFilter.ALL,

    val expiringFoods: List<ExpiringFoodCardUi> = emptyList(),
    val pantryRows: List<PantryRowUi> = emptyList(),

    val focusedExpiringCategoryId: Long? = null,
    val errorMessage: String? = null
)
```

---

### 5.3 PantryRowUi

```kotlin
data class PantryRowUi(
    val categoryId: Long,
    val name: String,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val totalQuantity: Int,
    val nearestExpirationDate: LocalDate?,
    val lotCount: Int
)
```

---

### 5.4 ExpiringFoodCardUi

```kotlin
data class ExpiringFoodCardUi(
    val categoryId: Long,
    val name: String,
    val expiringQuantity: Int,
    val nearestExpirationDate: LocalDate,
    val storageLocation: StorageLocation,
    val isFocused: Boolean = false
)
```

---

### 5.5 PantryEvent

```kotlin
sealed interface PantryEvent {
    data class OnFilterSelected(val filter: StorageLocationFilter) : PantryEvent
    data class OnFoodClick(val categoryId: Long) : PantryEvent
    data class OnExpiringFoodClick(val categoryId: Long) : PantryEvent
    data class OnMinusClick(val row: PantryRowUi) : PantryEvent
    data class OnPlusClick(val categoryId: Long) : PantryEvent
    data object OnFabClick : PantryEvent
}
```

Regole:

```text
− con una sola scadenza → decremento diretto
− con più scadenze → naviga a Dettaglio Alimento
+ → naviga sempre a Dettaglio Alimento
```

---

### 5.6 PantryEffect

```kotlin
sealed interface PantryEffect {
    data class NavigateToFoodDetail(val categoryId: Long) : PantryEffect
    data object OpenAddChoiceSheet : PantryEffect
    data class ShowSnackbar(val message: String) : PantryEffect
}
```

---

## 6. Dettaglio Alimento

### 6.1 Responsabilità

Il Dettaglio Alimento consente di modificare l’alimento e i suoi lotti.

Tutte le modifiche sono draft locali fino a:

```text
Salva modifiche
```

Back senza salvare:

```text
scarta draft
nessun modal di conferma
```

---

### 6.2 FoodDetailUiState

```kotlin
data class FoodDetailUiState(
    val isLoading: Boolean = true,

    val original: FoodDetailSnapshot? = null,
    val draft: FoodDetailDraft? = null,

    val activeSheet: FoodDetailSheet? = null,

    val nameError: String? = null,
    val genericErrorMessage: String? = null
)
```

---

### 6.3 FoodDetailSnapshot

```kotlin
data class FoodDetailSnapshot(
    val categoryId: Long,
    val name: String,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val imageUri: String?,
    val lots: List<ExpiryLotUi>,
    val barcodeLinks: List<BarcodeProductLinkUi>,
    val recipeIngredientLinks: List<RecipeIngredientLinkUi>
)
```

---

### 6.4 FoodDetailDraft

```kotlin
data class FoodDetailDraft(
    val categoryId: Long,
    val name: String,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val imageUri: String?,
    val lots: List<DraftExpiryLotUi>
)
```

---

### 6.5 DraftExpiryLotUi

```kotlin
data class DraftExpiryLotUi(
    val localId: String,
    val persistedLotId: Long?,
    val expirationDate: LocalDate,
    val quantity: Int
)
```

Regole:

```text
quantity = 0 → lotto rimosso dalla lista UI
se tutti i lotti sono rimossi, la pagina resta visibile
```

---

### 6.6 FoodDetailSheet

```kotlin
sealed interface FoodDetailSheet {
    data object AddExpiryLot : FoodDetailSheet
    data class EditExpiryLotDate(val localLotId: String) : FoodDetailSheet
}
```

---

### 6.7 FoodDetailEvent

```kotlin
sealed interface FoodDetailEvent {
    data object OnBackClick : FoodDetailEvent
    data object OnSaveClick : FoodDetailEvent

    data class OnNameChanged(val value: String) : FoodDetailEvent
    data class OnStorageLocationChanged(val value: StorageLocation) : FoodDetailEvent
    data class OnPerishabilityChanged(val value: PerishabilityType) : FoodDetailEvent

    data class OnLotIncrement(val localLotId: String) : FoodDetailEvent
    data class OnLotDecrement(val localLotId: String) : FoodDetailEvent
    data object OnAddExpiryLotClick : FoodDetailEvent
    data class OnExpiryLotAdded(val expirationDate: LocalDate, val quantity: Int) : FoodDetailEvent
    data class OnExpiryLotDateChanged(val localLotId: String, val newDate: LocalDate) : FoodDetailEvent

    data object OnManageLinksClick : FoodDetailEvent
    data object OnFabClick : FoodDetailEvent
}
```

---

### 6.8 FoodDetailEffect

```kotlin
sealed interface FoodDetailEffect {
    data object NavigateBack : FoodDetailEffect
    data class NavigateToManageFoodLinks(val categoryId: Long) : FoodDetailEffect
    data object OpenAddChoiceSheet : FoodDetailEffect
    data class ShowSnackbar(val message: String) : FoodDetailEffect
}
```

---

## 7. Gestisci collegamenti alimento

### 7.1 Responsabilità

Schermata per gestire:

```text
prodotti scansionati collegati
nomi ingredienti ricetta collegati
```

---

### 7.2 FoodLinksUiState

```kotlin
data class FoodLinksUiState(
    val isLoading: Boolean = true,
    val categoryId: Long,
    val categoryName: String = "",

    val barcodeLinks: List<BarcodeProductLinkUi> = emptyList(),
    val recipeIngredientLinks: List<RecipeIngredientLinkUi> = emptyList(),

    val newAliasText: String = "",
    val aliasError: String? = null,
    val genericErrorMessage: String? = null
)
```

---

### 7.3 FoodLinksEvent

```kotlin
sealed interface FoodLinksEvent {
    data object OnBackClick : FoodLinksEvent
    data class OnRemoveBarcodeLinkClick(val barcode: String) : FoodLinksEvent
    data class OnRemoveRecipeIngredientLinkClick(val linkId: Long) : FoodLinksEvent
    data class OnNewAliasChanged(val value: String) : FoodLinksEvent
    data object OnAddAliasClick : FoodLinksEvent
}
```

---

### 7.4 FoodLinksEffect

```kotlin
sealed interface FoodLinksEffect {
    data object NavigateBack : FoodLinksEffect
    data class ShowSnackbar(val message: String) : FoodLinksEffect
}
```

---

## 8. Flow Aggiunta alimento

### 8.1 Responsabilità

Il flow di aggiunta alimento comprende:

```text
AddChoice
Scan Barcode
ProductRecognized
Aggiunta Manuale
```

Durante questo flow la bottom nav è nascosta.

---

### 8.2 AddFoodUiState

```kotlin
data class AddFoodUiState(
    val isSaving: Boolean = false,

    val searchQuery: String = "",
    val categorySuggestions: List<CategoryBadgeUi> = emptyList(),
    val selectedCategory: SelectedCategoryUi? = null,
    val pendingNewCategory: PendingNewCategoryUi? = null,

    val expirationDate: LocalDate? = null,
    val quantity: Int = 1,

    val recognizedBarcodeProduct: RecognizedBarcodeProductUi? = null,

    val activeSheet: AddFoodSheet? = null,

    val categoryError: String? = null,
    val expirationDateError: String? = null,
    val quantityError: String? = null,
    val genericErrorMessage: String? = null
)
```

---

### 8.3 AddFoodSheet

```kotlin
sealed interface AddFoodSheet {
    data object AddChoice : AddFoodSheet
    data class ProductRecognized(val product: RecognizedBarcodeProductUi) : AddFoodSheet
    data object BarcodeNotRecognized : AddFoodSheet
}
```

---

### 8.4 CategoryBadgeUi

```kotlin
data class CategoryBadgeUi(
    val categoryId: Long?,
    val label: String,
    val isSelected: Boolean,
    val isCreateNew: Boolean
)
```

---

### 8.5 AddFoodEvent

```kotlin
sealed interface AddFoodEvent {
    data object OnBackClick : AddFoodEvent
    data object OnCancelClick : AddFoodEvent
    data object OnSaveClick : AddFoodEvent

    data class OnSearchQueryChanged(val value: String) : AddFoodEvent
    data class OnCategoryBadgeSelected(val badge: CategoryBadgeUi) : AddFoodEvent
    data object OnCreateNewCategoryClick : AddFoodEvent

    data class OnExpirationDateSelected(val date: LocalDate) : AddFoodEvent
    data object OnQuantityIncrement : AddFoodEvent
    data object OnQuantityDecrement : AddFoodEvent

    data object OnScanBarcodeClick : AddFoodEvent
    data class OnBarcodeScanned(val barcode: String) : AddFoodEvent

    data object OnManualInsertClick : AddFoodEvent
    data object OnCloseSheet : AddFoodEvent
}
```

---

### 8.6 AddFoodEffect

```kotlin
sealed interface AddFoodEffect {
    data object NavigateBackToOrigin : AddFoodEffect
    data object NavigateToScan : AddFoodEffect
    data object NavigateToManualInsert : AddFoodEffect
    data class ShowSnackbar(val message: String) : AddFoodEffect
}
```

---

## 9. Scan Barcode

### 9.1 ScanUiState

```kotlin
data class ScanUiState(
    val isResolvingBarcode: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val errorMessage: String? = null
)
```

---

### 9.2 ScanEvent

```kotlin
sealed interface ScanEvent {
    data object OnBackClick : ScanEvent
    data object OnRequestCameraPermission : ScanEvent
    data class OnCameraPermissionResult(val granted: Boolean) : ScanEvent
    data class OnBarcodeDetected(val barcode: String) : ScanEvent
    data object OnManualInsertClick : ScanEvent
}
```

---

### 9.3 ScanEffect

```kotlin
sealed interface ScanEffect {
    data object NavigateBackToOrigin : ScanEffect
    data object NavigateToManualInsert : ScanEffect
    data class OpenProductRecognizedSheet(val product: RecognizedBarcodeProductUi) : ScanEffect
    data object OpenBarcodeNotRecognizedSheet : ScanEffect
    data class ShowSnackbar(val message: String) : ScanEffect
}
```

---

## 10. Ricette — Lista

### 10.1 RecipeListUiState

```kotlin
data class RecipeListUiState(
    val selectedTab: RecipeTab = RecipeTab.RESULTS,

    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<RecipeCardUi> = emptyList(),
    val searchState: RecipeSearchState = RecipeSearchState.Idle,

    val favoriteRecipes: List<RecipeCardUi> = emptyList(),
    val favoritesState: FavoritesState = FavoritesState.Loading,

    val genericErrorMessage: String? = null
)
```

---

### 10.2 Stati

```kotlin
enum class RecipeTab {
    RESULTS,
    FAVORITES
}

sealed interface RecipeSearchState {
    data object Idle : RecipeSearchState
    data object Loading : RecipeSearchState
    data object Empty : RecipeSearchState
    data object Offline : RecipeSearchState
    data object Error : RecipeSearchState
    data object Success : RecipeSearchState
}

sealed interface FavoritesState {
    data object Loading : FavoritesState
    data object Empty : FavoritesState
    data object Success : FavoritesState
}
```

---

### 10.3 RecipeListEvent

```kotlin
sealed interface RecipeListEvent {
    data class OnTabSelected(val tab: RecipeTab) : RecipeListEvent
    data class OnSearchQueryChanged(val value: String) : RecipeListEvent
    data object OnSearchSubmit : RecipeListEvent
    data class OnRecipeClick(val recipeId: String) : RecipeListEvent
    data class OnFavoriteClick(val recipeId: String) : RecipeListEvent
    data object OnFabClick : RecipeListEvent
}
```

---

### 10.4 RecipeListEffect

```kotlin
sealed interface RecipeListEffect {
    data class NavigateToRecipeDetail(val recipeId: String) : RecipeListEffect
    data object OpenAddChoiceSheet : RecipeListEffect
    data class ShowSnackbar(val message: String) : RecipeListEffect
}
```

---

## 11. Dettaglio Ricetta

### 11.1 Responsabilità

Il Dettaglio Ricetta mostra:

```text
contenuto ricetta
ingredienti in dispensa
ingredienti da comprare
spostamenti temporanei
like
share lista spesa
modal per collegare ingrediente ad alimento
```

---

### 11.2 RecipeDetailUiState

```kotlin
data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val recipe: RecipeDetailUi? = null,

    val isFavorite: Boolean = false,

    val inPantry: List<RecipeIngredientAvailabilityUi> = emptyList(),
    val toBuy: List<RecipeIngredientAvailabilityUi> = emptyList(),

    val manuallyMovedToPantry: Set<String> = emptySet(),
    val manuallyMovedToShopping: Set<String> = emptySet(),

    val expandedIngredientId: String? = null,
    val activeSheet: RecipeDetailSheet? = null,

    val genericErrorMessage: String? = null
)
```

---

### 11.3 RecipeIngredientAvailabilityUi

```kotlin
data class RecipeIngredientAvailabilityUi(
    val ingredientUiId: String,
    val originalName: String,
    val amount: Double?,
    val unit: String?,
    val matchedCategories: List<MatchedFoodCategoryUi>
)
```

---

### 11.4 RecipeDetailSheet

```kotlin
sealed interface RecipeDetailSheet {
    data class LinkIngredientToFood(
        val ingredientUiId: String,
        val aliasOriginal: String
    ) : RecipeDetailSheet
}
```

---

### 11.5 RecipeDetailEvent

```kotlin
sealed interface RecipeDetailEvent {
    data object OnBackClick : RecipeDetailEvent
    data object OnFavoriteClick : RecipeDetailEvent
    data object OnShareClick : RecipeDetailEvent

    data class OnIngredientClick(val ingredientUiId: String) : RecipeDetailEvent
    data class OnMoveIngredientToPantry(val ingredientUiId: String) : RecipeDetailEvent
    data class OnMoveIngredientToShopping(val ingredientUiId: String) : RecipeDetailEvent

    data class OnChangeIngredientLinkClick(val ingredientUiId: String) : RecipeDetailEvent
    data class OnIngredientFoodLinkChanged(
        val ingredientUiId: String,
        val selectedCategoryIds: List<Long>,
        val removedCategoryIds: List<Long>
    ) : RecipeDetailEvent

    data object OnCloseSheet : RecipeDetailEvent
}
```

---

### 11.6 RecipeDetailEffect

```kotlin
sealed interface RecipeDetailEffect {
    data object NavigateBack : RecipeDetailEffect
    data class OpenShareSheet(val text: String) : RecipeDetailEffect
    data class ShowSnackbar(val message: String) : RecipeDetailEffect
}
```

---

## 12. Profilo

### 12.1 ProfileUiState

```kotlin
data class ProfileUiState(
    val isLoading: Boolean = true,

    val username: String = "",
    val language: String = "it",
    val theme: AppTheme = AppTheme.SYSTEM,

    val expirationNotificationsEnabled: Boolean = false,
    val freshNotificationDays: Int = 2,
    val longLifeNotificationDays: Int = 7,

    val isRequestingNotificationPermission: Boolean = false,
    val genericErrorMessage: String? = null
)
```

---

### 12.2 ProfileEvent

```kotlin
sealed interface ProfileEvent {
    data class OnUsernameChanged(val value: String) : ProfileEvent
    data object OnUsernameEditingFinished : ProfileEvent

    data class OnLanguageChanged(val value: String) : ProfileEvent
    data class OnThemeChanged(val value: AppTheme) : ProfileEvent

    data class OnNotificationsEnabledChanged(val enabled: Boolean) : ProfileEvent
    data class OnNotificationPermissionResult(val granted: Boolean) : ProfileEvent

    data object OnFreshDaysIncrement : ProfileEvent
    data object OnFreshDaysDecrement : ProfileEvent
    data object OnLongLifeDaysIncrement : ProfileEvent
    data object OnLongLifeDaysDecrement : ProfileEvent

    data object OnFabClick : ProfileEvent
}
```

---

### 12.3 ProfileEffect

```kotlin
sealed interface ProfileEffect {
    data object RequestNotificationPermission : ProfileEffect
    data object OpenAddChoiceSheet : ProfileEffect
    data class ShowSnackbar(val message: String) : ProfileEffect
}
```

---

## 13. Shared UI model

### 13.1 StorageLocationFilter

```kotlin
enum class StorageLocationFilter {
    ALL,
    FRIDGE,
    FREEZER,
    PANTRY
}
```

---

### 13.2 RecipeCardUi

```kotlin
data class RecipeCardUi(
    val externalId: String,
    val title: String,
    val imageUrl: String?,
    val preparationTimeMinutes: Int?,
    val isFavorite: Boolean
)
```

---

### 13.3 MatchedFoodCategoryUi

```kotlin
data class MatchedFoodCategoryUi(
    val categoryId: Long,
    val name: String,
    val hasActiveLot: Boolean
)
```

---

### 13.4 BarcodeProductLinkUi

```kotlin
data class BarcodeProductLinkUi(
    val barcode: String,
    val productName: String?,
    val brand: String?,
    val imageUrl: String?
)
```

---

### 13.5 RecipeIngredientLinkUi

```kotlin
data class RecipeIngredientLinkUi(
    val id: Long,
    val aliasOriginal: String,
    val normalizedAlias: String
)
```

---

## 14. Note implementative

### 14.1 Entity vs UI model

Le entity Room non dovrebbero essere esposte direttamente alla UI.

Si usano mapper:

```text
Entity / Domain model → Ui model
```

---

### 14.2 Validazione

La validazione form resta nel ViewModel.

Gli use case possono comunque restituire validation error per sicurezza.

Esempio:

```text
ViewModel valida categoria mancante
SaveAddedFoodUseCase valida di nuovo input obbligatori
```

---

### 14.3 Navigazione

La navigazione non è dentro lo `UiState`.

La navigazione viene emessa tramite `Effect`.

---

### 14.4 Sheet

Gli sheet sono parte dello `UiState` della schermata proprietaria.

Non esiste un modal manager globale.

---

### 14.5 Draft locali

Le schermate con modifiche non immediatamente persistenti mantengono un draft nel ViewModel.

Per MVP riguarda soprattutto:

```text
Dettaglio Alimento
Aggiunta Manuale
Dettaglio Ricetta per spostamenti temporanei
```
