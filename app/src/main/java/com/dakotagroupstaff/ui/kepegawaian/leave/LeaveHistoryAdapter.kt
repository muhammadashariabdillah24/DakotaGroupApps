package com.dakotagroupstaff.ui.kepegawaian.leave

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.local.entity.LeaveDetailsEntity
import com.dakotagroupstaff.data.remote.response.LeaveStatusHelper
import com.dakotagroupstaff.databinding.ItemLeaveHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class LeaveHistoryAdapter : ListAdapter<LeaveDetailsEntity, LeaveHistoryAdapter.ViewHolder>(DIFF_CALLBACK) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLeaveHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(private val binding: ItemLeaveHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(leave: LeaveDetailsEntity) {
            binding.apply {
                // Leave type
                tvLeaveType.text = leave.jenisCuti
                
                // Date range
                tvDateRange.text = formatDateRange(leave.tglAwal, leave.tglAkhir)
                
                // Description
                tvDescription.text = leave.keterangan
                
                // Approval status
                val approvalStatus = LeaveStatusHelper.getApprovalStatus(leave.atasan1Approve, leave.atasan2Approve)
                tvApprovalStatus.text = approvalStatus
                
                // Deduction info
                val deductionInfo = when {
                    leave.potongCuti == "Y" -> "Potong Cuti"
                    leave.dispensasi == "Y" -> "Dispensasi"
                    leave.potongGaji == "Y" -> "Potong Gaji"
                    else -> "-"
                }
                tvDeductionInfo.text = deductionInfo
            }
        }
        
        private fun formatDateRange(startTimestamp: Long, endTimestamp: Long): String {
            return try {
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                
                val startDate = Date(startTimestamp)
                val endDate = Date(endTimestamp)
                
                "${outputFormat.format(startDate)} - ${outputFormat.format(endDate)}"
            } catch (e: Exception) {
                "Invalid Date"
            }
        }
    }
    
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LeaveDetailsEntity>() {
            override fun areItemsTheSame(
                oldItem: LeaveDetailsEntity,
                newItem: LeaveDetailsEntity
            ): Boolean {
                return oldItem.leaveId == newItem.leaveId
            }
            
            override fun areContentsTheSame(
                oldItem: LeaveDetailsEntity,
                newItem: LeaveDetailsEntity
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
