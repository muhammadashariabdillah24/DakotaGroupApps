package com.dakotagroupstaff.ui.kepegawaian.salary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.databinding.ItemYearBinding

/**
 * Adapter for year filter dialog
 */
class YearFilterAdapter(
    private val years: List<Int>,
    private var selectedYear: Int,
    private val onYearSelected: (Int) -> Unit
) : RecyclerView.Adapter<YearFilterAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemYearBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(years[position])
    }

    override fun getItemCount(): Int = years.size

    inner class ViewHolder(
        private val binding: ItemYearBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val year = years[position]
                    selectedYear = year
                    notifyDataSetChanged()
                    onYearSelected(year)
                }
            }
        }

        fun bind(year: Int) {
            with(binding) {
                tvYear.text = year.toString()
                ivCheck.visibility = if (year == selectedYear) View.VISIBLE else View.INVISIBLE
            }
        }
    }
}
