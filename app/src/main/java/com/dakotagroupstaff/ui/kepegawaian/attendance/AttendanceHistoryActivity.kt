package com.dakotagroupstaff.ui.kepegawaian.attendance

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.databinding.ActivityAttendanceHistoryBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*

class AttendanceHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAttendanceHistoryBinding
    private val viewModel: AttendanceViewModel by viewModel()
    private val userPreferences: UserPreferences by inject()
    
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var historyAdapter: AttendanceHistoryAdapter
    private val calendar = Calendar.getInstance()
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    
    private var currentNip: String? = null
    private var currentPt: String? = null
    private var selectedDate: Long? = null // Track selected date for filtering
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupCalendar()
        setupHistoryList()
        setupObservers()
        setupListeners()
        
        loadUserSession()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { day ->
            // Handle day click - filter attendance by selected date
            onCalendarDayClicked(day)
        }
        
        binding.rvCalendar.apply {
            layoutManager = GridLayoutManager(this@AttendanceHistoryActivity, 7)
            adapter = calendarAdapter
        }
        
        updateCalendarView()
    }
    
    private fun setupHistoryList() {
        historyAdapter = AttendanceHistoryAdapter()
        
        binding.rvAttendanceHistory.apply {
            layoutManager = LinearLayoutManager(this@AttendanceHistoryActivity)
            adapter = historyAdapter
        }
    }
    
    private fun setupObservers() {
        // Observe attendance history
        viewModel.attendanceHistory.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Hanya show loading jika data cache kosong
                    if (calendarAdapter.currentList.isEmpty()) {
                        binding.loadingOverlay.visibility = View.VISIBLE
                    }
                }
                is Result.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    
                    // Update calendar
                    updateCalendarWithData()
                    
                    // Update history list based on selected date
                    updateHistoryList(result.data)
                }
                is Result.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    // Tetap tampilkan calendar dengan cache data yang ada
                    updateCalendarWithData()
                }
                null -> {
                    binding.loadingOverlay.visibility = View.GONE
                }
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            selectedDate = null // Reset filter when changing month
            updateCalendarView()
            loadAttendanceData()
        }
        
        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            selectedDate = null // Reset filter when changing month
            updateCalendarView()
            loadAttendanceData()
        }
    }
    
    private fun loadUserSession() {
        // Ambil NIP dan PT dari Intent (dikirim dari AttendanceActivity)
        val intentNip = intent.getStringExtra("NIP")
        val intentPt = intent.getStringExtra("PT")
        
        if (intentNip != null && intentPt != null) {
            // Gunakan data dari Intent
            currentNip = intentNip
            currentPt = intentPt
            
            // Data sudah di-load di AttendanceActivity, langsung tampilkan dari cache
            loadAttendanceDataFromCache()
        } else {
            // Fallback: load dari UserPreferences jika Intent kosong
            lifecycleScope.launch {
                val session = userPreferences.getSession().first()
                currentNip = session.nip
                currentPt = session.pt
                
                loadAttendanceData()
            }
        }
    }
    
    private fun loadAttendanceData() {
        currentNip?.let { nip ->
            currentPt?.let { pt ->
                viewModel.loadAttendanceHistory(pt, nip)
            }
        }
    }
    
    /**
     * Load attendance data dari cache (tanpa network call)
     * Digunakan saat data sudah di-load di AttendanceActivity
     */
    private fun loadAttendanceDataFromCache() {
        // Trigger load dari cache untuk memastikan data tersedia di ViewModel
        currentNip?.let { nip ->
            currentPt?.let { pt ->
                // Ini akan load dari Room Database cache, bukan dari network
                viewModel.loadAttendanceHistory(pt, nip)
            }
        }
    }
    
    private fun updateCalendarView() {
        binding.tvMonthYear.text = monthYearFormat.format(calendar.time)
        
        val days = generateCalendarDays()
        calendarAdapter.submitList(days)
    }
    
    private fun updateCalendarWithData() {
        val days = generateCalendarDays()
        calendarAdapter.submitList(days)
    }
    
    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)
        
        val maxDaysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        
        // Add empty days for alignment
        for (i in 0 until firstDayOfWeek) {
            days.add(CalendarDay(0, CalendarDayStatus.EMPTY))
        }
        
        // Add actual days
        val today = Calendar.getInstance()
        for (day in 1..maxDaysInMonth) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, day)
            val dayTimestamp = tempCalendar.timeInMillis
            
            val status = when {
                // Future dates
                dayTimestamp > today.timeInMillis -> CalendarDayStatus.FUTURE
                // Check attendance status
                else -> {
                    val attendanceStatus = viewModel.getAttendanceStatus(dayTimestamp)
                    when (attendanceStatus) {
                        "complete" -> CalendarDayStatus.COMPLETE
                        "partial" -> CalendarDayStatus.PARTIAL
                        "absent" -> CalendarDayStatus.ABSENT
                        else -> CalendarDayStatus.UNKNOWN
                    }
                }
            }
            
            days.add(CalendarDay(day, status))
        }
        
        return days
    }
    
    /**
     * Handle calendar day click
     * Filter attendance list by selected date
     */
    private fun onCalendarDayClicked(day: CalendarDay) {
        // Ignore clicks on empty cells or future dates
        if (day.status == CalendarDayStatus.EMPTY || day.status == CalendarDayStatus.FUTURE) {
            return
        }
        
        // Calculate timestamp for selected day
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, day.day)
        tempCalendar.set(Calendar.HOUR_OF_DAY, 0)
        tempCalendar.set(Calendar.MINUTE, 0)
        tempCalendar.set(Calendar.SECOND, 0)
        tempCalendar.set(Calendar.MILLISECOND, 0)
        selectedDate = tempCalendar.timeInMillis
        
        // Refresh history list with filter
        viewModel.attendanceHistory.value?.let { result ->
            if (result is Result.Success) {
                updateHistoryList(result.data)
                
                // Scroll to top of history list to show filtered results
                binding.rvAttendanceHistory.scrollToPosition(0)
            }
        }
    }
    
    /**
     * Update history list based on selected date filter
     * If no date selected, show all records
     * If date selected, show only records for that date
     */
    private fun updateHistoryList(allData: List<com.dakotagroupstaff.data.local.entity.AttendanceHistoryEntity>) {
        val filteredData = if (selectedDate != null) {
            // Filter by selected date
            val selectedDateCal = Calendar.getInstance()
            selectedDateCal.timeInMillis = selectedDate!!
            selectedDateCal.set(Calendar.HOUR_OF_DAY, 0)
            selectedDateCal.set(Calendar.MINUTE, 0)
            selectedDateCal.set(Calendar.SECOND, 0)
            selectedDateCal.set(Calendar.MILLISECOND, 0)
            val selectedDayStart = selectedDateCal.timeInMillis
            
            selectedDateCal.set(Calendar.HOUR_OF_DAY, 23)
            selectedDateCal.set(Calendar.MINUTE, 59)
            selectedDateCal.set(Calendar.SECOND, 59)
            selectedDateCal.set(Calendar.MILLISECOND, 999)
            val selectedDayEnd = selectedDateCal.timeInMillis
            
            allData.filter { attendance ->
                attendance.absTanggal in selectedDayStart..selectedDayEnd
            }.sortedByDescending { it.absTanggal }
        } else {
            // Show all data, sorted by date descending (newest first)
            allData.sortedByDescending { it.absTanggal }
        }
        
        historyAdapter.submitList(filteredData)
        
        // Update header text to show filter status
        binding.tvHistoryHeader.text = if (selectedDate != null) {
            val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
            "Riwayat Absensi - ${dateFormat.format(Date(selectedDate!!))}"
        } else {
            "Riwayat Detail Absensi"
        }
    }
}

/**
 * Calendar Day data class
 */
data class CalendarDay(
    val day: Int,
    val status: CalendarDayStatus
)

/**
 * Calendar Day Status enum
 */
enum class CalendarDayStatus {
    EMPTY,      // Empty cell for alignment
    COMPLETE,   // Both check in and check out
    PARTIAL,    // Only check in
    ABSENT,     // No attendance
    FUTURE,     // Future dates
    UNKNOWN     // Unknown status
}
