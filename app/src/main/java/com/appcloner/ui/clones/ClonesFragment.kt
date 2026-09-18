package com.appcloner.ui.clones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.appcloner.R
import com.appcloner.data.model.CloneInfo
import com.appcloner.databinding.FragmentClonesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Lists all created clones and lets the user launch, rename or delete each of them.
 */
@AndroidEntryPoint
class ClonesFragment : Fragment() {

    private var _binding: FragmentClonesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ClonesViewModel by viewModels()
    private lateinit var adapter: ClonesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClonesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        binding.goToAppsButton.setOnClickListener {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottom_navigation
            )?.selectedItemId = R.id.appListFragment
        }
        observeState()
        observeEvents()
    }

    private fun setupRecyclerView() {
        adapter = ClonesAdapter(
            onLaunch = viewModel::launchClone,
            onRename = ::showRenameDialog,
            onDelete = ::confirmDelete
        )
        binding.clonesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.clonesRecyclerView.adapter = adapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.clones)
                    val showEmpty = !state.isLoading && state.clones.isEmpty()
                    binding.emptyView.visibility = if (showEmpty) View.VISIBLE else View.GONE
                    binding.clonesRecyclerView.visibility =
                        if (state.clones.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    val message = when (event) {
                        is ClonesEvent.LaunchStarted -> getString(R.string.clone_launched)
                        is ClonesEvent.LaunchFailed ->
                            event.message ?: getString(R.string.clone_launch_error)
                        is ClonesEvent.CloneDeleted -> getString(R.string.clone_deleted)
                        is ClonesEvent.CloneRenamed -> getString(R.string.clone_renamed)
                    }
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRenameDialog(clone: CloneInfo) {
        val editText = EditText(requireContext()).apply {
            setText(clone.cloneLabel)
            setSelection(text.length)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename_clone_title)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.renameClone(clone, editText.text.toString())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(clone: CloneInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_clone)
            .setMessage(R.string.confirm_delete_clone)
            .setPositiveButton(R.string.yes) { _, _ -> viewModel.deleteClone(clone) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.clonesRecyclerView.adapter = null
        _binding = null
    }
}
