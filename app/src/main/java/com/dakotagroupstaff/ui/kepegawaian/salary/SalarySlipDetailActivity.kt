package com.dakotagroupstaff.ui.kepegawaian.salary

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.remote.response.SalarySlipData
import com.dakotagroupstaff.databinding.ActivitySalarySlipDetailBinding
import com.dakotagroupstaff.utils.SalaryDataHelper
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Salary Slip Detail Activity
 * Migrated from React Native: OldSystemSlipGaji/detail/index.js
 * 
 * Features:
 * - Display detailed salary slip information
 * - Export to PDF
 * - Save to Downloads folder
 */
class SalarySlipDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalarySlipDetailBinding
    private var salarySlip: SalarySlipData? = null

    companion object {
        const val EXTRA_SALARY_SLIP = "extra_salary_slip"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val TAG = "SalarySlipDetail"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalarySlipDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        salarySlip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SALARY_SLIP, SalarySlipData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_SALARY_SLIP)
        }

        setupToolbar()
        displaySalarySlip()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun displaySalarySlip() {
        salarySlip?.let { slip ->
            with(binding) {
                // Employee Information
                tvNip.text = ": ${slip.nip}"
                tvName.text = ": ${slip.nama}"
                tvDivision.text = ": ${getDivisionShortcut(slip.divisi)}"
                tvPosition.text = ": ${slip.namaJabatan}"
                tvArea.text = ": ${slip.area}"
                
                // Period
                val dateFormat = SimpleDateFormat("MM/yyyy", Locale("id", "ID"))
                try {
                    val priodeParts = slip.priode.split("/")
                    if (priodeParts.size >= 3) {
                        tvPeriod.text = ": ${priodeParts[0].padStart(2, '0')}/${priodeParts[2]}"
                    } else {
                        tvPeriod.text = ": ${slip.getFormattedPeriod()}"
                    }
                } catch (e: Exception) {
                    tvPeriod.text = ": ${slip.getFormattedPeriod()}"
                }

                // Build income and deduction layouts
                buildIncomeLayout(slip)
                buildDeductionLayout(slip)
            }
        } ?: run {
            Toast.makeText(this, "Data slip gaji tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun buildIncomeLayout(slip: SalarySlipData) {
        val layout = binding.layoutIncome
        layout.removeAllViews()

        // Title
        addSectionTitle(layout, "Pendapatan")

        // Income items — semua ditampilkan meskipun 0, sesuai aplikasi lama
        addIncomeItem(layout, "Gaji Pokok", slip.gapok)
        addIncomeItem(layout, "Insentif", slip.insentif)
        addIncomeItem(layout, "THR / Bonus", slip.thr)
        addIncomeItem(layout, "Insentif PPH21", slip.insentifPph)
        addIncomeItem(layout, "Pot Pengembalian", slip.pengembalianPo)

        // Tunjangan subsection
        addSubsectionTitle(layout, "Tunjangan")
        addIncomeItem(layout, "BPJS.P", slip.bpjsp)
        addIncomeItem(layout, "Transport", slip.transport)
        addIncomeItem(layout, "Kesehatan", slip.kesehatan)
        addIncomeItem(layout, "Keluarga", slip.keluarga)
        addIncomeItem(layout, "Jabatan", slip.jabatan)
        addIncomeItem(layout, "Asuransi", slip.asuransi)
        
        // Total Pendapatan
        addSubTotalSection(layout, "Total Pendapatan", slip.getTotalIncome())
    }

    private fun buildDeductionLayout(slip: SalarySlipData) {
        val layout = binding.layoutDeduction
        layout.removeAllViews()

        // Title
        addSectionTitle(layout, "Potongan")

        // Deduction items — semua ditampilkan meskipun 0, sesuai aplikasi lama
        addDeductionItem(layout, "Jamsostek", slip.jamsostek)
        addDeductionItem(layout, "BPJS.P", slip.bpjsp)
        addDeductionItem(layout, "Koperasi", slip.koperasi)
        addDeductionItem(layout, "Klaim", slip.klaim)
        addDeductionItem(layout, "BPJS.K", slip.bpjsk)
        addDeductionItem(layout, "PPh21", slip.pph21)
        addDeductionItem(layout, "Asuransi", slip.asuransi)
        addDeductionItem(layout, "Absensi", slip.absensi)
        addDeductionItem(layout, "Iuran Paguyuban", slip.iuranPaguyuban)
        addDeductionItem(layout, "Lain-Lain", slip.lain)

        // Total Potongan
        addSubTotalSection(layout, "Total Potongan", slip.getTotalDeductions())

        // Total Gaji (Gaji Bersih)
        addTotalSection(layout, slip.getNetSalary())
    }

    private fun addSectionTitle(parent: LinearLayout, title: String) {
        val textView = TextView(this).apply {
            text = title
            textSize = 12f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 12)
        }
        parent.addView(textView)
    }

    private fun addSubsectionTitle(parent: LinearLayout, title: String) {
        val textView = TextView(this).apply {
            text = title
            textSize = 10f
            setTextColor(Color.BLACK)
            setPadding(0, 8, 0, 8)
        }
        parent.addView(textView)
    }

    private fun addIncomeItem(parent: LinearLayout, label: String, amount: Int) {
        addSalaryItem(parent, label, amount)
    }

    private fun addDeductionItem(parent: LinearLayout, label: String, amount: Int) {
        addSalaryItem(parent, label, amount)
    }

    private fun addSalaryItem(parent: LinearLayout, label: String, amount: Int) {
        val container = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
            orientation = LinearLayout.HORIZONTAL
        }

        val labelView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0.6f
            )
            text = label
            textSize = 12f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val valueView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0.4f
            )
            text = ": ${SalaryDataHelper.formatCurrency(amount)}"
            textSize = 12f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        container.addView(labelView)
        container.addView(valueView)
        parent.addView(container)
    }

    private fun addTotalSection(parent: LinearLayout, totalSalary: Int) {
        val container = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(ContextCompat.getColor(this@SalarySlipDetailActivity, R.color.gray))
            setPadding(8, 8, 8, 8)
        }

        val labelView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0.6f
            )
            text = "Total Gaji"
            textSize = 12f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val valueView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0.4f
            )
            text = ": ${SalaryDataHelper.formatCurrency(totalSalary)}"
            textSize = 12f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        container.addView(labelView)
        container.addView(valueView)
        parent.addView(container)
    }
    
    private fun addSubTotalSection(parent: LinearLayout, label: String, amount: Int) {
        val container = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
                bottomMargin = 8
            }
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(ContextCompat.getColor(this@SalarySlipDetailActivity, android.R.color.darker_gray))
            setPadding(8, 8, 8, 8)
        }

        val labelView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0.6f
            )
            text = label
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val valueView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0.4f
            )
            text = ": ${SalaryDataHelper.formatCurrency(amount)}"
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        container.addView(labelView)
        container.addView(valueView)
        parent.addView(container)
    }

    private fun getDivisionShortcut(division: String): String {
        return when (division) {
            "Informasi Teknologi" -> "IT"
            "Customer Service" -> "CS"
            "Bongkar Muat" -> "BM"
            "Human Resources Development" -> "HRD"
            "Business Development" -> "BD"
            else -> division
        }
    }

    private fun setupListeners() {
        binding.fabDownload.setOnClickListener {
            checkPermissionAndDownload()
        }
    }

    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 and above - no permission needed for app-specific directory
            generateAndSavePDF()
        } else {
            // Below Android 10 - need storage permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                generateAndSavePDF()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateAndSavePDF()
            } else {
                Toast.makeText(this, "Izin penyimpanan diperlukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateAndSavePDF() {
        salarySlip?.let { slip ->
            try {
                Log.d(TAG, "Starting PDF generation...")
                
                // Ensure view is laid out and measured
                val content = binding.layoutContent
                if (content.width == 0 || content.height == 0) {
                    content.post {
                        generateAndSavePDF()
                    }
                    return
                }
                
                Log.d(TAG, "View dimensions: ${content.width}x${content.height}")
                
                // Create bitmap from view
                val bitmap = createBitmapFromView()
                Log.d(TAG, "Bitmap created: ${bitmap.width}x${bitmap.height}")

                // Create PDF
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val page = pdfDocument.startPage(pageInfo)

                // Draw bitmap on PDF
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                Log.d(TAG, "PDF document created")

                // Save PDF based on Android version
                val fileName = "SlipGaji-${slip.bulan}-${slip.tahun}.pdf"
                val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    savePDFToMediaStore(pdfDocument, fileName)
                } else {
                    savePDFToDownloads(pdfDocument, fileName)
                }

                pdfDocument.close()
                bitmap.recycle()

                if (savedUri != null) {
                    Toast.makeText(
                        this,
                        "PDF berhasil disimpan ke Downloads",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d(TAG, "PDF saved successfully to: $savedUri")
                } else {
                    throw Exception("Failed to save PDF")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating PDF", e)
                Toast.makeText(
                    this, 
                    "Gagal menyimpan PDF: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        } ?: run {
            Toast.makeText(this, "Data slip gaji tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Save PDF using MediaStore (Android 10+)
     */
    private fun savePDFToMediaStore(pdfDocument: PdfDocument, fileName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                    outputStream.flush()
                }
                Log.d(TAG, "PDF saved via MediaStore: $it")
                it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PDF to MediaStore", e)
            null
        }
    }

    /**
     * Save PDF to Downloads folder (Android 9 and below)
     */
    private fun savePDFToDownloads(pdfDocument: PdfDocument, fileName: String): Uri? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            // Create downloads directory if it doesn't exist
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
            }
            
            Log.d(TAG, "PDF saved to: ${file.absolutePath}")
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PDF to Downloads", e)
            null
        }
    }

    private fun createBitmapFromView(): Bitmap {
        val content = binding.layoutContent
        
        // Measure the view if not measured yet
        if (content.width == 0 || content.height == 0) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(
                resources.displayMetrics.widthPixels,
                View.MeasureSpec.EXACTLY
            )
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            content.measure(widthSpec, heightSpec)
            content.layout(0, 0, content.measuredWidth, content.measuredHeight)
        }
        
        val bitmap = Bitmap.createBitmap(
            content.width,
            content.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        content.draw(canvas)
        
        return bitmap
    }
}
