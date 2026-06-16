package com.example.pantrypal.data.product.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsProductResponseDto(
    val code: String? = null,
    val status: Int? = null,
    @SerialName("status_verbose")
    val statusVerbose: String? = null,
    val product: OpenFoodFactsProductDto? = null
)

@Serializable
data class OpenFoodFactsProductDto(
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("generic_name")
    val genericName: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("categories_tags")
    val categoriesTags: List<String>? = null,
    @SerialName("food_groups_tags")
    val foodGroupTags: List<String>? = null
)
