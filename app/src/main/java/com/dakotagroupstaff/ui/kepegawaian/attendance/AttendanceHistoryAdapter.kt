package com.dakotagroupstaff.ui.kepegawaian.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.local.entity.AttendanceHistoryEntity
import com.dakotagroupstaff.databinding.ItemAttendanceHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying attendance history list
 * Shows individual attendance records (M or K) as separate cards
 */
class AttendanceHistoryAdapter : ListAdapter<AttendanceHistoryEntity, AttendanceHistoryAdapter.ViewHolder>(
    DiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAttendanceHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))

        fun bind(attendance: AttendanceHistoryEntity) {
            val context = binding.root.context
            
            val keterangan = attendance.keterangan.uppercase()
            
            // Determine attendance type and style based on KETERANGAN
            val attendanceType: String
            val badgeColor: Int
            val isCheckIn: Boolean // For determining which time/location to show
            
            when (keterangan) {
                "M" -> {
                    attendanceType = "Absen Masuk"
                    badgeColor = ContextCompat.getColor(context, R.color.success)
                    isCheckIn = true
                }
                "K" -> {
                    attendanceType = "Absen Keluar"
                    badgeColor = ContextCompat.getColor(context, R.color.error)
                    isCheckIn = false
                }
                "HM" -> {
                    attendanceType = "Absen Masuk (WFH)"
                    badgeColor = android.graphics.Color.parseColor("#1976D2") // Blue for WFH Masuk
                    isCheckIn = true
                }
                "HK" -> {
                    attendanceType = "Absen Keluar (WFH)"
                    badgeColor = android.graphics.Color.parseColor("#0D47A1") // Darker blue for WFH Keluar
                    isCheckIn = false
                }
                "H" -> {
                    attendanceType = "Hadir"
                    badgeColor = ContextCompat.getColor(context, R.color.success)
                    isCheckIn = true
                }
                "O" -> {
                    attendanceType = "Out"
                    badgeColor = ContextCompat.getColor(context, R.color.error)
                    isCheckIn = false
                }
                else -> {
                    // Fallback using isCheckIn logic based on M
                    isCheckIn = keterangan.contains("M") || keterangan.contains("H")
                    attendanceType = if (isCheckIn) "Absen Masuk" else "Absen Keluar"
                    badgeColor = if (isCheckIn) {
                        ContextCompat.getColor(context, R.color.success)
                    } else {
                        ContextCompat.getColor(context, R.color.error)
                    }
                }
            }
            
            // Set attendance type with appropriate background color
            binding.tvAttendanceType.text = attendanceType
            binding.tvAttendanceType.setBackgroundColor(badgeColor)
            
            // Set attendance date (ABS_TANGGAL)
            val date = Date(attendance.absTanggal)
            binding.tvAttendanceDate.text = dateFormat.format(date)
            
            // Set attendance time
            // For In: use ABS_IN_TIME
            // For Out: use ABS_OUT_TIME
            val time = if (isCheckIn) {
                attendance.absInTime ?: "-"
            } else {
                attendance.absOutTime ?: "-"
            }
            binding.tvAttendanceTime.text = time
            
            // Set attendance location
            // For In: use ABS_IN_LOCATION
            // For Out: use ABS_OUT_LOCATION
            val location = if (isCheckIn) {
                attendance.absInLocation ?: "Lokasi tidak tersedia"
            } else {
                attendance.absOutLocation ?: "Lokasi tidak tersedia"
            }
            binding.tvAttendanceLocation.text = location
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<AttendanceHistoryEntity>() {
            override fun areItemsTheSame(
                oldItem: AttendanceHistoryEntity,
                newItem: AttendanceHistoryEntity
            ): Boolean {
                return oldItem.absId == newItem.absId
            }

            override fun areContentsTheSame(
                oldItem: AttendanceHistoryEntity,
                newItem: AttendanceHistoryEntity
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}