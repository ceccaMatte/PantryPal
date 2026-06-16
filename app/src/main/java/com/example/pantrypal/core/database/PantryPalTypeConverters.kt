package com.example.pantrypal.core.database

import androidx.room.TypeConverter
import com.example.pantrypal.domain.model.CategoryOrigin
import com.example.pantrypal.domain.model.IngredientRelationType
import com.example.pantrypal.domain.model.LinkOrigin
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import java.time.Instant
import java.time.LocalDate

class PantryPalTypeConverters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun storageLocationToString(value: StorageLocation?): String? = value?.name

    @TypeConverter
    fun stringToStorageLocation(value: String?): StorageLocation? = value?.let(StorageLocation::valueOf)

    @TypeConverter
    fun perishabilityToString(value: PerishabilityType?): String? = value?.name

    @TypeConverter
    fun stringToPerishability(value: String?): PerishabilityType? = value?.let(PerishabilityType::valueOf)

    @TypeConverter
    fun categoryOriginToString(value: CategoryOrigin?): String? = value?.name

    @TypeConverter
    fun stringToCategoryOrigin(value: String?): CategoryOrigin? = value?.let(CategoryOrigin::valueOf)

    @TypeConverter
    fun linkOriginToString(value: LinkOrigin?): String? = value?.name

    @TypeConverter
    fun stringToLinkOrigin(value: String?): LinkOrigin? = value?.let(LinkOrigin::valueOf)

    @TypeConverter
    fun relationTypeToString(value: IngredientRelationType?): String? = value?.name

    @TypeConverter
    fun stringToRelationType(value: String?): IngredientRelationType? = value?.let(IngredientRelationType::valueOf)
}
