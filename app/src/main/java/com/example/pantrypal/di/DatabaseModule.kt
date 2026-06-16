package com.example.pantrypal.di

import android.content.Context
import androidx.room.Room
import com.example.pantrypal.core.database.PantryPalDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PantryPalDatabase =
        Room.databaseBuilder(
            context,
            PantryPalDatabase::class.java,
            "pantrypal.db"
        ).build()

    @Provides
    fun provideFoodCategoryDao(database: PantryPalDatabase) = database.foodCategoryDao()

    @Provides
    fun provideExpiryLotDao(database: PantryPalDatabase) = database.expiryLotDao()

    @Provides
    fun provideBarcodeProductLinkDao(database: PantryPalDatabase) = database.barcodeProductLinkDao()

    @Provides
    fun provideRecipeDao(database: PantryPalDatabase) = database.recipeDao()

    @Provides
    fun provideRecipeIngredientLinkDao(database: PantryPalDatabase) = database.recipeIngredientLinkDao()
}
