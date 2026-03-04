package com.dakotagroupstaff.ui.operasional.loper

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.Window
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.remote.response.SubmitDeliveryRequest
import com.dakotagroupstaff.data.remote.response.DeliveryItem
import com.dakotagroupstaff.data.repository.DeliveryRepository
import com.dakotagroupstaff.databinding.ActivityLoperDetailBinding
import com.dakotagroupstaff.databinding.DialogSignatureBinding
import com.dakotagroupstaff.utils.ImageCompressor
import com.dakotagroupstaff.utils.ViewModelFactory
import com.dakotagroupstaff.util.ErrorMessageHelper
import com.dakotagroupstaff.util.SecurityChecker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.system.exitProcess

class LoperDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoperDetailBinding
    private var deliveryItem: DeliveryItem? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var photoUri: Uri? = null
    private var photoLatitude: Double = 0.0
    private var photoLongitude: Double = 0.0
    private var photoAddress: String = ""
    private var photoFileName: String = ""
    private var signatureBitmap: Bitmap? = null
    
    // Hold restored Base64 data to avoid re-compression and pass validation
    private var restoredFotoBase64: String? = null
    private var restoredTtdBase64: String? = null
    
    private lateinit var repository: DeliveryRepository
    private lateinit var userPreferences: UserPreferences
    
    // Track if BTT opened from Terkirim tab (true) or Tertunda tab (false)
    private var isFromSentTab: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoperDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize repository and preferences
        userPreferences = UserPreferences.getInstance(dataStore)
        val apiService = com.dakotagroupstaff.data.remote.retrofit.ApiConfig.getApiService(userPreferences = userPreferences)
        val database = com.dakotagroupstaff.data.local.room.AppDatabase.getDatabase(this)
        repository = DeliveryRepository(
            apiService = apiService,
            userPreferences = userPreferences,
            deliveryListDao = database.deliveryListDao()
        )

        // Get delivery item from intent
        deliveryItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DELIVERY_ITEM, DeliveryItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DELIVERY_ITEM)
        }
        
        // Get source tab info
        isFromSentTab = intent.getBooleanExtra(EXTRA_IS_FROM_SENT_TAB, false)

        setupToolbar()
        deliveryItem?.let {
            populateData(it)
            setupGoogleMaps(it)
            restoreExistingMedia(it)
        }
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun restoreExistingMedia(item: DeliveryItem) {
        if (isFromSentTab) {
            // Load from Room Database (Base64 data)
            restoreFromRoomDatabase(item)
        } else {
            // Load from MediaStore Gallery (for backward compatibility if needed)
            restoreExistingPhoto(item)
            restoreExistingSignature(item)
        }
    }
    
    /**
     * Restore photo and signature from Room Database (for Sent tab)
     */
    private fun restoreFromRoomDatabase(item: DeliveryItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entity = repository.getDeliveryByBtt(item.noBtt)
                
                withContext(Dispatchers.Main) {
                    if (entity != null) {
                        // Restore photo from Base64
                        entity.fotoBase64?.let { base64 ->
                            if (base64.isNotEmpty()) {
                                restoredFotoBase64 = base64
                                val bitmap = ImageCompressor.base64ToBitmap(base64)
                                bitmap?.let {
                                    // Display photo
                                    binding.ivPhotoPreview.setImageBitmap(it)
                                    binding.cardPhotoInfo.visibility = View.VISIBLE
                                    
                                    // Set photo metadata
                                    photoFileName = "IMG-${item.noLoper}-${item.noBtt}.jpg"
                                    binding.tvPhotoName.text = photoFileName
                                    
                                    // Restore GPS data
                                    entity.latitude?.let { lat ->
                                        entity.longitude?.let { lon ->
                                            photoLatitude = lat.toDoubleOrNull() ?: 0.0
                                            photoLongitude = lon.toDoubleOrNull() ?: 0.0
                                            
                                            if (photoLatitude != 0.0 && photoLongitude != 0.0) {
                                                binding.tvPhotoLocation.text = "Lat: $photoLatitude, Lng: $photoLongitude"
                                                // Optionally get address from coordinates
                                                getAddressFromLocation(photoLatitude, photoLongitude)
                                                binding.tvPhotoAddress.text = photoAddress.ifEmpty { "Alamat tidak tersedia" }
                                            } else {
                                                binding.tvPhotoLocation.text = "Lokasi tidak tersedia"
                                                binding.tvPhotoAddress.text = "-"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Restore signature from Base64
                        entity.ttdBase64?.let { base64 ->
                            if (base64.isNotEmpty()) {
                                restoredTtdBase64 = base64
                                val bitmap = ImageCompressor.base64ToBitmap(base64)
                                bitmap?.let {
                                    signatureBitmap = it
                                    displaySignature()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoperDetailActivity", "Error restoring from Room DB: ${e.message}", e)
            }
        }
    }

    private fun restoreExistingPhoto(item: DeliveryItem) {
        val fileName = "IMG-${item.noLoper}-${item.noBtt}.jpg"
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, id)
                photoUri = uri
                photoFileName = fileName
                displayPhotoInfo()
            }
        }
    }

    private fun restoreExistingSignature(item: DeliveryItem) {
        val fileName = "TTD-${item.noLoper}-${item.noBtt}.jpg"
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        
        contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, id)
                // Load bitmap from URI
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                signatureBitmap = bitmap
                displaySignature()
            }
        }
    }
    private fun populateData(item: DeliveryItem) {
        with(binding) {
            // Nomor Loper
            tvNoLoper.text = item.noLoper

            // NIP Supir
            tvNipSupir.text = item.nipSupir

            // Tanggal Loper - Format date
            tvTanggalLoper.text = formatDate(item.tanggalLoper)

            // Nomor BTT
            tvNoBtt.text = item.noBtt

            // Alamat - Combine full address
            val fullAddress = buildString {
                append(item.alamat)
                if (item.kelurahan.isNotEmpty()) {
                    append(", ${item.kelurahan}")
                }
                if (item.kecamatan.isNotEmpty()) {
                    append(", ${item.kecamatan}")
                }
                if (item.kota.isNotEmpty()) {
                    append(", ${item.kota}")
                }
                if (item.propinsi.isNotEmpty()) {
                    append(", ${item.propinsi}")
                }
                if (item.kodepos.isNotEmpty()) {
                    append(" ${item.kodepos}")
                }
            }
            tvAlamat.text = fullAddress

            // Telepon Penerima
            tvTelpPenerima.text = item.telpPenerima

            // Jumlah Koli
            tvJumlahKoli.text = "${item.jumlahKoli} koli"

            // Berat
            tvBerat.text = "${item.berat} kg"

            // Berat Volume
            tvBeratVolume.text = "${item.beratVolume} kg"

            // Pre-fill receiver name if available
            etNamaPenerima.setText(item.penerima)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupGoogleMaps(item: DeliveryItem) {
        // Build address query for Google Maps
        val addressParts = mutableListOf<String>()
        
        if (item.alamat.isNotEmpty()) addressParts.add(item.alamat)
        if (item.kelurahan.isNotEmpty()) addressParts.add(item.kelurahan)
        if (item.kecamatan.isNotEmpty()) addressParts.add(item.kecamatan)
        if (item.kota.isNotEmpty()) addressParts.add(item.kota)
        if (item.propinsi.isNotEmpty()) addressParts.add(item.propinsi)
        if (item.kodepos.isNotEmpty()) addressParts.add(item.kodepos)
        
        val fullAddress = addressParts.joinToString(", ")
        val encodedAddress = URLEncoder.encode(fullAddress, "UTF-8")
        
        // Display address text
        binding.tvMapAddress.text = fullAddress
        
        // Show loading indicator
        binding.progressBarMap.visibility = View.VISIBLE
        
        // Create HTML content with embedded Google Maps iframe
        val htmlContent = """
            <!DOCTYPE html>
            <html style="height: 100%; margin: 0; padding: 0;">
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    html, body {
                        height: 100%;
                        width: 100%;
                        overflow: hidden;
                    }
                    #map {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: none;
                    }
                </style>
            </head>
            <body>
                <iframe 
                    id="map"
                    src="https://maps.google.com/maps?q=$encodedAddress&output=embed&z=15"
                    frameborder="0"
                    allowfullscreen
                    loading="lazy">
                </iframe>
            </body>
            </html>
        """.trimIndent()

        // Configure WebView
        with(binding.webViewMap) {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Hide loading indicator when map is loaded
                    binding.progressBarMap.visibility = View.GONE
                }
                
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    binding.progressBarMap.visibility = View.GONE
                    Toast.makeText(
                        this@LoperDetailActivity,
                        "Gagal memuat peta: $description",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            settings.apply {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false
                setSupportZoom(true)
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            
            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun saveReceiverName() {
        val receiverName = binding.etNamaPenerima.text.toString().trim()

        if (receiverName.isEmpty()) {
            binding.etNamaPenerima.error = "Nama penerima tidak boleh kosong"
            return
        }

        // TODO: Implement save to backend API if needed
        Toast.makeText(
            this,
            "Nama penerima disimpan: $receiverName",
            Toast.LENGTH_SHORT
        ).show()

        // Close activity and return to list
        finish()
    }

    private fun setupButtons() {
        binding.btnTakePhoto.setOnClickListener {
            checkPermissionsAndTakePhoto()
        }

        binding.btnSignature.setOnClickListener {
            showSignatureDialog()
        }

        binding.btnSave.setOnClickListener {
            saveReceiverData()
        }
    }

    private fun checkPermissionsAndTakePhoto() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissions.launch(missingPermissions.toTypedArray())
        } else {
            takePhoto()
        }
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            takePhoto()
        } else {
            Toast.makeText(
                this,
                "Izin diperlukan untuk mengambil foto dengan lokasi",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun takePhoto() {
        val item = deliveryItem ?: return
        
        // Generate photo filename: IMG-NoLoper-NoBTT.jpg (as per requirement)
        photoFileName = "IMG-${item.noLoper}-${item.noBtt}.jpg"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, photoFileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DakotaLoper")
            }
        }

        photoUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        photoUri?.let { uri ->
            takePictureLauncher.launch(uri)
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            getCurrentLocationAndSavePhoto()
        } else {
            Toast.makeText(this, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndSavePhoto() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Izin lokasi tidak diberikan", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                // CRITICAL SECURITY CHECK: Detect Fake GPS
                if (SecurityChecker.isMockLocation(location)) {
                    showFakeGpsDialog()
                    return@addOnSuccessListener
                }
                
                photoLatitude = location.latitude
                photoLongitude = location.longitude
                
                // Get address from coordinates
                getAddressFromLocation(location.latitude, location.longitude)
                
                // Save GPS data to photo EXIF
                saveGpsDataToPhoto(photoUri, location.latitude, location.longitude)
                
                // Display photo info
                displayPhotoInfo()
            } else {
                Toast.makeText(
                    this,
                    "Tidak dapat mendapatkan lokasi. Foto disimpan tanpa data lokasi.",
                    Toast.LENGTH_SHORT
                ).show()
                displayPhotoInfo()
            }
        }.addOnFailureListener {
            Toast.makeText(
                this,
                "Gagal mendapatkan lokasi: ${it.message}",
                Toast.LENGTH_SHORT
            ).show()
            displayPhotoInfo()
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                photoAddress = addresses[0].getAddressLine(0) ?: "Alamat tidak ditemukan"
            } else {
                photoAddress = "Alamat tidak ditemukan"
            }
        } catch (e: IOException) {
            photoAddress = "Gagal mendapatkan alamat"
        }
    }

    private fun saveGpsDataToPhoto(uri: Uri?, latitude: Double, longitude: Double) {
        uri ?: return
        
        try {
            contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                
                // Set GPS latitude
                exif.setAttribute(
                    ExifInterface.TAG_GPS_LATITUDE,
                    convertToExifFormat(latitude)
                )
                exif.setAttribute(
                    ExifInterface.TAG_GPS_LATITUDE_REF,
                    if (latitude >= 0) "N" else "S"
                )
                
                // Set GPS longitude
                exif.setAttribute(
                    ExifInterface.TAG_GPS_LONGITUDE,
                    convertToExifFormat(longitude)
                )
                exif.setAttribute(
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    if (longitude >= 0) "E" else "W"
                )
                
                exif.saveAttributes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun convertToExifFormat(coordinate: Double): String {
        val absCoordinate = Math.abs(coordinate)
        val degrees = absCoordinate.toInt()
        val minutes = ((absCoordinate - degrees) * 60).toInt()
        val seconds = ((absCoordinate - degrees - minutes / 60.0) * 3600 * 1000).toInt()
        return "$degrees/1,$minutes/1,$seconds/1000"
    }

    private fun displayPhotoInfo() {
        photoUri?.let { uri ->
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                
                binding.ivPhotoPreview.setImageBitmap(bitmap)
                binding.tvPhotoName.text = photoFileName
                
                if (photoLatitude != 0.0 && photoLongitude != 0.0) {
                    binding.tvPhotoLocation.text = "Lat: $photoLatitude, Lng: $photoLongitude"
                    binding.tvPhotoAddress.text = photoAddress
                } else {
                    binding.tvPhotoLocation.text = "Lokasi tidak tersedia"
                    binding.tvPhotoAddress.text = "-"
                }
                
                binding.cardPhotoInfo.visibility = View.VISIBLE
                
                Toast.makeText(
                    this,
                    "Foto berhasil diambil",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Gagal menampilkan foto: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showSignatureDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogSignatureBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        )

        dialogBinding.btnClearSignature.setOnClickListener {
            dialogBinding.signatureView.clear()
        }

        dialogBinding.btnSaveSignature.setOnClickListener {
            if (dialogBinding.signatureView.isEmpty) {
                Toast.makeText(
                    this,
                    "Silakan tanda tangan terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                signatureBitmap = dialogBinding.signatureView.getSignatureBitmap()
                displaySignature()
                saveSignatureToFile()
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Tanda tangan berhasil disimpan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.show()
    }

    private fun displaySignature() {
        signatureBitmap?.let { bitmap ->
            binding.ivSignaturePreview.setImageBitmap(bitmap)
            binding.cardSignaturePreview.visibility = View.VISIBLE
        }
    }

    private fun saveSignatureToFile() {
        val item = deliveryItem ?: return
        val bitmap = signatureBitmap ?: return
        try {
            // Save signature to gallery with name TTD-{NoLoper}-{NoBtt}.jpg
            val fileName = "TTD-${item.noLoper}-${item.noBtt}.jpg"
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DakotaLoper")
                }
            }
            
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
        } catch (e: Exception) {
            // Ignore persistence failure, do not block user flow
        }
    }

    private fun saveReceiverData() {
        val receiverName = binding.etNamaPenerima.text.toString().trim()

        // Validation 1: Check Nama Penerima
        if (receiverName.isEmpty()) {
            Toast.makeText(
                this,
                "Harap Isi Nama Penerima",
                Toast.LENGTH_SHORT
            ).show()
            binding.etNamaPenerima.requestFocus()
            return
        }
        
        // Validation 2: Check Photo (New URI or Restored Base64)
        if (photoUri == null && restoredFotoBase64 == null) {
            Toast.makeText(
                this,
                "Harap Foto Barang Penerima!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Validation 3: Check Signature (New Bitmap or Restored Base64)
        if (signatureBitmap == null && restoredTtdBase64 == null) {
            Toast.makeText(
                this,
                "Harap Tanda Tangan Digital!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Validate that we have the required data
        val item = deliveryItem ?: run {
            Toast.makeText(this, "Data pengiriman tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isFromSentTab) {
            // BTT from "Daftar BTT Terkirim" - Submit to API
            submitToApi(item, receiverName)
        } else {
            // BTT from "Daftar BTT Tertunda" - Save locally only
            saveLocally(item, receiverName)
        }
    }
    
    /**
     * Save data locally (for Tertunda tab)
     * Convert photo & signature to Base64 and save to Room DB
     */
    private fun saveLocally(item: DeliveryItem, receiverName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convert photo to Base64
                val fotoBase64 = if (photoUri != null) {
                    try {
                        contentResolver.openInputStream(photoUri!!)?.use { inputStream ->
                            ImageCompressor.uriToBase64(
                                inputStream,
                                maxWidth = 800,
                                maxHeight = 800,
                                quality = 75
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                
                // Convert signature to Base64
                val ttdBase64 = if (signatureBitmap != null) {
                    try {
                        ImageCompressor.bitmapToBase64(
                            signatureBitmap!!,
                            maxWidth = 800,
                            maxHeight = 800,
                            quality = 75
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                
                // Save to Room DB with Base64 data
                repository.markDeliveryAsSent(
                    deliveryItem = item,
                    receiverName = receiverName,
                    fotoBase64 = fotoBase64,
                    ttdBase64 = ttdBase64,
                    latitude = photoLatitude.toString(),
                    longitude = photoLongitude.toString()
                )
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LoperDetailActivity,
                        "Data disimpan lokal. BTT dipindahkan ke Daftar BTT Terkirim",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LoperDetailActivity,
                        "Gagal menyimpan data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    /**
     * Submit data to API (for Terkirim tab)
     * Send photo, signature, and receiver name to backend
     */
    private fun submitToApi(item: DeliveryItem, receiverName: String) {
        // Get user NIP from preferences
        CoroutineScope(Dispatchers.IO).launch {
            val userNip = userPreferences.getNip().first()
            
            // Prepare photo data (use restored Base64 or convert from URI)
            val photoBase64 = restoredFotoBase64 ?: if (photoUri != null) {
                try {
                    contentResolver.openInputStream(photoUri!!)?.use { inputStream ->
                        ImageCompressor.uriToBase64(
                            inputStream,
                            maxWidth = 800,
                            maxHeight = 800,
                            quality = 75
                        )
                    } ?: ""
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoperDetailActivity, "Gagal memproses foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    ""
                }
            } else {
                ""
            }
            
            // Prepare signature data (use restored Base64 or convert from Bitmap)
            val signatureBase64 = restoredTtdBase64 ?: if (signatureBitmap != null) {
                try {
                    ImageCompressor.bitmapToBase64(
                        signatureBitmap!!,
                        maxWidth = 800,
                        maxHeight = 800,
                        quality = 75
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoperDetailActivity, "Gagal memproses tanda tangan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    ""
                }
            } else {
                ""
            }
            
            // Create request object
            val request = SubmitDeliveryRequest(
                noLoper = item.noLoper,
                noBtt = item.noBtt,
                bTerimaYn = "Y", // Assuming received (Y) for now
                reasonId = "", // Reason ID can be empty for normal delivery
                bPenerima = receiverName,
                nip = userNip,
                lat = photoLatitude.toString(),
                lon = photoLongitude.toString(),
                foto = photoBase64,
                ttd = signatureBase64
            )
            
            // Submit data to API with koli barcode data
            try {
                repository.submitDeliveryDataWithKoli(request, item.noBtt).collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            withContext(Dispatchers.Main) {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.btnSave.isEnabled = false
                            }
                        }
                        is Result.Success -> {
                            // Delete from local storage (datastore/Room) upon successful API submission
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    repository.removeSentDelivery(item.noBtt)
                                } catch (e: Exception) {
                                    // Ignore deletion errors, UI logic should proceed
                                }
                            }
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@LoperDetailActivity,
                                    ErrorMessageHelper.getDeliverySubmitSuccess(),
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                        is Result.Error -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@LoperDetailActivity,
                                    ErrorMessageHelper.getDeliverySubmitError(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(
                        this@LoperDetailActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        const val EXTRA_DELIVERY_ITEM = "extra_delivery_item"
        const val EXTRA_IS_FROM_SENT_TAB = "extra_is_from_sent_tab"
    }
    
    /**
     * Show Fake GPS detection dialog and force exit app
     */
    private fun showFakeGpsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("FAKE GPS Terdeteksi")
            .setMessage("Anda terdeteksi menggunakan FAKE GPS. Silahkan matikan FAKE GPS-nya lalu gunakan aplikasi Dakota Group Staff kembali.")
            .setCancelable(false)
            .setPositiveButton("Baik, Saya Mengerti") { _, _ ->
                finishAffinity()
                exitProcess(0)
            }
            .show()
    }
}
