package com.dakotagroupstaff.ui.operasional

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.dakotagroupstaff.R
import com.dakotagroupstaff.databinding.ActivityOperasionalMenuBinding
import com.dakotagroupstaff.ui.main.MainViewModel
import com.dakotagroupstaff.ui.operasional.assignment.AssignmentActivity
import com.dakotagroupstaff.ui.operasional.loper.LoperActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class OperasionalMenuActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOperasionalMenuBinding
    private val mainViewModel: MainViewModel by viewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperasionalMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Menu Operasional"
        }
    }
    
    private fun setupClickListeners() {
        // Surat Tugas menu
        binding.cardSuratTugas.setOnClickListener {
            mainViewModel.saveMenuToHistory(
                "assignment",
                "Surat Tugas",
                R.drawable.ic_operasional,
                AssignmentActivity::class.java.name
            )
            val intent = Intent(this, AssignmentActivity::class.java)
            startActivity(intent)
        }
        
        // Loper menu
        binding.cardLoper.setOnClickListener {
            mainViewModel.saveMenuToHistory(
                "loper",
                "Loper",
                R.drawable.ic_operasional,
                LoperActivity::class.java.name
            )
            val intent = Intent(this, LoperActivity::class.java)
            startActivity(intent)
        }
        
        // Future operational menus can be added here
        // binding.cardTracking.setOnClickListener { ... }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
