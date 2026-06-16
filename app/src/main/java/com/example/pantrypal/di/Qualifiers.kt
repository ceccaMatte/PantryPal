package com.example.pantrypal.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenFoodFactsRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SpoonacularRetrofit
