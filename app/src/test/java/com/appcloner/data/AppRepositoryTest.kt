package com.appcloner.data

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AppRepository]'s pure filtering/predicate logic.
 *
 * The full [android.content.pm.PackageManager] flow requires an Android runtime, so we
 * exercise the extracted pure helpers: [AppRepository.shouldInclude] and the
 * [isSystemApp] flag detection.
 */
class AppRepositoryTest {

    @Test
    fun shouldInclude_excludesSystemAppsByDefault() {
        assertFalse(AppRepository.shouldInclude(isSystemApp = true, includeSystemApps = false))
    }

    @Test
    fun shouldInclude_keepsUserAppsByDefault() {
        assertTrue(AppRepository.shouldInclude(isSystemApp = false, includeSystemApps = false))
    }

    @Test
    fun shouldInclude_keepsSystemAppsWhenRequested() {
        assertTrue(AppRepository.shouldInclude(isSystemApp = true, includeSystemApps = true))
    }

    @Test
    fun shouldInclude_keepsUserAppsWhenSystemAppsRequested() {
        assertTrue(AppRepository.shouldInclude(isSystemApp = false, includeSystemApps = true))
    }

    @Test
    fun isSystemFlagSet_detectsSystemFlag() {
        assertTrue(AppRepository.isSystemFlagSet(ApplicationInfo.FLAG_SYSTEM))
    }

    @Test
    fun isSystemFlagSet_falseWhenSystemFlagAbsent() {
        assertFalse(AppRepository.isSystemFlagSet(0))
    }

    @Test
    fun isSystemFlagSet_detectsSystemFlagAmongOtherFlags() {
        val flags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_INSTALLED
        assertTrue(AppRepository.isSystemFlagSet(flags))
    }
}
