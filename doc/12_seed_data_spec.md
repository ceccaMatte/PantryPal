# PantryPal — Seed Data Spec

> Versione 1.0  
> Scopo: definire i dati iniziali da caricare nel database locale al primo avvio dell’app.

---

## 1. Obiettivo del seed

Il seed iniziale serve a evitare che l’app parta completamente vuota dal punto di vista semantico.

Il seed fornisce:

```text
categorie alimentari interne di base
defaultStorageLocation
defaultPerishability
alias ingrediente ricetta → alimento interno
```

Il seed NON crea:

```text
lotti in dispensa
scadenze
ricette preferite
barcode link
impostazioni utente personalizzate
```

La dispensa dell’utente deve partire vuota.

---

## 2. Perché il seed è importante

PantryPal usa una distinzione tra:

```text
prodotto reale da barcode
alimento interno
ingrediente ricetta
```

Il seed aiuta in tre punti:

```text
autocomplete in Aggiunta Manuale
suggerimenti nel collegamento ingrediente → alimento
availability ricette per ingredienti comuni
```

Dato che abbiamo scelto un matching conservativo per le ricette, un ingrediente è considerato disponibile solo se esiste un mapping persistente:

```text
RecipeIngredientLinkEntity
```

Quindi il seed degli alias è importante.

Esempio:

```text
ingrediente ricetta: "milk"
FoodCategory: Latte
dispensa: Latte ×1
RecipeIngredientLink seed: milk → Latte
```

Risultato:

```text
milk = In dispensa
```

Senza quel link seed, anche se `milk` e `Latte` sono semanticamente simili, l’ingrediente resterebbe:

```text
Da comprare / Non collegato
```

---

## 3. Strategia di caricamento

### 3.1 Quando caricare il seed

Il seed viene caricato:

```text
solo al primo avvio
solo se il database è vuoto o se il seed non è ancora stato applicato
```

La strategia consigliata è usare DataStore per salvare una versione seed applicata:

```text
seedDataVersion = 1
```

Se `seedDataVersion < CURRENT_SEED_VERSION`, il seeder applica eventuali nuovi dati.

---

### 3.2 Idempotenza

Il seed deve essere idempotente.

Regola:

```text
eseguire il seed più volte non deve creare duplicati
```

Per ottenere questo:

```text
FoodCategory si riconosce tramite normalizedName
RecipeIngredientLink si riconosce tramite normalizedAlias + categoryId
```

---

### 3.3 Non usare ID hardcoded

I file seed non devono assumere ID numerici fissi.

Invece di:

```json
{
  "categoryId": 1,
  "alias": "milk"
}
```

usare:

```json
{
  "categoryNormalizedName": "latte",
  "aliasOriginal": "milk"
}
```

Durante il seed:

```text
1. crea/trova FoodCategory tramite normalizedName
2. usa l’id generato da Room
3. crea RecipeIngredientLink verso quell’id
```

---

## 4. File seed consigliati

Percorso consigliato:

```text
app/src/main/assets/seed/food_categories_seed.json
app/src/main/assets/seed/recipe_ingredient_links_seed.json
```

Possibile loader:

```text
core/database/seed/SeedDataLoader.kt
```

Possibile orchestratore:

```text
core/database/seed/DatabaseSeeder.kt
```

---

## 5. Schema seed categorie

### 5.1 JSON

```json
[
  {
    "name": "Latte",
    "normalizedName": "latte",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  }
]
```

Campi:

```text
name
normalizedName
defaultStorageLocation
defaultPerishability
```

Campi generati in fase di import:

```text
origin = SEED
imageUri = null
createdAt = now
updatedAt = now
lastUsedAt = null
```

---

### 5.2 StorageLocation

Valori ammessi:

```text
FRIDGE
FREEZER
PANTRY
```

---

### 5.3 PerishabilityType

Valori ammessi:

```text
FRESH
LONG_LIFE
```

---

## 6. Categorie seed MVP

Questa lista è volutamente piccola ma utile.

Non deve coprire tutto il mondo alimentare: deve solo rendere l’MVP usabile.

