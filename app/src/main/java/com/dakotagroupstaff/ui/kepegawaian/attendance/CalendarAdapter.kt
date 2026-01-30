package com.dakotagroupstaff.ui.kepegawaian.attendance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.R
import com.dakotagroupstaff.databinding.ItemCalendarDayBinding

/**
 * Calendar Adapter for RecyclerView
 * Shows monthly calendar with attendance status colors
 */
class CalendarAdapter(
    private val onDayClick: (CalendarDay) -> Unit
) : ListAdapter<CalendarDay, CalendarAdapter.CalendarDayViewHolder>(DiffCallback) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarDayViewHolder(binding, onDayClick)
    }
    
    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        val day = getItem(position)
        holder.bind(day)
    }
    
    class CalendarDayViewHolder(
        private val binding: ItemCalendarDayBinding,
        private val onDayClick: (CalendarDay) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(day: CalendarDay) {
            when (day.status) {
                CalendarDayStatus.EMPTY -> {
                    // Empty cell - hide content
                    binding.tvDayNumber.visibility = View.GONE
                    binding.tvDayName.visibility = View.GONE
                    binding.root.setCardBackgroundColor(Color.TRANSPARENT)
                    binding.root.isClickable = false
                }
                else -> {
                    binding.tvDayNumber.visibility = View.VISIBLE
                    binding.tvDayName.visibility = View.GONE // Hide day name, only show number
                    binding.tvDayNumber.text = day.day.toString()
                    
                    // Set background color based on status
                    val context = binding.root.context
                    val backgroundColor = when (day.status) {
                        CalendarDayStatus.COMPLETE -> {
                            // Green - Both check in and check out
                            ContextCompat.getColor(context, android.R.color.holo_green_light)
                        }
                        CalendarDayStatus.PARTIAL -> {
                            // Yellow - Only check in
                            ContextCompat.getColor(context, android.R.color.holo_orange_light)
                        }
                        CalendarDayStatus.ABSENT -> {
                            // Red - No attendance
                            ContextCompat.getColor(context, android.R.color.holo_red_light)
                        }
                        CalendarDayStatus.FUTURE -> {
                            // Gray - Future dates
                            ContextCompat.getColor(context, android.R.color.darker_gray)
                        }
                        else -> {
                            // White/default for unknown
                            Color.WHITE
                        }
                    }
                    
                    binding.root.setCardBackgroundColor(backgroundColor)
                    
                    // Set text color for better contrast
                    val textColor = when (day.status) {
                        CalendarDayStatus.FUTURE -> Color.WHITE
                        else -> Color.BLACK
                    }
                    binding.tvDayNumber.setTextColor(textColor)
                    
                    // Enable click for non-empty, non-future days
                    binding.root.isClickable = day.status != CalendarDayStatus.FUTURE
                    binding.root.setOnClickListener {
                        if (day.status != CalendarDayStatus.EMPTY && 
                            day.status != CalendarDayStatus.FUTURE) {
                            onDayClick(day)
                        }
                    }
                }
            }
        }
    }
    
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CalendarDay>() {
            override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
                return oldItem.day == newItem.day
            }
            
            override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
                return oldItem == newItem
            }
        }
    }
}
