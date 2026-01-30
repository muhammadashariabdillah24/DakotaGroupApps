# 📋 Analisis Fitur Cuti - Perbandingan Sistem Lama vs Sistem Baru

## 🎯 Ringkasan

Dokumen ini menganalisis fitur Cuti dari aplikasi lama (OldSystemCuti) dan membandingkannya dengan sistem backend baru (DakotaGroupApps-Backend) untuk implementasi di aplikasi DakotaGroupStaff.

---

## 📊 Perbandingan Sistem

### 1. **Aplikasi Lama (OldSystemCuti - React Native)**

#### Struktur File
- **File:** `OldSystemCuti/index.js` (1330 baris)
- **Framework:** React Native
- **State Management:** Redux
- **Bahasa:** JavaScript

#### Fitur Utama

**A. Pengambilan Data Awal**
```javascript
// Redux Actions yang dipanggil saat mount
- GetDataDeductLeaveBalance(body)      // Saldo cuti
- GetDataDeductLeaveBalanceDetail(body) // Detail riwayat cuti
- GetDataBioKaryawan(nip)              // Data atasan
- GetDataAttendance(body)              // Data absensi hari ini
- GetDataStatusCIS()                   // Daftar status cuti
```

**B. Form Pengajuan Cuti**

1. **Status Cuti**
   - Cuti Bersama (B)
   - Cuti (C)
   - Dispensi (G)
   - Izin (I)
   - Sakit (S)
   - Datang Terlambat (DT)
   - Pulang Cepat (PC)
   - Meninggalkan Pekerjaan (MP)
   - Klaim Obat (K)

2. **Field Input**
   - Status (wajib)
   - Tanggal Mulai (wajib untuk semua kecuali DT, PC, MP)
   - Tanggal Berakhir (wajib untuk semua kecuali DT, PC, MP)
   - Keterangan (wajib)
   - Atasan 1 (wajib) - bisa dari bio atau dipilih manual
   - Atasan 2 (wajib) - bisa dari bio atau dipilih manual

3. **Validasi Tanggal**
   - **Sakit:** Maximum date = hari ini (tidak bisa masa depan)
   - **Cuti/Izin:** Minimum date = hari ini (tidak bisa masa lalu)
   - Tanggal berakhir tidak boleh sebelum tanggal mulai
   - Validasi tanggal tidak boleh duplikat dengan pengajuan sebelumnya

4. **Logika Khusus untuk DT/PC/MP**
   - Tidak perlu input tanggal manual
   - Menggunakan tanggal hari ini otomatis
   - Harus sudah absen hari ini
   - Ada logika perhitungan jam keterlambatan:
     - DT: Jika terlambat ≤ 2 jam → cg-description (tidak potong cuti)
     - DT: Jika terlambat > 2 jam dan ada saldo cuti → potong cuti
     - DT: Jika terlambat > 2 jam dan tidak ada saldo → potong gaji
     - PC/MP: Jika pulang ≥ 6 jam sebelum waktu → cg-description
     - PC/MP: Lainnya mengikuti logika saldo cuti

5. **Upload Bukti**
   - Untuk status Sakit atau Izin
   - Setelah submit berhasil, muncul tombol "Upload Bukti"
   - Membuka WebView ke URL: `/hrd/postsuratdokter.asp?nip=${nip}&id=${id}`

#### Endpoint API Lama
```javascript
// Submit Cuti
Api.BASE_URL_DLI + `/Cust-intstaffapps/saldocuti/pengajuanCuti/
  ?tgla=${dateStart}
  &tgle=${dateEnd}
  &Status=${statusValue}
  &pgaji=${isSalaryDeduction}
  &pcuti=${isLeave}
  &keterangan=${description}
  &nip=${nip}
  &atasan1=${superior1}
  &atasan2=${superior2}
  &form=${isForm}`
```

**Response:** Array dengan 1 element berisi ID (10 karakter)

---

### 2. **Backend Baru (DakotaGroupApps-Backend - Fastify)**

