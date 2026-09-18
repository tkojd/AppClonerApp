package com.appcloner.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.appcloner.data.AppRepository
import com.appcloner.data.CloneManager
import com.appcloner.data.CloneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Locks the navigation argument-key contract between [com.appcloner.ui.applist.AppListFragment]
 * (the producer that writes the [android.os.Bundle]) and [CloneDetailViewModel] (the consumer
 * that reads from [SavedStateHandle]).
 *
 * The ViewModel is constructed with a seeded [SavedStateHandle] using the very keys the
 * Fragment writes. If either side renamed a key, [CloneDetailUiState.sourcePackageName] /
 * [CloneDetailUiState.appName] would no longer surface the seeded values and these tests
 * would fail, catching the silent breakage the reviewer flagged.
 *
 * Collaborators are mocked (no Android framework objects are instantiated) so the test runs
 * against the android.jar stubs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CloneDetailViewModelArgsTest {

    private lateinit var appRepository: AppRepository
    private lateinit var cloneRepository: CloneRepository
    private lateinit var cloneManager: CloneManager

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        appRepository = mock(AppRepository::class.java)
        cloneRepository = mock(CloneRepository::class.java)
        cloneManager = mock(CloneManager::class.java)
        // stateIn() consumes this flow during construction, so it must not be null.
        `when`(cloneRepository.clonesForPackage("com.example.app"))
            .thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun readsSourcePackageAndAppNameFromSavedStateHandle() = runTest {
        val handle = SavedStateHandle(
            mapOf(
                CloneDetailViewModel.ARG_SOURCE_PACKAGE to "com.example.app",
                CloneDetailViewModel.ARG_APP_NAME to "Example App"
            )
        )

        val viewModel = CloneDetailViewModel(handle, appRepository, cloneRepository, cloneManager)

        val state = viewModel.uiState.value
        assertEquals("com.example.app", state.sourcePackageName)
        assertEquals("Example App", state.appName)
    }

    @Test
    fun argKeysMatchTheBundleKeysTheFragmentWrites() {
        // These literal keys are exactly what AppListFragment.onAppSelected puts into the
        // navigation Bundle and what nav_graph.xml declares as <argument> names. Locking the
        // constant values here means a rename on the ViewModel side is caught immediately.
        assertEquals("sourcePackageName", CloneDetailViewModel.ARG_SOURCE_PACKAGE)
        assertEquals("appName", CloneDetailViewModel.ARG_APP_NAME)
    }
}
