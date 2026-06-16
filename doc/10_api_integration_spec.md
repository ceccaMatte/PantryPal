# PantryPal — API Integration Spec

> Versione 1.0  
> Scopo: definire l’integrazione con le API esterne usate da PantryPal: Open Food Facts per barcode/prodotti e Spoonacular per ricette.

---

## 1. API esterne scelte

### 1.1 Barcode / prodotti alimentari

API scelta:

```text
Open Food Facts
```

Uso nell’app:

```text
scan barcode
recupero dati prodotto esterno
prefill Aggiunta Manuale
suggerimento categoria interna
salvataggio BarcodeProductLink dopo conferma utente
```

Open Food Facts non sostituisce il database locale PantryPal.

---

### 1.2 Ricette

API scelta:

```text
Spoonacular
```

Uso nell’app:

```text
ricerca ricette nella tab Ricette
dettaglio ricetta
ricette suggerite in Home partendo dagli alimenti in dispensa
```

Spoonacular non è fonte persistente automatica: le ricette vengono salvate in Room solo se l’utente mette like.

---

## 2. Principi generali di integrazione

### 2.1 Separazione DTO / Domain / Entity

Le response API non devono entrare direttamente in UI o Room.

Flusso:

```text
Remote DTO
↓ mapper
Domain model
↓ mapper, solo se serve persistenza
Room Entity
↓ mapper
Ui model
```

Esempio:

```text
OpenFoodFactsProductDto
→ RecognizedBarcodeProduct
→ BarcodeProductLinkEntity, solo dopo Salva
→ RecognizedBarcodeProductUi
```

---

### 2.2 Nessuna persistenza automatica

Le API possono precompilare e suggerire, ma non salvano da sole.

Regole:

```text
Open Food Facts riconosce barcode
→ non salva subito BarcodeProductLink

Spoonacular restituisce ricetta
→ non salva subito FavoriteRecipe

Spoonacular restituisce ingredienti
→ non crea subito RecipeIngredientLink
```

La persistenza avviene solo tramite use case espliciti:

```text
SaveAddedFoodUseCase
ToggleFavoriteRecipeUseCase
LinkRecipeIngredientToFoodUseCase
```

---

### 2.3 Network layer

Per MVP usare un solo client HTTP.

Scelta consigliata:

```text
Retrofit + kotlinx.serialization
```

Alternativa accettabile:

```text
Ktor Client
```

Nel progetto usiamo una sola scelta, non entrambe.

Repository coinvolti:

```text
FoodRecognitionRepository → Open Food Facts
RecipeRepository → Spoonacular
```

---

### 2.4 Timeout

Timeout consigliati:

```text
connect timeout: 10 secondi
read timeout: 15 secondi
write timeout: 15 secondi
```

Se la rete fallisce:

```text
non bloccare il resto dell’app
mostrare stato errore leggero/snackbar
permettere inserimento manuale
```

---

### 2.5 Logging

In debug:

```text
loggare URL endpoint
loggare codice HTTP
non loggare API key Spoonacular
```

In release:

```text
logging disabilitato o minimale
```

---

## 3. Open Food Facts

### 3.1 Base URL

Ambiente produzione:

```text
https://world.openfoodfacts.org
```

Endpoint prodotto consigliato per nuova integrazione:

```text
GET /api/v3.6/product/{barcode}.json
```

Esempio:

```text
GET https://world.openfoodfacts.org/api/v3.6/product/3274080005003.json
```

Nota:

```text
la documentazione Open Food Facts indica v3/v3.6 come versione corrente consigliata per nuove integrazioni.
```

---

### 3.2 User-Agent

Ogni request deve inviare un User-Agent identificabile.

Formato consigliato:

```text
PantryPal/1.0 (matteo@example.com)
```

Per il progetto universitario si può usare un contatto reale o generico del progetto.

Esempio header:

```http
User-Agent: PantryPal/1.0 (student-project)
```

---

### 3.3 Rate limit

Open Food Facts applica rate limit.

Per PantryPal:

