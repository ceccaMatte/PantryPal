# PantryPal — Specifica Data Model finale aggiornata

## 1. Scopo del documento

Questo documento definisce il data model finale di PantryPal.

Il modello deve supportare:

- gestione local-first della dispensa;
- tracciamento di scadenze e quantità;
- aggregazione delle confezioni per alimento interno e data di scadenza;
- riconoscimento dei prodotti reali tramite barcode/Open Food Facts;
- matching globale tra label degli ingredienti ricetta e alimenti interni;
- correzione dei collegamenti sbagliati;
- creazione runtime di nuovi alimenti interni;
- salvataggio locale delle ricette preferite;
- impostazioni utente tramite DataStore.

Il punto centrale del modello è separare tre concetti diversi:

```text
Prodotto reale acquistato ≠ Alimento interno PantryPal ≠ Ingrediente ricetta API
```

Tutti e tre convergono su una base comune:

```text
FoodCategory / CategoriaAlimentare
```

---

## 2. Concetto centrale: FoodCategory / CategoriaAlimentare

La `FoodCategory` rappresenta l’alimento interno usato da PantryPal.

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
FoodCategory interna
        ↑
Label ingrediente ricetta
```

Il prodotto reale acquistato arriva da barcode/Open Food Facts.

L’ingrediente ricetta arriva dall’unica API ricette scelta dopo i test.

La dispensa confronta solo categorie interne.

Esempio:

```text
Barcode: Chicken Nuggets Findus
→ FoodCategory: Pollo fritto

Label ingrediente ricetta: fried chicken
→ FoodCategory: Pollo fritto

Dispensa:
Pollo fritto, scadenza 20/07, quantità 2
```

Il confronto ricetta/dispensa non usa barcode e non usa prodotti commerciali.

Il confronto usa questo percorso:

```text
Ingrediente ricetta
→ normalizedName
→ RecipeIngredientLink attivi
→ FoodCategory compatibili
→ ExpiryLot attivi
→ In dispensa / Da comprare
```

Un ingrediente ricetta è considerato “in dispensa” se almeno una delle categorie interne compatibili ha un lotto attivo con quantità maggiore di 0.

---

## 4. Vincolo importante: ricetta salvata ≠ mapping ingrediente

La ricetta è un contenuto:

```text
titolo
descrizione
immagine
tempo
porzioni
ingredienti con quantità
```

Questi dati vengono salvati in Room solo se l’utente mette like alla ricetta.

Il mapping tra label ingrediente e alimento interno, invece, è globale e persistente.

Esempi:

```text
pasta → Pasta
olio d'oliva → Olio
fried chicken → Pollo fritto
```

Questi link devono restare salvati anche se la ricetta da cui sono nati non viene salvata tra i preferiti.

Motivo: lo stesso ingrediente può comparire in tante ricette diverse.

Quindi:

```text
FavoriteRecipeEntity / RecipeIngredientEntity
```

rappresentano dati della ricetta preferita.

```text
RecipeIngredientLinkEntity
```

rappresenta il dizionario globale degli ingredienti riconosciuti dall’app.

---

## 5. Storage scelto

### Room

In Room vengono salvati i dati strutturali dell’app:

```text
FoodCategory
BarcodeProductLink
RecipeIngredientLink
ExpiryLot
FavoriteRecipe
RecipeIngredient
```

Non esiste più una tabella persistente `RecipeIngredientMatchEntity`.

Il match della singola ricetta viene calcolato a runtime usando `RecipeIngredientLinkEntity`.

### DataStore

In DataStore vengono salvate le impostazioni utente:

```text
UserSettings
```

Motivo: le impostazioni sono preferenze semplici chiave-valore e non richiedono query relazionali.

---

## 6. Entity: FoodCategory

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

    val origin: CategoryOrigin,

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
| `origin` | Origine della categoria: seed o utente |
| `createdAt` | Data creazione |
| `updatedAt` | Ultimo aggiornamento |
| `lastUsedAt` | Ultimo uso in inserimento/scansione/ricetta |

### Regole

- Le categorie seed vengono precaricate da file JSON.
- L’utente può creare nuove categorie runtime.
- Una categoria senza lotti attivi resta nel database.
- La categoria viene nascosta dalla dispensa se non ha quantità attive.
- La categoria resta disponibile per autocomplete, barcode e ricette.
- `origin` serve solo a distinguere categorie preinstallate da categorie create dall’utente.
- In Aggiunta Manuale il campo di testo serve solo a filtrare/cercare; per salvare serve sempre una categoria selezionata tramite badge.
- Una nuova categoria creata dall’utente viene scritta davvero in DB solo al salvataggio finale del form.

---

## 7. Entity: BarcodeProductLink

Nome Kotlin consigliato:

```kotlin
BarcodeProductLinkEntity
```

Questa entity rappresenta il collegamento:

```text
barcode / prodotto reale comprato → FoodCategory
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

    val productName: String,
    val genericName: String?,
    val brand: String?,
    val quantityLabel: String?,

    val imageUrl: String?,

    val rawCategoryTags: String?,
    val rawFoodGroupTags: String?,

    val origin: LinkOrigin,
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
| `rawCategoryTags` | Tag categoria API salvati per matching/debug futuro |
| `rawFoodGroupTags` | Tag food group API salvati per matching/debug futuro |
| `origin` | Origine del collegamento: seed, automatico o utente |
| `isActive` | False se il collegamento è stato disattivato |
| `lastUsedAt` | Ultima scansione/uso |