#### Struktur File
- **Controller:** `src/controllers/leave.controller.js`
- **Service:** `src/services/[PT]/leave.service.js` (DBS/DLB/Logistik)
- **Routes:** `src/routes/leave.routes.js`
- **Framework:** Fastify v5

#### API Endpoints

**A. GET Leave Balance**
```
POST /api/v1/leave/balance?pt=<pt>
Body: { nip, tahun }
```

Response:
```json
{
  "success": true,
  "message": "Leave balance retrieved successfully from PT [nama]",
  "data": [{
    "SALDOCUTI": "8",      // Sisa cuti
    "JUMLAHCUTI": "12",    // Total jatah cuti
    "CUTITERPAKAI": "4"    // Cuti terpakai
  }],
  "timestamp": "2025-10-28T10:30:15.123Z"
}
```

**B. GET Leave Details**
```
POST /api/v1/leave/details?pt=<pt>
Body: { nip, tahun }
```

Response:
```json
{
  "success": true,
  "data": [{
    "ID": "12345",
    "MULAI": "2025-01-15T00:00:00.000Z",
    "AKHIR": "2025-01-17T00:00:00.000Z",
    "STATUS": "Cuti",
    "KETERANGAN": "Cuti tahunan",
    "POTONGGAJI": "N",
    "POTONGCUTI": "Y",
    "DISPENSASI": "N",
    "BIAYA": "0",
    "AKTIF": "Y",
    "FORM": "Y",
    "ATASAN1 NIP": "M001",
    "ATASAN1": "Y",
    "ATASAN2 NIP": "M002",
    "ATASAN2": "Y",
    "SURAT": "N"
  }]
}
```

**C. Submit Leave Request**
```
POST /api/v1/leave/submit?pt=<pt>
Body: {
  nip, tgla, tgle, status, keterangan,
  atasan1, atasan2, form, pgaji, pcuti, pdispen, obat, surat
}
```

Response:
```json
{
  "success": true,
  "message": "Leave request submitted successfully to PT [nama]",
  "data": {
    "id": "ICS-20251101-001",
    "key": "D001125",
    "status": "C"
  }
}
```

**D. Approve Leave (Same Supervisor)**
```
POST /api/v1/leave/approve-same-supervisor?pt=<pt>
Body: { nipAtasan, leaveId, approvalValue }
```

**E. Approve Leave (Different Supervisor)**
```
POST /api/v1/leave/approve-different-supervisor?pt=<pt>
Body: { nipAtasan, leaveId, approvalValue }
```

**F. Reject Leave**
```
POST /api/v1/leave/reject?pt=<pt>
Body: { nipAtasan, leaveId, activeStatus }
```

#### Kelebihan Backend Baru
✅ Multi-PT support (DBS, DLB, Logistik)
✅ Parameterized SQL queries (keamanan)
✅ Validasi input yang ketat
✅ Response standardisasi dengan ResponseHandler
✅ Error handling yang lengkap
✅ Logging request
✅ OpenAPI/Swagger documentation

---

## 🎯 Implementasi di DakotaGroupStaff (Android - Kotlin)

### Arsitektur yang Direkomendasikan

```
┌─────────────────────────────────────────────────────┐
│                   UI Layer                          │
│   ┌──────────────────────────────────────────┐     │
│   │  LeaveActivity                            │     │
│   │  - LeaveBalanceFragment                   │     │
│   │  - LeaveSubmissionFragment                │     │
│   │  - LeaveHistoryFragment                   │     │
│   └──────────────────────────────────────────┘     │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│              ViewModel Layer                        │
│   ┌──────────────────────────────────────────┐     │
│   │  LeaveViewModel                           │     │
│   │  - StateFlow<LeaveBalance>                │     │
│   │  - StateFlow<List<LeaveDetail>>           │     │
│   │  - submitLeave()                          │     │
│   └──────────────────────────────────────────┘     │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│             Repository Layer                        │
│   ┌──────────────────────────────────────────┐     │
│   │  LeaveRepository                          │     │
│   │  - getLeaveBalance()                      │     │
│   │  - getLeaveDetails()                      │     │
│   │  - submitLeaveRequest()                   │     │
│   │  - Cache management (Room)                │     │
│   └──────────────────────────────────────────┘     │
└──────────────────┬──────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
┌──────────────┐      ┌──────────────┐
│ Remote (API) │      │ Local (Room) │
│ - ApiService │      │ - LeaveDao   │
└──────────────┘      └──────────────┘
```

