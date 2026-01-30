package com.dakotagroupstaff.data.local.model

/**
 * User session data model
 * Stored in DataStore for persistence
 */
data class UserSession(
    val nip: String = "",
    val nama: String = "",
    val atasan1: String = "",
    val atasan2: String = "",
    val pt: String = "C", // Default PT Logistik
    val imei: String = "",
    val simId: String = "",
    val email: String = "",
    val isLoggedIn: Boolean = false,
    val areaKerja: String = "",
    val taskCode: String = "",
    val taskDetail: String = "",
    val taskId: String = ""
)
