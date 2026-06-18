package com.example.pantrypal.data.product.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OpenFoodFactsProductResponseDto(
    val code: String? = null,
    // v3.6 returns "success"/"failure" (String); v2 returned 1/0 (Int)
    // JsonElement accepts both without throwing SerializationException
    val status: JsonElement? = null,
    @SerialName("status_verbose")
    val statusVerbose: String? = null,
    val product: OpenFoodFactsProductDto? = null
)

@Serializable
data class OpenFoodFactsProductDto(
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("product_name_it")
    val productNameIt: String? = null,
    @SerialName("product_name_en")
    val productNameEn: String? = null,
    @SerialName("generic_name")
    val genericName: String? = null,
    @SerialName("generic_name_it")
    val genericNameIt: String? = null,
    @SerialName("generic_name_en")
    val genericNameEn: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerialName("image_front_url")
    val imageFrontUrl: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("categories_tags")
    val categoriesTags: List<String>? = null,
    @SerialName("food_groups_tags")
    val foodGroupTags: List<String>? = null
)
