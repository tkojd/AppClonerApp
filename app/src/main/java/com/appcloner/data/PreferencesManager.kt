package com.appcloner.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for user preferences backed by [SharedPreferences].
 *
 * Both the Settings screen (which writes) and the app-list screen (which reads and reacts)
 * go through this helper, so the "show system apps" toggle has one owner instead of two
 * independent controls that can disagree.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Current value of the "show system apps" preference. */
    var showSystemApps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, value).apply()
        }

    /**
     * Emits the current "show system apps" value immediately and again whenever it changes,
     * so observers (e.g. the app list) stay in sync with the Settings toggle.
     */
    fun showSystemAppsFlow(): Flow<Boolean> = callbackFlow {
        trySendBlocking(showSystemApps)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SHOW_SYSTEM_APPS) {
                trySendBlocking(showSystemApps)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        const val PREFS_NAME = "app_cloner_prefs"
        const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
    }
}
