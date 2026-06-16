# PantryPal — Repository & Use Case Contract

> Versione 1.0  
> Scopo: definire i confini tra ViewModel, UseCase e Repository.  
> Questo documento traduce le decisioni applicative già fissate in contratti implementativi.

---

## 1. Principi architetturali

### 1.1 Struttura generale

L’app segue una struttura:

```text
Compose UI
↓
ViewModel
↓
UseCase, solo dove serve
↓
Repository
↓
Room / DataStore / API / WorkManager / NotificationManager
```

Regola generale:

```text
la UI non accede mai direttamente a Room, DataStore o API
```

---

### 1.2 Quando usare uno UseCase

Uno use case serve quando un’azione:

- coinvolge più repository;
- modifica più tabelle;
- richiede una transazione;
- applica regole business non banali;
- coordina dati locali e dati remoti.

Esempi:

```text
SaveAddedFoodUseCase
ResolveBarcodeUseCase
SaveFoodDetailChangesUseCase
ToggleFavoriteRecipeUseCase
GetRecipeAvailabilityUseCase
UpdateNotificationSettingsUseCase
```

---

### 1.3 Quando NON usare uno UseCase

Non serve uno use case per operazioni semplici e dirette.

Esempi:

```text
cambiare tema
modificare nome utente
cambiare lingua
cambiare filtro Dispensa
osservare lista preferiti
osservare righe Dispensa
```

Queste operazioni possono essere:

```text
ViewModel → Repository
```

---

### 1.4 Transazioni

Tutte le operazioni multi-tabella devono essere atomiche.

Regola:

```text
se un’azione scrive più entità Room,
deve essere eseguita in una Room transaction
```

Esempi:

```text
SaveAddedFoodUseCase
SaveFoodDetailChangesUseCase
ToggleFavoriteRecipeUseCase
```

Motivo:

```text
o tutte le modifiche riescono,
o nessuna modifica viene salvata
```

---

### 1.5 Repository senza logica UX

I repository non devono conoscere concetti UI come:

```text
badge selezionato
modal aperto
ritorno alla schermata di origine
errore mostrato sotto un campo
bottom navigation nascosta
```

Queste responsabilità restano in:

```text
ViewModel
UiState
Navigator
```

---

## 2. Repository

Repository definitivi:

```text
PantryRepository
FoodRecognitionRepository
RecipeRepository
SettingsRepository
NotificationRepository
```

Non sono previsti repository aggiuntivi per MVP.

---

## 3. PantryRepository

### 3.1 Responsabilità

Gestisce la dispensa locale.

Entità coinvolte:

```text
FoodCategoryEntity
ExpiryLotEntity
BarcodeProductLinkEntity
```

Responsabilità:

```text
categorie alimentari interne
lotti/scadenze
righe Dispensa
dettaglio alimento
barcode link locali
collegamenti barcode dell’alimento
query per alimenti in scadenza
```

---

### 3.2 Query principali

```kotlin
fun observePantryRows(filter: StorageLocationFilter): Flow<List<PantryRow>>
```

Restituisce una riga per alimento presente in dispensa.

La riga contiene:

```text
categoryId
name
storageLocation
perishability
totalQuantity
nearestExpirationDate
lotCount
```

Regola:

```text
mostra solo FoodCategory con almeno un ExpiryLot attivo
```

---

```kotlin
fun observeExpiringFoodRows(filter: StorageLocationFilter): Flow<List<ExpiringFoodRow>>
```

Restituisce gli alimenti in scadenza, deduplicati per categoria.

Ogni riga contiene:

```text
categoryId
name
expiringQuantity
nearestExpirationDate
storageLocation
perishability
```

Regola:

```text
se più lotti dello stesso alimento sono in scadenza,
mostra una sola riga/card con moltiplicatore
```

---

```kotlin
fun observeFoodDetail(categoryId: Long): Flow<FoodDetailData>
```

Restituisce:

```text
FoodCategory
lista ExpiryLot ordinati per expirationDate
barcode collegati attivi
nomi ricetta collegati attivi, tramite RecipeRepository o query dedicata se necessario
```

