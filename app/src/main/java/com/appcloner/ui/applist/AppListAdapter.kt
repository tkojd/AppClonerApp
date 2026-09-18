package com.appcloner.ui.applist

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appcloner.data.model.AppInfo
import com.appcloner.databinding.ItemAppBinding

/**
 * [ListAdapter] rendering [AppInfo] rows. Icons are resolved directly from the
 * [PackageManager] inside the view holder (best-effort, wrapped in try/catch) to avoid
 * holding [android.graphics.drawable.Drawable] references in the model.
 */
class AppListAdapter(
    private val onAppClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.AppViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppViewHolder(binding, onAppClick)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AppViewHolder(
        private val binding: ItemAppBinding,
        private val onAppClick: (AppInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.appName.text = app.appName
            binding.packageName.text = app.packageName
            loadIcon(app.packageName)
            binding.root.setOnClickListener { onAppClick(app) }
        }

        private fun loadIcon(packageName: String) {
            try {
                val pm = binding.root.context.packageManager
                binding.appIcon.setImageDrawable(pm.getApplicationIcon(packageName))
            } catch (e: PackageManager.NameNotFoundException) {
                binding.appIcon.setImageResource(
                    com.appcloner.R.drawable.ic_app_placeholder
                )
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
                oldItem == newItem
        }
    }
}
