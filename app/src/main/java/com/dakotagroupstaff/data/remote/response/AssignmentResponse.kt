package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Response models for Letter of Assignment (Surat Tugas) feature
 * Based on React Native implementation from OldSystemLetterOfAssign
 * 
 * APIs used:
 * - POST /delivery/letter-of-assign - Get assignment details
 * - POST /assignment/list - Get all assignments
 * - POST /assignment/update-location - Update GPS location
 */

/**
 * Letter of Assignment Details
 * From: /delivery/letter-of-assign
 * Fixed to match actual API response format
 */
data class LetterOfAssignmentData(
    @SerializedName("sID")
    val sID: String = "",  // Assignment ID (e.g., "001JC2026010001")
    
    @SerializedName("Keterangan")
    val keterangan: String = "",  // Assignment description
    
    @SerializedName("StartAgen")
    val startAgen: Int = 0,  // Starting agent code
    
    @SerializedName("TglBerangkat")
    val tglBerangkat: String = "",  // Departure date
    
    @SerializedName("TglKembali")
    val tglKembali: String = "",  // Return date
    
    @SerializedName("NoKendaraan")
    val noKendaraan: String = "",  // Vehicle license plate
    
    @SerializedName("Supir1NIP")
    val supir1NIP: String = "",  // Driver 1 NIP
    
    @SerializedName("Supir1Nama")
    val supir1Nama: String = "",  // Driver 1 Name
    
    @SerializedName("Supir2NIP")
    val supir2NIP: String = "",  // Driver 2 NIP
    
    @SerializedName("Supir2Nama")
    val supir2Nama: String = "",  // Driver 2 Name
    
    @SerializedName("TrUrut")
    val trUrut: String = "",  // Route order
    
    @SerializedName("TrStatus")
    val trStatus: String = "",  // Route status
    
    @SerializedName("TrKdCabang")
    val trKdCabang: String = "",  // Branch code
    
    @SerializedName("TrCabang")
    val trCabang: String = "",  // Branch name
    
    @SerializedName("TrCabangLatH")
    val trCabangLatH: String = "",  // Branch latitude
    
    @SerializedName("TrCabangLongH")
    val trCabangLongH: String = "",  // Branch longitude
    
    @SerializedName("TrLastEvent")
    val trLastEvent: String = "",  // Last event
    
    @SerializedName("TrAbsenIn")
    val trAbsenIn: String = "",  // Check-in time
    
    @SerializedName("TrAbsenOut")
    val trAbsenOut: String = "",  // Check-out time
    
    @SerializedName("TrKm")
    val trKm: String = "",  // KM reading
    
    @SerializedName("TrLat")
    val trLat: String = "",  // Current latitude
    
    @SerializedName("TrLong")
    val trLong: String = "",  // Current longitude
    
    @SerializedName("TrURLPic")
    val trURLPic: String = "",  // Photo URL
    
    @SerializedName("lastcekin")
    val lastcekin: String = "0",  // Last check-in
    
    @SerializedName("arah")
    val arah: String = ""  // Direction (B/P)
) {
    // Computed properties for backward compatibility
    val tgl: String get() = tglBerangkat
    val supir2NamaOrNull: String? get() = if (supir2Nama.isNotEmpty()) supir2Nama else null
    val mobil: String get() = ""  // Not provided in API
    val noMobil: String get() = noKendaraan
    val dariKota: String get() = ""  // Not provided in API
    val keKota: String get() = trCabang
    val rute: String get() = keterangan
    val tujuan: String get() = trCabang
    val kmAwal: String get() = trKm
    val kmAkhir: String? get() = null  // Not provided in API
    val status: String get() = if (trStatus.isEmpty()) "Aktif" else trStatus
}

/**
 * Assignment List Item
 * From: /assignment/list
 * Response contains only sID based on provided API response
 */
data class AssignmentListItem(
    @SerializedName("sID")
    val sID: String,  // Assignment ID
    
    // Optional fields that might be included in some responses
    @SerializedName("Tgl")
    val tgl: String? = null,  // Date (if included) - nullable
    
    @SerializedName("Supir1Nama")
    val supir1Nama: String? = null,  // Driver name (if included) - nullable
    
    @SerializedName("Mobil")
    val mobil: String? = null,  // Vehicle (if included) - nullable
    
    @SerializedName("NoMobil")
    val noMobil: String? = null,  // License plate (if included) - nullable
    
    @SerializedName("DariKota")
    val dariKota: String? = null,  // Origin (if included) - nullable
    
    @SerializedName("KeKota")
    val keKota: String? = null,  // Destination (if included) - nullable
    
    @SerializedName("Status")
    val status: String? = null,  // Status (if included) - nullable
    
    @SerializedName("TotalBiaya")
    val totalBiaya: String? = null  // Total cost (if included) - nullable
)

