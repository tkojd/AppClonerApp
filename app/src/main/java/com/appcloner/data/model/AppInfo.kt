package com.appcloner.data.model

import android.graphics.drawable.Drawable

/**
 * Lightweight domain model describing an installed application.
 *
 * This is intentionally NOT a Room entity: the list of installed apps is queried
 * live from [android.content.pm.PackageManager] rather than persisted. The [icon]
 * is a transient, UI-only convenience field and is never stored in the database.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val isCloneable: Boolean = true,
    /** Transient icon for UI display only. Loaded lazily via PackageManager; not persisted. */
    val icon: Drawable? = null
)
