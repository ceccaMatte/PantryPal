# PantryPal — Specifica Data Model aggiornata per matching molti-a-molti

## 1. Scopo del documento

Questo documento definisce il data model di PantryPal.

Il modello deve supportare:

- gestione local-first della dispensa;
- tracciamento di scadenze e quantità;
- aggregazione delle confezioni per alimento interno e data di scadenza;
- riconoscimento dei prodotti reali tramite barcode/Open Food Facts;
- matching tra ingredienti delle ricette e alimenti interni;
- correzione dei collegamenti sbagliati;
- creazione runtime di nuovi alimenti interni;
- salvataggio locale delle ricette preferite;
- impostazioni utente tramite DataStore.

Il punto centrale del modello è separare tre concetti diversi:

```text
Prodotto reale acquistato ≠ Alimento interno PantryPal ≠ Ingrediente ricetta API
```

Tutti e tre devono convergere su una base comune:

```text
FoodCategory / CategoriaAlimentare
```

---

## 2. Concetto centrale: CategoriaAlimentare

La `CategoriaAlimentare` rappresenta l’alimento interno usato da PantryPal.

Esempi:

```text
Latte
Pasta
Sale
Olio
Prosciutto crudo
Pollo fritto
Cotoletta impanata
Yogurt
Uova
Basilico
```

Non rappresenta necessariamente un prodotto commerciale preciso.

Esempio:

```text
Latte Parmalat Zymil 1L
Latte Coop Intero
Latte Granarolo Parzialmente Scremato
```

possono essere tutti collegati alla stessa categoria interna:

```text
Latte
```

La categoria interna è usata per:

- mostrare la dispensa;
- aggregare quantità;
- calcolare scadenze;
- filtrare per luogo;
- confrontare ingredienti ricetta e dispensa;
- suggerire autocomplete;
- ricordare collegamenti barcode;
- ricordare collegamenti ingredienti ricetta.

---

## 3. Relazione generale tra i concetti

```text
Barcode / prodotto reale comprato
        ↓
CategoriaAlimentare interna
        ↑
Ingrediente ricetta API
```

Il prodotto reale acquistato arriva da barcode/Open Food Facts.

L’ingrediente ricetta arriva da Spoonacular o Edamam.

La dispensa confronta solo categorie interne.

Esempio:

```text
Barcode: Chicken Nuggets Findus
→ CategoriaAlimentare: Pollo fritto

Ingrediente ricetta: fried chicken
→ CategoriaAlimentare: Pollo fritto

Dispensa:
Pollo fritto, scadenza 20/07, quantità 2
```

A questo punto il confronto ricetta/dispensa avviene tramite una relazione molti-a-molti:

```text
IngredienteRicetta
    └── RecipeIngredientMatch → una o più CategoriaAlimentare

LottoScadenza
    └── CategoriaAlimentare
```

Un ingrediente ricetta è considerato “in dispensa” se almeno una delle categorie interne compatibili ha un lotto attivo con quantità maggiore di 0.

---

## 4. Storage scelto

### Room

In Room vengono salvati i dati strutturali dell’app:

```text
CategoriaAlimentare
BarcodeProductLink
RecipeIngredientLink
ExpiryLot
FavoriteRecipe
RecipeIngredient
RecipeIngredientMatch
```

### DataStore

In DataStore vengono salvate le impostazioni utente:

```text
UserSettings
```

Motivo: le impostazioni sono preferenze semplici chiave-valore e non richiedono query relazionali.

---

## 5. Entity: CategoriaAlimentare

Nome Kotlin consigliato:

```kotlin
FoodCategoryEntity
```

Nome concettuale italiano:

```text
CategoriaAlimentare
```

### Campi

```kotlin
@Entity(
    tableName = "food_categories",
    indices = [
        Index(value = ["normalizedName"], unique = true)
    ]
)
data class FoodCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val normalizedName: String,

    val defaultStorageLocation: StorageLocation,
    val defaultPerishability: PerishabilityType,

    val imageUri: String?,

    val source: FoodCategorySource,

    val createdAt: Instant,
    val updatedAt: Instant,
    val lastUsedAt: Instant?
)
```

