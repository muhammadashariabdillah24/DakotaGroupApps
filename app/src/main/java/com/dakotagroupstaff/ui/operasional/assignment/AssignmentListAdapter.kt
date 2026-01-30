package com.dakotagroupstaff.ui.operasional.assignment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.remote.response.AssignmentListItem
import com.dakotagroupstaff.databinding.ItemAssignmentListBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying list of assignments in the selection dialog
 */
class AssignmentListAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<AssignmentListItem, AssignmentListAdapter.ViewHolder>(DIFF_CALLBACK) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssignmentListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemAssignmentListBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: AssignmentListItem) {
            binding.tvAssignmentId.text = item.sID ?: "Unknown"
            
            // Format date if available, otherwise show default message
            val dateText = if (!item.tgl.isNullOrEmpty()) {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                    val date = inputFormat.parse(item.tgl)
                    date?.let { outputFormat.format(it) } ?: item.tgl
                } catch (e: Exception) {
                    // Try simpler format
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                        val date = inputFormat.parse(item.tgl)
                        date?.let { outputFormat.format(it) } ?: item.tgl
                    } catch (e2: Exception) {
                        item.tgl ?: "Tanggal tidak tersedia"
                    }
                }
            } else {
                "Tanggal tidak tersedia"
            }
            
            binding.tvAssignmentDate.text = dateText
            
            binding.root.setOnClickListener {
                onItemClick(item.sID)
            }
        }
    }
    
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AssignmentListItem>() {
            override fun areItemsTheSame(
                oldItem: AssignmentListItem,
                newItem: AssignmentListItem
            ): Boolean {
                return oldItem.sID == newItem.sID
            }
            
            override fun areContentsTheSame(
                oldItem: AssignmentListItem,
                newItem: AssignmentListItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
