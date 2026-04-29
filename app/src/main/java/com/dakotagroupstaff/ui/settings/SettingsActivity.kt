package com.dakotagroupstaff.ui.settings

import android.os.Bundle
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.dakotagroupstaff.ui.base.BaseActivity
import com.dakotagroupstaff.databinding.ActivitySettingsBinding
import com.dakotagroupstaff.ui.login.LoginViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupMenu()
        setupLogoutButton()
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

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(com.dakotagroupstaff.R.string.logout))
            .setMessage("Apakah Anda yakin ingin logout?")
            .setPositiveButton("Ya") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        // Sign out dari Google jika login menggunakan Google
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).build()
        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()

        lifecycleScope.launch {
            loginViewModel.logout()
            navigateToLogin()
        }
    }
}