### Significato campi

| Campo | Significato |
|---|---|
| `id` | Identificativo locale della categoria |
| `name` | Nome visibile all’utente, es. `Latte` |
| `normalizedName` | Nome normalizzato per ricerca/matching, es. `latte` |
| `defaultStorageLocation` | Luogo predefinito: frigo/freezer/dispensa |
| `defaultPerishability` | Tipo di deperibilità: fresco/lunga conservazione |
| `imageUri` | Immagine scelta o salvata localmente, opzionale |
| `source` | Origine della categoria: seed, utente, suggerita |
| `createdAt` | Data creazione |
| `updatedAt` | Ultimo aggiornamento |
| `lastUsedAt` | Ultimo uso in inserimento/scansione/ricetta |

### Regole

- Le categorie seed vengono precaricate da file JSON.
- L’utente può creare nuove categorie runtime.
- Una categoria senza lotti attivi resta nel database.
- La categoria viene nascosta dalla dispensa se non ha quantità attive.
- La categoria resta disponibile per autocomplete, barcode e ricette.

---

## 6. Entity: BarcodeProductLink

Nome Kotlin consigliato:

```kotlin
BarcodeProductLinkEntity
```

Questa entity rappresenta il collegamento:

```text
barcode / prodotto reale comprato → CategoriaAlimentare
```

Esempio:

```text
8001234567890 → Latte Parmalat Zymil 1L → Latte
```

### Campi

```kotlin
@Entity(
    tableName = "barcode_product_links",
    foreignKeys = [
        ForeignKey(
            entity = FoodCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class BarcodeProductLinkEntity(
    @PrimaryKey
    val barcode: String,

    val categoryId: Long,

    val productName: String?,
    val genericName: String?,
    val brand: String?,
    val quantityLabel: String?,

    val imageUrl: String?,

    val rawCategoryTags: String?,
    val rawFoodGroupTags: String?,

    val source: LinkSource,
    val confidence: MatchConfidence,

    val isUserVerified: Boolean,
    val isActive: Boolean,

    val createdAt: Instant,
    val updatedAt: Instant,
    val lastUsedAt: Instant?
)
```

### Significato campi

| Campo | Significato |
|---|---|
| `barcode` | Codice a barre del prodotto |
| `categoryId` | Categoria interna associata |
| `productName` | Nome prodotto da Open Food Facts |
| `genericName` | Nome generico da Open Food Facts |
| `brand` | Marca |
| `quantityLabel` | Formato commerciale, es. `1L`, `500g` |
| `imageUrl` | Immagine prodotto da API |
| `rawCategoryTags` | Tag categoria API salvati per debug/matching futuro |
| `rawFoodGroupTags` | Tag food group API salvati per debug/matching futuro |
| `source` | Origine del collegamento |
| `confidence` | Confidenza del match |
| `isUserVerified` | True se confermato/corretto dall’utente |
| `isActive` | False se il collegamento è stato disattivato |
| `lastUsedAt` | Ultima scansione/uso |

### Regole

- Un barcode attivo punta a una sola categoria interna.
- Più barcode possono puntare alla stessa categoria interna.
- La dispensa non aggrega per barcode.
- Il barcode serve solo per riconoscere automaticamente la categoria.
- Se l’utente corregge un barcode, il vecchio collegamento non deve più essere usato.
- Per evitare associazioni sbagliate che ricompaiono, i link corretti/rimossi possono essere mantenuti con `isActive = false`.

---

## 7. Entity: RecipeIngredientLink

Nome Kotlin consigliato:

```kotlin
RecipeIngredientLinkEntity
```

Questa entity rappresenta il collegamento:

```text
nome ingrediente ricetta / id ingrediente API → CategoriaAlimentare
```

Esempio:

```text
olive oil → Olio
extra virgin olive oil → Olio
spaghetti → Pasta
fried chicken → Pollo fritto
```

### Campi

