# PantryPal — Room DAO & Query Contract

> Versione 1.0  
> Scopo: definire DAO, projection Room, query principali, transazioni e TypeConverter necessari per implementare il database locale PantryPal.

---

## 1. Obiettivo

Questo documento collega:

```text
Data model Room
↓
DAO
↓
Repository
↓
UseCase
↓
ViewModel
```

L’obiettivo è verificare che il modello dati supporti tutte le schermate e i flow già definiti:

```text
Home
Dispensa
Dettaglio Alimento
Aggiunta Manuale
Scan Barcode
Ricette
Dettaglio Ricetta
Gestisci collegamenti alimento
Notifiche
```

---

## 2. Scelte tecniche Room

### 2.1 DAO separati

Per MVP si usano DAO separati per area dati.

DAO previsti:

```text
FoodCategoryDao
ExpiryLotDao
BarcodeProductLinkDao
RecipeDao
RecipeIngredientLinkDao
```

Non viene creato un unico DAO gigante.

Motivo:

```text
migliore separazione
query più leggibili
repository più ordinati
meno coupling tra aree diverse
```

---

### 2.2 Query aggregate

Room può fare aggregazioni semplici e frequenti:

```text
SUM(quantity)
COUNT(lots)
MIN(expirationDate)
GROUP BY categoryId
```

La logica applicativa resta fuori dai DAO.

Esempi di logica fuori da Room:

```text
soglie fresh/long life
calcolo finale “in scadenza”
availability ricette
matching ingredienti
matching barcode
formattazione notifiche
```

---

### 2.3 Search alimento

Per MVP si usa ricerca semplice con `LIKE`.

Non si usa FTS.

Motivo:

```text
numero categorie limitato
implementazione più semplice
sufficiente per MVP universitario
```

Query tipo:

```sql
WHERE normalizedName LIKE '%' || :query || '%'
```

---

### 2.4 Date e tempo

TypeConverter scelti:

```text
LocalDate → String ISO yyyy-MM-dd
Instant → Long epochMillis
Enum → String
```

Motivo:

```text
LocalDate ISO è leggibile
yyyy-MM-dd mantiene ordinamento corretto come stringa
Instant epochMillis è semplice per timestamp tecnici
Enum come String è leggibile e stabile
```

---

## 3. TypeConverter

### 3.1 Converter richiesti

```kotlin
class PantryPalTypeConverters {

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? =
        value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? =
        value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToLong(value: Instant?): Long? =
        value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? =
        value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun storageLocationToString(value: StorageLocation?): String? =
        value?.name

    @TypeConverter
    fun stringToStorageLocation(value: String?): StorageLocation? =
        value?.let(StorageLocation::valueOf)

    @TypeConverter
    fun perishabilityToString(value: PerishabilityType?): String? =
        value?.name

    @TypeConverter
    fun stringToPerishability(value: String?): PerishabilityType? =
        value?.let(PerishabilityType::valueOf)

    @TypeConverter
    fun categoryOriginToString(value: CategoryOrigin?): String? =
        value?.name

    @TypeConverter
    fun stringToCategoryOrigin(value: String?): CategoryOrigin? =
        value?.let(CategoryOrigin::valueOf)

    @TypeConverter
    fun linkOriginToString(value: LinkOrigin?): String? =
        value?.name

    @TypeConverter
    fun stringToLinkOrigin(value: String?): LinkOrigin? =
        value?.let(LinkOrigin::valueOf)
}
```

Nota:

```text
i converter possono essere separati per file se si preferisce,
ma per MVP va bene un converter unico.
```

---

## 4. Database

### 4.1 Entità Room

Entità incluse:

```text
FoodCategoryEntity
BarcodeProductLinkEntity
RecipeIngredientLinkEntity
ExpiryLotEntity
FavoriteRecipeEntity
RecipeIngredientEntity
```

Settings non è in Room:

```text
UserSettings → DataStore
```

---

### 4.2 Database class

```kotlin
@Database(
    entities = [
        FoodCategoryEntity::class,
        BarcodeProductLinkEntity::class,
        RecipeIngredientLinkEntity::class,
        ExpiryLotEntity::class,
        FavoriteRecipeEntity::class,
        RecipeIngredientEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(PantryPalTypeConverters::class)
abstract class PantryPalDatabase : RoomDatabase() {

    abstract fun foodCategoryDao(): FoodCategoryDao
    abstract fun expiryLotDao(): ExpiryLotDao
    abstract fun barcodeProductLinkDao(): BarcodeProductLinkDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientLinkDao(): RecipeIngredientLinkDao
}
```

---

## 5. Projection Room condivise

Le projection sono classi usate dai DAO per restituire dati già parzialmente aggregati.

Non sono Entity.

---

### 5.1 PantryRowProjection

Usata per lista Dispensa.

```kotlin
data class PantryRowProjection(
    val categoryId: Long,
    val name: String,
    val imageUri: String?,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val totalQuantity: Int,
    val nearestExpirationDate: LocalDate?,
    val lotCount: Int
)
```