Nota:

```text
i nomi ricetta collegati appartengono concettualmente a RecipeRepository,
ma possono essere aggregati in un DTO di dettaglio tramite uno use case o query coordinata.
```

---

```kotlin
suspend fun getActiveLotsWithCategories(): List<LotWithCategory>
```

Usato da:

```text
GetHomeOverviewUseCase
RunExpirationNotificationCheckUseCase
GetHomeSuggestedRecipesUseCase
GetRecipeAvailabilityUseCase
```

---

```kotlin
suspend fun findActiveBarcodeLink(barcode: String): BarcodeProductLink?
```

Cerca un barcode link locale attivo.

---

```kotlin
suspend fun searchFoodCategories(query: String): List<FoodCategory>
```

Usato da:

```text
Aggiunta Manuale
modal associazione ingrediente → alimento
autocomplete/search badge
```

---

### 3.3 Command principali

```kotlin
suspend fun findCategoryByNormalizedName(normalizedName: String): FoodCategory?
```

---

```kotlin
suspend fun createFoodCategory(input: CreateFoodCategoryInput): Long
```

Crea una categoria con:

```text
origin = USER
```

Regola:

```text
se esiste già una categoria con stesso normalizedName,
non creare duplicati
```

---

```kotlin
suspend fun upsertExpiryLot(categoryId: Long, expirationDate: LocalDate, quantityDelta: Int)
```

Regola:

```text
se esiste ExpiryLot(categoryId, expirationDate)
    incrementa quantity
altrimenti
    crea nuovo lotto
```

---

```kotlin
suspend fun saveFoodDetailChanges(changes: FoodDetailChanges)
```

Operazione usata da `SaveFoodDetailChangesUseCase`.

Deve essere transazionale.

Applica:

```text
aggiornamento FoodCategory
creazione lotti
modifica lotti
fusione lotti con stessa data
eliminazione lotti arrivati a 0
```

---

```kotlin
suspend fun saveBarcodeProductLink(link: BarcodeProductLink)
```

Salva o aggiorna:

```text
barcode → FoodCategory
```

Regole:

```text
solo per barcode riconosciuti con dati prodotto completi
origin = USER quando confermato dall’utente
isActive = true
```

---

```kotlin
suspend fun deactivateBarcodeProductLink(barcode: String)
```

Disattiva un barcode link.

Nota:

```text
per i barcode link può avere senso mantenere isActive = false,
così si evita che associazioni corrette/rimosse ricompaiano automaticamente.
```

---

## 4. FoodRecognitionRepository

### 4.1 Responsabilità

Gestisce solo Open Food Facts.

Non conosce Room.

Non decide mai la categoria interna PantryPal.

Responsabilità:

```text
lookup barcode online
lettura dati prodotto esterno
normalizzazione DTO prodotto
```

---

### 4.2 Query principali

```kotlin
suspend fun lookupProductByBarcode(barcode: String): ExternalProductResult
```

Possibili risultati:

```text
Found(product)
NotFound
NetworkError
InvalidResponse
```

`product` contiene almeno:

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

Regola:

```text
se i dati prodotto non sono sufficienti,
il barcode viene trattato come non riconosciuto ai fini del BarcodeProductLink
```

---

## 5. RecipeRepository

### 5.1 Responsabilità

Gestisce il mondo ricette.

Entità coinvolte:

```text
FavoriteRecipeEntity
RecipeIngredientEntity
RecipeIngredientLinkEntity
```

Responsabilità:

```text
ricerca ricette online
dettaglio ricetta online
ricette preferite locali
ingredienti ricette preferite
mapping globale ingrediente ricetta → FoodCategory
```

---

### 5.2 Query API

```kotlin
suspend fun searchRecipes(query: RecipeSearchQuery): RecipeSearchResult
```

Usato da:

```text
Ricette tab Risultati
Home suggested recipes
```

---

```kotlin
suspend fun getRecipeDetail(externalId: String): RecipeDetail
```

Regole:

```text
online → recupera dettaglio da API
offline → disponibile solo se ricetta è preferita e salvata localmente
```

