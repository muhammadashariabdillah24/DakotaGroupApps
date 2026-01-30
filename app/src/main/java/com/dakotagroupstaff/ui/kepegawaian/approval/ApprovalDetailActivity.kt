package com.dakotagroupstaff.ui.kepegawaian.approval

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.remote.response.LeaveStatusHelper
import com.dakotagroupstaff.databinding.ActivityApprovalDetailBinding
import com.dakotagroupstaff.di.Injection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ApprovalDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityApprovalDetailBinding
    private lateinit var userPreferences: UserPreferences
    private var currentNip: String? = null
    private var currentPt: String? = null
    
    private val viewModel: ApprovalViewModel by viewModels {
        ApprovalViewModelFactory(
            Injection.provideLeaveRepository(applicationContext)
        )
    }
    
    // Data from intent
    private var approvalId: String = ""
    private var nipPengaju: String = ""
    private var namaPengaju: String = ""
    private var tglMulai: String = ""
    private var tglAkhir: String = ""
    private var status: String = ""
    private var keterangan: String = ""
    private var saldoCuti: String = ""
    private var atasan1Nip: String = ""
    private var atasan2Nip: String = ""
    private var surat: String = ""
    private var totalDays: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApprovalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userPreferences = UserPreferences.getInstance(dataStore)
        
        getIntentData()
        loadUserSession()
        setupUI()
        setupObservers()
        setupButtons()
    }
    
    private fun getIntentData() {
        approvalId = intent.getStringExtra(EXTRA_APPROVAL_ID) ?: ""
        nipPengaju = intent.getStringExtra(EXTRA_NIP_PENGAJU) ?: ""
        namaPengaju = intent.getStringExtra(EXTRA_NAMA_PENGAJU) ?: ""
        tglMulai = intent.getStringExtra(EXTRA_TGL_MULAI) ?: ""
        tglAkhir = intent.getStringExtra(EXTRA_TGL_AKHIR) ?: ""
        status = intent.getStringExtra(EXTRA_STATUS) ?: ""
        keterangan = intent.getStringExtra(EXTRA_KETERANGAN) ?: ""
        saldoCuti = intent.getStringExtra(EXTRA_SALDO_CUTI) ?: ""
        atasan1Nip = intent.getStringExtra(EXTRA_ATASAN1_NIP) ?: ""
        atasan2Nip = intent.getStringExtra(EXTRA_ATASAN2_NIP) ?: ""
        surat = intent.getStringExtra(EXTRA_SURAT) ?: ""
        totalDays = intent.getIntExtra(EXTRA_TOTAL_DAYS, 0)
    }
    
    private fun loadUserSession() {
        lifecycleScope.launch {
            val session = userPreferences.getSession().first()
            currentNip = session.nip
            currentPt = session.pt
        }
    }
    
    private fun setupUI() {
        // Set employee name
        binding.tvEmployeeName.text = namaPengaju
        
        // Set status with proper formatting
        binding.tvStatus.text = LeaveStatusHelper.getStatusName(status).uppercase()
        
        // Format and set dates + calculate total days
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        
        var calculatedDays = totalDays // Default to passed value
        
        try {
            val startDate = inputFormat.parse(tglMulai)
            val endDate = inputFormat.parse(tglAkhir)
            
            binding.tvStartDate.text = startDate?.let { outputFormat.format(it) } ?: tglMulai
            binding.tvEndDate.text = endDate?.let { outputFormat.format(it) } ?: tglAkhir
            
            // Calculate total days between start and end dates
            if (startDate != null && endDate != null) {
                val diffInMillis = endDate.time - startDate.time
                calculatedDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt() + 1 // +1 to include both start and end dates
                binding.tvTotalDays.text = "$calculatedDays hari"
            } else {
                // Fallback to passed totalDays if date parsing fails
                binding.tvTotalDays.text = "$totalDays hari"
            }
        } catch (e: Exception) {
            binding.tvStartDate.text = tglMulai
            binding.tvEndDate.text = tglAkhir
            // Fallback to passed totalDays if date parsing fails
            binding.tvTotalDays.text = "$totalDays hari"
        }
        
        // Set pengaju info
        binding.tvPengajuInfo.text = "$nipPengaju - $namaPengaju"
        
        // Set keterangan
        binding.tvKeterangan.text = keterangan.ifBlank { "Tidak ada keterangan" }
        
        // Set saldo cuti
        binding.tvSaldoCuti.text = "$saldoCuti hari"
        
        // Validate and disable "Potong Cuti" if necessary
        validatePotongCutiOption(calculatedDays)
        
        // Set bukti foto
        if ((status == "S" || status == "I") && surat.isNotBlank() && surat != "0") {
            binding.tvBuktiFoto.text = "Lihat Foto"
            binding.tvBuktiFoto.setOnClickListener {
                // TODO: Show image in dialog or new activity
                Toast.makeText(this, "Foto bukti: $surat", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.tvBuktiFoto.text = "-"
            binding.tvBuktiFoto.setTextColor(getColor(android.R.color.black))
        }
        
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }
        
        // Handle click outside to close
        binding.root.setOnClickListener {
            finish()
        }
        
        // Prevent bottom sheet from closing when clicking inside
        binding.bottomSheet.setOnClickListener {
            // Do nothing - prevent propagation to background
        }
    }
    
    private fun setupObservers() {
        // Observe approval result
        viewModel.approvalResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "Berhasil menyetujui", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is Result.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "Gagal menyetujui: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        
        // Observe rejection result
        viewModel.rejectionResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "Berhasil menolak", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is Result.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "Gagal menolak: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun setupButtons() {
        // Approve button
        binding.btnApprove.setOnClickListener {
            showApproveConfirmation()
        }
        
        // Reject button
        binding.btnReject.setOnClickListener {
            showRejectConfirmation()
        }
    }
    
    private fun showApproveConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin ingin menyetujui pengajuan ini?")
            .setPositiveButton("Ya") { _, _ ->
                approveLeaveRequest()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun showRejectConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin ingin menolak pengajuan ini?")
            .setPositiveButton("Ya") { _, _ ->
                rejectLeaveRequest()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun approveLeaveRequest() {
        val pt = currentPt ?: "C"
        val nip = currentNip ?: run {
            Toast.makeText(this, "NIP tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get selected deduction type from RadioGroup
        val selectedDeduction = getSelectedDeduction()
        
        // Call ViewModel to approve
        // ViewModel will automatically detect if ATASAN1 == ATASAN2 and call appropriate API
        viewModel.approveLeaveRequest(
            pt = pt,
            nipAtasan = nip,
            leaveId = approvalId,
            atasan1Nip = atasan1Nip,
            atasan2Nip = atasan2Nip,
            approvalValue = "Y",
            potongGaji = selectedDeduction.potongGaji,
            potongCuti = selectedDeduction.potongCuti,
            dispensasi = selectedDeduction.dispensasi
        )
    }
    
    private fun rejectLeaveRequest() {
        val pt = currentPt ?: "C"
        val nip = currentNip ?: run {
            Toast.makeText(this, "NIP tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Call ViewModel to reject
        viewModel.rejectLeaveRequest(
            pt = pt,
            nipAtasan = nip,
            leaveId = approvalId
        )
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutActionButtons.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
    
    /**
     * Get selected deduction type from RadioGroup
     * Returns a DeductionSelection object with Y/N values
     */
    private fun getSelectedDeduction(): DeductionSelection {
        return when (binding.radioGroupDeduction.checkedRadioButtonId) {
            binding.rbPotongGaji.id -> DeductionSelection(
                potongGaji = "Y",
                potongCuti = "N",
                dispensasi = "N"
            )
            binding.rbPotongCuti.id -> DeductionSelection(
                potongGaji = "N",
                potongCuti = "Y",
                dispensasi = "N"
            )
            binding.rbDispensasi.id -> DeductionSelection(
                potongGaji = "N",
                potongCuti = "N",
                dispensasi = "Y"
            )
            else -> DeductionSelection(
                potongGaji = "Y", // Default to Potong Gaji
                potongCuti = "N",
                dispensasi = "N"
            )
        }
    }
    
    /**
     * Validate if "Potong Cuti" option can be selected
     * Disables the option if total days exceeds available leave balance
     */
    private fun validatePotongCutiOption(totalDays: Int) {
        val saldoCutiInt = saldoCuti.toIntOrNull() ?: 0
        
        // Check if days exceed available balance
        if (totalDays > saldoCutiInt) {
            // Disable "Potong Cuti" radio button
            binding.rbPotongCuti.isEnabled = false
            binding.rbPotongCuti.alpha = 0.5f // Visual indicator that it's disabled
            
            // Show warning message
            binding.tvWarningPotongCuti.visibility = View.VISIBLE
            
            // If "Potong Cuti" was selected, switch to "Potong Gaji"
            if (binding.radioGroupDeduction.checkedRadioButtonId == binding.rbPotongCuti.id) {
                binding.rbPotongGaji.isChecked = true
            }
        } else {
            // Enable "Potong Cuti" radio button
            binding.rbPotongCuti.isEnabled = true
            binding.rbPotongCuti.alpha = 1.0f
            
            // Hide warning message
            binding.tvWarningPotongCuti.visibility = View.GONE
        }
    }
    
    /**
     * Data class to hold deduction selection
     */
    private data class DeductionSelection(
        val potongGaji: String,
        val potongCuti: String,
        val dispensasi: String
    )
    
    companion object {
        const val EXTRA_APPROVAL_ID = "extra_approval_id"
        const val EXTRA_NIP_PENGAJU = "extra_nip_pengaju"
        const val EXTRA_NAMA_PENGAJU = "extra_nama_pengaju"
        const val EXTRA_TGL_MULAI = "extra_tgl_mulai"
        const val EXTRA_TGL_AKHIR = "extra_tgl_akhir"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_KETERANGAN = "extra_keterangan"
        const val EXTRA_SALDO_CUTI = "extra_saldo_cuti"
        const val EXTRA_ATASAN1_NIP = "extra_atasan1_nip"
        const val EXTRA_ATASAN2_NIP = "extra_atasan2_nip"
        const val EXTRA_SURAT = "extra_surat"
        const val EXTRA_TOTAL_DAYS = "extra_total_days"
    }
}