---

### 5.2 ExpiringFoodProjection

Usata per Home e sezione “In scadenza”.

```kotlin
data class ExpiringFoodProjection(
    val categoryId: Long,
    val name: String,
    val imageUri: String?,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val expiringQuantity: Int,
    val nearestExpirationDate: LocalDate
)
```

Nota:

```text
questa projection può essere costruita anche nel repository/use case partendo da LotWithCategoryProjection,
perché le soglie in scadenza dipendono da SettingsRepository.
```

---

### 5.3 LotWithCategoryProjection

Usata da Home overview, notifiche e availability ricette.

```kotlin
data class LotWithCategoryProjection(
    val lotId: Long,
    val categoryId: Long,
    val categoryName: String,
    val normalizedName: String,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val expirationDate: LocalDate,
    val quantity: Int
)
```

---

### 5.4 FoodDetailProjection

Usata per dettaglio alimento.

```kotlin
data class FoodDetailProjection(
    val categoryId: Long,
    val name: String,
    val normalizedName: String,
    val imageUri: String?,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val origin: CategoryOrigin
)
```

---

### 5.5 BarcodeProductLinkProjection

```kotlin
data class BarcodeProductLinkProjection(
    val barcode: String,
    val categoryId: Long,
    val productName: String?,
    val genericName: String?,
    val brand: String?,
    val quantityLabel: String?,
    val imageUrl: String?,
    val rawCategoryTags: String?,
    val rawFoodGroupTags: String?,
    val origin: LinkOrigin,
    val isActive: Boolean
)
```

---

### 5.6 FavoriteRecipeCardProjection

```kotlin
data class FavoriteRecipeCardProjection(
    val id: Long,
    val externalId: String,
    val title: String,
    val imageUrl: String?,
    val preparationTimeMinutes: Int?,
    val servings: Int?
)
```

---

### 5.7 RecipeIngredientProjection

```kotlin
data class RecipeIngredientProjection(
    val id: Long,
    val recipeId: Long,
    val originalName: String,
    val normalizedName: String,
    val externalIngredientId: String?,
    val amount: Double?,
    val unit: String?
)
```

---

### 5.8 RecipeIngredientLinkProjection

```kotlin
data class RecipeIngredientLinkProjection(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val aliasOriginal: String,
    val normalizedAlias: String,
    val language: String?,
    val externalIngredientId: String?,
    val origin: LinkOrigin,
    val isActive: Boolean
)
```

---

## 6. FoodCategoryDao

### 6.1 Responsabilità

Gestisce:

```text
FoodCategoryEntity
ricerca categorie
creazione categorie utente
aggiornamento metadati categoria
query aggregate per Dispensa/Home
```

---

### 6.2 Interface

```kotlin
@Dao
interface FoodCategoryDao {

    @Query("SELECT * FROM food_categories WHERE id = :id")
    suspend fun getById(id: Long): FoodCategoryEntity?

    @Query("SELECT * FROM food_categories WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getByNormalizedName(normalizedName: String): FoodCategoryEntity?

    @Query("""
        SELECT *
        FROM food_categories
        WHERE normalizedName LIKE '%' || :query || '%'
        ORDER BY
            CASE
                WHEN normalizedName = :query THEN 0
                WHEN normalizedName LIKE :query || '%' THEN 1
                ELSE 2
            END,
            lastUsedAt DESC,
            name ASC
        LIMIT :limit
    """)
    suspend fun searchCategories(
        query: String,
        limit: Int = 8
    ): List<FoodCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: FoodCategoryEntity): Long

    @Update
    suspend fun update(category: FoodCategoryEntity)

    @Query("""
        UPDATE food_categories
        SET lastUsedAt = :usedAt,
            updatedAt = :usedAt
        WHERE id = :categoryId
    """)
    suspend fun markUsed(categoryId: Long, usedAt: Instant)

    @Query("""
        SELECT
            fc.id AS categoryId,
            fc.name AS name,
            fc.imageUri AS imageUri,
            fc.defaultStorageLocation AS storageLocation,
            fc.defaultPerishability AS perishability,
            SUM(el.quantity) AS totalQuantity,
            MIN(el.expirationDate) AS nearestExpirationDate,
            COUNT(el.id) AS lotCount
        FROM food_categories fc
        INNER JOIN expiry_lots el ON el.categoryId = fc.id
        WHERE el.quantity > 0
        GROUP BY fc.id
        ORDER BY nearestExpirationDate ASC, fc.name ASC
    """)
    fun observePantryRowsAll(): Flow<List<PantryRowProjection>>

    @Query("""
        SELECT
            fc.id AS categoryId,
            fc.name AS name,
            fc.imageUri AS imageUri,
            fc.defaultStorageLocation AS storageLocation,
            fc.defaultPerishability AS perishability,
            SUM(el.quantity) AS totalQuantity,
            MIN(el.expirationDate) AS nearestExpirationDate,
            COUNT(el.id) AS lotCount
        FROM food_categories fc
        INNER JOIN expiry_lots el ON el.categoryId = fc.id
        WHERE el.quantity > 0
          AND fc.defaultStorageLocation = :location
        GROUP BY fc.id
        ORDER BY nearestExpirationDate ASC, fc.name ASC
    """)
    fun observePantryRowsByLocation(
        location: StorageLocation
    ): Flow<List<PantryRowProjection>>

    @Query("""
        SELECT
            el.id AS lotId,
            fc.id AS categoryId,
            fc.name AS categoryName,
            fc.normalizedName AS normalizedName,
            fc.defaultStorageLocation AS storageLocation,
            fc.defaultPerishability AS perishability,
            el.expirationDate AS expirationDate,
            el.quantity AS quantity
        FROM expiry_lots el
        INNER JOIN food_categories fc ON fc.id = el.categoryId
        WHERE el.quantity > 0
        ORDER BY el.expirationDate ASC, fc.name ASC
    """)
    suspend fun getActiveLotsWithCategories(): List<LotWithCategoryProjection>

    @Query("""
        SELECT
            el.id AS lotId,
            fc.id AS categoryId,
            fc.name AS categoryName,
            fc.normalizedName AS normalizedName,
            fc.defaultStorageLocation AS storageLocation,
            fc.defaultPerishability AS perishability,
            el.expirationDate AS expirationDate,
            el.quantity AS quantity
        FROM expiry_lots el
        INNER JOIN food_categories fc ON fc.id = el.categoryId
        WHERE el.quantity > 0
          AND fc.id IN (:categoryIds)
        ORDER BY el.expirationDate ASC
    """)
    suspend fun getActiveLotsForCategories(
        categoryIds: List<Long>
    ): List<LotWithCategoryProjection>
}
```

