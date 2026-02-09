package com.dakotagroupstaff.ui.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.dakotagroupstaff.BuildConfig
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.databinding.ActivityLoginBinding
import com.dakotagroupstaff.util.ErrorMessageHelper
import com.dakotagroupstaff.util.ImageUrlHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModel()
    
    private var deviceId: String = ""
    private var serialNumber: String = ""
    private var selectedPt: String = "C"
    private var googleEmail: String = ""
    
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getDeviceInfo()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        loadAppLogo()
        setupPTDropdown()
        setupNipTextWatcher()
        checkPermission()
    }
    
    private fun setupGoogleSignIn() {
        // Configure Google Sign-In with Web Client ID
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()
            .requestProfile()
            .requestIdToken("691595542165-n68uosqr2j37hngrfvnaddfqupggvl7a.apps.googleusercontent.com")
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Setup ActivityResultLauncher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            } else {
                Toast.makeText(this, getString(R.string.error_google_sign_in_cancelled), Toast.LENGTH_SHORT).show()
            }
        }
        
        // Setup Google Sign-In Button click listener
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
        
        // Check if user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            updateUIWithGoogleAccount(account)
        }
    }
    
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
    
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            updateUIWithGoogleAccount(account)
        } catch (e: ApiException) {
            Toast.makeText(this, "${getString(R.string.error_google_sign_in_failed)}: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            googleEmail = ""
        }
    }
    
    private fun setupNipTextWatcher() {
        binding.etNip.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val nip = s?.toString()?.trim() ?: ""
                binding.btnGoogleSignIn.isEnabled = nip.isNotEmpty()
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateUIWithGoogleAccount(account: GoogleSignInAccount) {
        googleEmail = account.email ?: ""
        
        // Auto-login after successful Google Sign-In
        val nip = binding.etNip.text.toString().trim()
        if (nip.isNotEmpty()) {
            performLogin(nip, googleEmail)
        } else {
            Toast.makeText(this, getString(R.string.error_empty_nip), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPTDropdown() {
        val ptOptions = mapOf(
            "DBS" to "A",
            "DLB" to "B",
            "DLI" to "C"
        )
        
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            ptOptions.keys.toList()
        )
        
        binding.actvPt.setAdapter(adapter)
        binding.actvPt.setText("DLI", false)
        selectedPt = "C"
        
        binding.actvPt.setOnItemClickListener { _, _, position, _ ->
            val selectedName = ptOptions.keys.toList()[position]
            selectedPt = ptOptions[selectedName] ?: "C"
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getDeviceInfo()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceInfo() {
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                deviceId = telephonyManager.imei ?: "UNKNOWN_IMEI_${System.currentTimeMillis()}"
                serialNumber = telephonyManager.simSerialNumber ?: "UNKNOWN_SIM_${System.currentTimeMillis()}"
            } else {
                // Fallback jika permission tidak diberikan
                deviceId = "UNKNOWN_IMEI_${System.currentTimeMillis()}"
                serialNumber = "UNKNOWN_SIM_${System.currentTimeMillis()}"
            }
        } catch (e: Exception) {
            // Fallback untuk device yang tidak support atau error
            deviceId = "UNKNOWN_IMEI_${System.currentTimeMillis()}"
            serialNumber = "UNKNOWN_SIM_${System.currentTimeMillis()}"
            e.printStackTrace()
        }
    }

    private fun performLogin(nip: String, email: String) {
        // Log data yang akan dikirim untuk debugging
        if (BuildConfig.DEBUG) {
            android.util.Log.d("LoginActivity", "=== LOGIN ATTEMPT ===")
            android.util.Log.d("LoginActivity", "PT: $selectedPt")
            android.util.Log.d("LoginActivity", "NIP: $nip")
            android.util.Log.d("LoginActivity", "Device ID (IMEI): $deviceId")
            android.util.Log.d("LoginActivity", "Serial Number (SIM): $serialNumber")
            android.util.Log.d("LoginActivity", "Email: $email")
            android.util.Log.d("LoginActivity", "====================")
        }
        
        viewModel.login(selectedPt, nip, deviceId, serialNumber, email).observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "${getString(R.string.login_success)} Selamat datang, ${result.data.nama}",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Navigate to MainActivity
                    val intent = Intent(this, com.dakotagroupstaff.MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is Result.Error -> {
                    showLoading(false)
                    showErrorDialog(result.message)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.etNip.isEnabled = !isLoading
        binding.btnGoogleSignIn.isEnabled = !isLoading && binding.etNip.text.toString().trim().isNotEmpty()
        binding.actvPt.isEnabled = !isLoading
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.login_failed))
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage("Aplikasi memerlukan permission READ_PHONE_STATE untuk mendapatkan IMEI dan SIM ID perangkat Anda. Tanpa permission ini, aplikasi akan menggunakan ID fallback.")
            .setPositiveButton("OK") { dialog, _ ->
                getDeviceInfo() // Use fallback
                dialog.dismiss()
            }
            .show()
    }
    
    private fun loadAppLogo() {
        // Construct app logo URL using ImageUrlHelper
        val logoUrl = ImageUrlHelper.constructLogoUrl()
        
        // Debug log
        if (BuildConfig.DEBUG) {
            android.util.Log.d("LoginActivity", "=== APP LOGO DEBUG ===")
            android.util.Log.d("LoginActivity", "Logo URL: $logoUrl")
            android.util.Log.d("LoginActivity", "=====================")
        }
        
        // Load app logo with Glide
        Glide.with(this)
            .load(logoUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.e("LoginActivity", "=== APP LOGO LOAD FAILED ===")
                    android.util.Log.e("LoginActivity", "URL: $logoUrl")
                    android.util.Log.e("LoginActivity", "Error: ${e?.message}")
                    android.util.Log.e("LoginActivity", "Root causes:")
                    e?.rootCauses?.forEach { cause ->
                        android.util.Log.e("LoginActivity", "  - ${cause.message}")
                    }
                    android.util.Log.e("LoginActivity", "=================================")
                    return false // Return false to allow Glide to handle error drawable
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("LoginActivity", "=== APP LOGO LOADED SUCCESSFULLY ===")
                        android.util.Log.d("LoginActivity", "URL: $logoUrl")
                        android.util.Log.d("LoginActivity", "Data source: $dataSource")
                        android.util.Log.d("LoginActivity", "===================================")
                    }
                    return false // Return false to allow Glide to display the image
                }
            })
            .circleCrop() // Make logo circular to match design
            .into(binding.ivLogo)
    }
}
