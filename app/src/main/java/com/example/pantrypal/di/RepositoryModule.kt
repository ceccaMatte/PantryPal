package com.example.pantrypal.di

import com.example.pantrypal.data.notification.NotificationRepository
import com.example.pantrypal.data.notification.NotificationRepositoryImpl
import com.example.pantrypal.data.notification.NotificationScheduler
import com.example.pantrypal.data.notification.NotificationSchedulerImpl
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.pantry.PantryRepositoryImpl
import com.example.pantrypal.data.product.FoodRecognitionRepository
import com.example.pantrypal.data.product.FoodRecognitionRepositoryImpl
import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.data.recipe.RecipeRepositoryImpl
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.data.settings.SettingsRepositoryImpl
import com.example.pantrypal.core.util.DateProvider
import com.example.pantrypal.core.image.FileImageStorage
import com.example.pantrypal.core.image.ImageStorage
import com.example.pantrypal.core.util.SystemDateProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPantryRepository(impl: PantryRepositoryImpl): PantryRepository

    @Binds
    @Singleton
    abstract fun bindFoodRecognitionRepository(impl: FoodRecognitionRepositoryImpl): FoodRecognitionRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationScheduler(impl: NotificationSchedulerImpl): NotificationScheduler

    @Binds
    @Singleton
    abstract fun bindDateProvider(impl: SystemDateProvider): DateProvider

    @Binds
    @Singleton
    abstract fun bindImageStorage(impl: FileImageStorage): ImageStorage
}