---

### 6.3 Note

Per `StorageLocationFilter.ALL` il repository chiama:

```text
observePantryRowsAll()
```

Per filtri specifici chiama:

```text
observePantryRowsByLocation(location)
```

Il calcolo “in scadenza” con soglie fresh/long life non viene fatto qui, perché dipende da Settings/DataStore.

---

## 7. ExpiryLotDao

### 7.1 Responsabilità

Gestisce:

```text
lotti/scadenze
incremento/decremento
query lotti per categoria
delete lotti a 0
upsert per categoryId + expirationDate
```

---

### 7.2 Interface

```kotlin
@Dao
interface ExpiryLotDao {

    @Query("""
        SELECT *
        FROM expiry_lots
        WHERE categoryId = :categoryId
          AND quantity > 0
        ORDER BY expirationDate ASC
    """)
    fun observeActiveLotsForCategory(
        categoryId: Long
    ): Flow<List<ExpiryLotEntity>>

    @Query("""
        SELECT *
        FROM expiry_lots
        WHERE categoryId = :categoryId
          AND quantity > 0
        ORDER BY expirationDate ASC
    """)
    suspend fun getActiveLotsForCategory(
        categoryId: Long
    ): List<ExpiryLotEntity>

    @Query("""
        SELECT *
        FROM expiry_lots
        WHERE categoryId = :categoryId
          AND expirationDate = :expirationDate
        LIMIT 1
    """)
    suspend fun getLotByCategoryAndDate(
        categoryId: Long,
        expirationDate: LocalDate
    ): ExpiryLotEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(lot: ExpiryLotEntity): Long

    @Update
    suspend fun update(lot: ExpiryLotEntity)

    @Delete
    suspend fun delete(lot: ExpiryLotEntity)

    @Query("DELETE FROM expiry_lots WHERE id = :lotId")
    suspend fun deleteById(lotId: Long)

    @Query("""
        DELETE FROM expiry_lots
        WHERE categoryId = :categoryId
          AND id NOT IN (:activeLotIds)
    """)
    suspend fun deleteLotsNotIn(
        categoryId: Long,
        activeLotIds: List<Long>
    )

    @Query("""
        DELETE FROM expiry_lots
        WHERE categoryId = :categoryId
    """)
    suspend fun deleteAllLotsForCategory(categoryId: Long)

    @Query("""
        UPDATE expiry_lots
        SET quantity = quantity + :delta,
            updatedAt = :updatedAt
        WHERE id = :lotId
    """)
    suspend fun incrementLotQuantity(
        lotId: Long,
        delta: Int,
        updatedAt: Instant
    )

    @Query("""
        UPDATE expiry_lots
        SET quantity = quantity - 1,
            updatedAt = :updatedAt
        WHERE id = :lotId
          AND quantity > 0
    """)
    suspend fun decrementLotByOne(
        lotId: Long,
        updatedAt: Instant
    )

    @Query("""
        DELETE FROM expiry_lots
        WHERE quantity <= 0
    """)
    suspend fun deleteZeroQuantityLots()
}
```

---

### 7.3 Upsert lotto

Room non ha bisogno di un metodo SQL unico per upsert complesso.

