package com.example.pantrypal.data.pantry

import com.example.pantrypal.core.database.entity.BarcodeProductLinkEntity
import com.example.pantrypal.core.database.entity.ExpiryLotEntity
import com.example.pantrypal.core.database.entity.FoodCategoryEntity
import com.example.pantrypal.core.database.projection.LotWithCategoryProjection
import com.example.pantrypal.core.database.projection.PantryRowProjection
import com.example.pantrypal.core.database.projection.RecipeIngredientLinkProjection
import com.example.pantrypal.domain.model.BarcodeProductLink
import com.example.pantrypal.domain.model.ExpiryLot
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.RecipeIngredientLink

fun FoodCategoryEntity.toDomain(): FoodCategory =
    FoodCategory(
        id = id,
        name = name,
        normalizedName = normalizedName,
        defaultStorageLocation = defaultStorageLocation,
        defaultPerishability = defaultPerishability,
        imageUri = imageUri,
        origin = origin,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastUsedAt = lastUsedAt
    )

fun ExpiryLotEntity.toDomain(): ExpiryLot =
    ExpiryLot(
        id = id,
        categoryId = categoryId,
        expirationDate = expirationDate,
        quantity = quantity
    )

fun BarcodeProductLinkEntity.toDomain(): BarcodeProductLink =
    BarcodeProductLink(
        barcode = barcode,
        categoryId = categoryId,
        productName = productName,
        genericName = genericName,
        brand = brand,
        quantityLabel = quantityLabel,
        imageUrl = imageUrl,
        rawCategoryTags = rawCategoryTags,
        rawFoodGroupTags = rawFoodGroupTags,
        origin = origin,
        isActive = isActive
    )

fun RecipeIngredientLinkProjection.toDomain(): RecipeIngredientLink =
    RecipeIngredientLink(
        id = id,
        categoryId = categoryId,
        categoryName = categoryName,
        aliasOriginal = aliasOriginal,
        normalizedAlias = normalizedAlias,
        language = language,
        externalIngredientId = externalIngredientId,
        relationType = relationType,
        origin = origin,
        isActive = isActive
    )

fun PantryRowProjection.toDomain(): PantryRow =
    PantryRow(
        categoryId = categoryId,
        name = name,
        imageUri = imageUri,
        storageLocation = storageLocation,
        perishability = perishability,
        totalQuantity = totalQuantity,
        nearestExpirationDate = nearestExpirationDate,
        lotCount = lotCount
    )

fun LotWithCategoryProjection.toDomain(): LotWithCategory =
    LotWithCategory(
        lotId = lotId,
        categoryId = categoryId,
        categoryName = categoryName,
        normalizedName = normalizedName,
        storageLocation = storageLocation,
        perishability = perishability,
        expirationDate = expirationDate,
        quantity = quantity
    )