### File-file yang Perlu Dibuat

#### 1. **Data Layer**

**A. Remote API**
```kotlin
// data/remote/retrofit/ApiService.kt
interface ApiService {
    @POST("api/v1/leave/balance")
    suspend fun getLeaveBalance(
        @Query("pt") pt: String,
        @Body request: LeaveBalanceRequest
    ): ApiResponse<List<LeaveBalance>>
    
    @POST("api/v1/leave/details")
    suspend fun getLeaveDetails(
        @Query("pt") pt: String,
        @Body request: LeaveDetailsRequest
    ): ApiResponse<List<LeaveDetail>>
    
    @POST("api/v1/leave/submit")
    suspend fun submitLeaveRequest(
        @Query("pt") pt: String,
        @Body request: LeaveSubmissionRequest
    ): ApiResponse<LeaveSubmissionResponse>
}
```

**B. Request Models**
```kotlin
// data/remote/request/LeaveRequest.kt
data class LeaveBalanceRequest(
    val nip: String,
    val tahun: String
)

data class LeaveDetailsRequest(
    val nip: String,
    val tahun: String
)

data class LeaveSubmissionRequest(
    val nip: String,
    val tgla: String,        // Format: M/D/YYYY
    val tgle: String,        // Format: M/D/YYYY
    val status: String,      // C, I, S, etc.
    val keterangan: String,
    val atasan1: String,
    val atasan2: String,
    val form: String = "Y",
    val pgaji: String = "N",
    val pcuti: String = "N",
    val pdispen: String = "N",
    val obat: String = "0",
    val surat: String = "N"
)
```

**C. Response Models**
```kotlin
// data/remote/response/LeaveResponse.kt
data class LeaveBalance(
    @SerializedName("SALDOCUTI") val saldoCuti: String,
    @SerializedName("JUMLAHCUTI") val jumlahCuti: String,
    @SerializedName("CUTITERPAKAI") val cutiTerpakai: String
)

data class LeaveDetail(
    @SerializedName("ID") val id: String,
    @SerializedName("MULAI") val mulai: String,
    @SerializedName("AKHIR") val akhir: String,
    @SerializedName("STATUS") val status: String,
    @SerializedName("KETERANGAN") val keterangan: String,
    @SerializedName("POTONGGAJI") val potongGaji: String,
    @SerializedName("POTONGCUTI") val potongCuti: String,
    @SerializedName("DISPENSASI") val dispensasi: String,
    @SerializedName("BIAYA") val biaya: String,
    @SerializedName("AKTIF") val aktif: String,
    @SerializedName("FORM") val form: String,
    @SerializedName("ATASAN1 NIP") val atasan1Nip: String,
    @SerializedName("ATASAN1") val atasan1Approval: String,
    @SerializedName("ATASAN2 NIP") val atasan2Nip: String,
    @SerializedName("ATASAN2") val atasan2Approval: String,
    @SerializedName("SURAT") val surat: String
)

data class LeaveSubmissionResponse(
    val id: String,
    val key: String,
    val status: String
)
```

