package com.dakotagroupstaff.data.mapper

import com.dakotagroupstaff.data.local.entity.LeaveBalanceEntity
import com.dakotagroupstaff.data.local.entity.LeaveDetailsEntity
import com.dakotagroupstaff.data.remote.response.LeaveBalanceData
import com.dakotagroupstaff.data.remote.response.LeaveDetailsData

/**
 * Mapper for Leave Balance
 */
fun LeaveBalanceData.toEntity(nip: String, tahun: String): LeaveBalanceEntity {
    return LeaveBalanceEntity(
        nip = nip,
        tahun = tahun,
        saldoCuti = this.saldoCuti,
        jumlahCuti = this.jumlahCuti,
        cutiTerpakai = this.cutiTerpakai
    )
}

/**
 * Mapper for Leave Details
 * Converts API response format to cached entity format
 */
fun LeaveDetailsData.toEntity(nip: String, tahun: String): LeaveDetailsEntity {
    // Convert date strings to timestamps (simplified - you may need proper date parsing)
    val tglAwal = try {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .parse(this.mulai)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    
    val tglAkhir = try {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .parse(this.akhir)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    
    return LeaveDetailsEntity(
        leaveId = this.id,
        nip = nip,
        tahun = tahun,
        tglAwal = tglAwal,
        tglAkhir = tglAkhir,
        jenisCuti = this.status, // Using status as leave type (Cuti, Izin, Sakit, etc.)
        keterangan = this.keterangan,
        status = this.status,
        potongGaji = this.potongGaji,
        potongCuti = this.potongCuti,
        dispensasi = this.dispensasi,
        atasan1Approve = this.atasan1,
        atasan2Approve = this.atasan2
    )
}
