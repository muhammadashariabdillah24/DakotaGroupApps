package com.dakotagroupstaff.ui.operasional.letterofassign

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dakotagroupstaff.data.remote.response.LoadingData
import com.dakotagroupstaff.databinding.FragmentMuatBinding

/**
 * Fragment for displaying Muat (Loading) data
 */
class MuatFragment : Fragment() {

    private var _binding: FragmentMuatBinding? = null
    private val binding get() = _binding!!

    private val muatAdapter = MuatAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMuatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvMuat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = muatAdapter
        }
    }

    fun setData(data: List<LoadingData>) {
        if (data.isEmpty()) {
            binding.tvEmptyMuat.visibility = View.VISIBLE
            binding.rvMuat.visibility = View.GONE
        } else {
            binding.tvEmptyMuat.visibility = View.GONE
            binding.rvMuat.visibility = View.VISIBLE
            muatAdapter.submitList(data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = MuatFragment()
    }
}
