package com.dakotagroupstaff.ui.kepegawaian.approval

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.databinding.ActivityApprovalBinding
import com.dakotagroupstaff.di.Injection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ApprovalActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityApprovalBinding
    private lateinit var userPreferences: UserPreferences
    private lateinit var approvalAdapter: ApprovalAdapter
    private var currentNip: String? = null
    private var currentPt: String? = null
    
    private val viewModel: ApprovalViewModel by viewModels {
        ApprovalViewModelFactory(
            Injection.provideLeaveRepository(applicationContext)
        )
    }
    
    // Activity result launcher for approval detail
    private val approvalDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh pending approvals after approval/rejection
            refreshPendingApprovals()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApprovalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userPreferences = UserPreferences.getInstance(dataStore)
        
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        loadUserSessionAndCheck()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        approvalAdapter = ApprovalAdapter { approvalItem ->
            // Open detail activity
            val intent = Intent(this, ApprovalDetailActivity::class.java)
            intent.putExtra(ApprovalDetailActivity.EXTRA_APPROVAL_ID, approvalItem.idCuti)
            intent.putExtra(ApprovalDetailActivity.EXTRA_NIP_PENGAJU, approvalItem.nipPengaju)
            intent.putExtra(ApprovalDetailActivity.EXTRA_NAMA_PENGAJU, approvalItem.namaPengaju)
            intent.putExtra(ApprovalDetailActivity.EXTRA_TGL_MULAI, approvalItem.tglMulai)
            intent.putExtra(ApprovalDetailActivity.EXTRA_TGL_AKHIR, approvalItem.tglAkhir)
            intent.putExtra(ApprovalDetailActivity.EXTRA_STATUS, approvalItem.status)
            intent.putExtra(ApprovalDetailActivity.EXTRA_KETERANGAN, approvalItem.keterangan)
            intent.putExtra(ApprovalDetailActivity.EXTRA_SALDO_CUTI, approvalItem.saldoCuti)
            intent.putExtra(ApprovalDetailActivity.EXTRA_ATASAN1_NIP, approvalItem.atasan1Nip)
            intent.putExtra(ApprovalDetailActivity.EXTRA_ATASAN2_NIP, approvalItem.atasan2Nip)
            intent.putExtra(ApprovalDetailActivity.EXTRA_SURAT, approvalItem.surat)
            intent.putExtra(ApprovalDetailActivity.EXTRA_TOTAL_DAYS, approvalItem.totalDays)
            approvalDetailLauncher.launch(intent)
        }
        
        binding.rvApprovalList.apply {
            layoutManager = LinearLayoutManager(this@ApprovalActivity)
            adapter = approvalAdapter
        }
        
        // Setup Pull to Refresh
        binding.swipeRefresh.setOnRefreshListener {
            refreshPendingApprovals()
        }
    }
    
    private fun setupObservers() {
        viewModel.supervisorStatus.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    handleSupervisorCheck(result.data)
                }
                is Result.Error -> {
                    showLoading(false)
                    showError(result.message)
                }
            }
        }

        
        viewModel.pendingApprovals.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Show refresh indicator if refreshing
                    // Don't show loading screen as we're already in approval list view
                }
                is Result.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                    displayApprovalList(result.data)
                }
                is Result.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    showError(result.message)
                    displayEmptyState()
                }
            }
        }
    }
    
    /**
     * Load user session from DataStore and check supervisor status
     */
    private fun loadUserSessionAndCheck() {
        lifecycleScope.launch {
            val session = userPreferences.getSession().first()
            
            currentNip = session.nip
            currentPt = session.pt
            
            if (currentNip.isNullOrBlank()) {
                showError("NIP tidak ditemukan. Silakan login kembali.")
                finish()
                return@launch
            }
            
            // Check supervisor status with retrieved NIP and PT
            val pt = currentPt ?: "C"
            val nip = currentNip!!
            
            viewModel.checkSupervisorStatus(pt, nip)
        }
    }
    
    private fun handleSupervisorCheck(supervisorData: com.dakotagroupstaff.data.remote.response.SupervisorCheckData) {
        if (supervisorData.isActuallySupervisor()) {
            // User is a supervisor - show approval list
            showSupervisorView(supervisorData)
        } else {
            // User is not a supervisor - show access denied
            showAccessDenied()
        }
    }
    
    private fun showSupervisorView(supervisorData: com.dakotagroupstaff.data.remote.response.SupervisorCheckData) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutAccessDenied.visibility = View.GONE
        binding.layoutApprovalList.visibility = View.VISIBLE
        
        // Show supervisor info
        val subordinateCount = supervisorData.subordinatesCount ?: 0
        binding.tvSupervisorInfo.text = "Anda adalah atasan dari $subordinateCount karyawan"
        
        // Load pending approval list
        val pt = currentPt ?: "C"
        val nip = currentNip!!
        viewModel.getPendingApprovals(pt, nip)
    }
    
    private fun displayApprovalList(approvals: List<com.dakotagroupstaff.data.remote.response.PendingApprovalData>) {
        if (approvals.isEmpty()) {
            displayEmptyState()
        } else {
            binding.tvPendingCount.text = approvals.size.toString()
            binding.rvApprovalList.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
            approvalAdapter.submitList(approvals)
        }
    }
    
    private fun displayEmptyState() {
        binding.tvPendingCount.text = "0"
        binding.rvApprovalList.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = "Tidak ada pengajuan yang perlu disetujui"
    }
    
    private fun showAccessDenied() {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutApprovalList.visibility = View.GONE
        binding.layoutAccessDenied.visibility = View.VISIBLE
        
        binding.btnBackToMenu.setOnClickListener {
            finish()
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.layoutApprovalList.visibility = View.GONE
        binding.layoutAccessDenied.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Show access denied screen on error
        showAccessDenied()
    }
    
    private fun refreshPendingApprovals() {
        val pt = currentPt ?: "C"
        val nip = currentNip ?: return
        viewModel.getPendingApprovals(pt, nip)
    }
}