```kotlin
@Entity(
    tableName = "recipe_ingredient_links",
    foreignKeys = [
        ForeignKey(
            entity = FoodCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["normalizedAlias"]),
        Index(value = ["sourceApi", "externalIngredientId"])
    ]
)
data class RecipeIngredientLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val categoryId: Long,

    val aliasOriginal: String,
    val normalizedAlias: String,

    val language: String?,

    val sourceApi: RecipeSource?,
    val externalIngredientId: String?,

    val source: LinkSource,
    val confidence: MatchConfidence,

    val isUserVerified: Boolean,
    val isActive: Boolean,

    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Significato campi

| Campo | Significato |
|---|---|
| `categoryId` | Categoria interna collegata |
| `aliasOriginal` | Nome ingrediente come mostrabile all’utente/API |
| `normalizedAlias` | Nome normalizzato usato per matching |
| `language` | Lingua, es. `it`, `en`, opzionale |
| `sourceApi` | API origine, es. Spoonacular/Edamam |
| `externalIngredientId` | ID ingrediente API se disponibile |
| `source` | Origine collegamento |
| `confidence` | Confidenza |
| `isUserVerified` | True se confermato/corretto dall’utente |
| `isActive` | False se disattivato |
| `createdAt`, `updatedAt` | Audit locale |

### Regole

- Più nomi ricetta possono puntare alla stessa categoria interna.
- Lo stesso nome ricetta può puntare a più categorie interne compatibili.
- Questa tabella non rappresenta una funzione 1→1, ma una relazione molti-a-molti tra alias ricetta e categorie interne.
- Esempio: `pollo` può essere compatibile con `Petto di pollo`, `Pollo campese`, `Cosce di pollo`.
- Un nome ricetta può essere corretto dall’utente.
- Quando l’utente sceglie “Ricorda per il futuro”, si crea o aggiorna uno o più `RecipeIngredientLink`.
- Quando l’utente sceglie “Solo questa ricetta”, non è obbligatorio creare un link globale.
- I link disattivati non devono essere usati nei match automatici.
- Le correzioni utente hanno priorità sui match automatici.
- Se si introduce un vincolo unique, non deve essere solo su `normalizedAlias`, ma su `normalizedAlias + categoryId + sourceApi + externalIngredientId` dove applicabile.

---

## 8. Entity: ExpiryLot

Nome Kotlin consigliato:

```kotlin
ExpiryLotEntity
```

Nome concettuale italiano:

```text
LottoScadenza
```

Questa entity rappresenta la quantità reale in dispensa aggregata per:

```text
CategoriaAlimentare + dataScadenza
```

Non rappresenta una singola confezione fisica.

Esempio:

```text
Latte, scadenza 20/07, quantità 2
```

può derivare da due prodotti diversi:

```text
Latte Parmalat, scadenza 20/07
Latte Coop, scadenza 20/07
```

ma per PantryPal è un solo lotto:

```text
Latte — 20/07 — ×2
```

### Campi

```kotlin
@Entity(
    tableName = "expiry_lots",
    foreignKeys = [
        ForeignKey(
            entity = FoodCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["expirationDate"]),
        Index(value = ["categoryId", "expirationDate"], unique = true)
    ]
)
data class ExpiryLotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val categoryId: Long,

    val expirationDate: LocalDate,
    val quantity: Int,

    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Regola di aggregazione

Vincolo fondamentale:

```text
UNIQUE(categoryId, expirationDate)
```

Quando viene inserita una nuova quantità:

```text
stessa categoria + stessa scadenza → incrementa quantity
```

Esempio:

```text
Latte, 20/07, +1
Latte, 20/07, +1
```

risultato:

```text
Latte, 20/07, quantità 2
```

### Regole quantità

- `quantity` deve essere sempre maggiore di 0 nei lotti attivi.
- Se un decremento porta `quantity` a 0, il lotto viene eliminato oppure considerato non più attivo.
- La categoria interna resta nel DB anche se non ha più lotti.
- Tutti i conteggi dell’app contano quantità di confezioni, non categorie.

---

## 9. Entity: FavoriteRecipe

Nome Kotlin consigliato:

```kotlin
FavoriteRecipeEntity
```

