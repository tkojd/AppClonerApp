package com.appcloner.ui.applist

import com.appcloner.data.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure filtering logic in [AppListUiState.filteredApps].
 *
 * These avoid the Android runtime entirely: [AppInfo] is a plain data class and the
 * filtering derivation is pure Kotlin, so the tests run against the android.jar stubs.
 */
class AppListUiStateTest {

    private fun app(name: String, pkg: String) = AppInfo(
        packageName = pkg,
        appName = name,
        versionName = "1.0",
        versionCode = 1L,
        isSystemApp = false
    )

    private val apps = listOf(
        app("WhatsApp", "com.whatsapp"),
        app("Telegram", "org.telegram.messenger"),
        app("Signal", "org.thoughtcrime.securesms")
    )

    @Test
    fun filteredApps_returnsAllWhenQueryBlank() {
        val state = AppListUiState(apps = apps, searchQuery = "")
        assertEquals(3, state.filteredApps.size)
    }

    @Test
    fun filteredApps_matchesByAppNameCaseInsensitive() {
        val state = AppListUiState(apps = apps, searchQuery = "whats")
        assertEquals(1, state.filteredApps.size)
        assertEquals("WhatsApp", state.filteredApps.first().appName)
    }

    @Test
    fun filteredApps_matchesByPackageName() {
        val state = AppListUiState(apps = apps, searchQuery = "telegram")
        assertEquals(1, state.filteredApps.size)
        assertEquals("Telegram", state.filteredApps.first().appName)
    }

    @Test
    fun filteredApps_trimsWhitespaceInQuery() {
        val state = AppListUiState(apps = apps, searchQuery = "  signal  ")
        assertEquals(1, state.filteredApps.size)
    }

    @Test
    fun filteredApps_returnsEmptyWhenNoMatch() {
        val state = AppListUiState(apps = apps, searchQuery = "nonexistent")
        assertTrue(state.filteredApps.isEmpty())
    }
}