Il repository/use case può fare:

```text
getLotByCategoryAndDate()
se esiste → update quantity
se non esiste → insert
```

Questa operazione deve stare dentro una transaction quando fa parte di un salvataggio complesso.

---

## 8. BarcodeProductLinkDao

### 8.1 Responsabilità

Gestisce:

```text
barcode → FoodCategory
lookup barcode locale
link barcode mostrati in Dettaglio Alimento
disattivazione link barcode
```

---

### 8.2 Interface

```kotlin
@Dao
interface BarcodeProductLinkDao {

    @Query("""
        SELECT *
        FROM barcode_product_links
        WHERE barcode = :barcode
          AND isActive = 1
        LIMIT 1
    """)
    suspend fun findActiveByBarcode(
        barcode: String
    ): BarcodeProductLinkEntity?

    @Query("""
        SELECT *
        FROM barcode_product_links
        WHERE categoryId = :categoryId
          AND isActive = 1
        ORDER BY updatedAt DESC
    """)
    fun observeActiveLinksForCategory(
        categoryId: Long
    ): Flow<List<BarcodeProductLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: BarcodeProductLinkEntity)

    @Query("""
        UPDATE barcode_product_links
        SET isActive = 0,
            updatedAt = :updatedAt
        WHERE barcode = :barcode
    """)
    suspend fun deactivateByBarcode(
        barcode: String,
        updatedAt: Instant
    )

    @Query("""
        UPDATE barcode_product_links
        SET isActive = 0,
            updatedAt = :updatedAt
        WHERE categoryId = :categoryId
    """)
    suspend fun deactivateAllForCategory(
        categoryId: Long,
        updatedAt: Instant
    )
}
```

---

### 8.3 Regole

Per barcode link si può usare `isActive = false`.

Motivo:

```text
un barcode scollegato/corretto può essere mantenuto come storico tecnico
evita cancellazioni inutili
```

La UI mostra solo link attivi.

---

## 9. RecipeDao

### 9.1 Responsabilità

Gestisce:

```text
FavoriteRecipeEntity
RecipeIngredientEntity
preferiti
dettaglio ricetta preferita offline
salvataggio/rimozione preferiti
```

---

### 9.2 Interface

```kotlin
@Dao
interface RecipeDao {

    @Query("""
        SELECT *
        FROM favorite_recipes
        WHERE externalId = :externalId
        LIMIT 1
    """)
    suspend fun getFavoriteByExternalId(
        externalId: String
    ): FavoriteRecipeEntity?

    @Query("""
        SELECT
            id,
            externalId,
            title,
            imageUrl,
            preparationTimeMinutes,
            servings
        FROM favorite_recipes
        ORDER BY savedAt DESC
    """)
    fun observeFavoriteRecipeCards(): Flow<List<FavoriteRecipeCardProjection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavoriteRecipe(
        recipe: FavoriteRecipeEntity
    ): Long

    @Query("""
        DELETE FROM favorite_recipes
        WHERE externalId = :externalId
    """)
    suspend fun deleteFavoriteByExternalId(
        externalId: String
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(
        ingredients: List<RecipeIngredientEntity>
    )

    @Query("""
        DELETE FROM recipe_ingredients
        WHERE recipeId = :recipeId
    """)
    suspend fun deleteIngredientsForRecipe(
        recipeId: Long
    )

    @Query("""
        SELECT *
        FROM recipe_ingredients
        WHERE recipeId = :recipeId
        ORDER BY id ASC
    """)
    suspend fun getIngredientsForRecipe(
        recipeId: Long
    ): List<RecipeIngredientEntity>

    @Query("""
        SELECT *
        FROM recipe_ingredients
        WHERE recipeId = :recipeId
        ORDER BY id ASC
    """)
    fun observeIngredientsForRecipe(
        recipeId: Long
    ): Flow<List<RecipeIngredientEntity>>

    @Query("""
        SELECT COUNT(*)
        FROM favorite_recipes
        WHERE externalId = :externalId
    """)
    suspend fun isFavoriteCount(
        externalId: String
    ): Int
}
```

---

### 9.3 Note

`FavoriteRecipeEntity` ha `externalId` unique.

Per salvare una ricetta preferita:

```text
upsert FavoriteRecipe
delete ingredienti precedenti
insert ingredienti aggiornati
```

Tutto in transaction.

---

## 10. RecipeIngredientLinkDao

### 10.1 Responsabilità

Gestisce:

```text
RecipeIngredientLinkEntity
mapping globale ingrediente ricetta → FoodCategory
lookup availability
link mostrati in Dettaglio Alimento
correzione mapping errati
```

---

### 10.2 Interface

