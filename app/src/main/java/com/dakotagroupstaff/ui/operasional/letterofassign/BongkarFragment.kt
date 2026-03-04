package com.dakotagroupstaff.ui.operasional.letterofassign

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.data.remote.response.UnloadingData
import com.dakotagroupstaff.databinding.FragmentBongkarBinding

/**
 * Fragment for displaying Bongkar (Unloading) data
 */
class BongkarFragment : Fragment() {

    private var _binding: FragmentBongkarBinding? = null
    private val binding get() = _binding!!

    private val bongkarAdapter = BongkarAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBongkarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvBongkar.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bongkarAdapter
        }
    }

    fun setData(data: List<UnloadingData>) {
        if (data.isEmpty()) {
            binding.tvEmptyBongkar.visibility = View.VISIBLE
            binding.rvBongkar.visibility = View.GONE
        } else {
            binding.tvEmptyBongkar.visibility = View.GONE
            binding.rvBongkar.visibility = View.VISIBLE
            bongkarAdapter.submitList(data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = BongkarFragment()
    }
}