Le ricette vengono recuperate da API esterna, ma in locale vengono salvate solo le preferite.

### Campi

```kotlin
@Entity(
    tableName = "favorite_recipes",
    indices = [
        Index(value = ["source", "externalId"], unique = true)
    ]
)
data class FavoriteRecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val externalId: String,
    val source: RecipeSource,

    val title: String,
    val description: String?,

    val preparationTimeMinutes: Int?,
    val servings: Int?,

    val imageUrl: String?,
    val sourceUrl: String?,

    val savedAt: Instant,
    val updatedAt: Instant
)
```

### Regole

- Solo le ricette preferite vengono salvate in Room.
- `source + externalId` deve essere unico.
- Il like salva la ricetta senza conferma.
- Il dettaglio ricetta può arrivare da API o da cache locale se preferita.

---

## 10. Entity: RecipeIngredient

Nome Kotlin consigliato:

```kotlin
RecipeIngredientEntity
```

Rappresenta un ingrediente all’interno di una ricetta preferita.

Mantiene il dato originale dell’API, ma non contiene più direttamente `categoryId`.

Motivo: lo stesso ingrediente ricetta può essere compatibile con più alimenti interni.

Esempio:

```text
Ingrediente ricetta: pollo

Categorie interne compatibili:
- Petto di pollo
- Pollo campese
- Cosce di pollo
```

Quindi il collegamento con le categorie interne viene spostato nella tabella ponte:

```text
RecipeIngredientMatchEntity
```

### Campi

```kotlin
@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = FavoriteRecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recipeId"]),
        Index(value = ["normalizedName"]),
        Index(value = ["externalIngredientId"])
    ]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val recipeId: Long,

    val originalName: String,
    val normalizedName: String,

    val externalIngredientId: String?,

    val amount: Double?,
    val unit: String?
)
```

### Regole

- `RecipeIngredientEntity` conserva solo l’ingrediente originale della ricetta.
- Non contiene più `categoryId`.
- Il matching verso alimenti interni avviene tramite `RecipeIngredientMatchEntity`.
- Un ingrediente può avere zero, uno o più match verso categorie interne.
- Se non esistono match attivi, l’ingrediente viene considerato non riconosciuto.
- Nel MVP il confronto controlla solo presenza/assenza, non quantità sufficiente.
- Le quantità ricetta sono mantenute per visualizzazione.

---

## 11. Entity: RecipeIngredientMatch

Nome Kotlin consigliato:

```kotlin
RecipeIngredientMatchEntity
```

Questa entity rappresenta il collegamento tra un ingrediente specifico di una ricetta salvata e una categoria interna compatibile.

È una tabella ponte molti-a-molti tra:

```text
RecipeIngredientEntity
FoodCategoryEntity
```

Serve per supportare casi in cui un ingrediente generico può essere soddisfatto da più alimenti interni.

Esempio:

```text
pollo → Petto di pollo
pollo → Pollo campese
pollo → Cosce di pollo
```

### Campi