---

### 5.3 Query locali

```kotlin
fun observeFavoriteRecipes(): Flow<List<FavoriteRecipe>>
```

---

```kotlin
suspend fun getFavoriteRecipeDetail(externalId: String): RecipeDetail?
```

Restituisce dati locali se la ricetta è preferita.

---

```kotlin
suspend fun findIngredientLinks(normalizedAlias: String, externalIngredientId: String?): List<RecipeIngredientLink>
```

Restituisce tutti i link attivi per un alias ingrediente.

Supporta molti-a-molti:

```text
pollo → Petto di pollo
pollo → Cosce di pollo
pollo → Pollo campese
```

---

```kotlin
suspend fun getIngredientLinksForCategory(categoryId: Long): List<RecipeIngredientLink>
```

Usato nella pagina:

```text
Gestisci collegamenti alimento
```

per mostrare i nomi ricetta collegati a un alimento.

---

### 5.4 Command locali

```kotlin
suspend fun saveFavoriteRecipe(recipe: RecipeDetail)
```

Salva:

```text
FavoriteRecipe
RecipeIngredient
```

Deve essere transazionale.

---

```kotlin
suspend fun removeFavoriteRecipe(externalId: String)
```

Elimina:

```text
FavoriteRecipe
RecipeIngredient collegati via cascade
```

Non elimina:

```text
RecipeIngredientLink globali
```

---

```kotlin
suspend fun upsertRecipeIngredientLink(input: RecipeIngredientLinkInput)
```

Salva un mapping globale:

```text
alias ingrediente → FoodCategory
```

Regole:

```text
origin = USER quando creato/corretto dall’utente
isActive = true
```

---

```kotlin
suspend fun deleteRecipeIngredientLink(linkId: Long)
```

Usato quando l’utente rimuove/corregge un mapping errato.

Regola:

```text
mapping ingrediente errato corretto dall’utente → delete
```

Non si mantiene `isActive = false` per i mapping errati ingrediente → alimento.

---

```kotlin
suspend fun deleteIngredientLinksForAliasAndCategory(normalizedAlias: String, categoryId: Long)
```

Usato per rimuovere un link specifico:

```text
olio d’oliva → Salmone
```

quando viene corretto in:

```text
olio d’oliva → Olio
```

---

## 6. SettingsRepository

### 6.1 Responsabilità

Gestisce DataStore.

Dati gestiti:

```text
username
language
theme
expirationNotificationsEnabled
freshNotificationDays
longLifeNotificationDays
pantryStorageFilter
```

---

### 6.2 Query

```kotlin
fun observeSettings(): Flow<UserSettings>
```

---

```kotlin
suspend fun getSettings(): UserSettings
```

Usato da:

```text
GetHomeOverviewUseCase
RunExpirationNotificationCheckUseCase
```

---

### 6.3 Command

```kotlin
suspend fun updateUsername(username: String?)
suspend fun updateLanguage(language: String)
suspend fun updateTheme(theme: AppTheme)
suspend fun setNotificationsEnabled(enabled: Boolean)
suspend fun updateFreshNotificationDays(days: Int)
suspend fun updateLongLifeNotificationDays(days: Int)
suspend fun updatePantryStorageFilter(filter: StorageLocationFilter)
```

Regole:

```text
freshNotificationDays range: 1–7
longLifeNotificationDays range: 1–30
default FRESH: 2 giorni
default LONG_LIFE: 7 giorni
language salvata ma UI MVP solo italiano
```

---

## 7. NotificationRepository

### 7.1 Responsabilità

Gestisce infrastruttura notifiche.

Responsabilità:

```text
creare notification channel
richiedere/controllare permesso notifiche
schedulare WorkManager
cancellare WorkManager
mostrare notifica riepilogo
creare deep link verso Dispensa
```

Non calcola quali alimenti sono in scadenza.

---

### 7.2 Command

```kotlin
suspend fun areNotificationsAllowed(): Boolean
```

---

```kotlin
suspend fun requestNotificationPermission(): PermissionResult
```

Nota:

