package com.dakotagroupstaff.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.dakotagroupstaff.BuildConfig
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.remote.response.EmployeeBioData
import com.dakotagroupstaff.data.remote.response.EmployeeBioRequest
import com.dakotagroupstaff.data.remote.retrofit.ApiConfig
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.databinding.ActivityAccountInfoBinding
import com.dakotagroupstaff.ui.login.LoginViewModel
import com.dakotagroupstaff.util.ImageUrlHelper
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AccountInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountInfoBinding
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        observeSessionAndLoadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.account_info)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeSessionAndLoadData() {
        loginViewModel.getSession().observe(this) { session ->
            if (!session.isLoggedIn) {
                finish()
                return@observe
            }

            // Set basic info from session
            binding.tvName.text = session.nama
            binding.tvNip.text = session.nip
            binding.tvWorkArea.text = session.areaKerja ?: "-"

            // Load profile photo
            loadProfilePhoto(session.nip)

            // Load bio details from backend
            loadEmployeeBio(session.pt, session.nip)
        }
    }

    private fun loadProfilePhoto(nip: String) {
        // Get current session to get company code
        loginViewModel.getSession().observe(this) { session ->
            if (session.isLoggedIn) {
                val photoUrl = ImageUrlHelper.constructPhotoUrl(session.pt, nip)
                
                // Load profile photo with larger size and enhanced error handling
                Glide.with(this)
                    .asBitmap() // Force as bitmap for consistent rendering
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .override(150, 150) // Larger size: 150x150dp
                    .into(binding.ivProfile)
            }
        }
    }

    private fun loadEmployeeBio(pt: String, nip: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentGroup.visibility = View.INVISIBLE

        lifecycleScope.launch {
            try {
                val userPreferences = com.dakotagroupstaff.data.local.preferences.UserPreferences.getInstance(dataStore)
                val apiService = ApiConfig.getApiService(userPreferences = userPreferences)
                val response = apiService.getEmployeeBio(pt, EmployeeBioRequest(nip))
                val data = response.data?.firstOrNull()
                if (data != null) {
                    bindEmployeeBio(data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.contentGroup.visibility = View.VISIBLE
            }
        }
    }

    private fun bindEmployeeBio(data: EmployeeBioData) {
        binding.tvBpjs.text = data.bpjs.ifEmpty { "-" }
        binding.tvJamsostek.text = data.jamsostek.ifEmpty { "-" }
        binding.tvNpwp.text = data.npwp.ifEmpty { "-" }
        binding.tvSocialStatus.text = data.statusSosial.ifEmpty { "-" }
        binding.tvWorkingHours.text = if (data.masuk.isNotEmpty() && data.keluar.isNotEmpty()) {
            "${data.masuk} - ${data.keluar}"
        } else {
            "-"
        }
        binding.tvEmploymentStatus.text = data.statusPegawai.ifEmpty { "-" }
    }
}
