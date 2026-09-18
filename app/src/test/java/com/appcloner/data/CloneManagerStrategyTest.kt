package com.appcloner.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [CloneManager.decideStrategy], the pure strategy-selection logic.
 *
 * The full [CloneManager] depends on [android.content.pm.PackageManager], which throws
 * "Stub!" under the android.jar test harness. The branch logic was therefore extracted into
 * the pure [CloneManager.decideStrategy] function so it can be tested directly from two
 * boolean capability inputs.
 */
class CloneManagerStrategyTest {

    @Test
    fun decideStrategy_prefersManagedProfileWhenSupported() {
        assertEquals(
            CloneStrategy.MANAGED_PROFILE,
            CloneManager.decideStrategy(
                managedProfileSupported = true,
                multiUserSupported = true
            )
        )
    }

    @Test
    fun decideStrategy_managedProfileWinsEvenWithoutMultiUser() {
        assertEquals(
            CloneStrategy.MANAGED_PROFILE,
            CloneManager.decideStrategy(
                managedProfileSupported = true,
                multiUserSupported = false
            )
        )
    }

    @Test
    fun decideStrategy_fallsBackToMultiUserWhenNoManagedProfile() {
        assertEquals(
            CloneStrategy.MULTI_USER,
            CloneManager.decideStrategy(
                managedProfileSupported = false,
                multiUserSupported = true
            )
        )
    }

    @Test
    fun decideStrategy_fallbackWhenNoCapabilities() {
        assertEquals(
            CloneStrategy.FALLBACK,
            CloneManager.decideStrategy(
                managedProfileSupported = false,
                multiUserSupported = false
            )
        )
    }
}