/**
 * Update Location Response
 * From: /assignment/update-location
 */
data class UpdateLocationData(
    @SerializedName("SUCCESS")
    val success: String,  // "DONE" or "NOGPS"
    
    @SerializedName("sID")
    val sID: String,  // Assignment ID
    
    @SerializedName("hasGPS")
    val hasGPS: Boolean  // Whether GPS coordinates were provided
)

/**
 * Operational Cost Item (Master Data)
 * From: /operational-cost/items
 */
data class OperationalCostItem(
    @SerializedName("Item_ID")
    val itemId: String,  // Item ID/Code (e.g., "BBM001")
    
    @SerializedName("Item_Name")
    val itemName: String,  // Item name (e.g., "BBM Premium")
    
    @SerializedName("Item_Type")
    val itemType: String? = null,  // Item type/category
    
    @SerializedName("Item_Price")
    val itemPrice: String? = null  // Default price (optional)
)

/**
 * Down Payment Operational Cost
 * From: /operational-cost/list (type=dp)
 */
data class DownPaymentCost(
    @SerializedName("SJR_BPLKID")
    val itemId: String,  // Item ID
    
    @SerializedName("Item_Name")
    val itemName: String,  // Item name
    
    @SerializedName("SJR_Nominal")
    val nominal: String  // Amount
)

/**
 * Additional Operational Cost
 * From: /operational-cost/list (type=op)
 */
data class AdditionalOperationalCost(
    @SerializedName("SJR_BPLKID")
    val itemId: String,  // Item ID
    
    @SerializedName("Item_Name")
    val itemName: String,  // Item name
    
    @SerializedName("SJR_Nominal")
    val nominal: String,  // Amount
    
    @SerializedName("SJR_Ket")
    val keterangan: String? = null,  // Notes (optional)
    
    @SerializedName("SJR_ID")
    val sjrId: String? = null  // Record ID for deletion
)

/**
 * Fuel/BBM Record (Voucher Non-Cash)
 * From: /operational-cost/list (type=vcn)
 */
data class FuelRecord(
    @SerializedName("BBM_ID")
    val bbmId: String,  // Record ID
    
    @SerializedName("BBM_Date")
    val date: String,  // Date
    
    @SerializedName("BBM_Km")
    val km: String,  // KM reading
    
    @SerializedName("BBM_Isi")
    val isi: String,  // Amount/Volume
    
    @SerializedName("BBM_Harga")
    val harga: String  // Price
)

/**
 * Voucher Cost
 * From: /operational-cost/list (type=vc)
 */
data class VoucherCost(
    @SerializedName("BPLKID")
    val itemId: String,  // Item ID
    
    @SerializedName("Item_Name")
    val itemName: String,  // Item name
    
    @SerializedName("Harga")
    val harga: String  // Price
)

/**
 * Save Operational Cost Response
 * From: /operational-cost/save
 */
data class SaveOperationalCostData(
    @SerializedName("message")
    val message: String,  // Success message
    
    @SerializedName("sID")
    val sID: String,  // Assignment ID
    
    @SerializedName("recordsAffected")
    val recordsAffected: Int? = null  // Number of records affected
)

/**
 * Check Approval Status Response
 * From: /operational-cost/check-approval
 */
data class ApprovalStatusData(
    @SerializedName("approved")
    val approved: Boolean,  // Whether approved
    
    @SerializedName("status")
    val status: String,  // Status text
    
    @SerializedName("sID")
    val sID: String,  // Assignment ID
    
    @SerializedName("approvalDate")
    val approvalDate: String? = null,  // Approval date (if approved)
    
    @SerializedName("approvedBy")
    val approvedBy: String? = null  // Who approved (if approved)
)

/**
 * Request model for getting letter of assignment
 */
data class LetterOfAssignRequest(
    @SerializedName("nip")
    val nip: String
)

/**
 * Request model for update location
 */
data class UpdateLocationRequest(
    @SerializedName("sID")
    val sID: String,
    
    @SerializedName("lat")
    val lat: String? = null,
    
    @SerializedName("lon")
    val lon: String? = null
)

/**
 * Request model for operational cost list
 */
data class OperationalCostListRequest(
    @SerializedName("sID")
    val sID: String,
    
    @SerializedName("tipe")
    val tipe: String  // "op", "dp", "vcn", "vc"
)

/**
 * Request model for save operational cost
 */
data class SaveOperationalCostRequest(
    @SerializedName("sID")
    val sID: String,
    
    @SerializedName("itemId")
    val itemId: String,
    
    @SerializedName("nominal")
    val nominal: String,
    
    @SerializedName("keterangan")
    val keterangan: String? = null,
    
    @SerializedName("tipe")
    val tipe: String  // "insert" or "update"
)

/**
 * Request model for check approval
 */
data class CheckApprovalRequest(
    @SerializedName("sID")
    val sID: String
)
