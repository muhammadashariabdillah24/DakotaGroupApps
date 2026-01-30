package com.dakotagroupstaff.ui.operasional.assignment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.pref.SessionManager
import com.dakotagroupstaff.databinding.FragmentAdditionalCostBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AdditionalCostFragment : Fragment() {
    
    private var _binding: FragmentAdditionalCostBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AssignmentViewModel by activityViewModel()
    private val sessionManager: SessionManager by inject()
    
    private lateinit var adapter: AdditionalCostAdapter
    private val costList = mutableListOf<AdditionalCostItem>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdditionalCostBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initial state: hide approval status until data is loaded
        binding.cardApprovalStatus.isVisible = false
        
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
    }
    
    private fun setupRecyclerView() {
        adapter = AdditionalCostAdapter(
            items = costList,
            onDeleteClick = { item ->
                showDeleteConfirmation(item)
            }
        )
        binding.rvAdditionalCost.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AdditionalCostFragment.adapter
        }
    }
    
    private fun setupObservers() {
        // Observe additional costs
        viewModel.additionalCosts.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is Result.Success -> {
                    binding.progressBar.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    parseAdditionalCostData(result.data)
                }
                is Result.Error -> {
                    binding.progressBar.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    showEmptyState()
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Observe approval status
        viewModel.approvalStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    
                    // Only update status content, visibility is handled by showData/showEmptyState
                    binding.tvApprovalStatus.text = "Status: ${data.status}"
                    
                    // Update icon and color based on approval status
                    if (data.approved) {
                        binding.ivApprovalIcon.setImageResource(R.drawable.ic_check_circle)
                        binding.cardApprovalStatus.strokeColor = 
                            resources.getColor(R.color.success, null)
                        binding.ivApprovalIcon.setColorFilter(
                            resources.getColor(R.color.success, null)
                        )
                    } else {
                        binding.ivApprovalIcon.setImageResource(R.drawable.ic_clock)
                        binding.cardApprovalStatus.strokeColor = 
                            resources.getColor(R.color.warning, null)
                        binding.ivApprovalIcon.setColorFilter(
                            resources.getColor(R.color.warning, null)
                        )
                    }
                }
                else -> {
                    // Do nothing, visibility is handled elsewhere
                }
            }
        }
        
        // Observe assignment data to get sID
        viewModel.assignment.observe(viewLifecycleOwner) { result ->
            if (result is Result.Success) {
                val sID = result.data.sID
                sessionManager.getPt()?.let { pt ->
                    viewModel.getAdditionalOperationalCosts(pt, sID)
                    viewModel.checkOperationalCostApproval(pt, sID)
                }
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.assignment.value?.let { result ->
                if (result is Result.Success) {
                    sessionManager.getPt()?.let { pt ->
                        viewModel.getAdditionalOperationalCosts(pt, result.data.sID)
                    }
                }
            }
        }
    }
    
    private fun parseAdditionalCostData(data: List<List<String>>) {
        try {
            costList.clear()
            
            if (data.isEmpty()) {
                showEmptyState()
                return
            }
            
            var total = 0.0
            
            for (itemArray in data) {
                if (itemArray.size >= 4) {
                    val code = itemArray[0]
                    val name = itemArray[1]
                    val nominal = itemArray[2]
                    val id = itemArray[3]  // For deletion
                    
                    costList.add(AdditionalCostItem(code, name, nominal, id))
                    
                    // Calculate total
                    nominal.replace("[^0-9]".toRegex(), "").toDoubleOrNull()?.let {
                        total += it
                    }
                }
            }
            
            if (costList.isEmpty()) {
                showEmptyState()
            } else {
                showData()
                adapter.notifyDataSetChanged()
                
                // Update total
                binding.tvTotal.text = formatCurrency(total.toLong())
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            showEmptyState()
            Toast.makeText(context, "Error parsing data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeleteConfirmation(item: AdditionalCostItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Biaya")
            .setMessage("Apakah Anda yakin ingin menghapus ${item.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteOperationalCost(item)
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun deleteOperationalCost(item: AdditionalCostItem) {
        // Implementation for delete - would need API endpoint
        Toast.makeText(context, "Fitur hapus akan segera ditambahkan", Toast.LENGTH_SHORT).show()
        
        // For now, just refresh the list
        viewModel.assignment.value?.let { result ->
            if (result is Result.Success) {
                sessionManager.getPt()?.let { pt ->
                    viewModel.getAdditionalOperationalCosts(pt, result.data.sID)
                }
            }
        }
    }
    
    private fun showEmptyState() {
        binding.rvAdditionalCost.isVisible = false
        binding.layoutEmpty.isVisible = true
        binding.layoutTotal.isVisible = false
        binding.cardApprovalStatus.isVisible = false  // Hide approval status when no data
    }
    
    private fun showData() {
        binding.rvAdditionalCost.isVisible = true
        binding.layoutEmpty.isVisible = false
        binding.layoutTotal.isVisible = true
        binding.cardApprovalStatus.isVisible = true  // Show approval status when data exists
    }
    
    private fun formatCurrency(amount: Long): String {
        return try {
            val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
            formatter.format(amount).replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp $amount"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class AdditionalCostItem(
    val code: String,
    val name: String,
    val nominal: String,
    val id: String
)
