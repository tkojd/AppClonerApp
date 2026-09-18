package com.appcloner.ui.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appcloner.R
import com.appcloner.data.model.AppInfo
import com.appcloner.databinding.FragmentAppListBinding
import com.appcloner.ui.detail.CloneDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Displays a searchable, filterable list of installed applications. Selecting an app
 * navigates to [com.appcloner.ui.detail.CloneDetailFragment] to create a clone.
 */
@AndroidEntryPoint
class AppListFragment : Fragment() {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppListViewModel by viewModels()
    private lateinit var adapter: AppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupFilterChip()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(::onAppSelected)
        binding.appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.appsRecyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }
    }

    private fun setupFilterChip() {
        binding.systemAppsChip.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleSystemApps(isChecked)
        }
    }

    private fun onAppSelected(app: AppInfo) {
        // Reference the ViewModel's argument-key constants (the single source of truth for
        // this contract) rather than string literals, so a rename can't silently drift the
        // producer out of sync with the consumer.
        val args = Bundle().apply {
            putString(CloneDetailViewModel.ARG_SOURCE_PACKAGE, app.packageName)
            putString(CloneDetailViewModel.ARG_APP_NAME, app.appName)
        }
        findNavController().navigate(R.id.action_appList_to_cloneDetail, args)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE
                    val items = state.filteredApps
                    adapter.submitList(items)
                    val showEmpty = !state.isLoading && items.isEmpty()
                    binding.emptyView.visibility = if (showEmpty) View.VISIBLE else View.GONE
                    if (binding.systemAppsChip.isChecked != state.showSystemApps) {
                        binding.systemAppsChip.isChecked = state.showSystemApps
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.appsRecyclerView.adapter = null
        _binding = null
    }
}