```text
in Android Compose questa parte può richiedere integrazione con ActivityResult API.
Il repository può astrarre il risultato, ma il trigger UI può stare nel ViewModel/Screen.
```

---

```kotlin
fun scheduleDailyExpirationWorker()
```

Regola:

```text
una volta al giorno
circa alle 09:00
fuso orario del telefono
```

---

```kotlin
fun cancelDailyExpirationWorker()
```

---

```kotlin
fun showExpirationSummaryNotification(input: ExpirationNotificationContent)
```

Mostra una notifica unica riepilogativa.

Tap:

```text
Dispensa
Filtro = Tutti
Sezione “In scadenza” visibile
```

---

## 8. UseCase

Use case definitivi MVP:

```text
SaveAddedFoodUseCase
ResolveBarcodeUseCase
SaveFoodDetailChangesUseCase

GetHomeOverviewUseCase
GetHomeSuggestedRecipesUseCase

ToggleFavoriteRecipeUseCase
GetRecipeAvailabilityUseCase
LinkRecipeIngredientToFoodUseCase

UpdateNotificationSettingsUseCase
RunExpirationNotificationCheckUseCase
```

Possibili helper/use case secondari:

```text
FindOrCreateFoodCategoryUseCase
BuildShoppingListShareText
```

---

## 9. SaveAddedFoodUseCase

### 9.1 Responsabilità

Gestisce il salvataggio finale del flow Aggiunta Manuale.

È l’unico punto che scrive nel DB per il flow di aggiunta alimento.

---

### 9.2 Input

```kotlin
data class SaveAddedFoodInput(
    val selectedCategoryId: Long?,
    val pendingNewCategory: PendingNewCategory?,
    val expirationDate: LocalDate?,
    val quantity: Int,
    val recognizedBarcodeProduct: RecognizedBarcodeProduct?
)
```

Dove:

```text
selectedCategoryId = categoria esistente selezionata
pendingNewCategory = nuova categoria creata ma non ancora persistita
recognizedBarcodeProduct = dati barcode completi, se il flow deriva da scan riconosciuto
```

---

### 9.3 Validazioni

```text
categoria selezionata obbligatoria
data scadenza obbligatoria
quantity > 0
```

Errori:

```text
MissingCategory
MissingExpirationDate
InvalidQuantity
```

---

### 9.4 Regole

```text
1. se selectedCategoryId presente → usa categoria esistente
2. se pendingNewCategory presente → crea FoodCategory con origin = USER
3. crea o incrementa ExpiryLot(categoryId, expirationDate)
4. se recognizedBarcodeProduct presente:
   - salva BarcodeProductLink completo
   - origin = USER
   - isActive = true
5. se barcode non riconosciuto:
   - non salva nessun BarcodeProductLink
```

Operazione transazionale.

---

### 9.5 Output

```kotlin
sealed interface SaveAddedFoodResult {
    data object Success : SaveAddedFoodResult
    data class ValidationError(val error: SaveAddedFoodError) : SaveAddedFoodResult
    data class Failure(val cause: Throwable) : SaveAddedFoodResult
}
```

La navigazione di ritorno alla schermata di origine non è responsabilità dello use case.

---

## 10. ResolveBarcodeUseCase

### 10.1 Responsabilità

Risolve un barcode in dati utili per il form di aggiunta alimento.

Non salva nulla nel DB.

---

### 10.2 Input

```kotlin
data class ResolveBarcodeInput(
    val barcode: String
)
```

---

### 10.3 Flusso

```text
1. cerca BarcodeProductLink locale attivo
2. se trovato:
   - restituisce categoria collegata
   - form precompilato con categoria
   - badge categoria selezionato
3. se non trovato:
   - chiama Open Food Facts
4. se Open Food Facts riconosce prodotto:
   - restituisce dati prodotto
   - prova a generare suggerimenti categoria
   - se trova match, può pre-selezionare il badge migliore
   - se non trova match, precompila solo il campo ricerca con productName
5. se barcode non riconosciuto:
   - restituisce stato NotRecognized
   - Aggiunta Manuale vuota
```

---

### 10.4 Output

