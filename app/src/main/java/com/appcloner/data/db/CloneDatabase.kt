package com.appcloner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.appcloner.data.model.CloneInfo

/**
 * Room database holding persisted clone configurations.
 *
 * All fields on [CloneInfo] are primitives or [String]/[Boolean], so no
 * [androidx.room.TypeConverters] are required for version 1.
 */
@Database(entities = [CloneInfo::class], version = 1, exportSchema = false)
abstract class CloneDatabase : RoomDatabase() {
    abstract fun cloneDao(): CloneDao
}
