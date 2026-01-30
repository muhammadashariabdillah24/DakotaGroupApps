package com.dakotagroupstaff.ui.operasional.loper

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.data.remote.response.DeliveryItem
import com.dakotagroupstaff.databinding.ItemDeliveryBinding

class DeliveryAdapter(
    private val onItemClick: (DeliveryItem) -> Unit,
    private val onItemLongClick: ((DeliveryItem) -> Unit)? = null,
    private val isFromSentTab: Boolean = false
) : ListAdapter<DeliveryItem, DeliveryAdapter.DeliveryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryViewHolder {
        val binding = ItemDeliveryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeliveryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeliveryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeliveryViewHolder(
        private val binding: ItemDeliveryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeliveryItem) {
            with(binding) {
                // BTT Number
                tvBttNumber.text = item.noBtt
                
                // Receiver Name
                tvReceiver.text = item.penerima
                
                // Address - combine all address parts
                val fullAddress = buildString {
                    append(item.alamat)
                    if (item.kelurahan.isNotEmpty()) {
                        append(", ${item.kelurahan}")
                    }
                    if (item.kecamatan.isNotEmpty()) {
                        append(", ${item.kecamatan}")
                    }
                    if (item.kota.isNotEmpty()) {
                        append(", ${item.kota}")
                    }
                    if (item.propinsi.isNotEmpty()) {
                        append(", ${item.propinsi}")
                    }
                }
                tvAddress.text = fullAddress
                
                // Weight
                tvWeight.text = "${item.berat} kg"
                
                // Packages
                tvPackages.text = "${item.jumlahKoli} koli"
                
                // Service Type
                tvService.text = when (item.service) {
                    "R" -> "REGULER"
                    "S" -> "SUPER"
                    "E" -> "EXPRESS"
                    else -> item.service
                }
                
                // Click listener
                root.setOnClickListener {
                    onItemClick(item)
                }
                
                // Long click listener for deletion (sent items)
                onItemLongClick?.let { longClickHandler ->
                    root.setOnLongClickListener {
                        longClickHandler(item)
                        true
                    }
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeliveryItem>() {
            override fun areItemsTheSame(oldItem: DeliveryItem, newItem: DeliveryItem): Boolean {
                return oldItem.noBtt == newItem.noBtt
            }

            override fun areContentsTheSame(oldItem: DeliveryItem, newItem: DeliveryItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