**D. Local Database (Room)**
```kotlin
// data/local/entity/LeaveBalanceEntity.kt
@Entity(tableName = "leave_balance_cache")
data class LeaveBalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nip") val nip: String,
    @ColumnInfo(name = "tahun") val tahun: String,
    @ColumnInfo(name = "saldo_cuti") val saldoCuti: String,
    @ColumnInfo(name = "jumlah_cuti") val jumlahCuti: String,
    @ColumnInfo(name = "cuti_terpakai") val cutiTerpakai: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - cachedAt) < oneDayInMillis
    }
}

// data/local/entity/LeaveDetailsEntity.kt
@Entity(tableName = "leave_details_cache")
data class LeaveDetailsEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nip") val nip: String,
    @ColumnInfo(name = "tahun") val tahun: String,
    @ColumnInfo(name = "mulai") val mulai: String,
    @ColumnInfo(name = "akhir") val akhir: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "keterangan") val keterangan: String,
    @ColumnInfo(name = "potong_gaji") val potongGaji: String,
    @ColumnInfo(name = "potong_cuti") val potongCuti: String,
    @ColumnInfo(name = "dispensasi") val dispensasi: String,
    @ColumnInfo(name = "biaya") val biaya: String,
    @ColumnInfo(name = "aktif") val aktif: String,
    @ColumnInfo(name = "form") val form: String,
    @ColumnInfo(name = "atasan1_nip") val atasan1Nip: String,
    @ColumnInfo(name = "atasan1_approval") val atasan1Approval: String,
    @ColumnInfo(name = "atasan2_nip") val atasan2Nip: String,
    @ColumnInfo(name = "atasan2_approval") val atasan2Approval: String,
    @ColumnInfo(name = "surat") val surat: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
)
```

**E. DAO**
```kotlin
// data/local/dao/LeaveBalanceDao.kt
@Dao
interface LeaveBalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(leaveBalance: LeaveBalanceEntity)
    
    @Query("SELECT * FROM leave_balance_cache WHERE nip = :nip AND tahun = :tahun LIMIT 1")
    suspend fun getByNipAndYear(nip: String, tahun: String): LeaveBalanceEntity?
    
    @Query("DELETE FROM leave_balance_cache WHERE nip = :nip AND tahun = :tahun")
    suspend fun delete(nip: String, tahun: String)
    
    @Query("DELETE FROM leave_balance_cache")
    suspend fun clearAll()
}

// data/local/dao/LeaveDetailsDao.kt
@Dao
interface LeaveDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<LeaveDetailsEntity>)
    
    @Query("SELECT * FROM leave_details_cache WHERE nip = :nip AND tahun = :tahun ORDER BY mulai DESC")
    suspend fun getByNipAndYear(nip: String, tahun: String): List<LeaveDetailsEntity>
    
    @Query("DELETE FROM leave_details_cache WHERE nip = :nip AND tahun = :tahun")
    suspend fun delete(nip: String, tahun: String)
    
    @Query("DELETE FROM leave_details_cache")
    suspend fun clearAll()
}
```

#### 2. **Repository Layer**

```kotlin
// data/repository/LeaveRepository.kt
class LeaveRepository @Inject constructor(
    private val apiService: ApiService,
    private val leaveBalanceDao: LeaveBalanceDao,
    private val leaveDetailsDao: LeaveDetailsDao,
    private val sharedPreferences: SharedPreferences
) {
    private val pt: String
        get() = sharedPreferences.getString("pt", "C") ?: "C"
    
    /**
     * Get leave balance with cache strategy
     * Cache valid for 1 day
     */
    suspend fun getLeaveBalance(nip: String, tahun: String): Result<LeaveBalance> {
        return try {
            // Try cache first
            val cached = leaveBalanceDao.getByNipAndYear(nip, tahun)
            if (cached != null && cached.isValid()) {
                return Result.Success(cached.toLeaveBalance())
            }
            
            // Fetch from API
            val response = apiService.getLeaveBalance(
                pt = pt,
                request = LeaveBalanceRequest(nip, tahun)
            )
            
            if (response.success && response.data.isNotEmpty()) {
                val balance = response.data[0]
                
                // Save to cache
                leaveBalanceDao.insert(balance.toEntity(nip, tahun))
                
                Result.Success(balance)
            } else {
                Result.Error(response.error?.message ?: "Failed to get leave balance")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Get leave details/history
     */
    suspend fun getLeaveDetails(nip: String, tahun: String): Result<List<LeaveDetail>> {
        return try {
            val response = apiService.getLeaveDetails(
                pt = pt,
                request = LeaveDetailsRequest(nip, tahun)
            )
            
            if (response.success) {
                // Cache the details
                leaveDetailsDao.delete(nip, tahun)
                leaveDetailsDao.insertAll(response.data.map { it.toEntity(nip, tahun) })
                
                Result.Success(response.data)
            } else {
                Result.Error(response.error?.message ?: "Failed to get leave details")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Submit leave request
     */
    suspend fun submitLeaveRequest(request: LeaveSubmissionRequest): Result<LeaveSubmissionResponse> {
        return try {
            val response = apiService.submitLeaveRequest(pt = pt, request = request)
            
            if (response.success) {
                // Invalidate cache after submission
                leaveBalanceDao.delete(request.nip, request.tahun)
                leaveDetailsDao.delete(request.nip, request.tahun)
                
                Result.Success(response.data)
            } else {
                Result.Error(response.error?.message ?: "Failed to submit leave request")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
```

