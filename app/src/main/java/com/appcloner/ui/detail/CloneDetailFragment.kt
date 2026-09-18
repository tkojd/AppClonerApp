package com.appcloner.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.appcloner.R
import com.appcloner.data.model.CloneInfo
import com.appcloner.databinding.FragmentCloneDetailBinding
import com.appcloner.databinding.FragmentCloneDetailItemBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Shows a source app's details, its existing clones and a form to create a new clone.
 */
@AndroidEntryPoint
class CloneDetailFragment : Fragment() {

    private var _binding: FragmentCloneDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CloneDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCloneDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.createCloneButton.setOnClickListener {
            val label = binding.labelEditText.text?.toString().orEmpty()
            viewModel.createClone(label) { index ->
                getString(R.string.clone_label_default, index)
            }
        }
        observeState()
        observeClones()
        observeEvents()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.appName.text = state.appName
                    binding.appPackage.text =
                        getString(R.string.package_label, state.sourcePackageName)
                    loadIcon(state.sourcePackageName)
                    binding.compatibilityText.text = getString(
                        if (state.isManagedProfileSupported) R.string.profiles_supported
                        else R.string.profiles_not_supported
                    )
                }
            }
        }
    }

    private fun observeClones() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.existingClones.collect { clones ->
                    renderExistingClones(clones)
                }
            }
        }
    }

    private fun renderExistingClones(clones: List<CloneInfo>) {
        val container = binding.existingClonesContainer
        container.removeAllViews()
        binding.noExistingClones.visibility =
            if (clones.isEmpty()) View.VISIBLE else View.GONE
        val inflater = LayoutInflater.from(requireContext())
        clones.forEach { clone ->
            val itemBinding = FragmentCloneDetailItemBinding.inflate(inflater, container, false)
            itemBinding.existingCloneLabel.text = clone.cloneLabel
            itemBinding.existingCloneIndex.text =
                getString(R.string.clone_index, clone.cloneIndex)
            container.addView(itemBinding.root)
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is CloneDetailEvent.CloneCreated -> {
                            binding.labelEditText.text?.clear()
                            Snackbar.make(
                                binding.root,
                                getString(R.string.clone_created),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                        is CloneDetailEvent.CreateFailed -> {
                            Snackbar.make(
                                binding.root,
                                event.message ?: getString(R.string.clone_error),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun loadIcon(packageName: String) {
        try {
            val pm = requireContext().packageManager
            binding.appIcon.setImageDrawable(pm.getApplicationIcon(packageName))
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            binding.appIcon.setImageResource(R.drawable.ic_app_placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
