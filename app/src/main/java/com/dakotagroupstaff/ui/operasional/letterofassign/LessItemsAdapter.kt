package com.dakotagroupstaff.ui.operasional.letterofassign

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.remote.response.LessItemsData
import com.dakotagroupstaff.databinding.ItemLessItemsBinding

/**
 * Adapter for Less Items (Barang Kurang) list
 */
class LessItemsAdapter : ListAdapter<LessItemsData, LessItemsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLessItemsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLessItemsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LessItemsData) {
            binding.apply {
                tvBTTNo.text = item.bttNo
                tvJumlahKurang.text = item.jmlKurang
                tvKeterangan.text = item.keterangan.ifEmpty { "-" }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<LessItemsData>() {
        override fun areItemsTheSame(
            oldItem: LessItemsData,
            newItem: LessItemsData
        ): Boolean {
            return oldItem.bttID == newItem.bttID
        }

        override fun areContentsTheSame(
            oldItem: LessItemsData,
            newItem: LessItemsData
        ): Boolean {
            return oldItem == newItem
        }
    }
}
