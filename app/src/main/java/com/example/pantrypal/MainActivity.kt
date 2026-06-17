package com.example.pantrypal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.pantrypal.core.database.seed.DatabaseSeeder
import com.example.pantrypal.core.designsystem.PantryPalTheme
import com.example.pantrypal.core.navigation.PantryPalRoot
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.UserSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var databaseSeeder: DatabaseSeeder
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { databaseSeeder.seedIfNeeded() }
        setContent {
            val settings = settingsRepository.observeSettings()
                .collectAsStateWithLifecycle(initialValue = UserSettings())
                .value
            PantryPalTheme(appTheme = settings.theme) {
                PantryPalRoot()
            }
        }
    }
}
