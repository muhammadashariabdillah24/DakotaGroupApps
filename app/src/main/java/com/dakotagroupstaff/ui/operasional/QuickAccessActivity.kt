package com.dakotagroupstaff.ui.operasional

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dakotagroupstaff.R
import com.dakotagroupstaff.databinding.ActivityQuickAccessBinding
import com.dakotagroupstaff.ui.kepegawaian.attendance.AttendanceFragment
import com.dakotagroupstaff.ui.operasional.assignment.AssignmentFragment
import com.dakotagroupstaff.ui.operasional.loper.LoperFragment

/**
 * Quick Access Modal Activity
 * Full-screen modal with bottom navigation for quick access to:
 * - Surat Tugas (Assignment)
 * - Loper (Delivery)
 * - Absensi (Attendance)
 */
class QuickAccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickAccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set slide-up animation
        overridePendingTransition(R.anim.slide_up, R.anim.no_animation)
        
        setupHeader()
        setupBottomNavigation()
        
        // Load default fragment (Surat Tugas)
        if (savedInstanceState == null) {
            loadFragment(AssignmentFragment())
        }
    }
    
    private fun setupHeader() {
        binding.btnClose.setOnClickListener {
            finish()
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_assignment -> {
                    loadFragment(AssignmentFragment())
                    true
                }
                R.id.nav_loper -> {
                    loadFragment(LoperFragment())
                    true
                }
                R.id.nav_attendance -> {
                    loadFragment(AttendanceFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    override fun finish() {
        super.finish()
        // Set slide-down animation when closing
        overridePendingTransition(R.anim.no_animation, R.anim.slide_down)
    }
}