```kotlin
@Dao
interface RecipeIngredientLinkDao {

    @Query("""
        SELECT
            ril.id AS id,
            ril.categoryId AS categoryId,
            fc.name AS categoryName,
            ril.aliasOriginal AS aliasOriginal,
            ril.normalizedAlias AS normalizedAlias,
            ril.language AS language,
            ril.externalIngredientId AS externalIngredientId,
            ril.origin AS origin,
            ril.isActive AS isActive
        FROM recipe_ingredient_links ril
        INNER JOIN food_categories fc ON fc.id = ril.categoryId
        WHERE ril.normalizedAlias = :normalizedAlias
          AND ril.isActive = 1
        ORDER BY
            CASE ril.origin
                WHEN 'USER' THEN 0
                WHEN 'SEED' THEN 1
                WHEN 'AUTO' THEN 2
                ELSE 3
            END,
            fc.name ASC
    """)
    suspend fun findActiveLinksByAlias(
        normalizedAlias: String
    ): List<RecipeIngredientLinkProjection>

    @Query("""
        SELECT
            ril.id AS id,
            ril.categoryId AS categoryId,
            fc.name AS categoryName,
            ril.aliasOriginal AS aliasOriginal,
            ril.normalizedAlias AS normalizedAlias,
            ril.language AS language,
            ril.externalIngredientId AS externalIngredientId,
            ril.origin AS origin,
            ril.isActive AS isActive
        FROM recipe_ingredient_links ril
        INNER JOIN food_categories fc ON fc.id = ril.categoryId
        WHERE ril.externalIngredientId = :externalIngredientId
          AND ril.isActive = 1
        ORDER BY
            CASE ril.origin
                WHEN 'USER' THEN 0
                WHEN 'SEED' THEN 1
                WHEN 'AUTO' THEN 2
                ELSE 3
            END,
            fc.name ASC
    """)
    suspend fun findActiveLinksByExternalIngredientId(
        externalIngredientId: String
    ): List<RecipeIngredientLinkProjection>

    @Query("""
        SELECT
            ril.id AS id,
            ril.categoryId AS categoryId,
            fc.name AS categoryName,
            ril.aliasOriginal AS aliasOriginal,
            ril.normalizedAlias AS normalizedAlias,
            ril.language AS language,
            ril.externalIngredientId AS externalIngredientId,
            ril.origin AS origin,
            ril.isActive AS isActive
        FROM recipe_ingredient_links ril
        INNER JOIN food_categories fc ON fc.id = ril.categoryId
        WHERE ril.categoryId = :categoryId
          AND ril.isActive = 1
        ORDER BY ril.aliasOriginal ASC
    """)
    fun observeActiveLinksForCategory(
        categoryId: Long
    ): Flow<List<RecipeIngredientLinkProjection>>

    @Query("""
        SELECT *
        FROM recipe_ingredient_links
        WHERE normalizedAlias = :normalizedAlias
          AND categoryId = :categoryId
        LIMIT 1
    """)
    suspend fun getLinkByAliasAndCategory(
        normalizedAlias: String,
        categoryId: Long
    ): RecipeIngredientLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: RecipeIngredientLinkEntity): Long

    @Query("""
        DELETE FROM recipe_ingredient_links
        WHERE id = :linkId
    """)
    suspend fun deleteById(linkId: Long)

    @Query("""
        DELETE FROM recipe_ingredient_links
        WHERE normalizedAlias = :normalizedAlias
          AND categoryId = :categoryId
    """)
    suspend fun deleteByAliasAndCategory(
        normalizedAlias: String,
        categoryId: Long
    )

    @Query("""
        DELETE FROM recipe_ingredient_links
        WHERE categoryId = :categoryId
    """)
    suspend fun deleteAllForCategory(
        categoryId: Long
    )

    @Query("""
        SELECT *
        FROM recipe_ingredient_links
        WHERE normalizedAlias LIKE '%' || :query || '%'
          AND isActive = 1
        ORDER BY normalizedAlias ASC
        LIMIT :limit
    """)
    suspend fun searchAliases(
        query: String,
        limit: Int = 8
    ): List<RecipeIngredientLinkEntity>
}
```

---

### 10.3 Regole

Per mapping ingrediente errato corretto manualmente:

```text
delete fisico
```

Non usare:

```text
isActive = false
```

per questi casi.

Il campo `isActive` resta utile per seed/auto futuri, ma per MVP le correzioni utente eliminano il link.

---

## 11. Transazioni

Le transazioni possono stare nel database o in un DAO dedicato con metodi `@Transaction`.

Per MVP è accettabile implementare le transazioni nel repository usando:

```kotlin
database.withTransaction {
    ...
}
```

Questa è la scelta consigliata.

Motivo:

```text
la transazione spesso coordina più DAO
il repository ha già accesso ai DAO necessari
il codice resta leggibile
```

---

## 12. Transaction: SaveAddedFood

### 12.1 Operazioni

```text
1. se categoria nuova → insert FoodCategory
2. se categoria esistente → usa id esistente
3. upsert ExpiryLot(categoryId, expirationDate)
4. markUsed FoodCategory
5. se barcode riconosciuto → upsert BarcodeProductLink
```

---

### 12.2 Pseudocodice

