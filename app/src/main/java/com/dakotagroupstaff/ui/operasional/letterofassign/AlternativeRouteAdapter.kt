package com.dakotagroupstaff.ui.operasional.letterofassign

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.remote.response.AlternativeRouteData
import com.dakotagroupstaff.databinding.ItemAlternativeRouteBinding

/**
 * Adapter for Alternative Route list
 */
class AlternativeRouteAdapter(
    private val onRouteClick: (AlternativeRouteData) -> Unit
) : ListAdapter<AlternativeRouteData, AlternativeRouteAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlternativeRouteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAlternativeRouteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(route: AlternativeRouteData) {
            binding.apply {
                tvRouteName.text = route.agenNamaCad
                tvRouteInfo.text = "Dari: ${route.agenUtama}"
                
                root.setOnClickListener {
                    onRouteClick(route)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AlternativeRouteData>() {
        override fun areItemsTheSame(
            oldItem: AlternativeRouteData,
            newItem: AlternativeRouteData
        ): Boolean {
            return oldItem.agenIDCad == newItem.agenIDCad
        }

        override fun areContentsTheSame(
            oldItem: AlternativeRouteData,
            newItem: AlternativeRouteData
        ): Boolean {
            return oldItem == newItem
        }
    }
}
