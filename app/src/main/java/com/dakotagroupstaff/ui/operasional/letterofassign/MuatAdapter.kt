package com.dakotagroupstaff.ui.operasional.letterofassign

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.remote.response.LoadingData
import com.dakotagroupstaff.databinding.ItemMuatBinding

/**
 * Adapter for Muat (Loading) items
 */
class MuatAdapter : ListAdapter<LoadingData, MuatAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMuatBinding.inflate(
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
        private val binding: ItemMuatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LoadingData) {
            binding.tvBTT.text = item.loadHID
            binding.tvNama.text = item.tujuanNama
            binding.tvBerat.text = item.jmlSP
            binding.tvScan.text = if (item.isComplete()) "Complete" else "Pending"

            // Change text color based on completion status
            val textColor = if (item.isComplete()) {
                Color.GREEN
            } else {
                Color.RED
            }
            binding.tvScan.setTextColor(textColor)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LoadingData>() {
        override fun areItemsTheSame(oldItem: LoadingData, newItem: LoadingData): Boolean {
            return oldItem.loadHID == newItem.loadHID
        }

        override fun areContentsTheSame(oldItem: LoadingData, newItem: LoadingData): Boolean {
            return oldItem == newItem
        }
    }
}