```text
non usare Open Food Facts per search-as-you-type
chiamare Open Food Facts solo dopo scansione barcode
cache locale tramite BarcodeProductLink quando l’utente conferma
```

---

### 3.4 Campi da leggere

Dal prodotto esterno leggere solo campi utili al nostro MVP.

Campi logici:

```text
barcode
productName
genericName
brand
quantityLabel
imageUrl
rawCategoryTags
rawFoodGroupTags
```

Possibili campi Open Food Facts:

```text
code
product.product_name
product.generic_name
product.brands
product.quantity
product.image_url
product.categories_tags
product.food_groups_tags
```

Nota:

```text
la response può variare e alcuni campi possono essere mancanti.
Il mapper deve essere null-safe.
```

---

### 3.5 DTO minimo

```kotlin
@Serializable
data class OpenFoodFactsProductResponseDto(
    val code: String? = null,
    val status: Int? = null,
    val status_verbose: String? = null,
    val product: OpenFoodFactsProductDto? = null
)

@Serializable
data class OpenFoodFactsProductDto(
    val product_name: String? = null,
    val generic_name: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    val image_url: String? = null,
    val categories_tags: List<String>? = null,
    val food_groups_tags: List<String>? = null
)
```

---

### 3.6 Domain model

```kotlin
data class RecognizedBarcodeProduct(
    val barcode: String,
    val productName: String?,
    val genericName: String?,
    val brand: String?,
    val quantityLabel: String?,
    val imageUrl: String?,
    val rawCategoryTags: List<String>,
    val rawFoodGroupTags: List<String>
)
```

---

### 3.7 Quando il prodotto è considerato “riconosciuto”

Un prodotto è riconosciuto se:

```text
la response HTTP è 2xx
status indica prodotto trovato
product non è null
barcode è presente
almeno productName o genericName è presente
```

Se mancano troppi dati:

```text
trattare come non riconosciuto ai fini di BarcodeProductLink
```

---

### 3.8 Risultati repository

```kotlin
sealed interface ExternalProductResult {
    data class Found(val product: RecognizedBarcodeProduct) : ExternalProductResult
    data object NotFound : ExternalProductResult
    data object NetworkError : ExternalProductResult
    data object InvalidResponse : ExternalProductResult
    data object RateLimited : ExternalProductResult
}
```

---

### 3.9 Errori Open Food Facts

Mapping errori:

```text
HTTP 404 / status not found → NotFound
timeout / no connection → NetworkError
HTTP 429 / 503 rate limit → RateLimited
JSON non valido / campi incoerenti → InvalidResponse
altro HTTP non 2xx → NetworkError o InvalidResponse
```

Nel flow utente:

```text
NotFound → Barcode non riconosciuto, inserimento manuale
NetworkError → snackbar + possibilità inserimento manuale
InvalidResponse → snackbar + possibilità inserimento manuale
RateLimited → snackbar + possibilità inserimento manuale
```

---

### 3.10 Persistenza Open Food Facts

Non salvare la response raw.

Salvare solo in `BarcodeProductLinkEntity` dopo conferma utente:

```text
barcode
categoryId
productName
genericName
brand
quantityLabel
imageUrl
rawCategoryTags
rawFoodGroupTags
origin = USER
isActive = true
```

Condizioni:

```text
flow deriva da barcode riconosciuto
utente seleziona categoria
utente preme Salva
dati prodotto sufficienti
```

---

## 4. Spoonacular

### 4.1 Base URL

```text
https://api.spoonacular.com
```

Spoonacular richiede API key.

Per MVP usare chiamate dirette dall’app Android.

Nota importante:

```text
una API key dentro un’app mobile non è davvero segreta.
Per un progetto universitario è accettabile,
ma in produzione servirebbe un backend/proxy.
```

---

### 4.2 API key

Configurazione consigliata:

```text
local.properties
↓
BuildConfig.SPOONACULAR_API_KEY
```

Esempio `local.properties`:

```properties
SPOONACULAR_API_KEY=your_key_here
```

Esempio Gradle:

