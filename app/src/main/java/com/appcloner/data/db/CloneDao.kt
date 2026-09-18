package com.appcloner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appcloner.data.model.CloneInfo
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [CloneInfo] persistence.
 *
 * Read queries return [Flow] so the UI updates reactively; mutating operations
 * are suspend functions intended to run off the main thread.
 */
@Dao
interface CloneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClone(clone: CloneInfo): Long

    @Delete
    suspend fun deleteClone(clone: CloneInfo)

    @Query("DELETE FROM clones")
    suspend fun deleteAll()

    @Query("SELECT * FROM clones ORDER BY createdAt DESC")
    fun getAllClones(): Flow<List<CloneInfo>>

    @Query("SELECT * FROM clones WHERE sourcePackageName = :pkg ORDER BY cloneIndex ASC")
    fun getClonesForPackage(pkg: String): Flow<List<CloneInfo>>

    @Query("SELECT COUNT(*) FROM clones WHERE sourcePackageName = :pkg")
    suspend fun countClonesForPackage(pkg: String): Int

    @Update
    suspend fun updateClone(clone: CloneInfo)

    @Query("SELECT * FROM clones WHERE id = :id")
    suspend fun getCloneById(id: Long): CloneInfo?
}
