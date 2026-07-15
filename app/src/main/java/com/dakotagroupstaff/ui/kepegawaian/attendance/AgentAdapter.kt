package com.dakotagroupstaff.ui.kepegawaian.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.local.entity.AgentLocationEntity
import com.dakotagroupstaff.databinding.ItemAgentBinding

class AgentAdapter(
    private val onAgentSelected: (AgentLocationEntity) -> Unit
) : ListAdapter<AgentLocationEntity, AgentAdapter.AgentViewHolder>(AgentDiffCallback()) {

    private var originalList = listOf<AgentLocationEntity>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentViewHolder {
        val binding = ItemAgentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AgentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgentViewHolder, position: Int) {
        val agent = getItem(position)
        holder.bind(agent)
    }

    fun submitFullList(list: List<AgentLocationEntity>) {
        originalList = list
        super.submitList(list)
    }

    fun filter(query: String) {
        if (query.isEmpty()) {
            super.submitList(originalList)
            return
        }

        val filteredList = originalList.filter {
            it.namaAgen.contains(query, ignoreCase = true)
        }
        super.submitList(filteredList)
    }

    inner class AgentViewHolder(private val binding: ItemAgentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAgentSelected(getItem(position))
                }
            }
        }

        fun bind(agent: AgentLocationEntity) {
            binding.tvAgentName.text = agent.namaAgen
        }
    }

    class AgentDiffCallback : DiffUtil.ItemCallback<AgentLocationEntity>() {
        override fun areItemsTheSame(
            oldItem: AgentLocationEntity,
            newItem: AgentLocationEntity
        ): Boolean {
            return oldItem.kodeAgen == newItem.kodeAgen
        }

        override fun areContentsTheSame(
            oldItem: AgentLocationEntity,
            newItem: AgentLocationEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