#### 3. **ViewModel Layer**

```kotlin
// ui/leave/LeaveViewModel.kt
@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository,
    private val employeeRepository: EmployeeBioRepository
) : ViewModel() {
    
    private val _leaveBalance = MutableStateFlow<UiState<LeaveBalance>>(UiState.Idle)
    val leaveBalance: StateFlow<UiState<LeaveBalance>> = _leaveBalance.asStateFlow()
    
    private val _leaveDetails = MutableStateFlow<UiState<List<LeaveDetail>>>(UiState.Idle)
    val leaveDetails: StateFlow<UiState<List<LeaveDetail>>> = _leaveDetails.asStateFlow()
    
    private val _submitResult = MutableStateFlow<UiState<LeaveSubmissionResponse>>(UiState.Idle)
    val submitResult: StateFlow<UiState<LeaveSubmissionResponse>> = _submitResult.asStateFlow()
    
    private val _employeeBio = MutableStateFlow<UiState<EmployeeBio>>(UiState.Idle)
    val employeeBio: StateFlow<UiState<EmployeeBio>> = _employeeBio.asStateFlow()
    
    fun loadLeaveBalance(nip: String, tahun: String) {
        viewModelScope.launch {
            _leaveBalance.value = UiState.Loading
            when (val result = leaveRepository.getLeaveBalance(nip, tahun)) {
                is Result.Success -> _leaveBalance.value = UiState.Success(result.data)
                is Result.Error -> _leaveBalance.value = UiState.Error(result.message)
            }
        }
    }
    
    fun loadLeaveDetails(nip: String, tahun: String) {
        viewModelScope.launch {
            _leaveDetails.value = UiState.Loading
            when (val result = leaveRepository.getLeaveDetails(nip, tahun)) {
                is Result.Success -> _leaveDetails.value = UiState.Success(result.data)
                is Result.Error -> _leaveDetails.value = UiState.Error(result.message)
            }
        }
    }
    
    fun loadEmployeeBio(nip: String) {
        viewModelScope.launch {
            _employeeBio.value = UiState.Loading
            when (val result = employeeRepository.getEmployeeBio(nip)) {
                is Result.Success -> _employeeBio.value = UiState.Success(result.data)
                is Result.Error -> _employeeBio.value = UiState.Error(result.message)
            }
        }
    }
    
    fun submitLeaveRequest(request: LeaveSubmissionRequest) {
        viewModelScope.launch {
            _submitResult.value = UiState.Loading
            when (val result = leaveRepository.submitLeaveRequest(request)) {
                is Result.Success -> _submitResult.value = UiState.Success(result.data)
                is Result.Error -> _submitResult.value = UiState.Error(result.message)
            }
        }
    }
    
    fun resetSubmitResult() {
        _submitResult.value = UiState.Idle
    }
}
```

#### 4. **UI Layer**

**A. Leave Activity**
```kotlin
// ui/leave/LeaveActivity.kt
class LeaveActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLeaveBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewPager()
    }
    
    private fun setupViewPager() {
        val fragments = listOf(
            LeaveBalanceFragment(),
            LeaveSubmissionFragment(),
            LeaveHistoryFragment()
        )
        
        val adapter = LeaveViewPagerAdapter(this, fragments)
        binding.viewPager.adapter = adapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Saldo Cuti"
                1 -> "Pengajuan"
                2 -> "Riwayat"
                else -> ""
            }
        }.attach()
    }
}
```

