package com.appcloner.data

import com.appcloner.data.db.CloneDao
import com.appcloner.data.model.CloneInfo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository wrapping [CloneDao], exposing reactive reads and suspend-based CRUD
 * for persisted clone configurations.
 */
@Singleton
class CloneRepository @Inject constructor(
    private val cloneDao: CloneDao
) {
    /** All clones, newest first. */
    val allClones: Flow<List<CloneInfo>> = cloneDao.getAllClones()

    /** Clones of a specific source package, ordered by clone index. */
    fun clonesForPackage(pkg: String): Flow<List<CloneInfo>> = cloneDao.getClonesForPackage(pkg)

    /** Inserts a clone and returns its generated row id. */
    suspend fun addClone(clone: CloneInfo): Long = cloneDao.insertClone(clone)

    suspend fun removeClone(clone: CloneInfo) = cloneDao.deleteClone(clone)

    suspend fun removeAll() = cloneDao.deleteAll()

    suspend fun updateClone(clone: CloneInfo) = cloneDao.updateClone(clone)

    /** Returns the next clone index for a package (existing count + 1). */
    suspend fun nextCloneIndex(pkg: String): Int = cloneDao.countClonesForPackage(pkg) + 1

    suspend fun getClone(id: Long): CloneInfo? = cloneDao.getCloneById(id)
}