### Regole

- Un barcode attivo punta a una sola categoria interna.
- Più barcode possono puntare alla stessa categoria interna.
- La dispensa non aggrega per barcode.
- Il barcode serve solo per riconoscere automaticamente la categoria.
- Se l’utente corregge un barcode, il vecchio collegamento non deve più essere usato.
- Per evitare associazioni sbagliate che ricompaiono, i link corretti/rimossi possono essere mantenuti con `isActive = false`.
- Non si salva `confidence` nel DB: eventuali score servono solo a runtime per ordinare suggerimenti.
- Non si salvano link barcode parziali.
- Se il barcode non è riconosciuto da Open Food Facts, il successivo inserimento è trattato come manuale puro e non crea `BarcodeProductLink`.
- Un `BarcodeProductLink` viene creato solo se esistono dati prodotto validi e una categoria selezionata dall’utente.

---

## 8. Entity: RecipeIngredientLink

Nome Kotlin consigliato:

```kotlin
RecipeIngredientLinkEntity
```

Questa entity rappresenta il dizionario globale:

```text
label ingrediente ricetta / id ingrediente API → FoodCategory
```

Esempi:

```text
olive oil → Olio
extra virgin olive oil → Olio
spaghetti → Pasta
fried chicken → Pollo fritto
pollo → Petto di pollo
pollo → Cosce di pollo
pollo → Pollo campese
```

Questa tabella supporta relazioni molti-a-molti.

Lo stesso alias può puntare a più categorie interne compatibili.

La stessa categoria interna può essere raggiunta da più alias diversi.

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
        Index(value = ["externalIngredientId"]),
        Index(value = ["normalizedAlias", "categoryId"], unique = true)
    ]
)
data class RecipeIngredientLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val categoryId: Long,

    val aliasOriginal: String,
    val normalizedAlias: String,

    val language: String?,

    val externalIngredientId: String?,

    val relationType: IngredientRelationType,

    val origin: LinkOrigin,
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
| `externalIngredientId` | ID ingrediente API se disponibile |
| `relationType` | Tipo di relazione tra alias e categoria interna |
| `origin` | Origine del collegamento: seed, automatico o utente |
| `isActive` | False se disattivato |
| `createdAt`, `updatedAt` | Audit locale |

### Regole