```kotlin
sealed interface ResolveBarcodeResult {
    data class KnownLocalCategory(
        val barcode: String,
        val categoryId: Long,
        val categoryName: String
    ) : ResolveBarcodeResult

    data class RecognizedExternalProduct(
        val product: RecognizedBarcodeProduct,
        val suggestedCategories: List<FoodCategorySuggestion>,
        val preselectedCategoryId: Long?
    ) : ResolveBarcodeResult

    data object NotRecognized : ResolveBarcodeResult
    data object NetworkError : ResolveBarcodeResult
}
```

---

## 11. SaveFoodDetailChangesUseCase

### 11.1 Responsabilità

Applica le modifiche draft del Dettaglio Alimento.

---

### 11.2 Input

```kotlin
data class SaveFoodDetailChangesInput(
    val categoryId: Long,
    val updatedName: String?,
    val updatedStorageLocation: StorageLocation,
    val updatedPerishability: PerishabilityType,
    val updatedImageUri: String?,
    val draftLots: List<DraftExpiryLot>
)
```

`draftLots` rappresenta lo stato finale della UI al momento del salvataggio.

---

### 11.3 Regole

```text
1. aggiorna FoodCategory
2. rimuove dal DB i lotti non più presenti o arrivati a 0
3. crea o aggiorna i lotti con quantity > 0
4. se due draft hanno stessa expirationDate, fonde le quantità
5. se tutti i lotti sono rimossi, FoodCategory resta nel DB
```

Operazione transazionale.

---

### 11.4 Output

```kotlin
sealed interface SaveFoodDetailChangesResult {
    data object Success : SaveFoodDetailChangesResult
    data class Failure(val cause: Throwable) : SaveFoodDetailChangesResult
}
```

---

## 12. GetHomeOverviewUseCase

### 12.1 Responsabilità

Calcola la overview locale della Home.

Non dipende da API ricette.

---

### 12.2 Input

Nessun input obbligatorio.

---

### 12.3 Regole

```text
1. legge lotti attivi con FoodCategory
2. calcola totale confezioni = SUM(quantity)
3. calcola conteggi per luogo
4. legge soglie da SettingsRepository
5. calcola alimenti in scadenza
6. deduplica per FoodCategory
7. moltiplicatore = somma quantity dei lotti in scadenza
8. ordina per nearestExpirationDate
```

---

### 12.4 Output

```kotlin
data class HomeOverview(
    val username: String?,
    val totalPackages: Int,
    val fridgePackages: Int,
    val freezerPackages: Int,
    val pantryPackages: Int,
    val expiringFoods: List<HomeExpiringFood>
)
```

---

## 13. GetHomeSuggestedRecipesUseCase

### 13.1 Responsabilità

Calcola il blocco opzionale ricette suggerite in Home.

Separato dalla overview locale.

---

### 13.2 Flusso

```text
1. legge FoodCategory con almeno un ExpiryLot attivo
2. se lista vuota → Empty
3. se offline/API errore → Offline/Error
4. manda tutti i nomi alimenti all’API ricette
5. restituisce le ricette suggerite dall’API
```

Non esiste logica sofisticata.

Non esiste flag `isStaple`.

---

### 13.3 Output

```kotlin
sealed interface HomeSuggestedRecipesResult {
    data class Success(val recipes: List<RecipeCard>) : HomeSuggestedRecipesResult
    data object EmptyPantry : HomeSuggestedRecipesResult
    data object Offline : HomeSuggestedRecipesResult
    data object Error : HomeSuggestedRecipesResult
}
```

---

## 14. ToggleFavoriteRecipeUseCase

### 14.1 Responsabilità

Gestisce like/unlike ricetta.

---

### 14.2 Input

```kotlin
data class ToggleFavoriteRecipeInput(
    val recipe: RecipeDetail,
    val currentlyFavorite: Boolean
)
```

---

### 14.3 Regole

Se non è preferita:

```text
salva FavoriteRecipe
salva RecipeIngredient
```

Se è già preferita:

```text
elimina FavoriteRecipe
elimina RecipeIngredient via cascade
```

Non elimina:

