package com.dakotagroupstaff.ui.kepegawaian.approval

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.remote.response.PendingApprovalData
import com.dakotagroupstaff.databinding.ItemApprovalBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying pending approval list
 * Migrated from OldSystemApproval
 */
class ApprovalAdapter(
    private val onItemClick: (PendingApprovalData) -> Unit
) : ListAdapter<PendingApprovalData, ApprovalAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApprovalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemApprovalBinding,
        private val onItemClick: (PendingApprovalData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        private val outputDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

        fun bind(item: PendingApprovalData) {
            with(binding) {
                // Employee info
                tvEmployeeName.text = item.namaPengaju
                tvEmployeeNip.text = "NIP: ${item.nipPengaju}"

                // Leave type
                tvLeaveType.text = item.status

                // Date range with total days - calculate from date range
                var calculatedDays = item.totalDays // Default fallback
                var startDateFormatted: String? = null
                var endDateFormatted: String? = null
                
                try {
                    val startDate = inputDateFormat.parse(item.tglMulai)
                    val endDate = inputDateFormat.parse(item.tglAkhir)
                    
                    startDateFormatted = startDate?.let { outputDateFormat.format(it) }
                    endDateFormatted = endDate?.let { outputDateFormat.format(it) }
                    
                    // Calculate total days between start and end dates
                    if (startDate != null && endDate != null) {
                        val diffInMillis = endDate.time - startDate.time
                        calculatedDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt() + 1 // +1 to include both start and end dates
                    }
                } catch (e: Exception) {
                    startDateFormatted = item.tglMulai
                    endDateFormatted = item.tglAkhir
                }

                tvDateRange.text = "$startDateFormatted - $endDateFormatted ($calculatedDays hari)"

                // Description
                tvDescription.text = item.keterangan.ifBlank { "Tidak ada keterangan" }

                // Leave balance
                val saldoCuti = item.saldoCuti.toIntOrNull() ?: 0
                tvLeaveBalance.text = "Saldo Cuti: $saldoCuti hari"

                // Item click
                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PendingApprovalData>() {
            override fun areItemsTheSame(
                oldItem: PendingApprovalData,
                newItem: PendingApprovalData
            ): Boolean {
                return oldItem.idCuti == newItem.idCuti
            }

            override fun areContentsTheSame(
                oldItem: PendingApprovalData,
                newItem: PendingApprovalData
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
