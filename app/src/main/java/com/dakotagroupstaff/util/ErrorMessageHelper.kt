package com.dakotagroupstaff.util

/**
 * Helper object untuk mengkonversi error teknis menjadi pesan user-friendly dalam Bahasa Indonesia
 * Menghindari menampilkan pesan error teknis seperti "HTTP 404", "Unauthorized", dll
 */
object ErrorMessageHelper {
    
    /**
     * Konversi HTTP status code ke pesan user-friendly
     */
    fun getHttpErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            401 -> "Sesi Anda telah berakhir, silakan login kembali"
            403 -> "Anda tidak memiliki akses untuk melakukan aksi ini"
            404 -> "Data tidak ditemukan"
            408 -> "Koneksi timeout, silakan coba lagi"
            500, 502, 503, 504 -> "Terjadi kesalahan pada server, coba lagi nanti"
            else -> "Terjadi kesalahan, silakan coba lagi"
        }
    }
    
    /**
     * Deteksi jenis error dari message
     */
    fun parseErrorMessage(errorMessage: String): String {
        return when {
            // Network errors
            errorMessage.contains("Unable to resolve host", ignoreCase = true) ||
            errorMessage.contains("No address associated with hostname", ignoreCase = true) ||
            errorMessage.contains("Network is unreachable", ignoreCase = true) ||
            errorMessage.contains("Unable to connect", ignoreCase = true) ||
            errorMessage.contains("Failed to connect", ignoreCase = true) ||
            errorMessage.contains("Connection refused", ignoreCase = true) ||
            errorMessage.contains("SocketTimeoutException", ignoreCase = true) ||
            errorMessage.contains("Network error", ignoreCase = true) ->
                "Tidak ada koneksi internet, periksa koneksi Anda"
            
            // Auth errors
            errorMessage.contains("Unauthorized", ignoreCase = true) ||
            errorMessage.contains("401", ignoreCase = true) ->
                "Sesi Anda telah berakhir, silakan login kembali"
            
            // Not found errors
            errorMessage.contains("Not Found", ignoreCase = true) ||
            errorMessage.contains("404", ignoreCase = true) ->
                "Data tidak ditemukan"
            
            // Server errors
            errorMessage.contains("500", ignoreCase = true) ||
            errorMessage.contains("502", ignoreCase = true) ||
            errorMessage.contains("503", ignoreCase = true) ||
            errorMessage.contains("Internal Server Error", ignoreCase = true) ->
                "Terjadi kesalahan pada server, coba lagi nanti"
            
            // Timeout errors  
            errorMessage.contains("timeout", ignoreCase = true) ->
                "Koneksi timeout, silakan coba lagi"
            
            // If message already in Indonesian or user-friendly, keep it
            !errorMessage.contains("HTTP", ignoreCase = true) &&
            !errorMessage.contains("Exception", ignoreCase = true) &&
            !errorMessage.contains("Error:", ignoreCase = true) ->
                errorMessage
            
            // Default fallback
            else -> "Terjadi kesalahan, silakan coba lagi"
        }
    }
    
    // ============ ABSENSI (ATTENDANCE) ============
    
    fun getAttendanceSuccessMessage(isCheckIn: Boolean): String {
        return if (isCheckIn) {
            "Absen Masuk Berhasil"
        } else {
            "Absen Pulang Berhasil"
        }
    }
    
    fun getAttendanceErrorMessage(isCheckIn: Boolean): String {
        return if (isCheckIn) {
            "Absen Masuk Gagal"
        } else {
            "Absen Pulang Gagal"
        }
    }
    
    fun getAttendanceHistoryErrorMessage(): String {
        return "Gagal memuat riwayat absensi"
    }
    
    // ============ SURAT TUGAS (ASSIGNMENT) ============
    
    fun getAssignmentLocationUpdateSuccess(): String {
        return "Lokasi berhasil diperbarui"
    }
    
    fun getAssignmentLocationUpdateError(): String {
        return "Gagal memperbarui lokasi"
    }
    
    fun getAssignmentCostSaveSuccess(): String {
        return "Biaya berhasil disimpan"
    }
    
    fun getAssignmentCostSaveError(): String {
        return "Gagal menyimpan biaya"
    }
    
    fun getAssignmentListLoadError(): String {
        return "Gagal memuat daftar surat tugas"
    }
    
    // ============ LOPER (DELIVERY) ============
    
    fun getDeliverySubmitSuccess(): String {
        return "Data pengiriman berhasil disimpan"
    }
    
    fun getDeliverySubmitError(): String {
        return "Gagal menyimpan data pengiriman"
    }
    
    fun getDeliveryListLoadError(): String {
        return "Gagal memuat daftar BTT"
    }
    
    fun getDeliveryPhotoUploadSuccess(): String {
        return "Foto berhasil diunggah"
    }
    
    fun getDeliveryPhotoUploadError(): String {
        return "Gagal mengunggah foto"
    }
    
    // ============ SLIP GAJI (SALARY) ============
    
    fun getSalaryListLoadSuccess(): String {
        return "Data slip gaji berhasil dimuat"
    }
    
    fun getSalaryListLoadError(): String {
        return "Gagal memuat slip gaji"
    }
    
    // ============ CUTI/IZIN/SAKIT (LEAVE) ============
    
    fun getLeaveSubmitSuccess(): String {
        return "Pengajuan cuti berhasil dikirim"
    }
    
    fun getLeaveSubmitError(): String {
        return "Gagal mengirim pengajuan cuti"
    }
    
    fun getLeaveHistoryLoadSuccess(): String {
        return "Data cuti berhasil dimuat"
    }
    
    fun getLeaveHistoryLoadError(): String {
        return "Gagal memuat data cuti"
    }
    
    fun getLeaveBalanceLoadError(): String {
        return "Gagal memuat saldo cuti"
    }
    
    // ============ APPROVAL ============
    
    fun getApprovalSubmitSuccess(): String {
        return "Persetujuan berhasil dikirim"
    }
    
    fun getApprovalSubmitError(): String {
        return "Gagal memberikan persetujuan"
    }
    
    fun getApprovalRejectSuccess(): String {
        return "Penolakan berhasil dikirim"
    }
    
    fun getApprovalRejectError(): String {
        return "Gagal menolak pengajuan"
    }
    
    fun getApprovalListLoadError(): String {
        return "Gagal memuat daftar persetujuan"
    }
    
    // ============ LOGIN ============
    
    fun getLoginError(): String {
        return "Login gagal, periksa NIP dan koneksi internet"
    }
    
    fun getLoginSuccess(): String {
        return "Login berhasil"
    }
}
