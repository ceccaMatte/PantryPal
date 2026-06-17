package com.example.pantrypal.data.recipe.cache

import com.example.pantrypal.core.database.dao.ApiCacheDao
import com.example.pantrypal.core.database.entity.ApiCacheEntryEntity
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.domain.model.ApiCacheType
import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetail
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Singleton
class RecipeCacheStore @Inject constructor(
    private val apiCacheDao: ApiCacheDao,
    private val json: Json,
    private val textNormalizer: TextNormalizer
) {
    suspend fun getSearch(query: String): List<RecipeCard>? {
        val entry = apiCacheDao.getByKey(searchKey(query)).takeIfValid() ?: return null
        return try {
            json.decodeFromString<CachedRecipeSearchPayload>(entry.payloadJson).recipes.map { it.toDomain() }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    suspend fun putSearch(query: String, recipes: List<RecipeCard>) {
        upsert(
            cacheKey = searchKey(query),
            type = ApiCacheType.RECIPE_SEARCH,
            ttl = Duration.ofHours(24),
            payloadJson = json.encodeToString(
                CachedRecipeSearchPayload.serializer(),
                CachedRecipeSearchPayload(recipes.map { it.toCachePayload() })
            )
        )
    }

    suspend fun getByIngredients(ingredients: List<String>): List<RecipeCard>? {
        val entry = apiCacheDao.getByKey(byIngredientsKey(ingredients)).takeIfValid() ?: return null
        return try {
            json.decodeFromString<CachedRecipeByIngredientsPayload>(entry.payloadJson).recipes.map { it.toDomain() }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    suspend fun putByIngredients(ingredients: List<String>, recipes: List<RecipeCard>) {
        upsert(
            cacheKey = byIngredientsKey(ingredients),
            type = ApiCacheType.RECIPE_BY_INGREDIENTS,
            ttl = Duration.ofHours(24),
            payloadJson = json.encodeToString(
                CachedRecipeByIngredientsPayload.serializer(),
                CachedRecipeByIngredientsPayload(recipes.map { it.toCachePayload() })
            )
        )
    }

    suspend fun getDetail(externalId: String): RecipeDetail? {
        val entry = apiCacheDao.getByKey(detailKey(externalId)).takeIfValid() ?: return null
        return try {
            json.decodeFromString<CachedRecipeDetailPayload>(entry.payloadJson).toDomain()
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    suspend fun putDetail(recipe: RecipeDetail) {
        upsert(
            cacheKey = detailKey(recipe.externalId),
            type = ApiCacheType.RECIPE_DETAIL,
            ttl = Duration.ofDays(7),
            payloadJson = json.encodeToString(
                CachedRecipeDetailPayload.serializer(),
                recipe.toCachePayload()
            )
        )
    }

    suspend fun deleteExpired() {
        apiCacheDao.deleteExpired(Clock.systemUTC().instant())
    }

    fun searchKey(query: String): String =
        "recipe_search:${textNormalizer.normalizeFoodText(query)}:number=10"

    fun byIngredientsKey(ingredients: List<String>): String =
        "recipe_by_ingredients:${ingredients.normalizedIngredientKey()}:number=5"

    fun detailKey(externalId: String): String = "recipe_detail:${externalId.trim()}"

    private suspend fun upsert(cacheKey: String, type: ApiCacheType, ttl: Duration, payloadJson: String) {
        val now = Clock.systemUTC().instant()
        apiCacheDao.upsert(
            ApiCacheEntryEntity(
                cacheKey = cacheKey,
                type = type,
                payloadJson = payloadJson,
                createdAt = now,
                expiresAt = now.plus(ttl)
            )
        )
    }

    private fun ApiCacheEntryEntity?.takeIfValid(now: Instant = Clock.systemUTC().instant()): ApiCacheEntryEntity? =
        this?.takeIf { it.expiresAt.isAfter(now) }

    private fun List<String>.normalizedIngredientKey(): String =
        map { textNormalizer.normalizeFoodText(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .joinToString(",")
}
