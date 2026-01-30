package com.dakotagroupstaff.ui.operasional.assignment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.pref.SessionManager
import com.dakotagroupstaff.databinding.FragmentDownPaymentBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class DownPaymentFragment : Fragment() {
    
    private var _binding: FragmentDownPaymentBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AssignmentViewModel by activityViewModel()
    private val sessionManager: SessionManager by inject()
    
    private lateinit var adapter: DownPaymentAdapter
    private val costList = mutableListOf<DownPaymentItem>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
    }
    
    private fun setupRecyclerView() {
        adapter = DownPaymentAdapter(costList)
        binding.rvDownPayment.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@DownPaymentFragment.adapter
        }
    }
    
    private fun setupObservers() {
        // Observe down payment costs
        viewModel.downPaymentCosts.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is Result.Success -> {
                    binding.progressBar.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    parseDownPaymentData(result.data)
                }
                is Result.Error -> {
                    binding.progressBar.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    showEmptyState()
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Observe assignment data to get sID
        viewModel.assignment.observe(viewLifecycleOwner) { result ->
            if (result is Result.Success) {
                val sID = result.data.sID
                sessionManager.getPt()?.let { pt ->
                    viewModel.getDownPaymentCosts(pt, sID)
                }
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.assignment.value?.let { result ->
                if (result is Result.Success) {
                    sessionManager.getPt()?.let { pt ->
                        viewModel.getDownPaymentCosts(pt, result.data.sID)
                    }
                }
            }
        }
    }
    
    private fun parseDownPaymentData(data: List<List<String>>) {
        try {
            costList.clear()
            
            if (data.isEmpty()) {
                showEmptyState()
                return
            }
            
            var total = 0.0
            
            for (itemArray in data) {
                if (itemArray.size >= 3) {
                    val code = itemArray[0]
                    val name = itemArray[1]
                    val nominal = itemArray[2]
                    
                    costList.add(DownPaymentItem(code, name, nominal))
                    
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
    
    private fun showEmptyState() {
        binding.rvDownPayment.isVisible = false
        binding.layoutEmpty.isVisible = true
        binding.layoutTotal.isVisible = false
    }
    
    private fun showData() {
        binding.rvDownPayment.isVisible = true
        binding.layoutEmpty.isVisible = false
        binding.layoutTotal.isVisible = true
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

data class DownPaymentItem(
    val code: String,
    val name: String,
    val nominal: String
)
