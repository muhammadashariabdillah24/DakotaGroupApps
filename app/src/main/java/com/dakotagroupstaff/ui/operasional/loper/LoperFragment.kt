package com.dakotagroupstaff.ui.operasional.loper

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dakotagroupstaff.databinding.FragmentLoperBinding

/**
 * Loper Fragment for Quick Access Modal
 * Displays Loper (Delivery) list and actions
 */
class LoperFragment : Fragment() {

    private var _binding: FragmentLoperBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
    }

    private fun setupViews() {
        binding.btnOpenLoperActivity.setOnClickListener {
            val intent = Intent(requireContext(), LoperActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
