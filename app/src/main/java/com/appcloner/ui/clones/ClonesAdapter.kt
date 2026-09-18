package com.appcloner.ui.clones

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appcloner.R
import com.appcloner.data.model.CloneInfo
import com.appcloner.databinding.ItemCloneBinding

/**
 * [ListAdapter] rendering [CloneInfo] rows with launch, rename and delete callbacks.
 */
class ClonesAdapter(
    private val onLaunch: (CloneInfo) -> Unit,
    private val onRename: (CloneInfo) -> Unit,
    private val onDelete: (CloneInfo) -> Unit
) : ListAdapter<CloneInfo, ClonesAdapter.CloneViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CloneViewHolder {
        val binding = ItemCloneBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CloneViewHolder(binding, onLaunch, onRename, onDelete)
    }

    override fun onBindViewHolder(holder: CloneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CloneViewHolder(
        private val binding: ItemCloneBinding,
        private val onLaunch: (CloneInfo) -> Unit,
        private val onRename: (CloneInfo) -> Unit,
        private val onDelete: (CloneInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(clone: CloneInfo) {
            val context = binding.root.context
            binding.cloneLabel.text = clone.cloneLabel
            binding.cloneSource.text = clone.sourcePackageName
            binding.cloneIndexBadge.text =
                context.getString(R.string.clone_index, clone.cloneIndex)
            loadIcon(clone.sourcePackageName)

            binding.launchButton.setOnClickListener { onLaunch(clone) }
            binding.overflowButton.setOnClickListener { showOverflow(it, clone) }
        }

        private fun showOverflow(anchor: View, clone: CloneInfo) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, MENU_RENAME, 0, R.string.rename_clone)
            popup.menu.add(0, MENU_DELETE, 1, R.string.delete_clone)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_RENAME -> { onRename(clone); true }
                    MENU_DELETE -> { onDelete(clone); true }
                    else -> false
                }
            }
            popup.show()
        }

        private fun loadIcon(packageName: String) {
            try {
                val pm = binding.root.context.packageManager
                binding.cloneIcon.setImageDrawable(pm.getApplicationIcon(packageName))
            } catch (e: PackageManager.NameNotFoundException) {
                binding.cloneIcon.setImageResource(R.drawable.ic_app_placeholder)
            }
        }
    }

    companion object {
        private const val MENU_RENAME = 1
        private const val MENU_DELETE = 2

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CloneInfo>() {
            override fun areItemsTheSame(oldItem: CloneInfo, newItem: CloneInfo): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CloneInfo, newItem: CloneInfo): Boolean =
                oldItem == newItem
        }
    }
}
