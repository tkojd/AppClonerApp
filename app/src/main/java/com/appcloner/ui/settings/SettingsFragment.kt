package com.appcloner.ui.settings

import android.content.pm.PackageManager
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
import com.appcloner.data.PreferencesManager
import com.appcloner.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Plain settings screen (no PreferenceFragmentCompat dependency): a "show system apps"
 * toggle, a destructive "clear all clones" action, and an about section showing the app
 * version resolved from the [PackageManager].
 *
 * The "show system apps" toggle writes through the shared [PreferencesManager], which is
 * the single source of truth also observed by
 * [com.appcloner.ui.applist.AppListViewModel]; toggling it here immediately affects the
 * app list.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupShowSystemAppsSwitch()
        setupClearAllClones()
        showVersion()
        observeEvents()
    }

    private fun setupShowSystemAppsSwitch() {
        binding.showSystemAppsSwitch.isChecked = preferencesManager.showSystemApps
        binding.showSystemAppsSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.showSystemApps = isChecked
        }
    }

    private fun setupClearAllClones() {
        binding.clearAllClonesButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_clear_all_clones)
                .setMessage(R.string.confirm_delete_all)
                .setPositiveButton(R.string.yes) { _, _ -> viewModel.clearAllClones() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun showVersion() {
        val versionName = try {
            val pm = requireContext().packageManager
            val pkg = requireContext().packageName
            pm.getPackageInfo(pkg, 0).versionName ?: getString(R.string.version_unknown)
        } catch (e: PackageManager.NameNotFoundException) {
            getString(R.string.version_unknown)
        }
        binding.versionText.text = getString(R.string.settings_version) + ": " + versionName
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is SettingsEvent.AllClonesCleared -> Snackbar.make(
                            binding.root,
                            getString(R.string.all_clones_deleted),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
