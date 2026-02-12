package com.dakotagroupstaff.ui.operasional.loper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.local.room.AppDatabase
import com.dakotagroupstaff.data.remote.retrofit.ApiConfig
import com.dakotagroupstaff.data.repository.DeliveryRepository
import com.dakotagroupstaff.data.repository.LoperRepository
import com.dakotagroupstaff.databinding.ActivityLoperBinding
import com.dakotagroupstaff.utils.ViewModelFactory
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class LoperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoperBinding
    private lateinit var deliveryAdapter: DeliveryAdapter
    private lateinit var userPreferences: UserPreferences


    private val viewModel: LoperViewModel by viewModels {
        val userPref = UserPreferences.getInstance(dataStore)
        val apiService = ApiConfig.getApiService(userPreferences = userPref)
        val database = AppDatabase.getDatabase(this)
        val deliveryRepository = DeliveryRepository(apiService, userPref, database.deliveryListDao())
        val loperRepository = LoperRepository.getInstance(apiService, userPref)
        ViewModelFactory.getInstance(
            this, 
            deliveryRepository = deliveryRepository,
            loperRepository = loperRepository
        )
    }
    
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission granted or denied, continue normally
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPreferences = UserPreferences.getInstance(dataStore)

        setupToolbar()
        setupViewPager()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // Automatically refresh data when returning to this screen
        reloadDelivery()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = BttTabType.values().size

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> BttListFragment.newInstance(BttTabType.PENDING)
                    1 -> BttListFragment.newInstance(BttTabType.SENT)
                    else -> BttListFragment.newInstance(BttTabType.OVERDUE)
                }
            }
        }
        binding.viewPagerBtt.adapter = adapter
        binding.viewPagerBtt.offscreenPageLimit = BttTabType.values().size

        TabLayoutMediator(binding.tabLayoutBtt, binding.viewPagerBtt) { tab, position ->
            tab.text = when (position) {
                0 -> "Daftar BTT Tertunda"
                1 -> "Daftar BTT Terkirim"
                else -> "Daftar BTT Melebihi Tengat Waktu"
            }
        }.attach()
    }




    private fun loadData() {
        lifecycleScope.launch {
            val nip = userPreferences.getNip().first()
            viewModel.getDeliveryList(nip)
        }
    }

    fun reloadDelivery() {
        loadData()
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }




}