```text
RecipeIngredientLink globali
```

Operazione transazionale.

---

## 15. GetRecipeAvailabilityUseCase

### 15.1 Responsabilità

Divide gli ingredienti di una ricetta tra:

```text
In dispensa
Da comprare
```

---

### 15.2 Input

```kotlin
data class GetRecipeAvailabilityInput(
    val ingredients: List<RecipeIngredientData>
)
```

---

### 15.3 Flusso

```text
per ogni ingrediente:
    normalizza originalName
    cerca RecipeIngredientLink attivi
    ottiene FoodCategory compatibili
    controlla se almeno una categoria compatibile ha ExpiryLot attivo
    se sì → In dispensa
    altrimenti → Da comprare
```

Non salva nulla.

Non usa `RecipeIngredientMatchEntity`.

---

### 15.4 Output

```kotlin
data class RecipeAvailability(
    val inPantry: List<RecipeIngredientAvailabilityItem>,
    val toBuy: List<RecipeIngredientAvailabilityItem>
)
```

---

## 16. LinkRecipeIngredientToFoodUseCase

### 16.1 Responsabilità

Crea o corregge un mapping globale:

```text
ingrediente ricetta → FoodCategory
```

---

### 16.2 Input

```kotlin
data class LinkRecipeIngredientToFoodInput(
    val aliasOriginal: String,
    val externalIngredientId: String?,
    val selectedCategoryIds: List<Long>,
    val removedCategoryIds: List<Long>
)
```

---

### 16.3 Regole

```text
1. normalizza aliasOriginal
2. per ogni removedCategoryId:
   - elimina il RecipeIngredientLink corrispondente
3. per ogni selectedCategoryId:
   - crea o aggiorna RecipeIngredientLink
   - origin = USER
   - isActive = true
4. supporta molti-a-molti
```

I link errati rimossi dall’utente vengono eliminati dal DB.

---

## 17. UpdateNotificationSettingsUseCase

### 17.1 Responsabilità

Gestisce il toggle notifiche ON/OFF.

---

### 17.2 Input

```kotlin
data class UpdateNotificationSettingsInput(
    val enabled: Boolean
)
```

---

### 17.3 Regole

Se `enabled = true`:

```text
1. richiede/controlla permesso notifiche
2. se permesso concesso:
   - salva expirationNotificationsEnabled = true
   - schedula worker giornaliero
   - esegue subito RunExpirationNotificationCheckUseCase
3. se permesso negato:
   - salva expirationNotificationsEnabled = false
   - non schedula worker
```

Se `enabled = false`:

```text
salva expirationNotificationsEnabled = false
cancella worker giornaliero
mantiene soglie salvate
```

---

## 18. RunExpirationNotificationCheckUseCase

### 18.1 Responsabilità

Esegue il controllo scadenze e mostra una notifica riepilogativa se necessario.

---

### 18.2 Flusso

```text
1. legge settings
2. legge lotti attivi con FoodCategory
3. calcola alimenti in scadenza usando soglie settings/default
4. se lista vuota → non fa nulla
5. se lista non vuota → costruisce notifica riepilogo
6. chiede a NotificationRepository di mostrarla
```

---

### 18.3 Regole

```text
nessun filtro anti-spam
nessun lastExpirationNotificationDate
ogni OFF → ON può produrre una notifica
il worker giornaliero può produrre una notifica ogni giorno
```

---

## 19. Helper: BuildShoppingListShareText

Questo può essere implementato come semplice formatter nel `RecipeDetailViewModel`.

Input:

```text
stato visuale corrente del Dettaglio Ricetta
```

Usa anche gli spostamenti temporanei tra:

```text
In dispensa
Da comprare
```

Non accede al DB.

Non richiede repository.

---

## 20. Cosa resta fuori dai Repository

I repository non devono gestire:

```text
navigazione
back stack
bottom nav visibile/nascosta
validazione visuale dei campi
messaggi errore UI
stato dei modal
spostamenti temporanei ingredienti nella pagina ricetta
```

Queste responsabilità appartengono a:

```text
ViewModel
UiState
Navigator
Composable
