package com.dakotagroupstaff.ui.operasional.letterofassign

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.remote.response.UnloadingData
import com.dakotagroupstaff.databinding.ItemBongkarBinding

/**
 * Adapter for Bongkar (Unloading) items
 */
class BongkarAdapter : ListAdapter<UnloadingData, BongkarAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBongkarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemBongkarBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UnloadingData) {
            binding.tvBTT.text = item.bttID
            binding.tvBerat.text = item.berat
            binding.tvScanKoli.text = "${item.discan} / ${item.jberangkat}"

            // Change text color based on completion status
            val textColor = if (item.isComplete()) {
                Color.GREEN
            } else {
                Color.RED
            }
            binding.tvScanKoli.setTextColor(textColor)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UnloadingData>() {
        override fun areItemsTheSame(oldItem: UnloadingData, newItem: UnloadingData): Boolean {
            return oldItem.bttID == newItem.bttID
        }

        override fun areContentsTheSame(oldItem: UnloadingData, newItem: UnloadingData): Boolean {
            return oldItem == newItem
        }
    }
}
