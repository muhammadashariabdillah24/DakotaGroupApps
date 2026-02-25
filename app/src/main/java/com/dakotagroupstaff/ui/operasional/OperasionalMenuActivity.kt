package com.dakotagroupstaff.ui.operasional

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.databinding.ActivityOperasionalMenuBinding
import com.dakotagroupstaff.ui.main.MainViewModel
import com.dakotagroupstaff.ui.operasional.assignment.AssignmentActivity
import com.dakotagroupstaff.ui.operasional.letterofassign.LetterOfAssignActivity
import com.dakotagroupstaff.ui.operasional.loper.LoperActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class OperasionalMenuActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOperasionalMenuBinding
    private val mainViewModel: MainViewModel by viewModel()
    private val userPreferences: UserPreferences by inject()
    
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
    
    private fun setupClickListeners() {        // Surat Tugas menu - Route based on PT
        binding.cardSuratTugas.setOnClickListener {
            // Get PT from user preferences to determine routing
            lifecycleScope.launch {
                val pt = userPreferences.getPt().first()
                
                when (pt) {
                    "C" -> {
                        // PT DLI - Use existing Assignment Activity
                        mainViewModel.saveMenuToHistory(
                            "assignment",
                            "Surat Tugas",
                            R.drawable.ic_menu_surat_tugas,
                            AssignmentActivity::class.java.name
                        )
                        val intent = Intent(this@OperasionalMenuActivity, AssignmentActivity::class.java)
                        startActivity(intent)
                    }
                    "A", "B" -> {
                        // PT DBS (A) or PT DLB (B) - Use new Letter of Assign Activity
                        mainViewModel.saveMenuToHistory(
                            "letter-of-assign",
                            "Surat Tugas",
                            R.drawable.ic_menu_surat_tugas,
                            LetterOfAssignActivity::class.java.name
                        )
                        val intent = Intent(this@OperasionalMenuActivity, LetterOfAssignActivity::class.java)
                        startActivity(intent)
                    }
                    else -> {
                        // Fallback to existing Assignment Activity
                        mainViewModel.saveMenuToHistory(
                            "assignment",
                            "Surat Tugas",
                            R.drawable.ic_menu_surat_tugas,
                            AssignmentActivity::class.java.name
                        )
                        val intent = Intent(this@OperasionalMenuActivity, AssignmentActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }
        
        // Loper menu
        binding.cardLoper.setOnClickListener {
            mainViewModel.saveMenuToHistory(
                "loper",
                "Loper",
                R.drawable.ic_menu_loper,
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
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
