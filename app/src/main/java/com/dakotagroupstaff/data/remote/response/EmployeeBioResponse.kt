package com.dakotagroupstaff.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * Employee Bio Response Data
 * Endpoint: POST /employee/bio?pt=<pt>
 */
data class EmployeeBioRequest(
    @SerializedName("nip")
    val nip: String
)

data class EmployeeBioData(
    @SerializedName("NIP")
    val nip: String,

    @SerializedName("Nama")
    val nama: String,

    @SerializedName("BPJS")
    val bpjs: String,

    @SerializedName("JAMSOSTEK")
    val jamsostek: String,

    @SerializedName("STATUS SOSIAL")
    val statusSosial: String,

    @SerializedName("NPWP")
    val npwp: String,

    @SerializedName("SHIFT")
    val shift: String,

    @SerializedName("MASUK")
    val masuk: String,

    @SerializedName("KELUAR")
    val keluar: String,

    @SerializedName("StatusPegawai")
    val statusPegawai: String
)
