package com.dakotagroupstaff.ui.kepegawaian.salary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.remote.response.SalarySlipData
import com.dakotagroupstaff.databinding.ItemSalarySlipBinding
import com.dakotagroupstaff.utils.SalaryDataHelper

/**
 * Adapter for salary slip list
 * Similar to React Native FlatList renderItem
 */
class SalarySlipAdapter(
    private val onItemClick: (SalarySlipData) -> Unit
) : ListAdapter<SalarySlipData, SalarySlipAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSalarySlipBinding.inflate(
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
        private val binding: ItemSalarySlipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(salarySlip: SalarySlipData) {
            with(binding) {
                // Month label (first 3 chars, uppercase)
                tvMonth.text = salarySlip.bulan.take(3).uppercase()
                
                // Period (e.g., "Juni 2025")
                tvPeriod.text = salarySlip.getFormattedPeriod()
                
                // Net salary
                val netSalary = salarySlip.getNetSalary()
                tvNetSalary.text = "Gaji Bersih: ${SalaryDataHelper.formatCurrency(netSalary)}"
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SalarySlipData>() {
            override fun areItemsTheSame(
                oldItem: SalarySlipData,
                newItem: SalarySlipData
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: SalarySlipData,
                newItem: SalarySlipData
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
