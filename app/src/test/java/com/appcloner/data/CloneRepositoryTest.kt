package com.appcloner.data

import com.appcloner.data.db.CloneDao
import com.appcloner.data.model.CloneInfo
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for [CloneRepository] using a mocked [CloneDao].
 *
 * Room requires an Android runtime, so instead of a real database we verify that the
 * repository delegates correctly to the DAO.
 */
class CloneRepositoryTest {

    private lateinit var dao: CloneDao
    private lateinit var repository: CloneRepository

    private fun sampleClone() = CloneInfo(
        id = 0,
        sourcePackageName = "com.example.app",
        cloneLabel = "Example Clone",
        cloneIndex = 1,
        createdAt = 1000L,
        lastUsed = 1000L
    )

    @Before
    fun setUp() {
        dao = mock(CloneDao::class.java)
        `when`(dao.getAllClones()).thenReturn(emptyFlow())
        repository = CloneRepository(dao)
    }

    @Test
    fun addClone_delegatesToDaoInsert() = runTest {
        val clone = sampleClone()
        `when`(dao.insertClone(clone)).thenReturn(42L)

        val id = repository.addClone(clone)

        assertEquals(42L, id)
        verify(dao).insertClone(clone)
    }

    @Test
    fun nextCloneIndex_returnsCountPlusOne() = runTest {
        `when`(dao.countClonesForPackage("com.example.app")).thenReturn(2)

        val next = repository.nextCloneIndex("com.example.app")

        assertEquals(3, next)
    }

    @Test
    fun nextCloneIndex_returnsOneWhenNoClonesExist() = runTest {
        `when`(dao.countClonesForPackage("com.example.app")).thenReturn(0)

        val next = repository.nextCloneIndex("com.example.app")

        assertEquals(1, next)
    }

    @Test
    fun removeAll_delegatesToDaoDeleteAll() = runTest {
        repository.removeAll()

        verify(dao).deleteAll()
    }

    @Test
    fun removeClone_delegatesToDaoDelete() = runTest {
        val clone = sampleClone()

        repository.removeClone(clone)

        verify(dao).deleteClone(clone)
    }

    @Test
    fun updateClone_delegatesToDaoUpdate() = runTest {
        val clone = sampleClone()

        repository.updateClone(clone)

        verify(dao).updateClone(clone)
    }

    @Test
    fun getClone_delegatesToDaoGetById() = runTest {
        val clone = sampleClone().copy(id = 7)
        `when`(dao.getCloneById(7L)).thenReturn(clone)

        val result = repository.getClone(7L)

        assertEquals(clone, result)
    }
}
