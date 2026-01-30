package com.dakotagroupstaff.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.local.entity.RecentMenuEntity
import com.dakotagroupstaff.databinding.ItemRecentMenuBinding

class RecentMenuAdapter(
    private val onItemClick: (RecentMenuEntity) -> Unit
) : ListAdapter<RecentMenuEntity, RecentMenuAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemRecentMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(menu: RecentMenuEntity, onItemClick: (RecentMenuEntity) -> Unit) {
            binding.ivMenuIcon.setImageResource(menu.iconResId)
            binding.tvMenuName.text = menu.menuName
            binding.root.setOnClickListener { onItemClick(menu) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<RecentMenuEntity>() {
            override fun areItemsTheSame(oldItem: RecentMenuEntity, newItem: RecentMenuEntity): Boolean {
                return oldItem.menuId == newItem.menuId
            }

            override fun areContentsTheSame(oldItem: RecentMenuEntity, newItem: RecentMenuEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
