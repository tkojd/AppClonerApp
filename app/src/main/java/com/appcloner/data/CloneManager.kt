package com.appcloner.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.appcloner.data.model.CloneInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Available strategies for isolating a cloned application on this device.
 */
enum class CloneStrategy {
    /** Device supports managed profiles (work profile) - strongest isolation available to apps. */
    MANAGED_PROFILE,

    /** Device exposes multi-user support that can host a separate app instance. */
    MULTI_USER,

    /**
     * Fallback: no isolation primitive is available to a normal (non-owner) app, so we
     * launch the original app and track the clone as a logical entry only.
     */
    FALLBACK
}

/**
 * Encapsulates the clone lifecycle and the honest reality of app cloning on Android.
 *
 * ## What is actually achievable
 * True, fully isolated app instances (separate data dirs, separate accounts) on a
 * non-rooted device require **elevated privileges** that a normal app cannot grant itself:
 *
 * - **Device Owner / Profile Owner** provisioning via [android.app.admin.DevicePolicyManager].
 *   Only then can an app create managed profiles and install apps into them. This must be
 *   set up during device provisioning or via ADB/enterprise enrollment; a regular sideloaded
 *   app cannot self-promote to owner.
 * - **OEM-level multi-user / "dual apps"** features, which are vendor specific and not part
 *   of the public SDK.
 *
 * Because of this, [CloneManager] detects the best available [CloneStrategy] and degrades
 * gracefully. Where no isolation primitive is reachable, it still records the clone as a
 * logical entry and launches the source app, documenting the limitation rather than
 * pretending isolation exists.
 */
@Singleton
class CloneManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager get() = context.packageManager

    /**
     * Whether the device advertises managed-profile support. Requires API 21+ and the
     * [PackageManager.FEATURE_MANAGED_USERS] system feature.
     *
     * Note: this reports device *capability*, not whether this app holds the owner
     * privileges required to actually provision a profile.
     */
    fun isManagedProfileSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)
    }

    /**
     * Determines the best clone strategy for this device based on its capabilities.
     */
    fun getCloneStrategy(): CloneStrategy =
        decideStrategy(
            managedProfileSupported = isManagedProfileSupported(),
            multiUserSupported = isMultiUserSupported()
        )

    /**
     * Launches the clone.
     *
     * For [CloneStrategy.FALLBACK] this launches the original source application, which is
     * the only action a non-privileged app can safely perform. True isolation would require
     * owner privileges as documented on this class.
     *
     * @return [Result.success] when a launch intent was resolved and started, otherwise
     * [Result.failure] with a descriptive message.
     */
    suspend fun launchClone(clone: CloneInfo): Result<Unit> {
        val intent = getSourceLaunchIntent(clone.sourcePackageName)
            ?: return Result.failure(
                IllegalStateException(
                    "No launch intent found for package ${clone.sourcePackageName}; " +
                        "the source app may be uninstalled or non-launchable."
                )
            )
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Returns the launch [Intent] for a package, or null if none is registered. */
    fun getSourceLaunchIntent(packageName: String): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }

    /**
     * Best-effort detection of multi-user support. This is a device capability check only;
     * hosting an app in another user still requires privileges a normal app lacks.
     */
    private fun isMultiUserSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            packageManager.hasSystemFeature("android.software.managed_users")
    }

    companion object {
        /**
         * Pure strategy-selection logic, split out from the [PackageManager]-dependent
         * capability checks so it can be unit-tested without an Android runtime.
         *
         * Preference order: managed profile > multi-user > fallback. Note that even when a
         * non-fallback strategy is selected, actually provisioning an isolated instance
         * requires Device/Profile Owner privileges that a normal sideloaded app cannot hold
         * (see the class KDoc); the strategy therefore only reports the best *reachable*
         * option, and the current implementation always relaunches the source app.
         */
        fun decideStrategy(
            managedProfileSupported: Boolean,
            multiUserSupported: Boolean
        ): CloneStrategy = when {
            managedProfileSupported -> CloneStrategy.MANAGED_PROFILE
            multiUserSupported -> CloneStrategy.MULTI_USER
            else -> CloneStrategy.FALLBACK
        }
    }
}
