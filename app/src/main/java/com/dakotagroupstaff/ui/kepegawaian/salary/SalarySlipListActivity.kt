package com.dakotagroupstaff.ui.kepegawaian.salary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.pref.SessionManager
import com.dakotagroupstaff.data.remote.response.SalarySlipData
import com.dakotagroupstaff.databinding.ActivitySalarySlipListBinding
import com.dakotagroupstaff.databinding.DialogYearFilterBinding
import com.dakotagroupstaff.di.Injection
import com.dakotagroupstaff.utils.SalaryDataHelper
import java.util.Calendar

/**
 * Salary Slip List Activity
 * Migrated from React Native: OldSystemSlipGaji/index.js
 * 
 * Features:
 * - Display salary slips grouped by year
 * - Year filter dialog
 * - Pull to refresh
 * - Navigate to detail screen
 */
class SalarySlipListActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalarySlipListBinding
    private lateinit var viewModel: SalaryViewModel
    private lateinit var adapter: SalarySlipAdapter
    private lateinit var sessionManager: SessionManager

    private var allSalarySlips: List<SalarySlipData> = emptyList()
    private var availableYears: List<Int> = emptyList()
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var isManualRefresh: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalarySlipListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupListeners()
        loadData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = SalarySlipAdapter { salarySlip ->
            navigateToDetail(salarySlip)
        }

        binding.rvSalarySlips.apply {
            layoutManager = LinearLayoutManager(this@SalarySlipListActivity)
            adapter = this@SalarySlipListActivity.adapter
        }
    }

    private fun setupViewModel() {
        val factory = ViewModelFactory(
            Injection.provideSalaryRepository(this)
        )
        viewModel = ViewModelProvider(this, factory)[SalaryViewModel::class.java]

        // Observe salary slips
        viewModel.salarySlips.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    allSalarySlips = result.data
                    processData()
                }
                is Result.Error -> {
                    showLoading(false)
                    showError(result.message)
                }
            }
        }
    }

    private fun setupListeners() {
        // Year filter click
        binding.cardYearFilter.setOnClickListener {
            showYearFilterDialog()
        }

        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            isManualRefresh = true
            loadData()
        }
    }

    private fun loadData() {
        val pt = sessionManager.getPt() ?: "Logistik"
        val nip = sessionManager.getNip() ?: ""
        val imei = sessionManager.getImei() ?: ""
        val simId = sessionManager.getSimId() ?: ""

        if (nip.isEmpty()) {
            showError("NIP tidak ditemukan")
            return
        }
        
        if (imei.isEmpty() || simId.isEmpty()) {
            showError("Device information not found. Please login again.")
            return
        }

        viewModel.loadSalarySlips(pt, nip, imei, simId)
    }

    override fun onResume() {
        super.onResume()
        // Reset manual refresh flag when returning to the activity
        isManualRefresh = false
    }

    private fun processData() {
        // Get available years
        availableYears = SalaryDataHelper.getAvailableYears(allSalarySlips)

        // Set selected year to current year or latest available
        if (selectedYear == 0 || !availableYears.contains(selectedYear)) {
            selectedYear = availableYears.firstOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        }

        // Update UI
        binding.tvSelectedYear.text = selectedYear.toString()

        // Filter slips by selected year
        val filteredSlips = SalaryDataHelper.filterByYear(allSalarySlips, selectedYear)
            .sortedByDescending { it.getMonth() } // Sort by month descending

        // Update adapter
        adapter.submitList(filteredSlips)

        // Show/hide empty state
        if (filteredSlips.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvSalarySlips.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvSalarySlips.visibility = View.VISIBLE
        }
    }

    private fun showYearFilterDialog() {
        if (availableYears.isEmpty()) {
            Toast.makeText(this, "Tidak ada data tahun", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogYearFilterBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val yearAdapter = YearFilterAdapter(
            years = availableYears,
            selectedYear = selectedYear
        ) { year ->
            selectedYear = year
            dialog.dismiss()
            processData() // Re-process with new year
        }

        dialogBinding.rvYears.apply {
            layoutManager = LinearLayoutManager(this@SalarySlipListActivity)
            adapter = yearAdapter
        }

        dialog.show()
    }

    private fun navigateToDetail(salarySlip: SalarySlipData) {
        val intent = Intent(this, SalarySlipDetailActivity::class.java).apply {
            putExtra(SalarySlipDetailActivity.EXTRA_SALARY_SLIP, salarySlip)
        }
        startActivity(intent)
    }

    private fun showLoading(isLoading: Boolean) {
        // Only show SwipeRefreshLayout indicator if this is a manual refresh
        if (isManualRefresh) {
            binding.swipeRefreshLayout.isRefreshing = isLoading
            binding.progressBar.visibility = View.GONE
        } else {
            // For initial load, only show progress bar
            binding.swipeRefreshLayout.isRefreshing = false
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Reset flag after loading completes
        if (!isLoading) {
            isManualRefresh = false
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