**B. Leave Balance Fragment**
```kotlin
// ui/leave/LeaveBalanceFragment.kt
@AndroidEntryPoint
class LeaveBalanceFragment : Fragment() {
    private val viewModel: LeaveViewModel by viewModels()
    private var _binding: FragmentLeaveBalanceBinding? = null
    private val binding get() = _binding!!
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        loadData()
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.leaveBalance.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> showBalance(state.data)
                    is UiState.Error -> showError(state.message)
                    is UiState.Idle -> {}
                }
            }
        }
    }
    
    private fun showBalance(balance: LeaveBalance) {
        binding.apply {
            tvSaldoCuti.text = balance.saldoCuti
            tvJumlahCuti.text = balance.jumlahCuti
            tvCutiTerpakai.text = balance.cutiTerpakai
            
            // Update progress
            val percentage = if (balance.jumlahCuti.toIntOrNull() != null) {
                (balance.cutiTerpakai.toFloatOrNull() ?: 0f) / 
                (balance.jumlahCuti.toFloatOrNull() ?: 1f) * 100
            } else 0f
            progressBar.progress = percentage.toInt()
        }
    }
}
```

**C. Leave Submission Fragment**
```kotlin
// ui/leave/LeaveSubmissionFragment.kt
@AndroidEntryPoint
class LeaveSubmissionFragment : Fragment() {
    private val viewModel: LeaveViewModel by viewModels()
    private var _binding: FragmentLeaveSubmissionBinding? = null
    private val binding get() = _binding!!
    
    // Form state
    private var selectedStatus: LeaveStatus? = null
    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null
    private var atasan1Nip: String? = null
    private var atasan2Nip: String? = null
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        setupObservers()
        loadEmployeeBio()
    }
    
    private fun setupListeners() {
        binding.apply {
            // Status selection
            btnSelectStatus.setOnClickListener {
                showStatusDialog()
            }
            
            // Date pickers
            btnStartDate.setOnClickListener {
                if (selectedStatus == null) {
                    Toast.makeText(context, "Pilih status terlebih dahulu", Toast.LENGTH_SHORT).show()
                } else {
                    showStartDatePicker()
                }
            }
            
            btnEndDate.setOnClickListener {
                if (selectedStatus == null) {
                    Toast.makeText(context, "Pilih status terlebih dahulu", Toast.LENGTH_SHORT).show()
                } else if (selectedStartDate == null) {
                    Toast.makeText(context, "Pilih tanggal mulai terlebih dahulu", Toast.LENGTH_SHORT).show()
                } else {
                    showEndDatePicker()
                }
            }
            
            // Atasan selection
            btnSelectAtasan1.setOnClickListener { showAtasanDialog(1) }
            btnSelectAtasan2.setOnClickListener { showAtasanDialog(2) }
            
            // Submit
            btnSubmit.setOnClickListener { submitLeaveRequest() }
        }
    }
    
    private fun showStatusDialog() {
        val statuses = LeaveStatus.values()
        val items = statuses.map { it.indonesianName }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pilih Status")
            .setItems(items) { _, which ->
                selectedStatus = statuses[which]
                binding.tvSelectedStatus.text = selectedStatus?.indonesianName
                
                // Update date constraints based on status
                updateDateConstraints()
            }
            .show()
    }
    
    private fun updateDateConstraints() {
        when (selectedStatus) {
            LeaveStatus.SAKIT -> {
                // Maximum today for sick leave
                maxDate = Date()
            }
            LeaveStatus.CUTI, LeaveStatus.IZIN -> {
                // Minimum today for regular leave
                minDate = Date()
            }
            else -> {
                // Default constraints
            }
        }
    }
    
    private fun showStartDatePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
        
        when (selectedStatus) {
            LeaveStatus.SAKIT -> {
                constraintsBuilder.setEnd(Date().time)
            }
            LeaveStatus.CUTI, LeaveStatus.IZIN -> {
                constraintsBuilder.setStart(Date().time)
            }
            else -> {}
        }
        
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Pilih Tanggal Mulai")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()
        
        picker.addOnPositiveButtonClickListener { selection ->
            selectedStartDate = Date(selection)
            binding.tvStartDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(selectedStartDate)
        }
        
        picker.show(childFragmentManager, "START_DATE_PICKER")
    }
    
    private fun submitLeaveRequest() {
        // Validation
        if (!validateForm()) return
        
        val nip = getUserNip()
        val tahun = getCurrentYear()
        
        val request = LeaveSubmissionRequest(
            nip = nip,
            tgla = formatDate(selectedStartDate!!),
            tgle = formatDate(selectedEndDate!!),
            status = selectedStatus!!.code,
            keterangan = binding.etKeterangan.text.toString(),
            atasan1 = atasan1Nip!!,
            atasan2 = atasan2Nip!!,
            form = "Y",
            pgaji = if (shouldDeductSalary()) "Y" else "N",
            pcuti = if (shouldDeductLeave()) "Y" else "N"
        )
        
        viewModel.submitLeaveRequest(request)
    }
    
    private fun formatDate(date: Date): String {
        // Format: M/D/YYYY
        val calendar = Calendar.getInstance()
        calendar.time = date
        return "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.YEAR)}"
    }
}

enum class LeaveStatus(val code: String, val indonesianName: String) {
    CUTI("C", "Cuti"),
    IZIN("I", "Izin"),
    SAKIT("S", "Sakit"),
    CUTI_BERSAMA("B", "Cuti Bersama"),
    DISPENSASI("G", "Dispensasi"),
    DATANG_TERLAMBAT("D", "Datang Terlambat"),
    PULANG_CEPAT("P", "Pulang Cepat"),
    MENINGGALKAN_PEKERJAAN("M", "Meninggalkan Pekerjaan"),
    KLAIM_OBAT("K", "Klaim Obat")
}
```