```json
[
  {
    "name": "Latte",
    "normalizedName": "latte",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Yogurt",
    "normalizedName": "yogurt",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Uova",
    "normalizedName": "uova",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Formaggio",
    "normalizedName": "formaggio",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Burro",
    "normalizedName": "burro",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Mozzarella",
    "normalizedName": "mozzarella",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Prosciutto",
    "normalizedName": "prosciutto",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Insalata",
    "normalizedName": "insalata",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Pomodoro",
    "normalizedName": "pomodoro",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Carote",
    "normalizedName": "carote",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Zucchine",
    "normalizedName": "zucchine",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Mele",
    "normalizedName": "mele",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Banane",
    "normalizedName": "banane",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Limoni",
    "normalizedName": "limoni",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Petto di pollo",
    "normalizedName": "petto di pollo",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Carne macinata",
    "normalizedName": "carne macinata",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Salmone",
    "normalizedName": "salmone",
    "defaultStorageLocation": "FRIDGE",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Pane",
    "normalizedName": "pane",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "FRESH"
  },
  {
    "name": "Pasta",
    "normalizedName": "pasta",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Riso",
    "normalizedName": "riso",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Farina",
    "normalizedName": "farina",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Zucchero",
    "normalizedName": "zucchero",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Sale",
    "normalizedName": "sale",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Olio",
    "normalizedName": "olio",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Tonno",
    "normalizedName": "tonno",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Passata di pomodoro",
    "normalizedName": "passata di pomodoro",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Ceci",
    "normalizedName": "ceci",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Fagioli",
    "normalizedName": "fagioli",
    "defaultStorageLocation": "PANTRY",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Piselli surgelati",
    "normalizedName": "piselli surgelati",
    "defaultStorageLocation": "FREEZER",
    "defaultPerishability": "LONG_LIFE"
  },
  {
    "name": "Verdure surgelate",
    "normalizedName": "verdure surgelate",
    "defaultStorageLocation": "FREEZER",
    "defaultPerishability": "LONG_LIFE"
  }
]
```

---

## 7. Schema seed alias ricette

### 7.1 JSON

```json
[
  {
    "aliasOriginal": "milk",
    "normalizedAlias": "milk",
    "categoryNormalizedName": "latte",
    "language": "en",
    "externalIngredientId": null
  }
]
```

Campi:

```text
aliasOriginal
normalizedAlias
categoryNormalizedName
language
externalIngredientId
```

Campi generati in fase di import:

```text
origin = SEED
isActive = true
createdAt = now
updatedAt = now
```

---

### 7.2 Note su externalIngredientId

Per il seed MVP:

```text
externalIngredientId = null
```

Motivo:

```text
gli ID ingredienti Spoonacular possono essere usati in futuro,
ma per MVP gli alias testuali sono sufficienti
```

Il matching availability userà:

```text
externalIngredientId, se presente
normalizedAlias
```

---

## 8. Alias ricette seed MVP

Questi alias servono per coprire casi comuni italiani e inglesi.

