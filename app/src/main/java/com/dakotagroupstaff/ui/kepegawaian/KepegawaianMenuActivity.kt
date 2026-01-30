package com.dakotagroupstaff.ui.kepegawaian

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dakotagroupstaff.R
import com.dakotagroupstaff.databinding.ActivityKepegawaianMenuBinding
import com.dakotagroupstaff.ui.kepegawaian.approval.ApprovalActivity
import com.dakotagroupstaff.ui.kepegawaian.attendance.AttendanceActivity
import com.dakotagroupstaff.ui.kepegawaian.leave.LeaveHistoryActivity
import com.dakotagroupstaff.ui.kepegawaian.salary.SalarySlipListActivity
import com.dakotagroupstaff.ui.main.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class KepegawaianMenuActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityKepegawaianMenuBinding
    private val mainViewModel: MainViewModel by viewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKepegawaianMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupMenuCards()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupMenuCards() {
        // Absensi Menu
        binding.cardAttendance.setOnClickListener {
            mainViewModel.saveMenuToHistory(
                "attendance",
                getString(R.string.attendance_title),
                R.drawable.ic_kepegawaian,
                AttendanceActivity::class.java.name
            )
            val intent = Intent(this, AttendanceActivity::class.java)
            startActivity(intent)
        }
        
        // Cuti Menu
        binding.cardLeave.setOnClickListener {
            mainViewModel.saveMenuToHistory(
                "leave",
                "Cuti/Izin",
                R.drawable.ic_kepegawaian,
                LeaveHistoryActivity::class.java.name
            )
            val intent = Intent(this, LeaveHistoryActivity::class.java)
            startActivity(intent)
        }
        
        // Gaji Menu
        binding.cardSalary.setOnClickListener {
            mainViewModel.saveMenuToHistory(
                "salary",
                "Gaji",
                R.drawable.ic_kepegawaian,
                SalarySlipListActivity::class.java.name
            )
            val intent = Intent(this, SalarySlipListActivity::class.java)
            startActivity(intent)
        }
        
        // Approval Menu
        binding.cardApproval.setOnClickListener {
            mainViewModel.saveMenuToHistory(
                "approval",
                "Approval",
                R.drawable.ic_kepegawaian,
                ApprovalActivity::class.java.name
            )
            val intent = Intent(this, ApprovalActivity::class.java)
            startActivity(intent)
        }
    }
}
