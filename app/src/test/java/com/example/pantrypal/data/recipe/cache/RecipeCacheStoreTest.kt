package com.example.pantrypal.data.recipe.cache

import com.example.pantrypal.core.database.dao.ApiCacheDao
import com.example.pantrypal.core.database.entity.ApiCacheEntryEntity
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.domain.model.ApiCacheType
import com.example.pantrypal.domain.model.RecipeCard
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeCacheStoreTest {
    private val dao = FakeApiCacheDao()
    private val store = RecipeCacheStore(
        apiCacheDao = dao,
        json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
        textNormalizer = TextNormalizer()
    )

    @Test
    fun byIngredientsKeyIsStableAcrossCaseAndOrder() {
        assertEquals(
            store.byIngredientsKey(listOf("Pasta", "Latte")),
            store.byIngredientsKey(listOf("latte", "pasta", "LATTE"))
        )
    }

    @Test
    fun cacheRoundTripUsesInternalPayloads() = runBlocking {
        val recipes = listOf(
            RecipeCard(
                externalId = "mock-pasta",
                title = "Pasta al Pomodoro",
                imageUrl = null,
                preparationTimeMinutes = 20
            )
        )

        store.putSearch("Pasta", recipes)

        assertEquals(recipes, store.getSearch("pasta"))
        assertEquals(ApiCacheType.RECIPE_SEARCH, dao.entries.values.single().type)
    }

    @Test
    fun expiredCacheEntryReturnsNull() = runBlocking {
        dao.entries[store.searchKey("pasta")] = ApiCacheEntryEntity(
            cacheKey = store.searchKey("pasta"),
            type = ApiCacheType.RECIPE_SEARCH,
            payloadJson = """{"recipes":[]}""",
            createdAt = Instant.EPOCH,
            expiresAt = Instant.EPOCH
        )

        assertNull(store.getSearch("pasta"))
    }
}

private class FakeApiCacheDao : ApiCacheDao {
    val entries = mutableMapOf<String, ApiCacheEntryEntity>()

    override suspend fun getByKey(cacheKey: String): ApiCacheEntryEntity? = entries[cacheKey]

    override suspend fun upsert(entry: ApiCacheEntryEntity) {
        entries[entry.cacheKey] = entry
    }

    override suspend fun deleteExpired(now: Instant) {
        entries.values.removeAll { it.expiresAt.isBefore(now) }
    }
}
