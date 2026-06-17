package com.example.pantrypal.core.designsystem

import com.example.pantrypal.domain.model.AppTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryThemeTest {
    @Test
    fun lightDarkAndSystemResolveExpectedThemeMode() {
        assertFalse(resolveDarkTheme(AppTheme.LIGHT, systemDark = true))
        assertTrue(resolveDarkTheme(AppTheme.DARK, systemDark = false))
        assertTrue(resolveDarkTheme(AppTheme.SYSTEM, systemDark = true))
        assertFalse(resolveDarkTheme(AppTheme.SYSTEM, systemDark = false))
    }
}
