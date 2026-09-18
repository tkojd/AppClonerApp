package com.appcloner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted configuration for a single clone of a source application.
 *
 * Stored in the "clones" Room table. Timestamps are kept as primitive epoch-millis
 * [Long] values so that no type converters are required.
 */
@Entity(tableName = "clones")
data class CloneInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Package name of the original application being cloned. */
    val sourcePackageName: String,
    /** User-visible label for this clone (e.g. "WhatsApp (Clone 2)"). */
    val cloneLabel: String,
    /** The Nth clone of this package (1, 2, 3, ...). */
    val cloneIndex: Int,
    /** Creation time, epoch millis. */
    val createdAt: Long,
    /** Last launch time, epoch millis. */
    val lastUsed: Long,
    /** Whether the clone is currently active/enabled. */
    val isActive: Boolean = true,
    /** Associated managed-profile / user id, or -1 when none. */
    val profileId: Int = -1
)