```kotlin
database.withTransaction {
    val categoryId = when {
        input.selectedCategoryId != null -> input.selectedCategoryId
        input.pendingNewCategory != null -> {
            val existing = foodCategoryDao.getByNormalizedName(
                input.pendingNewCategory.normalizedName
            )
            existing?.id ?: foodCategoryDao.insert(
                input.pendingNewCategory.toEntity(now)
            )
        }
        else -> error("Missing category")
    }

    val existingLot = expiryLotDao.getLotByCategoryAndDate(
        categoryId = categoryId,
        expirationDate = input.expirationDate
    )

    if (existingLot == null) {
        expiryLotDao.insert(
            ExpiryLotEntity(
                categoryId = categoryId,
                expirationDate = input.expirationDate,
                quantity = input.quantity,
                createdAt = now,
                updatedAt = now
            )
        )
    } else {
        expiryLotDao.update(
            existingLot.copy(
                quantity = existingLot.quantity + input.quantity,
                updatedAt = now
            )
        )
    }

    foodCategoryDao.markUsed(categoryId, now)

    if (input.recognizedBarcodeProduct != null) {
        barcodeProductLinkDao.upsert(
            input.recognizedBarcodeProduct.toBarcodeProductLinkEntity(
                categoryId = categoryId,
                now = now
            )
        )
    }
}
```

---

## 13. Transaction: SaveFoodDetailChanges

### 13.1 Operazioni

```text
1. aggiorna FoodCategory
2. normalizza/fonde draftLots per expirationDate
3. elimina lotti non presenti nello snapshot finale
4. crea/aggiorna lotti con quantity > 0
5. elimina eventuali lotti quantity <= 0
```

---

### 13.2 Regola fusione date

Se nello snapshot finale esistono più draft con la stessa data:

```text
somma le quantity
```

Esempio:

```text
20/07 ×2
20/07 ×1
→ 20/07 ×3
```

---

### 13.3 Pseudocodice

```kotlin
database.withTransaction {
    foodCategoryDao.update(updatedCategory)

    val finalLotsByDate = input.draftLots
        .filter { it.quantity > 0 }
        .groupBy { it.expirationDate }
        .mapValues { (_, lots) -> lots.sumOf { it.quantity } }

    expiryLotDao.deleteAllLotsForCategory(input.categoryId)

    finalLotsByDate.forEach { (date, quantity) ->
        expiryLotDao.insert(
            ExpiryLotEntity(
                categoryId = input.categoryId,
                expirationDate = date,
                quantity = quantity,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
```

Nota:

```text
per MVP è accettabile delete+insert dei lotti del dettaglio alimento.
È semplice e coerente con lo snapshot finale del draft.
```

Attenzione:

```text
non eliminare FoodCategory anche se finalLotsByDate è vuota
```

---

## 14. Transaction: ToggleFavoriteRecipe

### 14.1 Like

```text
1. insert/update FavoriteRecipe
2. delete ingredienti precedenti della ricetta
3. insert RecipeIngredient aggiornati
```

---

### 14.2 Unlike

```text
1. delete FavoriteRecipe by externalId
2. RecipeIngredient eliminati via cascade
```

Non eliminare:

```text
RecipeIngredientLinkEntity
```

---

### 14.3 Pseudocodice

```kotlin
database.withTransaction {
    val favoriteId = recipeDao.upsertFavoriteRecipe(recipeEntity)

    recipeDao.deleteIngredientsForRecipe(favoriteId)

    recipeDao.insertIngredients(
        recipe.ingredients.map { it.toEntity(recipeId = favoriteId) }
    )
}
```

---

## 15. Transaction: LinkRecipeIngredientToFood

### 15.1 Operazioni

```text
1. normalizza alias
2. elimina link rimossi dall’utente
3. crea/aggiorna link selezionati
```

---

### 15.2 Pseudocodice

```kotlin
database.withTransaction {
    input.removedCategoryIds.forEach { categoryId ->
        recipeIngredientLinkDao.deleteByAliasAndCategory(
            normalizedAlias = normalizedAlias,
            categoryId = categoryId
        )
    }

    input.selectedCategoryIds.forEach { categoryId ->
        val existing = recipeIngredientLinkDao.getLinkByAliasAndCategory(
            normalizedAlias = normalizedAlias,
            categoryId = categoryId
        )

        val entity = existing?.copy(
            aliasOriginal = input.aliasOriginal,
            externalIngredientId = input.externalIngredientId,
            origin = LinkOrigin.USER,
            isActive = true,
            updatedAt = now
        ) ?: RecipeIngredientLinkEntity(
            categoryId = categoryId,
            aliasOriginal = input.aliasOriginal,
            normalizedAlias = normalizedAlias,
            externalIngredientId = input.externalIngredientId,
            language = null,
            origin = LinkOrigin.USER,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        recipeIngredientLinkDao.upsert(entity)
    }
}
```

---

## 16. Query per Home overview

### 16.1 Dati necessari