- `RecipeIngredientLinkEntity` è indipendente dalle singole ricette.
- I link ingrediente/alimento restano salvati anche se la ricetta da cui sono nati non è preferita.
- Più nomi ricetta possono puntare alla stessa categoria interna.
- Lo stesso nome ricetta può puntare a più categorie interne compatibili.
- Questa tabella non rappresenta una funzione 1→1, ma una relazione molti-a-molti tra alias ricetta e categorie interne.
- Esempio: `pollo` può essere compatibile con `Petto di pollo`, `Pollo campese`, `Cosce di pollo`.
- Un nome ricetta può essere corretto dall’utente.
- Ogni associazione manuale ingrediente/alimento viene sempre ricordata per il futuro.
- Non esiste distinzione persistente tra “solo questa ricetta” e “ricorda per il futuro”.
- I link disattivati non devono essere usati nei match automatici.
- I link `origin = USER` hanno priorità sui link automatici/seed quando si ordinano i suggerimenti.
- Il vincolo `normalizedAlias + categoryId` evita duplicati inutili mantenendo il supporto molti-a-molti.

---

## 9. Entity: ExpiryLot

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
FoodCategory + expirationDate
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

### Regole quantità

- `quantity` deve essere sempre maggiore di 0 nei lotti attivi.
- Se un decremento porta `quantity` a 0, il lotto viene eliminato oppure considerato non più attivo.
- La categoria interna resta nel DB anche se non ha più lotti.
- Tutti i conteggi dell’app contano quantità di confezioni, non categorie.
- Nel Dettaglio Alimento le modifiche ai lotti sono locali/draft finché l’utente non preme “Salva modifiche”.
- Se tutti i lotti vengono portati a 0 nella UI, la pagina resta visibile; al salvataggio l’alimento sparisce dalla lista Dispensa ma la categoria resta nel DB.

---

## 10. Entity: FavoriteRecipe

Nome Kotlin consigliato:

```kotlin
FavoriteRecipeEntity
```

Le ricette vengono recuperate da una sola API esterna scelta dopo i test.

In locale vengono salvate solo le ricette preferite.

### Campi

```kotlin
@Entity(
    tableName = "favorite_recipes",
    indices = [
        Index(value = ["externalId"], unique = true)
    ]
)
data class FavoriteRecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val externalId: String,

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
- `externalId` deve essere unico.
- Il like salva la ricetta senza conferma.
- L’unlike rimuove la ricetta preferita e i suoi ingredienti salvati.
- Il dettaglio ricetta può arrivare da API o da cache locale se preferita.
- Non si salva `RecipeSource`, perché il progetto userà una sola sorgente ricette definitiva.
- Offline sono consultabili solo le ricette preferite già salvate localmente.
- La rimozione di una ricetta preferita non elimina i link globali `RecipeIngredientLinkEntity`.

---

## 11. Entity: RecipeIngredient

Nome Kotlin consigliato:

```kotlin
RecipeIngredientEntity
```

Rappresenta un ingrediente all’interno di una ricetta preferita.

Mantiene il dato originale dell’API, ma non contiene direttamente `categoryId`.

Motivo: il collegamento tra label ingrediente e alimenti interni è globale e sta in `RecipeIngredientLinkEntity`.

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

- `RecipeIngredientEntity` conserva solo l’ingrediente originale della ricetta preferita.
- Non contiene `categoryId`.
- Non contiene match verso categorie interne.
- Il matching verso alimenti interni avviene a runtime tramite `RecipeIngredientLinkEntity`.
- Un ingrediente può avere zero, uno o più link verso categorie interne compatibili.
- Se non esistono link attivi, l’ingrediente viene considerato non riconosciuto.
- Nel MVP il confronto controlla solo presenza/assenza, non quantità sufficiente.
- Le quantità ricetta sono mantenute per visualizzazione.
- Per ricette non preferite, ingredienti e dettaglio restano solo in RAM.

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
    val longLifeNotificationDays: Int,

    val pantryStorageFilter: StorageLocationFilter
)
```