```kotlin
@Entity(
    tableName = "recipe_ingredient_matches",
    foreignKeys = [
        ForeignKey(
            entity = RecipeIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeIngredientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recipeIngredientId"]),
        Index(value = ["categoryId"]),
        Index(value = ["recipeIngredientId", "categoryId"], unique = true)
    ]
)
data class RecipeIngredientMatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val recipeIngredientId: Long,
    val categoryId: Long,

    val relationType: IngredientRelationType,

    val matchSource: MatchSource,
    val confidence: MatchConfidence,

    val isUserConfirmed: Boolean,
    val isUserRejected: Boolean,
    val isActive: Boolean,

    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Significato campi

| Campo | Significato |
|---|---|
| `recipeIngredientId` | Ingrediente specifico della ricetta |
| `categoryId` | Categoria interna compatibile |
| `relationType` | Tipo di relazione: esatta, compatibile, generica |
| `matchSource` | Origine del match |
| `confidence` | Confidenza del match |
| `isUserConfirmed` | True se confermato dall’utente |
| `isUserRejected` | True se l’utente ha escluso questa categoria |
| `isActive` | False se il match non deve più essere usato |
| `createdAt`, `updatedAt` | Audit locale |

### Regole

- Un ingrediente ricetta può avere più categorie interne compatibili.
- Una categoria interna può soddisfare più ingredienti ricetta.
- Se almeno una categoria compatibile ha un lotto attivo, l’ingrediente viene mostrato come “In dispensa”.
- Se più categorie compatibili sono presenti in dispensa, la UI può mostrare il match migliore o indicare “più alimenti compatibili”.
- Se l’utente rifiuta un match, impostare `isUserRejected = true` e `isActive = false`.
- Il vincolo `recipeIngredientId + categoryId` evita duplicati sulla stessa ricetta.
- Questa tabella rappresenta il matching locale della singola ricetta.
- I match globali e riutilizzabili restano in `RecipeIngredientLinkEntity`.

---

## 12. UserSettings in DataStore

Nome Kotlin consigliato:

```kotlin
UserSettings
```

Non è una entity Room.

### Campi

```kotlin
data class UserSettings(
    val username: String,
    val language: String,
    val theme: AppTheme,

    val expirationNotificationsEnabled: Boolean,
    val freshNotificationDays: Int,
    val longLifeNotificationDays: Int
)
```

### Regole

- Tema, lingua e toggle notifiche sono in auto-save.
- Il nome utente viene salvato all’uscita dal campo.
- Se le notifiche sono disabilitate, i campi soglia vengono nascosti.
- Le soglie sono separate per fresco e lunga conservazione.

---

## 13. Enum

### StorageLocation

```kotlin
enum class StorageLocation {
    FRIDGE,
    FREEZER,
    PANTRY
}
```

### PerishabilityType

```kotlin
enum class PerishabilityType {
    FRESH,
    LONG_LIFE
}
```

### FoodCategorySource

```kotlin
enum class FoodCategorySource {
    SEED,
    USER_CREATED,
    AUTO_SUGGESTED
}
```

### RecipeSource

```kotlin
enum class RecipeSource {
    SPOONACULAR,
    EDAMAM
}
```

### LinkSource

```kotlin
enum class LinkSource {
    SEED,
    AUTO,
    USER_CREATED,
    USER_CONFIRMED,
    USER_CORRECTED
}
```

### MatchConfidence

```kotlin
enum class MatchConfidence {
    HIGH,
    MEDIUM,
    LOW
}
```

### MatchSource

```kotlin
enum class MatchSource {
    NONE,
    BARCODE_LINK,
    RECIPE_INGREDIENT_LINK,
    TEXT_ALIAS,
    USER_SELECTED,
    AUTO_MATCH
}
```

### IngredientRelationType

```kotlin
enum class IngredientRelationType {
    EXACT,
    COMPATIBLE,
    BROADER,
    FALLBACK
}
```

Significato:

| Valore | Significato |
|---|---|
| `EXACT` | Match specifico: `petto di pollo` → `Petto di pollo` |
| `COMPATIBLE` | Match compatibile: `pollo` → `Petto di pollo`, `Pollo campese` |
| `BROADER` | L’ingrediente ricetta è più generico della categoria interna |
| `FALLBACK` | Match debole usato come proposta |

### AppTheme

```kotlin
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}
```

---

## 14. Nota sulla confidence

La confidenza è salvata nel data model, ma non viene decisa dal data model.

La confidenza viene calcolata dal livello di matching, non da Room.

Il data model deve solo conservare il risultato:

```text
HIGH / MEDIUM / LOW
```

Regola pratica prevista:

```text
HIGH
- barcode già confermato dall’utente;
- ingrediente già corretto/confermato dall’utente;
- match esatto su alias seed affidabile;
- match esatto su externalIngredientId già noto.

MEDIUM
- match testuale buono ma non confermato;
- match da tag API coerente ma non specifico;
- similarità alta ma non perfetta.