Home overview richiede:

```text
username da DataStore
lotti attivi + categorie da Room
soglie fresh/long life da DataStore
```

DAO usato:

```text
FoodCategoryDao.getActiveLotsWithCategories()
```

---

### 16.2 Calcolo nel UseCase

`GetHomeOverviewUseCase` calcola:

```text
totalPackages = SUM(quantity)
fridgePackages
freezerPackages
pantryPackages
expiringFoods con soglie settings
deduplica per categoryId
moltiplicatore expiringQuantity
nearestExpirationDate
```

Motivo:

```text
le soglie dipendono da SettingsRepository,
quindi non conviene codificarle in SQL
```

---

## 17. Query per Dispensa

### 17.1 Lista generale

DAO:

```text
FoodCategoryDao.observePantryRowsAll()
FoodCategoryDao.observePantryRowsByLocation(location)
```

---

### 17.2 Sezione “In scadenza”

Per coerenza con Home e settings:

```text
il repository/use case parte dai lotti attivi
applica soglie settings
deduplica per alimento
```

Non serve una query SQL specifica con soglie.

---

### 17.3 Decremento rapido

Quando l’utente preme `−` in Dispensa:

```text
se lotCount = 1 → decremento diretto
se lotCount > 1 → naviga Dettaglio Alimento
```

Per decremento diretto:

```text
recupera unico lotto attivo della categoria
decrementa quantity
se quantity arriva a 0 → elimina lotto
```

Pseudocodice:

```kotlin
database.withTransaction {
    val lots = expiryLotDao.getActiveLotsForCategory(categoryId)

    if (lots.size == 1) {
        val lot = lots.first()
        if (lot.quantity <= 1) {
            expiryLotDao.deleteById(lot.id)
        } else {
            expiryLotDao.decrementLotByOne(lot.id, now)
        }
    }
}
```

Questa logica sta in `PantryRepository` o in un piccolo use case interno.

---

## 18. Query per Dettaglio Alimento

### 18.1 Dati necessari

Dettaglio Alimento richiede:

```text
FoodCategory
ExpiryLot attivi
BarcodeProductLink attivi
RecipeIngredientLink attivi verso questa categoria
```

DAO usati:

```text
FoodCategoryDao.getById(categoryId)
ExpiryLotDao.observeActiveLotsForCategory(categoryId)
BarcodeProductLinkDao.observeActiveLinksForCategory(categoryId)
RecipeIngredientLinkDao.observeActiveLinksForCategory(categoryId)
```

---

### 18.2 Aggregazione

L’aggregazione finale in `FoodDetailData` può essere fatta in:

```text
PantryRepository
```

oppure in uno use case dedicato se serve combinare dati da `RecipeRepository`.

Per MVP:

```text
FoodDetailViewModel usa repository/use case che gli restituisce già FoodDetailData
```

Così la UI resta semplice.

---

## 19. Query per barcode

### 19.1 Scan barcode

DAO:

```text
BarcodeProductLinkDao.findActiveByBarcode(barcode)
```

Se trovato:

```text
ResolveBarcodeUseCase ritorna KnownLocalCategory
```

Se non trovato:

```text
FoodRecognitionRepository chiama Open Food Facts
```

---

### 19.2 Link barcode in Dettaglio

DAO:

```text
BarcodeProductLinkDao.observeActiveLinksForCategory(categoryId)
```

---

### 19.3 Rimozione link barcode

DAO:

```text
BarcodeProductLinkDao.deactivateByBarcode(barcode, now)
```

Non delete fisico.

---

## 20. Query per ricette preferite

### 20.1 Lista preferiti

DAO:

```text
RecipeDao.observeFavoriteRecipeCards()
```

---

### 20.2 Dettaglio preferito offline

Passaggi:

```text
1. RecipeDao.getFavoriteByExternalId(externalId)
2. RecipeDao.getIngredientsForRecipe(recipe.id)
3. mapper Entity → RecipeDetail
4. GetRecipeAvailabilityUseCase ricalcola disponibilità
```

---

### 20.3 Like/unlike

Like/unlike devono usare transaction.

Vedi:

```text
Transaction: ToggleFavoriteRecipe
```

---

## 21. Query per availability ricetta

### 21.1 Flusso

Per ogni ingrediente:

```text
1. RecipeIngredientLinkDao.findActiveLinksByExternalIngredientId(externalIngredientId), se presente
2. RecipeIngredientLinkDao.findActiveLinksByAlias(normalizedAlias)
3. unione link senza duplicati
4. FoodCategoryDao.getActiveLotsForCategories(categoryIds)
5. se almeno un lotto attivo → In dispensa
6. altrimenti → Da comprare
```

---

### 21.2 Regola conservativa

Non fare query automatica su `FoodCategory.normalizedName` per decidere availability.

Regola:

```text
availability solo da RecipeIngredientLink persistenti
```

Quindi questo NON basta:

```text
ingrediente: latte
FoodCategory: Latte
```

