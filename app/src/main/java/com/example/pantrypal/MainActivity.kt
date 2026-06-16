package com.example.pantrypal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.pantrypal.core.database.seed.DatabaseSeeder
import com.example.pantrypal.core.designsystem.PantryPalTheme
import com.example.pantrypal.core.navigation.PantryPalRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var databaseSeeder: DatabaseSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { databaseSeeder.seedIfNeeded() }
        setContent {
            PantryPalTheme {
                PantryPalRoot()
            }
        }
    }
}
