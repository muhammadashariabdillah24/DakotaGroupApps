package com.dakotagroupstaff.ui.operasional.assignment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.databinding.ItemDownPaymentBinding

class DownPaymentAdapter(
    private val items: List<DownPaymentItem>
) : RecyclerView.Adapter<DownPaymentAdapter.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownPaymentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount(): Int = items.size
    
    class ViewHolder(
        private val binding: ItemDownPaymentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: DownPaymentItem) {
            binding.tvCode.text = item.code
            binding.tvName.text = item.name
            binding.tvNominal.text = formatNumber(item.nominal)
        }
        
        private fun formatNumber(nominal: String): String {
            return try {
                val number = nominal.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
                String.format("%,d", number).replace(",", ".")
            } catch (e: Exception) {
                nominal
            }
        }
    }
}
