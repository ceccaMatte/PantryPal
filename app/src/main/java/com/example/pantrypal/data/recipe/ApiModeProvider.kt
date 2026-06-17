package com.example.pantrypal.data.recipe

import com.example.pantrypal.BuildConfig
import com.example.pantrypal.domain.model.PantryPalApiMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiModeProvider @Inject constructor() {
    val mode: PantryPalApiMode = PantryPalApiMode.fromRaw(BuildConfig.PANTRYPAL_API_MODE)
}