```json
[
  {
    "aliasOriginal": "latte",
    "normalizedAlias": "latte",
    "categoryNormalizedName": "latte",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "milk",
    "normalizedAlias": "milk",
    "categoryNormalizedName": "latte",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "yogurt",
    "normalizedAlias": "yogurt",
    "categoryNormalizedName": "yogurt",
    "language": null,
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "uova",
    "normalizedAlias": "uova",
    "categoryNormalizedName": "uova",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "egg",
    "normalizedAlias": "egg",
    "categoryNormalizedName": "uova",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "eggs",
    "normalizedAlias": "eggs",
    "categoryNormalizedName": "uova",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "formaggio",
    "normalizedAlias": "formaggio",
    "categoryNormalizedName": "formaggio",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "cheese",
    "normalizedAlias": "cheese",
    "categoryNormalizedName": "formaggio",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "butter",
    "normalizedAlias": "butter",
    "categoryNormalizedName": "burro",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "burro",
    "normalizedAlias": "burro",
    "categoryNormalizedName": "burro",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "mozzarella",
    "normalizedAlias": "mozzarella",
    "categoryNormalizedName": "mozzarella",
    "language": null,
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "prosciutto",
    "normalizedAlias": "prosciutto",
    "categoryNormalizedName": "prosciutto",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "ham",
    "normalizedAlias": "ham",
    "categoryNormalizedName": "prosciutto",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "lettuce",
    "normalizedAlias": "lettuce",
    "categoryNormalizedName": "insalata",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "insalata",
    "normalizedAlias": "insalata",
    "categoryNormalizedName": "insalata",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "tomato",
    "normalizedAlias": "tomato",
    "categoryNormalizedName": "pomodoro",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "tomatoes",
    "normalizedAlias": "tomatoes",
    "categoryNormalizedName": "pomodoro",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "pomodoro",
    "normalizedAlias": "pomodoro",
    "categoryNormalizedName": "pomodoro",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "carrot",
    "normalizedAlias": "carrot",
    "categoryNormalizedName": "carote",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "carrots",
    "normalizedAlias": "carrots",
    "categoryNormalizedName": "carote",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "carote",
    "normalizedAlias": "carote",
    "categoryNormalizedName": "carote",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "zucchini",
    "normalizedAlias": "zucchini",
    "categoryNormalizedName": "zucchine",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "zucchine",
    "normalizedAlias": "zucchine",
    "categoryNormalizedName": "zucchine",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "apple",
    "normalizedAlias": "apple",
    "categoryNormalizedName": "mele",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "apples",
    "normalizedAlias": "apples",
    "categoryNormalizedName": "mele",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "mele",
    "normalizedAlias": "mele",
    "categoryNormalizedName": "mele",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "banana",
    "normalizedAlias": "banana",
    "categoryNormalizedName": "banane",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "bananas",
    "normalizedAlias": "bananas",
    "categoryNormalizedName": "banane",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "banane",
    "normalizedAlias": "banane",
    "categoryNormalizedName": "banane",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "lemon",
    "normalizedAlias": "lemon",
    "categoryNormalizedName": "limoni",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "lemons",
    "normalizedAlias": "lemons",
    "categoryNormalizedName": "limoni",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "limoni",
    "normalizedAlias": "limoni",
    "categoryNormalizedName": "limoni",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "petto di pollo",
    "normalizedAlias": "petto di pollo",
    "categoryNormalizedName": "petto di pollo",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "chicken breast",
    "normalizedAlias": "chicken breast",
    "categoryNormalizedName": "petto di pollo",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "ground beef",
    "normalizedAlias": "ground beef",
    "categoryNormalizedName": "carne macinata",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "carne macinata",
    "normalizedAlias": "carne macinata",
    "categoryNormalizedName": "carne macinata",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "salmon",
    "normalizedAlias": "salmon",
    "categoryNormalizedName": "salmone",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "salmone",
    "normalizedAlias": "salmone",
    "categoryNormalizedName": "salmone",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "bread",
    "normalizedAlias": "bread",
    "categoryNormalizedName": "pane",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "pane",
    "normalizedAlias": "pane",
    "categoryNormalizedName": "pane",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "pasta",
    "normalizedAlias": "pasta",
    "categoryNormalizedName": "pasta",
    "language": null,
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "rice",
    "normalizedAlias": "rice",
    "categoryNormalizedName": "riso",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "riso",
    "normalizedAlias": "riso",
    "categoryNormalizedName": "riso",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "flour",
    "normalizedAlias": "flour",
    "categoryNormalizedName": "farina",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "farina",
    "normalizedAlias": "farina",
    "categoryNormalizedName": "farina",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "sugar",
    "normalizedAlias": "sugar",
    "categoryNormalizedName": "zucchero",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "zucchero",
    "normalizedAlias": "zucchero",
    "categoryNormalizedName": "zucchero",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "salt",
    "normalizedAlias": "salt",
    "categoryNormalizedName": "sale",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "sale",
    "normalizedAlias": "sale",
    "categoryNormalizedName": "sale",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "oil",
    "normalizedAlias": "oil",
    "categoryNormalizedName": "olio",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "olive oil",
    "normalizedAlias": "olive oil",
    "categoryNormalizedName": "olio",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "olio",
    "normalizedAlias": "olio",
    "categoryNormalizedName": "olio",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "olio d’oliva",
    "normalizedAlias": "olio d oliva",
    "categoryNormalizedName": "olio",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "tuna",
    "normalizedAlias": "tuna",
    "categoryNormalizedName": "tonno",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "tonno",
    "normalizedAlias": "tonno",
    "categoryNormalizedName": "tonno",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "tomato sauce",
    "normalizedAlias": "tomato sauce",
    "categoryNormalizedName": "passata di pomodoro",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "passata di pomodoro",
    "normalizedAlias": "passata di pomodoro",
    "categoryNormalizedName": "passata di pomodoro",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "chickpeas",
    "normalizedAlias": "chickpeas",
    "categoryNormalizedName": "ceci",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "ceci",
    "normalizedAlias": "ceci",
    "categoryNormalizedName": "ceci",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "beans",
    "normalizedAlias": "beans",
    "categoryNormalizedName": "fagioli",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "fagioli",
    "normalizedAlias": "fagioli",
    "categoryNormalizedName": "fagioli",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "peas",
    "normalizedAlias": "peas",
    "categoryNormalizedName": "piselli surgelati",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "piselli",
    "normalizedAlias": "piselli",
    "categoryNormalizedName": "piselli surgelati",
    "language": "it",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "frozen vegetables",
    "normalizedAlias": "frozen vegetables",
    "categoryNormalizedName": "verdure surgelate",
    "language": "en",
    "externalIngredientId": null
  },
  {
    "aliasOriginal": "verdure surgelate",
    "normalizedAlias": "verdure surgelate",
    "categoryNormalizedName": "verdure surgelate",
    "language": "it",
    "externalIngredientId": null
  }
]
```

---

## 9. Regole per default storage

### 9.1 Frigo

Usare `FRIDGE` per alimenti freschi o normalmente refrigerati:

```text
latte
yogurt
uova
formaggio
burro
mozzarella
salumi
verdure fresche
carne
pesce
```

---

### 9.2 Dispensa

Usare `PANTRY` per alimenti secchi o a lunga conservazione:

```text
pasta
riso
farina
zucchero
sale
olio
legumi in scatola
tonno
passata
pane
frutta normalmente non refrigerata
```

---

### 9.3 Freezer

Usare `FREEZER` per categorie esplicitamente surgelate:

```text
piselli surgelati
verdure surgelate
```

Non mettere nel freezer categorie generiche come `Petto di pollo`, anche se un utente potrebbe conservarle lì.

Per MVP:

```text
il seed rappresenta il default più comune,
l’utente può modificarlo nel Dettaglio Alimento
```

---

## 10. Regole per perishability

### 10.1 Fresh

Usare `FRESH` per alimenti freschi con scadenza breve:

```text
latte
yogurt
uova
formaggio
carne
pesce
verdure
frutta
pane
```

---

### 10.2 Long life

Usare `LONG_LIFE` per alimenti con scadenza lunga:

```text
pasta
riso
farina
zucchero
sale
olio
conserve
legumi
surgelati
```

Nota:

```text
i surgelati sono LONG_LIFE perché usano soglia lunga per notifiche/scadenza.
```

---

## 11. Seeder: flusso implementativo

### 11.1 Pseudocodice

```kotlin
class DatabaseSeeder(
    private val database: PantryPalDatabase,
    private val foodCategoryDao: FoodCategoryDao,
    private val recipeIngredientLinkDao: RecipeIngredientLinkDao,
    private val settingsRepository: SettingsRepository,
    private val seedDataLoader: SeedDataLoader,
    private val textNormalizer: TextNormalizer
) {

    suspend fun seedIfNeeded() {
        val appliedVersion = settingsRepository.getSeedDataVersion()

        if (appliedVersion >= CURRENT_SEED_VERSION) return

        database.withTransaction {
            seedFoodCategories()
            seedRecipeIngredientLinks()
        }

        settingsRepository.setSeedDataVersion(CURRENT_SEED_VERSION)
    }
}
```

---

### 11.2 Seed FoodCategory

```kotlin
private suspend fun seedFoodCategories() {
    val categories = seedDataLoader.loadFoodCategories()

    categories.forEach { seed ->
        val normalized = textNormalizer.normalize(seed.name)

        val existing = foodCategoryDao.getByNormalizedName(normalized)

        if (existing == null) {
            foodCategoryDao.insert(
                FoodCategoryEntity(
                    name = seed.name,
                    normalizedName = normalized,
                    defaultStorageLocation = seed.defaultStorageLocation,
                    defaultPerishability = seed.defaultPerishability,
                    imageUri = null,
                    origin = CategoryOrigin.SEED,
                    createdAt = now,
                    updatedAt = now,
                    lastUsedAt = null
                )
            )
        }
    }
}
```

