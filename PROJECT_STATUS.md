# Dakota Group Staff - Android Application

Aplikasi Android modern untuk manajemen staff Dakota Group dengan fitur attendance, leave management, salary, dan employee management.

## 🏗️ Arsitektur

Aplikasi ini menggunakan **Clean Architecture** dengan **MVVM Pattern**, mengikuti best practices dari Android Expert level:

```
app/
├── data/
│   ├── local/
│   │   ├── model/          # Data models (UserSession)
│   │   └── preferences/    # DataStore preferences
│   ├── remote/
│   │   ├── response/       # API response models
│   │   └── retrofit/       # Retrofit service & config
│   └── repository/         # Repository pattern
├── di/                     # Dependency Injection (Koin)
├── ui/
│   ├── login/             # Login feature
│   └── ...                # Other features (TODO)
└── DakotaGroupStaffApp    # Application class
```

## 🛠️ Tech Stack

### Core
- **Kotlin** - 100% Kotlin
- **MVVM** - Architecture pattern
- **Koin** - Dependency Injection
- **Coroutines & Flow** - Asynchronous operations

### Networking
- **Retrofit** - REST API client
- **OkHttp** - HTTP client & logging
- **Gson** - JSON serialization

### Local Storage
- **DataStore** - Preferences storage (replacing SharedPreferences)
- **Room** - Local database (for future features)

### UI
- **ViewBinding** - Type-safe view access
- **Material Design 3** - Modern UI components
- **Navigation Component** - Fragment navigation (TODO)

### Other
- **Glide** - Image loading
- **LeakCanary** - Memory leak detection (debug)
- **Google Play Services Location** - GPS tracking

## 📱 Fitur yang Sudah Diimplementasikan

### ✅ **Authentication (Login)**
- Multi-company support (PT DBS, PT DLB, PT Logistik)
- Device validation (IMEI & SIM ID detection)
- Email validation
- Session management dengan DataStore
- Auto-redirect based on login status

### ✅ **Attendance (Absensi)** - COMPLETED
- **Location-based Check In/Out**
  - GPS location detection dengan FusedLocationProviderClient
  - Nearest agent finder menggunakan Haversine formula
  - Distance calculation & range validation
  - Real-time location status indicator
  
- **Agent Location Caching**
  - 30-day cache strategy dengan Room Database
  - Offline-first approach untuk GPS validation
  - Auto-sync dari API GET /agent/locations
  
- **Check In/Out Submission**
  - Validasi jarak (dalam radius cabang/agen)
  - Submit ke API POST /attendance/submit
  - Device binding (IMEI + SIM ID)
  - Konfirmasi dialog sebelum submit
  
- **Attendance History**
  - Calendar view dengan color-coded status:
    - 🟢 **Hijau**: Absen masuk (M) & pulang (K) lengkap pada tanggal yang sama
    - 🟡 **Kuning**: Hanya absen masuk (M) saja
    - 🔴 **Merah**: Tidak ada absensi
    - ⚪ **Abu-abu**: Tanggal yang akan datang
  - Menggunakan field KETERANGAN dari API (M=Masuk, K=Keluar)
  - Monthly navigation (previous/next month)
  - Data dari API POST /attendance dengan cache strategy
  - Last 3 months history

## 🔌 API Integration

Base URL: `https://stagingdakota.my.id/api/v1/`

### API Response Format

Backend menggunakan standardized response format dengan 2 struktur berbeda:

**Success Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {...},
  "timestamp": "2025-12-11T..."
}
```

**Error Response:**
```json
{
  "success": false,
  "error": {
    "message": "Error description",
    "statusCode": 400,
    "details": "...",
    "timestamp": "2025-12-11T..."
  }
}
```

> **Note:** App menggunakan `ApiResponse<T>` wrapper class dengan helper method `getResponseMessage()` untuk handle kedua format ini.

### Implemented Endpoints:
- ✅ `POST /auth/login?pt=<pt>` - Login dengan device validation
- ✅ `GET /agent/locations?pt=<pt>` - Get active agents with GPS coordinates
- ✅ `POST /attendance?pt=<pt>` - Get attendance history (last 3 months)
- ✅ `POST /attendance/submit?pt=<pt>` - Submit attendance (check in/out)

### TODO Endpoints:
- ✅ ~~`POST /attendance?pt=<pt>` - Get attendance records~~ (DONE)
- ✅ ~~`POST /attendance/submit?pt=<pt>` - Submit attendance~~ (DONE)
- ⏳ `POST /employee/bio?pt=<pt>` - Get employee bio data
- ⏳ `POST /salary/slips?pt=<pt>` - Get salary slips
- ⏳ `POST /leave/balance?pt=<pt>` - Get leave balance
- ⏳ `POST /leave/submit?pt=<pt>` - Submit leave request

## 🚀 Setup & Build

1. Clone repository
2. Buka di Android Studio
3. Sync Gradle
4. Update `local.properties` dengan SDK path dan BASE_URL
5. Build & Run

### Requirements
- Android Studio Hedgehog | 2023.1.1+
- JDK 17
- Min SDK: 24 (Android 7.0)
- Target SDK: 36 (Android 15)

## 📝 Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## 🎯 Next Steps

### High Priority:
1. ✅ ~~Implement Splash Screen & Session Check~~ (DONE)
2. ✅ ~~Build Main Dashboard~~ (DONE)
3. ✅ ~~Implement Attendance Feature (GPS + Submit)~~ (DONE)
4. ⏳ Implement Leave Management

### Medium Priority:
5. ⏳ Employee Bio Screen
6. ⏳ Salary Slip Viewer
7. ⏳ Dark Mode Support
8. ⏳ Offline-first with Room Database

### Low Priority:
9. ⏳ Notification System
10. ⏳ Settings Screen
11. ⏳ Profile Management

## 📚 Code Style & Conventions

- Kotlin official code style
- MVVM architecture
- Repository pattern
- Sealed classes for state management
- LiveData for UI observation
- Coroutines for async operations

## 🔒 Security

- HTTPS only
- Certificate pinning (TODO)
- Encrypted SharedPreferences (TODO)
- Device binding (IMEI + SIM ID)

---

**Built with ❤️ following Android Best Practices**
