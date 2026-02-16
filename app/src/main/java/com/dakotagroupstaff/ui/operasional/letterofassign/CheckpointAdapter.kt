package com.dakotagroupstaff.ui.operasional.letterofassign

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.remote.response.LetterOfAssignDetail
import com.dakotagroupstaff.databinding.ItemCheckpointBinding
import java.text.SimpleDateFormat
import java.util.*

class CheckpointAdapter(
    private val onCheckinClick: (LetterOfAssignDetail) -> Unit
) : ListAdapter<LetterOfAssignDetail, CheckpointAdapter.CheckpointViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckpointViewHolder {
        val binding = ItemCheckpointBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CheckpointViewHolder(binding, onCheckinClick)
    }

    override fun onBindViewHolder(holder: CheckpointViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class CheckpointViewHolder(
        private val binding: ItemCheckpointBinding,
        private val onCheckinClick: (LetterOfAssignDetail) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(checkpoint: LetterOfAssignDetail, position: Int) {
            binding.apply {
                // Checkpoint number
                tvCheckpointNumber.text = checkpoint.trUrut.ifEmpty { (position + 1).toString() }
                
                // Checkpoint name
                tvCheckpointName.text = checkpoint.trCabang.ifEmpty { "Checkpoint ${position + 1}" }
                
                // Checkpoint status (B = Berangkat, P = Pulang)
                val statusText = when (checkpoint.trStatus) {
                    "B" -> "Status: Berangkat"
                    "P" -> "Status: Pulang"
                    else -> "Status: ${checkpoint.trStatus}"
                }
                tvCheckpointStatus.text = statusText
                
                // Check if already checked in
                val isCheckedIn = checkpoint.isCheckedIn()
                
                if (isCheckedIn) {
                    // Show check-in info
                    layoutCheckinInfo.visibility = View.VISIBLE
                    btnCheckin.visibility = View.GONE
                    
                    // Format check-in time
                    val checkinTime = try {
                        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        val date = parser.parse(checkpoint.trAbsenIn)
                        date?.let { formatter.format(it) } ?: checkpoint.trAbsenIn
                    } catch (e: Exception) {
                        checkpoint.trAbsenIn
                    }
                    
                    tvCheckinTime.text = "Check-in: $checkinTime"
                    tvCheckinKm.text = "Odometer: ${checkpoint.trKm} KM"
                    
                    // Green check icon
                    ivCheckpointStatus.setImageResource(R.drawable.ic_check_circle)
                    ivCheckpointStatus.setColorFilter(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
                    )
                } else {
                    // Not checked in yet
                    layoutCheckinInfo.visibility = View.GONE
                    
                    // Check if this is the current checkpoint (next to check in)
                    val isCurrentCheckpoint = checkpoint.isCurrentCheckpoint(checkpoint.lastCekin)
                    
                    if (isCurrentCheckpoint) {
                        // Show check-in button
                        btnCheckin.visibility = View.VISIBLE
                        btnCheckin.setOnClickListener { onCheckinClick(checkpoint) }
                        
                        // Orange pending icon
                        ivCheckpointStatus.setImageResource(R.drawable.ic_pending)
                        ivCheckpointStatus.setColorFilter(
                            ContextCompat.getColor(binding.root.context, android.R.color.holo_orange_dark)
                        )
                    } else {
                        // Future checkpoint
                        btnCheckin.visibility = View.GONE
                        
                        // Gray future icon
                        ivCheckpointStatus.setImageResource(R.drawable.ic_circle_outline)
                        ivCheckpointStatus.setColorFilter(
                            ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LetterOfAssignDetail>() {
            override fun areItemsTheSame(
                oldItem: LetterOfAssignDetail,
                newItem: LetterOfAssignDetail
            ): Boolean {
                return oldItem.trUrut == newItem.trUrut && oldItem.sID == newItem.sID
            }

            override fun areContentsTheSame(
                oldItem: LetterOfAssignDetail,
                newItem: LetterOfAssignDetail
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
