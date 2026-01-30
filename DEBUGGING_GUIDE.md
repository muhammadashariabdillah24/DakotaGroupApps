# Panduan Debugging - JSON Parsing Error

## 🐛 Error yang Terjadi
```
Error submitting attendance: java.lang.IllegalStateException: 
Expected BEGIN_OBJECT but was STRING at line 1 column 2 path $
```

## 🔍 Root Cause
Error ini disebabkan oleh **ketidakcocokan struktur JSON** antara response success dan error dari backend:

**Success Response:**
```json
{
  "success": true,
  "message": "...",     ← message di level root
  "data": {...}
}
```

**Error Response:**
```json
{
  "success": false,
  "error": {
    "message": "...",   ← message di dalam object error
    "statusCode": 400
  }
}
```

Model `ApiResponse<T>` sebelumnya memiliki field `message` yang **non-nullable**, sehingga Gson gagal parsing error response.

## ✅ Solusi yang Diterapkan

### 1. **ApiResponse Model - Made Nullable**
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,      // ✅ Sekarang nullable
    val data: T? = null,
    val error: ErrorResponse? = null,
    val timestamp: String? = null
) {
    fun getResponseMessage(): String {
        return message ?: error?.message ?: "Unknown error"
    }
}
```

### 2. **Enhanced Logging**
Menambahkan interceptor untuk menangkap raw response:
- Log URL endpoint
- Log HTTP status code
- Log Content-Type header
- Log raw response body

### 3. **Repository Updates**
Semua repository sekarang menggunakan `getResponseMessage()` untuk mendapatkan pesan dari response success atau error.

## 📋 Langkah Testing

### Step 1: Build & Install
```bash
cd "d:\Documents_Backup\ProjectsAndroid\Dakota\DakotaGroupStaff"
./gradlew assembleDebug
```

### Step 2: Install ke Device/Emulator
- Klik Run di Android Studio, ATAU
- Install manual: `adb install app/build/outputs/apk/debug/app-debug.apk`

### Step 3: Buka Logcat di Android Studio
1. Buka tab **Logcat** (bottom panel)
2. Filter by: `ApiConfig` atau `AttendanceRepository`
3. Level: **Debug**

### Step 4: Test Attendance Submission
1. Login ke aplikasi
2. Buka menu **Kepegawaian** → **Absensi**
3. Pastikan GPS aktif dan lokasi terdeteksi
4. Tekan tombol **"Absen Masuk"**
5. Perhatikan log di Logcat

## 📊 Log yang Diharapkan

### ✅ Success Case:
```
D/ApiConfig: === RAW RESPONSE ===
D/ApiConfig: URL: https://stagingdakota.my.id/api/v1/attendance/submit?pt=Logistik
D/ApiConfig: Status Code: 200
D/ApiConfig: Content-Type: application/json; charset=utf-8
D/ApiConfig: Response Body: {"success":true,"message":"Attendance submitted successfully to PT Logistik","data":{...},"timestamp":"..."}
D/ApiConfig: ====================
D/AttendanceRepository: Submit response: success=true, message=Attendance submitted successfully to PT Logistik
```

### ❌ Error Case (Invalid Branch Code):
```
D/ApiConfig: === RAW RESPONSE ===
D/ApiConfig: URL: https://stagingdakota.my.id/api/v1/attendance/submit?pt=Logistik
D/ApiConfig: Status Code: 400
D/ApiConfig: Content-Type: application/json; charset=utf-8
D/ApiConfig: Response Body: {"success":false,"error":{"message":"Invalid branch code (kodeCabang)","statusCode":400,"timestamp":"..."}}
D/ApiConfig: ====================
D/AttendanceRepository: HTTP Error 400: {"success":false,"error":{...}}
```

### 🔴 Jika Masih Error:
```
E/AttendanceRepository: Error submitting attendance: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING...
```

## 🔎 Analisis Log

### Jika Response Body adalah String Biasa:
```
D/ApiConfig: Response Body: "SUCCESS"
```
**Artinya:** Backend mengirim plain string, bukan JSON object.
**Solusi:** Backend perlu diubah untuk selalu mengirim JSON.

### Jika Response Body adalah JSON Valid:
```
D/ApiConfig: Response Body: {"success":true,...}
```
**Artinya:** Response sudah benar, error ada di parsing.
**Solusi:** Periksa model class dan Gson configuration.

### Jika Content-Type Bukan JSON:
```
D/ApiConfig: Content-Type: text/plain
```
**Artinya:** Backend mengirim response dengan Content-Type salah.
**Solusi:** Backend perlu set header `Content-Type: application/json`.

## 🚨 Troubleshooting

### Problem 1: Tidak Ada Log "RAW RESPONSE"
**Penyebab:** Interceptor tidak jalan
**Solusi:**
- Pastikan build variant adalah **debug** (bukan release)
- Rebuild project: `./gradlew clean assembleDebug`

### Problem 2: Error 400 - Invalid Branch Code
**Penyebab:** kodeCabang tidak valid atau tidak ditemukan di database
**Solusi:**
- Periksa data agent locations di cache
- Pastikan nearest agent memiliki `md5Code` yang valid
- Check log: `D/AttendanceRepository: Submitting attendance: ...kodeCabang=...`

### Problem 3: Error "Attendance submission failed"
**Penyebab:** Response `success=false` dari backend
**Solusi:**
- Lihat log raw response untuk pesan error sebenarnya
- Periksa validasi di backend (NIP, kodeCabang, latitude, longitude)

## 📝 Informasi yang Dibutuhkan

Ketika melaporkan error, sertakan:
1. ✅ **Full Logcat output** (filter: `ApiConfig` dan `AttendanceRepository`)
2. ✅ **Screenshot error dialog** di aplikasi
3. ✅ **HTTP Status Code** dari log
4. ✅ **Raw Response Body** dari log
5. ✅ **Request parameters** (pt, nip, kodeCabang, schedule)

## 🔧 Development Notes

### Files Modified:
1. `ApiResponse.kt` - Made fields nullable, added `getResponseMessage()`
2. `ApiConfig.kt` - Added response interceptor for debugging
3. `AttendanceRepository.kt` - Updated to use `getResponseMessage()`
4. `AuthRepository.kt` - Updated to use `getResponseMessage()`

### Testing Checklist:
- [ ] Build successful
- [ ] App installs without crash
- [ ] Login works
- [ ] GPS location detected
- [ ] Nearest agent found
- [ ] Submit attendance (check logs)
- [ ] Verify success/error message displayed
- [ ] Check attendance history updated

---

**Last Updated:** 2025-12-11  
**Status:** Ready for Testing ✅