Nota:

```text
anche se il JSON contiene normalizedName,
è meglio ricalcolarlo con la funzione reale dell’app
per evitare divergenze.
```

---

### 11.3 Seed RecipeIngredientLink

```kotlin
private suspend fun seedRecipeIngredientLinks() {
    val links = seedDataLoader.loadRecipeIngredientLinks()

    links.forEach { seed ->
        val category = foodCategoryDao.getByNormalizedName(
            seed.categoryNormalizedName
        ) ?: return@forEach

        val normalizedAlias = textNormalizer.normalize(seed.aliasOriginal)

        val existing = recipeIngredientLinkDao.getLinkByAliasAndCategory(
            normalizedAlias = normalizedAlias,
            categoryId = category.id
        )

        if (existing == null) {
            recipeIngredientLinkDao.upsert(
                RecipeIngredientLinkEntity(
                    categoryId = category.id,
                    aliasOriginal = seed.aliasOriginal,
                    normalizedAlias = normalizedAlias,
                    language = seed.language,
                    externalIngredientId = seed.externalIngredientId,
                    origin = LinkOrigin.SEED,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}
```

---

## 12. Versioning seed

### 12.1 Versione iniziale

```kotlin
const val CURRENT_SEED_VERSION = 1
```

Salvare in DataStore:

```text
seedDataVersion
```

---

### 12.2 Aggiornamenti futuri

Se in futuro si aggiungono nuove categorie o alias:

```text
CURRENT_SEED_VERSION = 2
```

Il seeder deve:

```text
aggiungere ciò che manca
non modificare forzatamente categorie utente
non sovrascrivere mapping USER
non creare duplicati
```

---

### 12.3 Priorità origin

Nel ranking dei mapping:

```text
USER prima di SEED
SEED prima di AUTO
```

Quindi se l’utente crea un mapping diverso:

```text
origin = USER
```

deve avere priorità.

---

## 13. Cosa non fare nel seed

Non inserire:

```text
ExpiryLotEntity
BarcodeProductLinkEntity
FavoriteRecipeEntity
RecipeIngredientEntity
UserSettings personalizzati
```

Non impostare:

```text
username
notifiche abilitate
soglie modificate dall’utente
tema utente
lingua diversa dal default
```

I default di settings arrivano da DataStore/default app, non dal seed Room.

---

## 14. Seed e UI

### 14.1 Aggiunta Manuale

Grazie al seed, quando l’utente scrive:

```text
latte
```

può vedere:

```text
[Latte]
+ Crea “latte”
```

Se esiste match esatto, si può nascondere o de-prioritizzare `+ Crea`.

---

### 14.2 Bottom sheet ingrediente ricetta

Grazie agli alias seed, quando l’utente apre:

```text
Collega ingrediente a un alimento
```

per un ingrediente comune, l’app può suggerire badge sensati.

Esempio:

```text
ingredient: chicken breast
suggested badge: Petto di pollo
```

---

### 14.3 Availability ricette

Grazie ai link seed, alcuni ingredienti comuni possono essere classificati subito:

```text
milk → Latte
eggs → Uova
olive oil → Olio
```

Ma la regola resta conservativa:

```text
availability solo da RecipeIngredientLink persistente
```

Il seed crea link persistenti iniziali.

---

## 15. Regole finali sintetiche

```text
Il seed crea:
    FoodCategoryEntity origin=SEED
    RecipeIngredientLinkEntity origin=SEED

Il seed non crea:
    lotti/scadenze
    barcode link
    ricette preferite
    impostazioni utente personalizzate

Le categorie seed sono identificate da:
    normalizedName

Gli alias seed sono identificati da:
    normalizedAlias + categoryId

I file JSON non usano ID Room hardcoded.

Il seeder deve essere:
    idempotente
    versionato
    eseguito al primo avvio
    non distruttivo per i dati utente

Matching ricette:
    conservativo
    availability solo da RecipeIngredientLink persistenti

Il seed migliora:
    autocomplete
    suggerimenti categoria
    availability ingredienti comuni
