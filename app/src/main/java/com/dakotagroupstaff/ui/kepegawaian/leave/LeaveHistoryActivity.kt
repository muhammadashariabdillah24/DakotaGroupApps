package com.dakotagroupstaff.ui.kepegawaian.leave

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.entity.LeaveDetailsEntity
import com.dakotagroupstaff.data.local.pref.SessionManager
import com.dakotagroupstaff.databinding.ActivityLeaveHistoryBinding
import com.dakotagroupstaff.util.ErrorMessageHelper
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

class LeaveHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLeaveHistoryBinding
    private val viewModel: LeaveViewModel by viewModel()
    private lateinit var adapter: LeaveHistoryAdapter
    
    private val sessionManager: SessionManager by inject()
    
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var shouldRefreshOnResume = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaveHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        // Initialize year display
        binding.tvYear.text = currentYear.toString()
        
        loadLeaveData()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Riwayat Cuti & Izin"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun setupRecyclerView() {
        adapter = LeaveHistoryAdapter()
        binding.rvLeaveHistory.apply {
            layoutManager = LinearLayoutManager(this@LeaveHistoryActivity)
            adapter = this@LeaveHistoryActivity.adapter
        }
    }
    
    private fun setupObservers() {
        // Observe leave balance
        viewModel.leaveBalance.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Balance loading handled by swipe refresh
                }
                is Result.Success -> {
                    displayLeaveBalance(result.data)
                }
                is Result.Error -> {
                    binding.tvBalanceInfo.text = "Gagal memuat saldo cuti"
                }
            }
        }
        
        // Observe leave details
        viewModel.leaveDetails.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Result.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    displayLeaveHistory(result.data)
                }
                is Result.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this, ErrorMessageHelper.getLeaveBalanceLoadError(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadLeaveData(forceRefresh = true)
        }
        
        binding.btnSubmitLeave.setOnClickListener {
            // Navigate to leave submission activity
            shouldRefreshOnResume = true
            val intent = android.content.Intent(this, LeaveSubmissionActivity::class.java)
            startActivity(intent)
        }
        
        binding.tvYear.setOnClickListener {
            // Show year picker dialog
            showYearPicker()
        }
    }
    
    private fun loadLeaveData(forceRefresh: Boolean = false) {
        val nip = sessionManager.getNip() ?: return
        val pt = sessionManager.getPt() ?: return
        
        viewModel.getLeaveBalance(pt, nip, currentYear.toString(), forceRefresh)
        viewModel.getLeaveDetails(pt, nip, currentYear.toString(), forceRefresh)
    }
    
    private fun displayLeaveBalance(balance: com.dakotagroupstaff.data.local.entity.LeaveBalanceEntity) {
        binding.apply {
            tvBalanceInfo.text = "${balance.saldoCuti} hari tersisa"
            tvTotalLeave.text = "Total: ${balance.jumlahCuti} hari"
            tvUsedLeave.text = "Terpakai: ${balance.cutiTerpakai} hari"
        }
    }
    
    private fun displayLeaveHistory(leaveList: List<LeaveDetailsEntity>) {
        if (leaveList.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvLeaveHistory.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvLeaveHistory.visibility = View.VISIBLE
            adapter.submitList(leaveList)
        }
    }
    
    private fun showYearPicker() {
        val years = (2020..2030).toList()
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pilih Tahun")
            .setItems(years.map { it.toString() }.toTypedArray()) { _, which ->
                currentYear = years[which]
                binding.tvYear.text = currentYear.toString()
                loadLeaveData(forceRefresh = true)
            }
            .create()
        dialog.show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onResume() {
        super.onResume()
        // Only refresh if returning from leave submission activity
        if (shouldRefreshOnResume) {
            loadLeaveData(forceRefresh = true)
            shouldRefreshOnResume = false
        }
    }
}
