package com.dakotagroupstaff.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.remote.response.EmployeeBioData
import com.dakotagroupstaff.data.remote.response.EmployeeBioRequest
import com.dakotagroupstaff.data.remote.retrofit.ApiConfig
import com.dakotagroupstaff.databinding.ActivityAccountInfoBinding
import com.dakotagroupstaff.ui.base.BaseActivity
import com.dakotagroupstaff.ui.login.LoginViewModel
import com.dakotagroupstaff.util.ImageUrlHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.google.gson.Gson

class AccountInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityAccountInfoBinding
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        observeSessionAndLoadData()
        requestPhonePermissionsAndLoadNumbers()
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
        loginViewModel.getSession().observe(this) { session ->
            if (session.isLoggedIn) {
                val photoUrl = ImageUrlHelper.constructPhotoUrl(session.pt, nip)
                Glide.with(this)
                    .asBitmap()
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .override(150, 150)
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
                val bioJson = userPreferences.getBioData().first()
                if (bioJson.isNotEmpty()) {
                    val data = Gson().fromJson(bioJson, EmployeeBioData::class.java)
                    if (data != null) {
                        bindEmployeeBio(data)
                    }
                } else {
                    // Fallback to fetch API if cache is empty
                    val apiService = ApiConfig.getApiService(userPreferences = userPreferences)
                    val response = apiService.getEmployeeBio(pt, EmployeeBioRequest(nip))
                    val data = response.data?.firstOrNull()
                    if (data != null) {
                        bindEmployeeBio(data)
                        // Also save it
                        userPreferences.saveBioData(Gson().toJson(data))
                        val jabCode = data.jabCode?.trim() ?: ""
                        val jabNama = data.jabNama?.trim() ?: ""
                        userPreferences.updateJabatan(jabCode, jabNama)
                    }
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
        binding.tvBpjs.text = data.bpjs?.ifEmpty { "-" } ?: "-"
        binding.tvJamsostek.text = data.jamsostek?.ifEmpty { "-" } ?: "-"
        binding.tvNpwp.text = data.npwp?.ifEmpty { "-" } ?: "-"
        binding.tvSocialStatus.text = data.statusSosial?.ifEmpty { "-" } ?: "-"
        binding.tvWorkingHours.text = if (!data.masuk.isNullOrEmpty() && !data.keluar.isNullOrEmpty()) {
            "${data.masuk} - ${data.keluar}"
        } else {
            "-"
        }
        binding.tvEmploymentStatus.text = data.statusPegawai?.ifEmpty { "-" } ?: "-"
        binding.tvJabatan.text = data.jabNama?.ifEmpty { "-" } ?: "-"
    }

    // ─── Phone Number (SIM 1 & SIM 2) ───────────────────────────────────────

    private fun requestPhonePermissionsAndLoadNumbers() {
        val requiredPermissions = buildList {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
            // READ_PHONE_NUMBERS diperlukan di Android 8+ untuk membaca nomor telepon
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS)
                        != PackageManager.PERMISSION_GRANTED) {
                    add(Manifest.permission.READ_PHONE_NUMBERS)
                }
            }
        }

        if (requiredPermissions.isEmpty()) {
            displayPhoneNumbers()
        } else {
            requestPermissions(requiredPermissions.toTypedArray(), REQUEST_CODE_PHONE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PHONE) {
            // Cukup salah satu permission yang diberikan agar bisa mencoba baca nomor
            val anyGranted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (anyGranted) {
                displayPhoneNumbers()
            } else {
                // Semua permission ditolak — tampilkan "-"
                binding.tvPhone1.text = "-"
                binding.tvPhone2.text = "-"
                // Card masih bisa diklik tapi tidak melakukan copy karena nilainya "-"
                setupPhoneCardClickListeners("-", "-")
            }
        }
    }

    private fun displayPhoneNumbers() {
        val phone1 = readSimPhoneNumber(simSlotIndex = 0)
        val phone2 = readSimPhoneNumber(simSlotIndex = 1)

        binding.tvPhone1.text = phone1
        binding.tvPhone2.text = phone2

        setupPhoneCardClickListeners(phone1, phone2)
    }

    /**
     * Membaca nomor telepon SIM berdasarkan slot index (0 = SIM 1, 1 = SIM 2).
     * Mengembalikan "-" jika nomor tidak tersedia, kosong, atau terjadi error.
     *
     * Strategi:
     * - Android 10+ (Q): gunakan SubscriptionManager via getSystemService dengan class
     * - Android 8-9 (O-P): gunakan SubscriptionManager.from()
     * - Android 7 ke bawah: hanya SIM 1 via TelephonyManager.line1Number
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    private fun readSimPhoneNumber(simSlotIndex: Int): String {
        return try {
            val rawNumber: String? = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    val subMgr = getSystemService(SubscriptionManager::class.java)
                    val subs = subMgr?.activeSubscriptionInfoList
                    if (!subs.isNullOrEmpty() && subs.size > simSlotIndex) {
                        subs[simSlotIndex].number
                    } else null
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val subMgr = SubscriptionManager.from(this)
                    val subs = subMgr?.activeSubscriptionInfoList
                    if (!subs.isNullOrEmpty() && subs.size > simSlotIndex) {
                        subs[simSlotIndex].number
                    } else null
                }
                else -> {
                    // Android 7 dan ke bawah — hanya SIM 1
                    if (simSlotIndex == 0) {
                        val telephony = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                        telephony.line1Number
                    } else null
                }
            }

            val cleaned = rawNumber?.trim()
            if (cleaned.isNullOrEmpty() || cleaned.equals("null", ignoreCase = true)
                || cleaned.equals("unknown", ignoreCase = true)) {
                "-"
            } else {
                cleaned
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "-"
        }
    }

    private fun setupPhoneCardClickListeners(phone1: String, phone2: String) {
        binding.cardPhone1.setOnClickListener {
            if (phone1 != "-") copyToClipboard(phone1)
        }
        binding.cardPhone2.setOnClickListener {
            if (phone2 != "-") copyToClipboard(phone2)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("phone_number", text)
        clipboard.setPrimaryClip(clip)
        showToast(getString(R.string.phone_number_copied))
    }

    companion object {
        private const val REQUEST_CODE_PHONE = 1001
    }
}