```kotlin
buildConfigField(
    "String",
    "SPOONACULAR_API_KEY",
    "\"${properties["SPOONACULAR_API_KEY"]}\""
)
```

Regole:

```text
non committare API key
non loggare API key
se API key assente, mostrare stato errore configurazione o usare mock locale in debug
```

---

### 4.3 Lingua

Spoonacular lavora in inglese.

Implicazioni MVP:

```text
ricette e ingredienti possono arrivare in inglese
query italiane possono dare risultati deboli o nulli
seed alias inglesi aiutano il matching
nessuna traduzione automatica per MVP
```

Non introdurre servizio di traduzione.

---

## 5. Spoonacular — Ricette suggerite Home

### 5.1 Endpoint

```text
GET /recipes/findByIngredients
```

Uso:

```text
Home suggested recipes
```

Parametri:

```text
ingredients = nomi FoodCategory presenti in dispensa, separati da virgola
number = 5
ranking = 1
ignorePantry = false
apiKey = SPOONACULAR_API_KEY
```

Scelta `ignorePantry = false`:

```text
PantryPal manda tutti gli alimenti presenti.
Non introduce flag isStaple e non filtra sale/olio/pepe lato app.
```

---

### 5.2 Input

Da `GetHomeSuggestedRecipesUseCase`:

```kotlin
data class SuggestedRecipesInput(
    val pantryFoodNames: List<String>
)
```

Regole:

```text
se pantryFoodNames è vuota → EmptyPantry
se offline → Offline
altrimenti chiamata API
```

---

### 5.3 DTO minimo

```kotlin
@Serializable
data class SpoonacularByIngredientsRecipeDto(
    val id: Long,
    val title: String,
    val image: String? = null,
    val imageType: String? = null,
    val usedIngredientCount: Int? = null,
    val missedIngredientCount: Int? = null
)
```

---

### 5.4 Domain output

```kotlin
data class RecipeCard(
    val externalId: String,
    val title: String,
    val imageUrl: String?,
    val preparationTimeMinutes: Int?,
    val isFavorite: Boolean
)
```

Nota:

```text
findByIngredients potrebbe non restituire preparationTimeMinutes.
Se manca, usare null.
```

---

## 6. Spoonacular — Ricerca ricette

### 6.1 Endpoint

```text
GET /recipes/complexSearch
```

Uso:

```text
tab Ricette → ricerca libera
```

Parametri MVP:

```text
query = testo utente
number = 10
addRecipeInformation = false
apiKey = SPOONACULAR_API_KEY
```

Non usare filtri avanzati per MVP.

---

### 6.2 DTO minimo

```kotlin
@Serializable
data class SpoonacularComplexSearchResponseDto(
    val results: List<SpoonacularRecipeSearchItemDto> = emptyList(),
    val offset: Int? = null,
    val number: Int? = null,
    val totalResults: Int? = null
)

@Serializable
data class SpoonacularRecipeSearchItemDto(
    val id: Long,
    val title: String,
    val image: String? = null,
    val imageType: String? = null
)
```

---

### 6.3 Stati UI

Mapping:

```text
query vuota → Idle
request in corso → Loading
results vuota → Empty
no connection → Offline
HTTP/API error → Error
results non vuota → Success
```

---

## 7. Spoonacular — Dettaglio ricetta

### 7.1 Endpoint

```text
GET /recipes/{id}/information
```

Uso:

```text
Dettaglio Ricetta
salvataggio preferito
```

Parametri MVP:

```text
includeNutrition = false
apiKey = SPOONACULAR_API_KEY
```

Non richiedere:

```text
nutrition
winePairing
tasteData
equipment
priceBreakdown
```

Motivo:

```text
ridurre DTO, quota e complessità
```

---

### 7.2 DTO minimo

```kotlin
@Serializable
data class SpoonacularRecipeInformationDto(
    val id: Long,
    val title: String,
    val image: String? = null,
    val readyInMinutes: Int? = null,
    val servings: Int? = null,
    val sourceUrl: String? = null,
    val summary: String? = null,
    val extendedIngredients: List<SpoonacularIngredientDto> = emptyList()
)

@Serializable
data class SpoonacularIngredientDto(
    val id: Long? = null,
    val name: String? = null,
    val original: String? = null,
    val amount: Double? = null,
    val unit: String? = null
)
```