**D. Leave History Fragment**
```kotlin
// ui/leave/LeaveHistoryFragment.kt
@AndroidEntryPoint
class LeaveHistoryFragment : Fragment() {
    private val viewModel: LeaveViewModel by viewModels()
    private var _binding: FragmentLeaveHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: LeaveHistoryAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        loadData()
    }
    
    private fun setupRecyclerView() {
        adapter = LeaveHistoryAdapter()
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@LeaveHistoryFragment.adapter
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.leaveDetails.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> showHistory(state.data)
                    is UiState.Error -> showError(state.message)
                    is UiState.Idle -> {}
                }
            }
        }
    }
}
```

---

## 📋 Checklist Implementasi

### Phase 1: Setup Data Layer ✅
- [ ] Buat model Request (LeaveBalanceRequest, LeaveDetailsRequest, LeaveSubmissionRequest)
- [ ] Buat model Response (LeaveBalance, LeaveDetail, LeaveSubmissionResponse)
- [ ] Tambahkan endpoint ke ApiService
- [ ] Buat Entity untuk Room (LeaveBalanceEntity, LeaveDetailsEntity)
- [ ] Buat DAO (LeaveBalanceDao, LeaveDetailsDao)
- [ ] Update AppDatabase untuk include DAO baru

### Phase 2: Repository Layer ✅
- [ ] Buat LeaveRepository
- [ ] Implement getLeaveBalance() dengan caching strategy
- [ ] Implement getLeaveDetails()
- [ ] Implement submitLeaveRequest()
- [ ] Implement mapper functions (Entity ↔ Model)

### Phase 3: ViewModel Layer ✅
- [ ] Buat LeaveViewModel
- [ ] Setup StateFlow untuk balance, details, submission
- [ ] Implement load functions
- [ ] Implement submit function
- [ ] Handle error states