LOW
- match debole;
- fallback da categoria generica;
- match solo parziale;
- proposta automatica da confermare.
```

Priorità consigliata:

```text
USER_CORRECTED
> USER_CONFIRMED
> USER_CREATED
> SEED
> AUTO HIGH
> AUTO MEDIUM
> AUTO LOW
```

Questa parte verrà dettagliata nella specifica sul matching.

---

## 15. Nota sulle immagini

Le immagini non sono un punto centrale del data model, ma i campi devono permettere di gestirle.

Scelta consigliata:

```text
FoodCategory.imageUri
BarcodeProductLink.imageUrl
FavoriteRecipe.imageUrl
```

Significato:

- `FoodCategory.imageUri`: immagine rappresentativa dell’alimento interno;
- `BarcodeProductLink.imageUrl`: immagine del prodotto commerciale da Open Food Facts;
- `FavoriteRecipe.imageUrl`: immagine ricetta dall’API.

La strategia precisa di download/cache/placeholder viene definita fuori dal data model.

---

## 16. Seed iniziale categorie

Le categorie interne iniziali vengono precaricate da un file seed.

Percorso consigliato:

```text
assets/seed/food_categories.json
```

Il seed deve contenere:

```json
{
  "name": "Latte",
  "aliases": ["latte", "milk", "whole milk", "skim milk"],
  "defaultStorageLocation": "FRIDGE",
  "defaultPerishability": "FRESH"
}
```

Il seed crea:

- una `FoodCategoryEntity`;
- uno o più `RecipeIngredientLinkEntity` o alias equivalenti;
- eventuali riferimenti iniziali utili al matching.

Le categorie seed non impediscono la creazione runtime di nuove categorie.

---

## 17. Query principali richieste

### 17.1 Home overview

Serve ottenere:

- totale confezioni;
- totale per luogo;
- confezioni in scadenza;
- nomi deduplicati degli alimenti in scadenza;
- ricette suggerite.

La query di conteggio deve sommare `ExpiryLot.quantity`.

### 17.2 Dispensa

Serve ottenere una riga per categoria interna con:

- categoria;
- quantità totale;
- scadenza più vicina;
- numero di lotti diversi;
- luogo;
- deperibilità.

Pseudo-query:

```sql
SELECT
    category.id,
    category.name,
    category.defaultStorageLocation,
    category.defaultPerishability,
    SUM(lot.quantity) AS totalQuantity,
    MIN(lot.expirationDate) AS nearestExpirationDate,
    COUNT(lot.id) AS lotCount
FROM food_categories category
JOIN expiry_lots lot ON lot.categoryId = category.id
GROUP BY category.id
HAVING totalQuantity > 0
```

### 17.3 Lotti per dettaglio alimento

Serve ottenere tutti i lotti di una categoria:

```text
categoriaId → lista ExpiryLot ordinata per expirationDate
```

### 17.4 Barcode lookup

```text
barcode → BarcodeProductLink attivo → FoodCategory
```

Se non esiste, si chiama Open Food Facts.

### 17.5 Ingredient recipe matching

Il matching ingredienti ricetta deve restituire una lista di categorie interne compatibili, non una singola categoria.

```text
normalized ingredient name / externalIngredientId
→ RecipeIngredientLink attivi
→ lista FoodCategory compatibili
→ RecipeIngredientMatch per la ricetta corrente
```

Esempio:

```text
pollo
→ Petto di pollo
→ Pollo campese
→ Cosce di pollo
```

### 17.6 Confronto ricetta/dispensa

Per ogni ingrediente ricetta:

```text
RecipeIngredient
→ RecipeIngredientMatch attivi
→ lista categoryId compatibili
```

L’ingrediente è “In dispensa” se:

```text
esiste almeno un RecipeIngredientMatch attivo
e almeno una categoria compatibile ha un ExpiryLot con quantity > 0
```

Altrimenti:

```text
Da comprare
```

Esempio:

```text
Ingrediente ricetta: pollo
Match compatibili: Petto di pollo, Pollo campese

Dispensa:
Petto di pollo ×1

