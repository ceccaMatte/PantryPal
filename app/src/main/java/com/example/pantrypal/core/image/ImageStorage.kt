package com.example.pantrypal.core.image

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

interface ImageStorage {
    suspend fun saveCategoryImageFromUrl(categoryId: Long, imageUrl: String): String?
    suspend fun saveRecipeImageFromUrl(externalRecipeId: String, imageUrl: String): String?
    fun getRecipeLocalImageUri(externalRecipeId: String): String?
    fun deleteRecipeImage(externalRecipeId: String): Boolean
}

object NoOpImageStorage : ImageStorage {
    override suspend fun saveCategoryImageFromUrl(categoryId: Long, imageUrl: String): String? = null
    override suspend fun saveRecipeImageFromUrl(externalRecipeId: String, imageUrl: String): String? = null
    override fun getRecipeLocalImageUri(externalRecipeId: String): String? = null
    override fun deleteRecipeImage(externalRecipeId: String): Boolean = true
}

@Singleton
class FileImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) : ImageStorage {
    override suspend fun saveCategoryImageFromUrl(categoryId: Long, imageUrl: String): String? =
        saveFromUrl(imageUrl, categoryFile(categoryId))

    override suspend fun saveRecipeImageFromUrl(externalRecipeId: String, imageUrl: String): String? =
        saveFromUrl(imageUrl, recipeFile(externalRecipeId))

    override fun getRecipeLocalImageUri(externalRecipeId: String): String? =
        recipeFile(externalRecipeId).takeIf { it.exists() }?.toUriString()

    override fun deleteRecipeImage(externalRecipeId: String): Boolean =
        runCatching {
            val file = recipeFile(externalRecipeId)
            !file.exists() || file.delete()
        }.getOrDefault(false)

    private suspend fun saveFromUrl(imageUrl: String, target: File): String? =
        withContext(Dispatchers.IO) {
            if (imageUrl.isBlank()) return@withContext null
            if (target.exists()) return@withContext target.toUriString()
            runCatching {
                target.parentFile?.mkdirs()
                val request = Request.Builder().url(imageUrl).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val body = response.body ?: return@runCatching null
                    val temp = File(target.parentFile, "${target.name}.tmp")
                    body.byteStream().use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (target.exists()) temp.delete() else temp.renameTo(target)
                    target.takeIf { it.exists() }?.toUriString()
                }
            }.onFailure {
                Log.d("ImageStorage", "Image download failed: ${it.message}")
            }.getOrNull()
        }

    private fun categoryFile(categoryId: Long): File =
        File(imagesDir("categories"), "category_$categoryId.jpg")

    private fun recipeFile(externalRecipeId: String): File =
        File(imagesDir("recipes"), "recipe_${externalRecipeId.safeFileName()}.jpg")

    private fun imagesDir(child: String): File =
        File(context.filesDir, "pantrypal_images/$child")

    private fun File.toUriString(): String = toURI().toString()

    private fun String.safeFileName(): String =
        replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "unknown" }
}