### Regole

- Tema, lingua e toggle notifiche sono in auto-save.
- Il nome utente viene salvato all’uscita dal campo.
- Se le notifiche sono disabilitate, i campi soglia vengono nascosti.
- Le soglie sono separate per fresco e lunga conservazione.
- Il filtro luogo della Dispensa è persistente.

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

### StorageLocationFilter

```kotlin
enum class StorageLocationFilter {
    ALL,
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

### CategoryOrigin

```kotlin
enum class CategoryOrigin {
    SEED,
    USER
}
```

### LinkOrigin

```kotlin
enum class LinkOrigin {
    SEED,
    AUTO,
    USER
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

## 14. Score e confidence fuori dal database

Il database non salva:

```text
MatchConfidence
MatchSource
```

La bontà di un match viene calcolata a runtime dal livello di matching.

Lo score serve solo per:

- ordinare i badge;
- scegliere il primo valore precompilato;
- scartare proposte assurde;
- aiutare debug interni.

Esempio runtime:

```kotlin
data class MatchCandidate(
    val categoryId: Long,
    val score: Int
)
```

La UX resta sempre coerente:

```text
1. L’app precompila il miglior match disponibile.
2. Mostra sempre alternative come badge/chip.
3. Permette sempre di cambiare.
4. Permette sempre di creare un nuovo alimento.
```

Quindi non servono comportamenti diversi tra HIGH/MEDIUM/LOW.

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
- uno o più `RecipeIngredientLinkEntity`;
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
categoryId → lista ExpiryLot ordinata per expirationDate
```

### 17.4 Barcode lookup

```text
barcode → BarcodeProductLink attivo → FoodCategory
```

Se non esiste, si chiama Open Food Facts.

Se Open Food Facts non riconosce il barcode, non viene salvato alcun link barcode parziale.

### 17.5 Ingredient recipe matching

Il matching ingredienti ricetta deve restituire una lista di categorie interne compatibili, non una singola categoria.

```text
normalized ingredient name / externalIngredientId
→ RecipeIngredientLink attivi
→ lista FoodCategory compatibili
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
ingrediente ricetta
→ normalizedName / externalIngredientId
→ RecipeIngredientLink attivi
→ categoryId compatibili
→ ExpiryLot attivi
```

L’ingrediente è “In dispensa” se:

```text
esiste almeno un RecipeIngredientLink attivo
e almeno una categoria compatibile ha un ExpiryLot con quantity > 0
```

Altrimenti:

```text
Da comprare
```

Esempio:

```text
Ingrediente ricetta: pollo
Link compatibili: Petto di pollo, Pollo campese

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

Se un barcode non viene riconosciuto da Open Food Facts:

- non creare link barcode parziale;
- trattare il flow successivo come inserimento manuale puro.

### RecipeIngredientLink

Se un ingrediente ricetta viene corretto globalmente:

- disattivare il vecchio link globale se errato;
- creare/attivare uno o più nuovi link globali.

La disattivazione di un link ingrediente/alimento non dipende dalla cancellazione di una ricetta preferita.

---

## 19. Regole funzionali collegate al data model

### Aggiunta alimento

Input obbligatori:

```text
selectedCategory oppure pendingNewCategory selezionata
expirationDate
quantity
```

Regole:

```text
se manca la categoria selezionata → errore UI
se manca la data scadenza → errore UI
se quantity < 1 → errore UI
```

Salvataggio:

```text
se categoria nuova → crea FoodCategory
se esiste ExpiryLot(categoryId, expirationDate) → incrementa quantity
altrimenti → crea nuovo ExpiryLot
se il flow arriva da barcode riconosciuto con dati prodotto validi → crea/aggiorna BarcodeProductLink
```

### Decremento da riga Dispensa

Se la categoria ha un solo lotto:

```text
decremento diretto
```

Se la categoria ha due o più lotti:

```text
naviga a Dettaglio Alimento
```

### Incremento da riga Dispensa

```text
+ su alimento → Dettaglio Alimento
```

Motivo: bisogna scegliere se incrementare una scadenza esistente o crearne una nuova.

### Dettaglio alimento

Nel dettaglio alimento tutte le modifiche sono locali/draft finché l’utente non preme “Salva modifiche”.

Modifiche locali:

```text
+ / − sui lotti
nuova scadenza
modifica data scadenza
cambio luogo
cambio deperibilità
```

Se l’utente esce con back senza salvare:

```text
le modifiche locali vengono scartate
nessun modal di conferma
nessuna scrittura nel DB
```

### Ricette

Il matching ricette usa categorie interne compatibili tramite `RecipeIngredientLinkEntity`.

Il flusso è:

```text
ingrediente ricetta
→ RecipeIngredientLink
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

Gli spostamenti manuali tra “In dispensa” e “Da comprare” nel Dettaglio Ricetta sono solo temporanei e non persistenti.

---

## 20. Data model finale

```text
Room
├── FoodCategoryEntity
├── BarcodeProductLinkEntity
├── RecipeIngredientLinkEntity
├── ExpiryLotEntity
├── FavoriteRecipeEntity
└── RecipeIngredientEntity

DataStore
└── UserSettings
```

Relazioni:

```text
FoodCategory 1 ─── N BarcodeProductLink
FoodCategory 1 ─── N RecipeIngredientLink
FoodCategory 1 ─── N ExpiryLot

FavoriteRecipe 1 ─── N RecipeIngredient

RecipeIngredientLink N ─── 1 FoodCategory
```

Relazione logica di matching:

```text
RecipeIngredient / ingrediente runtime
    normalizedName / externalIngredientId
        ↓
RecipeIngredientLink attivi
        ↓
FoodCategory compatibili
        ↓
ExpiryLot attivi
```

---

## 21. Decisioni fissate

1. Il barcode non identifica direttamente un lotto in dispensa.
2. Il barcode identifica un prodotto commerciale collegato a una categoria interna.
3. Il barcode link viene salvato solo se il prodotto è riconosciuto e i dati prodotto sono validi.
4. Se il barcode è sconosciuto, il successivo inserimento è manuale puro e non crea link barcode parziale.
5. La quantità della dispensa è aggregata per categoria interna e data di scadenza.
6. Le ricette non vengono confrontate con barcode o prodotti commerciali.
7. Gli ingredienti ricetta vengono normalizzati verso una o più categorie interne compatibili.
8. Il confronto ricetta/dispensa avviene tramite `RecipeIngredientLinkEntity`, non tramite una tabella `RecipeIngredientMatchEntity`.
9. `RecipeIngredientMatchEntity` non fa parte del data model persistente.
10. Le categorie interne possono essere create runtime.
11. I collegamenti barcode e ingredienti ricetta possono essere corretti.
12. I collegamenti sbagliati vengono disattivati, non necessariamente eliminati.
13. Le impostazioni utente stanno in DataStore.
14. Le ricette salvate in locale sono solo le preferite.
15. I dati delle ricette non preferite restano solo in RAM.
16. I mapping ingrediente/alimento restano persistenti anche se la ricetta non è preferita.
17. La cancellazione di una ricetta preferita non cancella i mapping globali ingrediente/alimento.
18. Le immagini sono supportate dal modello ma la strategia di caching è fuori scope.
19. Il modello supporta associazioni molti-a-molti tra label ingrediente ricetta e categorie interne tramite `RecipeIngredientLinkEntity`.
20. La bontà dei match viene calcolata a runtime, non salvata in Room.
21. `RecipeSource`, `MatchConfidence`, `MatchSource`, `FoodCategorySource`, `LinkSource` e `RecipeIngredientMatchEntity` non fanno parte del data model finale.
22. Le uniche source/origin persistenti sono `CategoryOrigin` e `LinkOrigin`.