### Phase 4: UI Layer ✅
- [ ] Buat LeaveActivity dengan ViewPager
- [ ] Buat LeaveBalanceFragment (tampilan saldo cuti)
- [ ] Buat LeaveSubmissionFragment (form pengajuan)
- [ ] Buat LeaveHistoryFragment (riwayat cuti)
- [ ] Buat LeaveHistoryAdapter
- [ ] Buat layout files

### Phase 5: Features ✅
- [ ] Status selection dialog
- [ ] Date picker dengan constraints (Sakit, Cuti, Izin)
- [ ] Atasan selection dari bio atau manual
- [ ] Validation logic
- [ ] Calculate logic untuk potong gaji/cuti
- [ ] Upload bukti untuk Sakit/Izin (WebView)

### Phase 6: Testing & Polish ✅
- [ ] Test all API calls
- [ ] Test cache mechanism
- [ ] Test form validation
- [ ] Test date constraints
- [ ] Handle offline mode
- [ ] Error handling & user feedback
- [ ] Loading states

---

## 🎯 Perbedaan Utama Sistem Lama vs Baru

| Aspek | Sistem Lama (React Native) | Sistem Baru (Kotlin Android) |
|-------|---------------------------|------------------------------|
| **Platform** | React Native | Native Android (Kotlin) |
| **State Management** | Redux | StateFlow + ViewModel |
| **API Call** | Fetch/Axios | Retrofit |
| **Caching** | Redux Store (in-memory) | Room Database (persistent) |
| **Date Picker** | RNDateTimePicker | MaterialDatePicker |
| **Backend** | ASP Classic | Fastify (Modern REST API) |
| **Multi-PT** | Tidak (hardcoded DLI) | Ya (DBS, DLB, Logistik) |
| **Validation** | Client-side only | Client + Server side |
| **Error Handling** | Toast messages | Standardized error response |

---

## 💡 Rekomendasi Implementasi

### 1. **Prioritas Fitur**
✅ **Phase 1 (MVP):**
- Leave balance display
- Leave history display
- Simple leave submission (Cuti, Izin, Sakit)

✅ **Phase 2:**
- Advanced date validation
- Atasan selection
- Duplicate date checking

✅ **Phase 3:**
- Upload bukti (WebView)
- Special logic untuk DT/PC/MP
- Approval flow (jika diperlukan)

### 2. **Caching Strategy**
- Leave Balance: Cache 1 hari
- Leave Details: Refresh setiap kali masuk halaman
- Invalidate cache setelah submit berhasil

### 3. **Offline Support**
- Tampilkan cached data jika offline
- Queue submission saat offline (optional)
- Sync saat kembali online

### 4. **User Experience**
- Loading states yang jelas
- Error messages yang informatif
- Confirmation dialog sebelum submit
- Success feedback setelah submit

---

## 📚 Referensi

1. **Backend API Documentation:**
   - [LEAVE_API_MIGRATION.md](file://d:/Documents_Backup/ProjectsAndroid/Dakota/DakotaGroupApps-Backend/LEAVE_API_MIGRATION.md)
   - [LEAVE_SUBMISSION_API_MIGRATION.md](file://d:/Documents_Backup/ProjectsAndroid/Dakota/DakotaGroupApps-Backend/LEAVE_SUBMISSION_API_MIGRATION.md)

2. **Old System Reference:**
   - [OldSystemCuti/index.js](file://d:/Documents_Backup/ProjectsAndroid/Dakota/OldSystemCuti/index.js)

3. **Existing Implementation:**
   - [AttendanceRepository.kt](file://d:/Documents_Backup/ProjectsAndroid/Dakota/DakotaGroupStaff/app/src/main/java/com/dakotagroupstaff/data/repository/AttendanceRepository.kt)
   - [SalaryRepository.kt](file://d:/Documents_Backup/ProjectsAndroid/Dakota/DakotaGroupStaff/app/src/main/java/com/dakotagroupstaff/data/repository/SalaryRepository.kt)

---

**Dibuat:** 18 Desember 2025  
**Status:** 📋 Planning Document  
**Next Step:** Mulai implementasi Phase 1 (Data Layer)
