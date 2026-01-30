package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Attendance History Response Data
 * Based on API endpoint: POST /api/v1/attendance?pt=<pt>
 * Backend returns: NIP, NAMA, CABANGABSEN, TANGGALJAMABSEN, KETERANGAN
 */
data class AttendanceHistoryData(
    @SerializedName("ABS_ID")
    val absId: Int,
    
    @SerializedName("ABS_NIP")
    val absNip: String,
    
    @SerializedName("ABS_TANGGAL")
    val absTanggal: String, // Format: "YYYY-MM-DD" or timestamp
    
    @SerializedName("ABS_SCHEDULE")
    val absSchedule: String,
    
    @SerializedName("ABS_IN_TIME")
    val absInTime: String? = null,
    
    @SerializedName("ABS_IN_LOCATION")
    val absInLocation: String? = null,
    
    @SerializedName("ABS_OUT_TIME")
    val absOutTime: String? = null,
    
    @SerializedName("ABS_OUT_LOCATION")
    val absOutLocation: String? = null,
    
    @SerializedName("ABS_STATUS")
    val absStatus: String,
    
    @SerializedName("KETERANGAN")
    val keterangan: String = "H" // M=Masuk, K=Keluar, H=Hadir, O=Out
)

/**
 * Submit Attendance Request
 * For API endpoint: POST /api/v1/attendance/submit?pt=<pt>
 */
data class SubmitAttendanceRequest(
    val nip: String,
    val kodeCabang: String, // MD5 code from nearest agent
    val latitude: String,
    val longitude: String,
    val schedule: String, // "M" for Masuk (In), "K" for Keluar (Out)
    val deviceId: String? = null,
    val serialNumber: String? = null
)

/**
 * Submit Attendance Response Data
 * Backend returns: { pt, nip, kodeCabang, latitude, longitude, schedule, absId }
 */
data class SubmitAttendanceData(
    @SerializedName("pt")
    val pt: String? = null,
    
    @SerializedName("nip")
    val nip: String? = null,
    
    @SerializedName("kodeCabang")
    val kodeCabang: String? = null,
    
    @SerializedName("latitude")
    val latitude: String? = null,
    
    @SerializedName("longitude")
    val longitude: String? = null,
    
    @SerializedName("schedule")
    val schedule: String? = null,
    
    @SerializedName("absId")
    val absId: Int? = null
)