Serve:

```text
RecipeIngredientLink(latte → Latte)
```

---

## 22. Query per collegamento ingrediente → alimento

### 22.1 Suggerimenti nel bottom sheet

Per suggerire categorie nel bottom sheet:

```text
FoodCategoryDao.searchCategories(query)
RecipeIngredientLinkDao.searchAliases(query)
```

Il ranking finale può stare in:

```text
FoodCategoryMatcher
```

---

### 22.2 Salvataggio mapping

DAO:

```text
RecipeIngredientLinkDao.getLinkByAliasAndCategory()
RecipeIngredientLinkDao.upsert()
RecipeIngredientLinkDao.deleteByAliasAndCategory()
```

Sempre dentro transaction.

---

## 23. Indici necessari

Gli indici sono già stati previsti nel data model, ma qui si confermano perché supportano query reali.

### 23.1 FoodCategoryEntity

Necessario:

```text
Index(normalizedName, unique = true)
```

Supporta:

```text
getByNormalizedName
searchCategories
evitare duplicati categorie
```

---

### 23.2 ExpiryLotEntity

Necessari:

```text
Index(categoryId)
Index(expirationDate)
Index(categoryId, expirationDate, unique = true)
```

Supportano:

```text
lotti per alimento
ordinamento scadenze
upsert lotto stessa categoria+data
home overview
notifiche
```

---

### 23.3 BarcodeProductLinkEntity

Necessari:

```text
Index(barcode, unique = true)
Index(categoryId)
```

Supportano:

```text
lookup barcode scan
link barcode nel dettaglio alimento
```

---

### 23.4 RecipeIngredientLinkEntity

Necessari:

```text
Index(categoryId)
Index(normalizedAlias)
Index(externalIngredientId)
Index(normalizedAlias, categoryId, unique = true)
```

Supportano:

```text
availability ricette
collegamenti alimento
upsert mapping
delete mapping errato
```

---

### 23.5 FavoriteRecipeEntity

Necessario:

```text
Index(externalId, unique = true)
```

Supporta:

```text
like/unlike
dettaglio preferito offline
evitare duplicati preferiti
```

---

### 23.6 RecipeIngredientEntity

Necessari:

```text
Index(recipeId)
Index(normalizedName)
Index(externalIngredientId)
```

Supportano:

```text
dettaglio ricetta preferita offline
availability ricette
```

---

## 24. Delete rules

### 24.1 FoodCategory

`FoodCategory` non viene eliminata nel normale uso MVP.

Regola:

```text
categoria senza lotti resta nel DB
```

Motivo:

```text
autocomplete
barcode link
recipe ingredient link
storico logico
```

Foreign key:

```text
onDelete = RESTRICT
```

---

### 24.2 ExpiryLot

Se quantity arriva a 0:

```text
elimina lotto
```

Non serve soft delete per MVP.

---

### 24.3 BarcodeProductLink

Quando scollegato:

```text
isActive = false
```

Non delete fisico.

---

### 24.4 RecipeIngredientLink

Quando corretto manualmente e considerato errato:

```text
delete fisico
```

Non soft delete.

---

### 24.5 FavoriteRecipe

Quando unlike:

```text
delete FavoriteRecipe
RecipeIngredient eliminati via cascade
RecipeIngredientLink non toccati
```

---

## 25. Repository mapping

### 25.1 PantryRepository usa

```text
FoodCategoryDao
ExpiryLotDao
BarcodeProductLinkDao
PantryPalDatabase.withTransaction
```

---

### 25.2 FoodRecognitionRepository usa

```text
OpenFoodFactsApi
```

Nessun DAO.

---

### 25.3 RecipeRepository usa

```text
RecipeDao
RecipeIngredientLinkDao
FoodCategoryDao, solo se serve join/lookup categorie per availability
SpoonacularApi
PantryPalDatabase.withTransaction
```

---

### 25.4 SettingsRepository usa

```text
DataStore
```

Nessun DAO.

---

### 25.5 NotificationRepository usa

```text
WorkManager
NotificationManager
```

Nessun DAO.

---

## 26. Regole finali sintetiche

```text
DAO separati:
    FoodCategoryDao
    ExpiryLotDao
    BarcodeProductLinkDao
    RecipeDao
    RecipeIngredientLinkDao

Aggregazioni Room:
    SUM(quantity)
    COUNT(lots)
    MIN(expirationDate)
    GROUP BY categoryId

Logica fuori da Room:
    soglie in scadenza
    matching
    availability ricette
    notifiche
    navigazione

Search:
    LIKE semplice, niente FTS

Date:
    LocalDate come String ISO yyyy-MM-dd
    Instant come Long epochMillis

Transazioni:
    database.withTransaction nel repository

FoodCategory:
    mai eliminata nel normale MVP

ExpiryLot:
    quantity 0 → delete

BarcodeProductLink:
    rimozione → isActive=false

RecipeIngredientLink:
    mapping errato corretto → delete fisico
```