---

### 7.3 Domain model

```kotlin
data class RecipeDetail(
    val externalId: String,
    val title: String,
    val description: String?,
    val preparationTimeMinutes: Int?,
    val servings: Int?,
    val imageUrl: String?,
    val sourceUrl: String?,
    val ingredients: List<RecipeIngredientData>
)

data class RecipeIngredientData(
    val originalName: String,
    val normalizedName: String,
    val externalIngredientId: String?,
    val amount: Double?,
    val unit: String?
)
```

Mapping ingredienti:

```text
originalName = ingredient.original ?: ingredient.name
normalizedName = normalizeFoodText(originalName)
externalIngredientId = ingredient.id?.toString()
amount = ingredient.amount
unit = ingredient.unit
```

---

### 7.4 Summary HTML

Spoonacular può restituire `summary` con HTML.

Per MVP:

```text
rimuovere tag HTML nel mapper
tenere testo semplice come description
```

Non renderizzare HTML nella UI.

---

## 8. Offline behavior

### 8.1 Barcode

Se offline durante scan barcode e barcode non esiste localmente:

```text
NetworkError
mostra snackbar
permetti inserimento manuale
non salvare BarcodeProductLink
```

Se barcode esiste localmente:

```text
funziona offline
preseleziona categoria locale
```

---

### 8.2 Ricette

Offline:

```text
ricerca ricette API non disponibile
Home suggested recipes non disponibile
dettaglio ricetta non preferita non disponibile
ricette preferite disponibili da Room
dettaglio ricetta preferita disponibile da Room
```

---

### 8.3 Preferiti

Quando una ricetta è preferita, salvare localmente:

```text
FavoriteRecipeEntity
RecipeIngredientEntity
```

Questo permette:

```text
apertura offline
ricalcolo disponibilità con dispensa attuale
```

---

## 9. Error handling comune

### 9.1 Errori network

Tipi comuni:

```kotlin
sealed interface ApiError {
    data object Offline : ApiError
    data object Timeout : ApiError
    data object NotFound : ApiError
    data object Unauthorized : ApiError
    data object RateLimited : ApiError
    data object QuotaExceeded : ApiError
    data object InvalidResponse : ApiError
    data class HttpError(val code: Int) : ApiError
    data class Unknown(val cause: Throwable?) : ApiError
}
```

---

### 9.2 Mapping HTTP

```text
401 / 403 → Unauthorized
404 → NotFound
429 → RateLimited
402 Spoonacular → QuotaExceeded
5xx → HttpError / temporaneo
timeout → Timeout
no connection → Offline
JSON parse error → InvalidResponse
```

---

### 9.3 UX errori

Barcode:

```text
errore rete → snackbar + inserimento manuale
not found → sheet barcode non riconosciuto
invalid response → snackbar + inserimento manuale
```

Ricette:

```text
offline → empty/offline state nella sezione
quota finita → snackbar o stato errore leggero
errore dettaglio → schermata error con retry/back
```

---

## 10. Quote e limiti

### 10.1 Open Food Facts

Regole app:

```text
non chiamare search endpoint per autocomplete
non fare polling
non chiamare API se barcode già noto localmente
usare User-Agent custom
```

---

### 10.2 Spoonacular

Regole app:

```text
limitare number a 5 per Home suggested recipes
limitare number a 10 per search ricette
non caricare dettaglio finché l’utente non apre la ricetta
non richiedere nutrition
non richiedere dati extra
```

Se quota finita:

```text
mostrare stato errore leggero
non rompere Home overview locale
preferiti offline restano disponibili
```

---

## 11. Package suggeriti

### 11.1 Network core

```text
core/network
├── NetworkModule.kt
├── ApiError.kt
├── NetworkResult.kt
└── JsonConfig.kt
```

---

