package com.appcloner.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.appcloner.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that queries the device [PackageManager] for installed applications
 * and maps them into the [AppInfo] domain model.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager get() = context.packageManager

    /**
     * Streams the list of installed applications.
     *
     * System apps are excluded unless [includeSystemApps] is true. The query runs
     * on [Dispatchers.IO] since it touches the package database and disk.
     */
    fun getInstalledApps(includeSystemApps: Boolean): Flow<List<AppInfo>> = flow {
        val installed = queryInstalledApplications()
        val apps = installed
            .filter { shouldInclude(it.isSystemApp(), includeSystemApps) }
            .map { mapToAppInfo(it) }
            .sortedBy { it.appName.lowercase() }
        emit(apps)
    }.flowOn(Dispatchers.IO)

    /** Returns the [AppInfo] for a single package, or null if it is not installed. */
    suspend fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val appInfo = getApplicationInfoCompat(packageName)
            mapToAppInfo(appInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** Loads the launcher icon for a package, or null if unavailable. */
    fun loadIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** Maps a framework [ApplicationInfo] into our domain [AppInfo]. */
    internal fun mapToAppInfo(appInfo: ApplicationInfo): AppInfo {
        val label = packageManager.getApplicationLabel(appInfo).toString()
        val (versionName, versionCode) = readVersion(appInfo.packageName)
        val isSystem = appInfo.isSystemApp()
        return AppInfo(
            packageName = appInfo.packageName,
            appName = label,
            versionName = versionName,
            versionCode = versionCode,
            isSystemApp = isSystem,
            // A user-installed app with a launch intent is a sensible cloning candidate.
            isCloneable = packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
        )
    }

    private fun readVersion(packageName: String): Pair<String?, Long> {
        return try {
            val info = getPackageInfoCompat(packageName)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            info.versionName to code
        } catch (e: PackageManager.NameNotFoundException) {
            null to 0L
        }
    }

    private fun queryInstalledApplications(): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
    }

    private fun getApplicationInfoCompat(packageName: String): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    }

    private fun getPackageInfoCompat(packageName: String): android.content.pm.PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }

    companion object {
        /** Pure predicate: whether an app should be included given the system-app filter. */
        fun shouldInclude(isSystemApp: Boolean, includeSystemApps: Boolean): Boolean {
            return includeSystemApps || !isSystemApp
        }

        /** Pure helper: whether the given [ApplicationInfo.flags] bitmask marks a system app. */
        fun isSystemFlagSet(flags: Int): Boolean {
            return (flags and ApplicationInfo.FLAG_SYSTEM) != 0
        }
    }
}

/** True if this [ApplicationInfo] represents a system application. */
internal fun ApplicationInfo.isSystemApp(): Boolean {
    return AppRepository.isSystemFlagSet(flags)
}
