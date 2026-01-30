package com.dakotagroupstaff.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dakotagroupstaff.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupMenu()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(com.dakotagroupstaff.R.string.settings)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupMenu() {
        binding.cardAccountInfo.setOnClickListener {
            startActivity(Intent(this, AccountInfoActivity::class.java))
        }
        binding.cardDataUsage.setOnClickListener {
            startActivity(Intent(this, DataUsageActivity::class.java))
        }
        binding.cardHelpCenter.setOnClickListener {
            startActivity(Intent(this, HelpCenterActivity::class.java))
        }
    }
}