### 11.2 Open Food Facts

```text
data/product/remote
├── OpenFoodFactsApi.kt
├── dto/OpenFoodFactsProductResponseDto.kt
├── mapper/OpenFoodFactsMapper.kt
└── FoodRecognitionRepositoryImpl.kt
```

---

### 11.3 Spoonacular

```text
data/recipe/remote
├── SpoonacularApi.kt
├── dto/SpoonacularRecipeDtos.kt
├── mapper/SpoonacularRecipeMapper.kt
└── RecipeRepositoryImpl.kt
```

---

## 12. Retrofit interfaces

### 12.1 Open Food Facts API

```kotlin
interface OpenFoodFactsApi {

    @GET("api/v3.6/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OpenFoodFactsProductResponseDto
}
```

Header User-Agent:

```text
aggiunto tramite OkHttp Interceptor
```

---

### 12.2 Spoonacular API

```kotlin
interface SpoonacularApi {

    @GET("recipes/findByIngredients")
    suspend fun findRecipesByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 5,
        @Query("ranking") ranking: Int = 1,
        @Query("ignorePantry") ignorePantry: Boolean = false,
        @Query("apiKey") apiKey: String = BuildConfig.SPOONACULAR_API_KEY
    ): List<SpoonacularByIngredientsRecipeDto>

    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("number") number: Int = 10,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = false,
        @Query("apiKey") apiKey: String = BuildConfig.SPOONACULAR_API_KEY
    ): SpoonacularComplexSearchResponseDto

    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: String,
        @Query("includeNutrition") includeNutrition: Boolean = false,
        @Query("apiKey") apiKey: String = BuildConfig.SPOONACULAR_API_KEY
    ): SpoonacularRecipeInformationDto
}
```

Nota:

```text
in alternativa, apiKey può essere aggiunta tramite interceptor/query interceptor.
Per MVP va bene anche come @Query esplicita.
```

---

## 13. Repository behavior

### 13.1 FoodRecognitionRepository

```kotlin
interface FoodRecognitionRepository {
    suspend fun lookupProductByBarcode(barcode: String): ExternalProductResult
}
```

Implementazione:

```text
chiama OpenFoodFactsApi
mappa DTO → RecognizedBarcodeProduct
gestisce errori → ExternalProductResult
```

---

### 13.2 RecipeRepository

Metodi remoti:

```kotlin
suspend fun searchRecipes(query: RecipeSearchQuery): RecipeSearchResult
suspend fun searchRecipesByIngredients(ingredients: List<String>): RecipeSearchResult
suspend fun getRemoteRecipeDetail(externalId: String): RecipeDetailResult
```

Metodi locali già definiti nel repository contract:

```text
observeFavoriteRecipes()
getFavoriteRecipeDetail()
saveFavoriteRecipe()
removeFavoriteRecipe()
findIngredientLinks()
upsertRecipeIngredientLink()
```

---

## 14. Decisioni finali

```text
Open Food Facts:
    unica API barcode
    usare endpoint v3.6 product
    no API key
    User-Agent custom obbligatorio
    chiamata solo su barcode scan
    no search-as-you-type

Spoonacular:
    unica API ricette
    API key in local.properties/BuildConfig
    direct mobile call accettabile per progetto universitario
    endpoint Home: findByIngredients
    endpoint search: complexSearch
    endpoint detail: {id}/information
    includeNutrition = false
    nessuna traduzione automatica
    ricette persistite solo se favorite

Offline:
    dispensa sempre disponibile
    barcode noto localmente disponibile
    ricette preferite disponibili
    API ricette e barcode remoto non disponibili
```

---

## 15. Fonti consultate

```text
Open Food Facts API documentation:
https://openfoodfacts.github.io/openfoodfacts-server/api/

Open Food Facts tutorial product by barcode:
https://openfoodfacts.github.io/openfoodfacts-server/api/tutorial-off-api/

Spoonacular API docs:
https://spoonacular.com/food-api/docs

Spoonacular pricing / quota FAQ:
https://spoonacular.com/food-api/pricing
```