Risultato:
pollo → In dispensa
```

---

## 18. Regole di cancellazione/disattivazione

### FoodCategory

Non eliminare automaticamente una categoria interna quando non ha lotti attivi.

La categoria resta per:

- autocomplete;
- barcode collegati;
- ingredienti ricetta collegati;
- reinserimento futuro.

### ExpiryLot

Quando la quantità arriva a 0:

- eliminare il lotto;
- oppure mantenerlo non attivo se in futuro si decide di introdurre storico.

Per MVP: eliminare il lotto è sufficiente.

### BarcodeProductLink

Se un barcode viene corretto:

- disattivare il vecchio link;
- creare/attivare il nuovo link.

### RecipeIngredientLink

Se un ingrediente ricetta viene corretto globalmente:

- disattivare il vecchio link globale se errato;
- creare/attivare uno o più nuovi link globali.

### RecipeIngredientMatch

Se un match specifico di una ricetta viene corretto:

- disattivare o rifiutare il match sbagliato;
- creare/attivare il nuovo match corretto;
- se l’utente sceglie “Ricorda anche per il futuro”, aggiornare anche `RecipeIngredientLinkEntity`.

---

## 19. Regole funzionali collegate al data model

### Aggiunta alimento

Input:

```text
categoriaId
dataScadenza
quantità
```

Regola:

```text
se esiste ExpiryLot(categoriaId, dataScadenza)
    incrementa quantità
altrimenti
    crea nuovo ExpiryLot
```

### Decremento da riga Dispensa

Se la categoria ha un solo lotto:

```text
decremento diretto
```

Se la categoria ha due o più lotti:

```text
aprire bottom sheet per scegliere quale scadenza decrementare
```

### Decremento da Dettaglio alimento

Nel dettaglio alimento il decremento è sempre diretto sul singolo lotto.

### Ricette

Il matching ricette usa categorie interne compatibili.

Il flusso è:

```text
RecipeIngredient
→ RecipeIngredientMatch
→ FoodCategory
→ ExpiryLot
```

Nel MVP:

```text
si controlla presenza/assenza su almeno una categoria compatibile
non si controlla quantità sufficiente
```

Esempio:

```text
pollo → Petto di pollo / Pollo campese
se una delle due categorie è presente in dispensa → ingrediente in dispensa
```

---

## 20. Data model finale

```text
Room
├── FoodCategoryEntity
├── BarcodeProductLinkEntity
├── RecipeIngredientLinkEntity
├── ExpiryLotEntity
├── FavoriteRecipeEntity
├── RecipeIngredientEntity
└── RecipeIngredientMatchEntity

DataStore
└── UserSettings
```

Relazioni:

```text
FoodCategory 1 ─── N BarcodeProductLink
FoodCategory 1 ─── N RecipeIngredientLink
FoodCategory 1 ─── N ExpiryLot

FavoriteRecipe 1 ─── N RecipeIngredient

RecipeIngredient 1 ─── N RecipeIngredientMatch
FoodCategory 1 ─── N RecipeIngredientMatch

RecipeIngredient N ─── N FoodCategory
tramite RecipeIngredientMatch
```

---

## 21. Decisioni fissate

1. Il barcode non identifica direttamente un lotto in dispensa.
2. Il barcode identifica un prodotto commerciale collegato a una categoria interna.
3. La quantità della dispensa è aggregata per categoria interna e data di scadenza.
4. Le ricette non vengono confrontate con barcode o prodotti commerciali.
5. Gli ingredienti ricetta vengono normalizzati verso una o più categorie interne compatibili.
6. Il confronto ricetta/dispensa avviene tramite `RecipeIngredientMatch`, non tramite un singolo `categoryId` su `RecipeIngredient`.
7. Le categorie interne possono essere create runtime.
8. I collegamenti barcode e ingredienti ricetta possono essere corretti.
9. I collegamenti sbagliati vengono disattivati, non necessariamente eliminati.
10. Le impostazioni utente stanno in DataStore.
11. Le ricette salvate in locale sono solo le preferite.
12. Le immagini sono supportate dal modello ma la strategia di caching è fuori scope.
13. La confidence è salvata nel modello ma calcolata dal livello di matching.
14. Il modello supporta associazioni molti-a-molti tra ingredienti ricetta e categorie interne.
