package com.dakotagroupstaff.data.remote.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

/**
 * Salary Slip Response Data
 * Based on API endpoint: POST /salary/slips?pt=<pt>
 * 
 * Example response item:
 * {
 *   "NAMA": "MUHAMMAD ASHARI ABDILLAH",
 *   "NIP": "0012404003",
 *   "ID": "00106250014",
 *   "PRIODE": "6/28/2025",
 *   "BULAN": "June",
 *   "TAHUN": "2025",
 *   "GAPOK": 0,
 *   "INSENTIF": 0,
 *   ...
 * }
 */
@Parcelize
data class SalarySlipData(
    @SerializedName("NAMA")
    val nama: String,
    
    @SerializedName("NIP")
    val nip: String,
    
    @SerializedName("ID")
    val id: String,
    
    @SerializedName("GAPOK")
    val gapok: Int,
    
    @SerializedName("INSENTIF")
    val insentif: Int,
    
    @SerializedName("PENGEMBALIANPO")
    val pengembalianPo: Int,
    
    @SerializedName("INSENTIFPPH")
    val insentifPph: Int,
    
    @SerializedName("THR")
    val thr: String,
    
    @SerializedName("BPJSP")
    val bpjsp: String? = null,
    
    @SerializedName("TRANSPORT")
    val transport: Int,
    
    @SerializedName("KESEHATAN")
    val kesehatan: Int,
    
    @SerializedName("KELUARGA")
    val keluarga: Int,
    
    @SerializedName("JABATAN")
    val jabatan: Int,
    
    @SerializedName("ASURANSI")
    val asuransi: Int,
    
    @SerializedName("JAMSOSTEK")
    val jamsostek: Int,
    
    @SerializedName("KOPERASI")
    val koperasi: Int,
    
    @SerializedName("KLAIM")
    val klaim: Int,
    
    @SerializedName("BPJSK")
    val bpjsk: String? = null,
    
    @SerializedName("PPH21")
    val pph21: Int,
    
    @SerializedName("ABSENSI")
    val absensi: Int,
    
    @SerializedName("LAIN")
    val lain: Int,
    
    @SerializedName("BULAN")
    val bulan: String, // e.g., "June", "May"
    
    @SerializedName("DIVISI")
    val divisi: String,
    
    @SerializedName("NAMAJABATAN")
    val namaJabatan: String,
    
    @SerializedName("AREA")
    val area: String,
    
    /**
     * PRIODE format: "M/D/YYYY" or "MM/DD/YYYY"
     * Examples: "6/28/2025", "12/30/2024"
     */
    @SerializedName("PRIODE")
    val priode: String,
    
    @SerializedName("TAHUN")
    val tahun: String, // e.g., "2025", "2024"
    
    @SerializedName("IURANPAGUYUBAN")
    val iuranPaguyuban: Int
) : Parcelable {
    /**
     * Parse the PRIODE string to extract year
     * @return Year as Int (e.g., 2025)
     */
    fun getYear(): Int {
        return try {
            tahun.toIntOrNull() ?: parsePriodeDate()?.let { 
                SimpleDateFormat("M/d/yyyy", Locale.US).parse(it)?.let { date ->
                    Calendar.getInstance().apply { time = date }.get(Calendar.YEAR)
                }
            } ?: 0
        } catch (e: Exception) {
            tahun.toIntOrNull() ?: 0
        }
    }
    
    /**
     * Parse the PRIODE string to extract month
     * @return Month as Int (1-12)
     */
    fun getMonth(): Int {
        return try {
            // Try to parse from PRIODE string "M/D/YYYY"
            val parts = priode.split("/")
            if (parts.size >= 1) {
                parts[0].toIntOrNull() ?: 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get month name from PRIODE
     * @return Month name (e.g., "January", "February")
     */
    fun getMonthName(): String {
        return bulan
    }
    
    /**
     * Parse PRIODE to Date object
     * @return Date object or null if parsing fails
     */
    fun parsePriodeDate(): String? {
        return priode.takeIf { it.isNotEmpty() }
    }
    
    /**
     * Get formatted period string for display
     * @return Formatted string like "June 2025"
     */
    fun getFormattedPeriod(): String {
        return "$bulan $tahun"
    }
    
    /**
     * Calculate total income (pendapatan)
     */
    fun getTotalIncome(): Int {
        val thrAmount = thr.replace(".", "").toIntOrNull() ?: 0
        val bpjspAmount = bpjsp?.replace(".", "")?.toIntOrNull() ?: 0
        return gapok + insentif + pengembalianPo + insentifPph + 
               thrAmount + bpjspAmount + transport + kesehatan + keluarga + jabatan + asuransi
    }
    
    /**
     * Calculate total deductions (potongan)
     */
    fun getTotalDeductions(): Int {
        val bpjskAmount = bpjsk?.replace(".", "")?.toIntOrNull() ?: 0
        val bpjspAmount = bpjsp?.replace(".", "")?.toIntOrNull() ?: 0
        return jamsostek + koperasi + klaim + pph21 + absensi + lain + iuranPaguyuban + bpjskAmount + bpjspAmount
    }
    
    /**
     * Calculate net salary (gaji bersih)
     */
    fun getNetSalary(): Int {
        return getTotalIncome() - getTotalDeductions()
    }
}

/**
 * Request body for getting salary slips
 */
data class SalarySlipsRequest(
    val nip: String,
    val imei: String,
    val simId: String
)

/**
 * Data class for grouping salary slips by year
 */
data class SalaryByYear(
    val year: Int,
    val salarySlips: List<SalarySlipData>
) {
    /**
     * Group salary slips by month
     * @return Map of month number to list of salary slips
     */
    fun groupByMonth(): Map<Int, List<SalarySlipData>> {
        return salarySlips.groupBy { it.getMonth() }
    }
    
    /**
     * Get total net salary for the year
     */
    fun getTotalNetSalary(): Int {
        return salarySlips.sumOf { it.getNetSalary() }
    }
}

/**
 * Data class for grouping salary slips by month
 */
data class SalaryByMonth(
    val year: Int,
    val month: Int,
    val monthName: String,
    val salarySlips: List<SalarySlipData>
) {
    /**
     * Get total net salary for the month
     */
    fun getTotalNetSalary(): Int {
        return salarySlips.sumOf { it.getNetSalary() }
    }
    
    /**
     * Get formatted period (e.g., "June 2025")
     */
    fun getFormattedPeriod(): String {
        return "$monthName $year"
    }
}
