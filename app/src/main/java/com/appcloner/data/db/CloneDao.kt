package com.appcloner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.appcloner.data.model.CloneInfo
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [CloneInfo] persistence.
 *
 * Read queries return [Flow] so the UI updates reactively; mutating operations
 * are suspend functions intended to run off the main thread.
 *
 * Declared as an abstract class (rather than an interface) so that
 * [insertCloneWithNextIndex] can compute the next [CloneInfo.cloneIndex] and insert the
 * row inside a single Room [Transaction], avoiding the count-then-insert race that a
 * separate "read count" + "insert" pair would allow.
 */
@Dao
abstract class CloneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertClone(clone: CloneInfo): Long

    @Delete
    abstract suspend fun deleteClone(clone: CloneInfo)

    @Query("DELETE FROM clones")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM clones ORDER BY createdAt DESC")
    abstract fun getAllClones(): Flow<List<CloneInfo>>

    @Query("SELECT * FROM clones WHERE sourcePackageName = :pkg ORDER BY cloneIndex ASC")
    abstract fun getClonesForPackage(pkg: String): Flow<List<CloneInfo>>

    @Query("SELECT COUNT(*) FROM clones WHERE sourcePackageName = :pkg")
    abstract suspend fun countClonesForPackage(pkg: String): Int

    @Query("SELECT COALESCE(MAX(cloneIndex), 0) FROM clones WHERE sourcePackageName = :pkg")
    abstract suspend fun maxCloneIndexForPackage(pkg: String): Int

    @Update
    abstract suspend fun updateClone(clone: CloneInfo)

    @Query("SELECT * FROM clones WHERE id = :id")
    abstract suspend fun getCloneById(id: Long): CloneInfo?

    /**
     * Atomically computes the next [CloneInfo.cloneIndex] for the clone's source package
     * (max existing index + 1) and inserts the clone, all within one transaction. This is
     * the race-safe replacement for a separate count-then-insert: concurrent creations can
     * no longer read the same count and assign a duplicate index.
     *
     * The passed-in [clone]'s `cloneIndex` is ignored. When [labelForIndex] is provided it
     * is used to build the stored [CloneInfo.cloneLabel] from the freshly computed index
     * (used for the auto-generated default label); when it is null the clone's own
     * [CloneInfo.cloneLabel] is kept as-is (used when the user typed a custom label).
     *
     * @return the [CloneInfo.cloneIndex] assigned to the inserted row.
     */
    @Transaction
    open suspend fun insertCloneWithNextIndex(
        clone: CloneInfo,
        labelForIndex: ((Int) -> String)? = null
    ): Int {
        val nextIndex = maxCloneIndexForPackage(clone.sourcePackageName) + 1
        val label = labelForIndex?.invoke(nextIndex) ?: clone.cloneLabel
        insertClone(clone.copy(cloneIndex = nextIndex, cloneLabel = label))
        return nextIndex
    }
}
