package com.dakotagroupstaff.ui.settings

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import com.dakotagroupstaff.R
import com.dakotagroupstaff.databinding.ActivityDataUsageBinding
import java.text.SimpleDateFormat
import java.util.*

class DataUsageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataUsageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadDataUsage()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.data_usage)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadDataUsage() {
        try {
            val dataUsageList = getDataUsageForLast3Months()
            binding.chartView.setData(dataUsageList)
            
            // Calculate total
            val totalMB = dataUsageList.sumOf { it.usageMB }
            binding.tvTotalUsage.text = String.format("%.2f MB", totalMB)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.tvTotalUsage.text = "Data tidak tersedia"
        }
    }

    private fun getDataUsageForLast3Months(): List<MonthlyDataUsage> {
        val calendar = Calendar.getInstance()
        val dataUsageList = mutableListOf<MonthlyDataUsage>()
        
        // Get data for last 3 months
        for (i in 2 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)
            
            val monthName = SimpleDateFormat("MMM yyyy", Locale("id", "ID")).format(calendar.time)
            
            // Start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startTime = calendar.timeInMillis
            
            // End of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endTime = calendar.timeInMillis
            
            val usageBytes = getAppDataUsage(startTime, endTime)
            val usageMB = usageBytes / (1024.0 * 1024.0)
            
            dataUsageList.add(MonthlyDataUsage(monthName, usageMB))
        }
        
        return dataUsageList
    }

    private fun getAppDataUsage(startTime: Long, endTime: Long): Long {
        try {
            val networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
            val uid = applicationInfo.uid
            
            var totalBytes = 0L
            
            // Mobile data
            try {
                val subscriberId = (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).subscriberId
                if (subscriberId != null) {
                    val mobileStats = networkStatsManager.querySummary(
                        ConnectivityManager.TYPE_MOBILE,
                        subscriberId,
                        startTime,
                        endTime
                    )
                    
                    val bucket = NetworkStats.Bucket()
                    while (mobileStats.hasNextBucket()) {
                        mobileStats.getNextBucket(bucket)
                        if (bucket.uid == uid) {
                            totalBytes += bucket.rxBytes + bucket.txBytes
                        }
                    }
                    mobileStats.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // WiFi data
            try {
                val wifiStats = networkStatsManager.querySummary(
                    ConnectivityManager.TYPE_WIFI,
                    null,
                    startTime,
                    endTime
                )
                
                val bucket = NetworkStats.Bucket()
                while (wifiStats.hasNextBucket()) {
                    wifiStats.getNextBucket(bucket)
                    if (bucket.uid == uid) {
                        totalBytes += bucket.rxBytes + bucket.txBytes
                    }
                }
                wifiStats.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return totalBytes
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }
}

data class MonthlyDataUsage(
    val monthName: String,
    val usageMB: Double
)
