package com.appcloner.util

import android.content.Context
import android.os.Build

/**
 * Small utility around package-visibility capabilities.
 *
 * ## Why there is no runtime permission request here
 * The app declares [android.Manifest.permission.QUERY_ALL_PACKAGES] in the manifest.
 * On Android 11+ (API 30) this is an **install-time** permission, not a dangerous
 * runtime permission, so it is granted at install and there is never a runtime prompt
 * to show. Below API 30 the full package list is available without any permission at
 * all. Consequently this app has no dangerous runtime permissions to request, and this
 * helper deliberately exposes only capability checks rather than a permission-request
 * flow that Android would ignore.
 */
object PermissionHelper {

    /**
     * Whether the app can enumerate installed packages. This is effectively always true
     * given the manifest declaration, but the check is centralised here so callers do not
     * scatter version logic. Returns true on all supported API levels.
     */
    fun canQueryAllPackages(context: Context): Boolean {
        // Pre-API 30: unrestricted package visibility.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        // API 30+: QUERY_ALL_PACKAGES is install-time and declared in the manifest.
        return context.packageManager != null
    }

    /**
     * Documented no-op place-holder for future dangerous permissions. Returns true because
     * the current feature set requires no runtime permission grants.
     */
    fun hasRequiredRuntimePermissions(@Suppress("UNUSED_PARAMETER") context: Context): Boolean = true

    /** The install-time permission this app relies on for package visibility. */
    const val QUERY_ALL_PACKAGES: String = "android.permission.QUERY_ALL_PACKAGES"
}
