package com.dakotagroupstaff.ui.kepegawaian.leave

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.remote.response.LeaveType
import com.dakotagroupstaff.databinding.ActivityLeaveSubmissionBinding
import com.dakotagroupstaff.data.local.pref.SessionManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*

class LeaveSubmissionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLeaveSubmissionBinding
    private val viewModel: LeaveViewModel by viewModel()
    
    private val sessionManager: SessionManager by inject()
    
    private var atasan1Nip: String = ""
    private var atasan1Nama: String = ""
    private var atasan2Nip: String = ""
    private var atasan2Nama: String = ""
    private var currentLeaveBalance: Int = 0
    private var superAtasanList: List<com.dakotagroupstaff.data.remote.response.SuperAtasanData> = emptyList()
    private var needSupervisorSelection: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaveSubmissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupObservers()
        setupListeners()
        
        loadEmployeeData()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Ajukan Cuti/Izin"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun setupObservers() {
        // Observe selected leave type
        viewModel.selectedLeaveType.observe(this) { leaveType ->
            binding.btnLeaveType.text = leaveType?.displayName ?: "Pilih Jenis Cuti/Izin"
            
            // Show/hide date fields based on leave type
            val showDates = leaveType?.code !in listOf("DT", "PC", "MP")
            binding.layoutDates.visibility = if (showDates) View.VISIBLE else View.GONE
        }
        
        // Observe start date
        viewModel.startDate.observe(this) { date ->
            if (date != null) {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                binding.btnStartDate.text = dateFormat.format(date)
            } else {
                binding.btnStartDate.text = "Tanggal Mulai"
            }
        }
        
        // Observe end date
        viewModel.endDate.observe(this) { date ->
            if (date != null) {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                binding.btnEndDate.text = dateFormat.format(date)
            } else {
                binding.btnEndDate.text = "Tanggal Akhir"
            }
        }
        
        // Observe leave balance
        viewModel.leaveBalance.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    currentLeaveBalance = result.data.saldoCuti.toIntOrNull() ?: 0
                    binding.tvLeaveBalance.text = "Sisa Cuti: $currentLeaveBalance hari"
                }
                else -> {
                    binding.tvLeaveBalance.text = "Sisa Cuti: -"
                }
            }
        }
        
        // Observe submit result
        viewModel.submitResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSubmit.isEnabled = false
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(this, "Pengajuan berhasil dikirim", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
                null -> {
                    // Reset state - no action needed
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                }
            }
        }
        
        // Observe Super Atasan list
        viewModel.superAtasanList.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    superAtasanList = result.data
                    updateSupervisorUI()
                }
                is Result.Error -> {
                    Toast.makeText(this, "Gagal memuat daftar atasan: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnLeaveType.setOnClickListener {
            showLeaveTypeDialog()
        }
        
        binding.btnStartDate.setOnClickListener {
            showDatePicker(isStartDate = true)
        }
        
        binding.btnEndDate.setOnClickListener {
            val startDate = viewModel.startDate.value
            if (startDate == null) {
                Toast.makeText(this, "Pilih tanggal mulai terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showDatePicker(isStartDate = false)
        }
        
        binding.btnSubmit.setOnClickListener {
            submitLeaveRequest()
        }
        
        // Description text change
        binding.etDescription.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.setDescription(s.toString())
            }
        })
    }
    
    private fun loadEmployeeData() {
        val nip = sessionManager.getNip() ?: return
        val pt = sessionManager.getPt() ?: return
        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
        
        // Load leave balance
        viewModel.getLeaveBalance(pt, nip, currentYear)
        
        // Get atasan info from session
        val atasan1FromSession = sessionManager.getAtasan1()
        val atasan2FromSession = sessionManager.getAtasan2()
        
        // Check if atasan data is empty or null
        needSupervisorSelection = atasan1FromSession.isNullOrEmpty() || atasan2FromSession.isNullOrEmpty()
        
        if (needSupervisorSelection) {
            // Fetch Super Atasan list from API
            viewModel.getSuperAtasan(pt)
            
            // Show selection UI
            binding.tvAtasan1.text = "Atasan 1: (Pilih Atasan)"
            binding.tvAtasan2.text = "Atasan 2: (Pilih Atasan)"
            binding.btnSelectAtasan1.visibility = View.VISIBLE
            binding.btnSelectAtasan2.visibility = View.VISIBLE
        } else {
            // Use atasan from session
            atasan1Nip = atasan1FromSession!!
            atasan2Nip = atasan2FromSession!!
            
            // Show static text
            binding.tvAtasan1.text = "Atasan 1: $atasan1Nip"
            binding.tvAtasan2.text = "Atasan 2: $atasan2Nip"
            binding.btnSelectAtasan1.visibility = View.GONE
            binding.btnSelectAtasan2.visibility = View.GONE
        }
    }
    
    private fun showLeaveTypeDialog() {
        val leaveTypes = LeaveType.values()
        val items = leaveTypes.map { it.displayName }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Pilih Jenis Cuti/Izin")
            .setItems(items) { _, which ->
                viewModel.selectLeaveType(leaveTypes[which])
            }
            .show()
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        
        // Set initial date
        val initialDate = if (isStartDate) {
            viewModel.startDate.value ?: Date()
        } else {
            viewModel.endDate.value ?: viewModel.startDate.value ?: Date()
        }
        calendar.time = initialDate
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                val selectedDate = selectedCalendar.time
                
                if (isStartDate) {
                    viewModel.setStartDate(selectedDate)
                } else {
                    viewModel.setEndDate(selectedDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set min/max dates based on leave type
        val minDate = viewModel.getMinimumDate()
        val maxDate = viewModel.getMaximumDate()
        
        if (!isStartDate) {
            // For end date, minimum is start date
            viewModel.startDate.value?.let {
                datePickerDialog.datePicker.minDate = it.time
            }
        } else {
            datePickerDialog.datePicker.minDate = minDate.time
        }
        
        maxDate?.let {
            datePickerDialog.datePicker.maxDate = it.time
        }
        
        datePickerDialog.show()
    }
    
    private fun submitLeaveRequest() {
        val nip = sessionManager.getNip() ?: return
        val pt = sessionManager.getPt() ?: return
        
        // Determine atasan values
        val atasan1: String
        val atasan2: String
        
        if (needSupervisorSelection) {
            // Use selected super atasan
            if (atasan1Nip.isEmpty()) {
                Toast.makeText(this, "Pilih Atasan 1 terlebih dahulu", Toast.LENGTH_SHORT).show()
                return
            }
            if (atasan2Nip.isEmpty()) {
                Toast.makeText(this, "Pilih Atasan 2 terlebih dahulu", Toast.LENGTH_SHORT).show()
                return
            }
            atasan1 = atasan1Nip
            atasan2 = atasan2Nip
        } else {
            // Use hardcoded atasan from session or fallback
            atasan1 = sessionManager.getAtasan1() ?: "0010807032"
            atasan2 = sessionManager.getAtasan2() ?: "0010807032"
        }
        
        viewModel.submitLeaveRequest(pt, nip, atasan1, atasan2, currentLeaveBalance)
    }
    
    private fun updateSupervisorUI() {
        // Update button click listeners for supervisor selection
        binding.btnSelectAtasan1.setOnClickListener {
            showSupervisorSelectionDialog(isAtasan1 = true)
        }
        
        binding.btnSelectAtasan2.setOnClickListener {
            showSupervisorSelectionDialog(isAtasan1 = false)
        }
    }
    
    private fun showSupervisorSelectionDialog(isAtasan1: Boolean) {
        if (superAtasanList.isEmpty()) {
            Toast.makeText(this, "Daftar atasan belum dimuat", Toast.LENGTH_SHORT).show()
            return
        }
        
        val items = superAtasanList.map { "${it.nama} (${it.nip})" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle(if (isAtasan1) "Pilih Atasan 1" else "Pilih Atasan 2")
            .setItems(items) { _, which ->
                val selected = superAtasanList[which]
                
                if (isAtasan1) {
                    atasan1Nip = selected.nip
                    atasan1Nama = selected.nama
                    binding.tvAtasan1.text = "Atasan 1: ${selected.nama} (${selected.nip})"
                } else {
                    atasan2Nip = selected.nip
                    atasan2Nama = selected.nama
                    binding.tvAtasan2.text = "Atasan 2: ${selected.nama} (${selected.nip})"
                }
            }
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
