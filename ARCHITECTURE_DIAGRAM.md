# Leave Feature - Architecture Diagram

## System Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                           USER INTERFACE                             │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌────────────────────────┐      ┌─────────────────────────┐        │
│  │ LeaveHistoryActivity   │      │ LeaveSubmissionActivity │        │
│  ├────────────────────────┤      ├─────────────────────────┤        │
│  │ - Leave Balance Card   │      │ - Leave Type Selector   │        │
│  │ - Year Filter          │◄─────┤ - Date Pickers          │        │
│  │ - History RecyclerView │      │ - Description Field     │        │
│  │ - Submit Button        │      │ - Atasan Info           │        │
│  │ - Pull-to-Refresh      │      │ - Submit Button         │        │
│  └────────────┬───────────┘      └──────────┬──────────────┘        │
│               │                             │                        │
└───────────────┼─────────────────────────────┼────────────────────────┘
                │                             │
                └──────────────┬──────────────┘
                               │
┌──────────────────────────────▼────────────────────────────────────────┐
│                         VIEWMODEL LAYER                               │
├───────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    LeaveViewModel                             │   │
│  ├──────────────────────────────────────────────────────────────┤   │
│  │  LiveData:                                                    │   │
│  │  • leaveBalance: LiveData<Result<LeaveBalanceEntity>>       │   │
│  │  • leaveDetails: LiveData<Result<List<LeaveDetailsEntity>>> │   │
│  │  • submitResult: LiveData<Result<LeaveSubmissionData>>      │   │
│  │  • selectedLeaveType: LiveData<LeaveType?>                  │   │
│  │  • startDate: LiveData<Date?>                                │   │
│  │  • endDate: LiveData<Date?>                                  │   │
│  │                                                               │   │
│  │  Methods:                                                     │   │
│  │  • getLeaveBalance()                                          │   │
│  │  • getLeaveDetails()                                          │   │
│  │  • submitLeaveRequest()                                       │   │
│  │  • selectLeaveType()                                          │   │
│  │  • setStartDate() / setEndDate()                             │   │
│  │  • validateForm()                                             │   │
│  └────────────────────────┬─────────────────────────────────────┘   │
│                           │                                           │
└───────────────────────────┼───────────────────────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────────────────┐
│                      REPOSITORY LAYER                                 │
├───────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                   LeaveRepository                             │   │
│  ├──────────────────────────────────────────────────────────────┤   │
│  │  Strategy: Offline-First                                      │   │
│  │                                                               │   │
│  │  getLeaveBalance(pt, nip, tahun, forceRefresh)              │   │
│  │  ┌──────────────────────────────────────────────────┐       │   │
│  │  │ 1. Check local cache (Room)                      │       │   │
│  │  │ 2. If exists → Emit cached data                  │       │   │
│  │  │ 3. Fetch from API in background                  │       │   │
│  │  │ 4. Update cache                                  │       │   │
│  │  │ 5. Emit fresh data                               │       │   │
│  │  └──────────────────────────────────────────────────┘       │   │
│  │                                                               │   │
│  │  getLeaveDetails(pt, nip, tahun, forceRefresh)              │   │
│  │  ┌──────────────────────────────────────────────────┐       │   │
│  │  │ 1. Check local cache (Room)                      │       │   │
│  │  │ 2. If exists → Emit cached data                  │       │   │
│  │  │ 3. Fetch from API in background                  │       │   │
│  │  │ 4. Update cache                                  │       │   │
│  │  │ 5. Emit fresh data                               │       │   │
│  │  └──────────────────────────────────────────────────┘       │   │
│  │                                                               │   │
│  │  submitLeaveRequest(pt, request)                             │   │
│  │  ┌──────────────────────────────────────────────────┐       │   │
│  │  │ 1. Validate request                              │       │   │
│  │  │ 2. Call API                                      │       │   │
│  │  │ 3. If success → Refresh cache                    │       │   │
│  │  │ 4. Return result                                 │       │   │
│  │  └──────────────────────────────────────────────────┘       │   │
│  └──────────┬────────────────────────────────┬──────────────────┘   │
│             │                                │                       │
└─────────────┼────────────────────────────────┼───────────────────────┘
              │                                │
              ▼                                ▼
┌──────────────────────────┐    ┌──────────────────────────────────┐
│    LOCAL DATA SOURCE     │    │     REMOTE DATA SOURCE           │
├──────────────────────────┤    ├──────────────────────────────────┤
│                          │    │                                  │
│  ┌────────────────────┐ │    │  ┌────────────────────────────┐ │
│  │ Room Database      │ │    │  │ Retrofit ApiService        │ │
│  ├────────────────────┤ │    │  ├────────────────────────────┤ │
│  │                    │ │    │  │ Base URL:                  │ │
│  │ Tables:            │ │    │  │ http://localhost:3000/api  │ │
│  │                    │ │    │  │                            │ │
│  │ • leave_balance    │ │    │  │ Endpoints:                 │ │
│  │   - nip (PK)       │ │    │  │                            │ │
│  │   - tahun          │ │    │  │ POST /leave/balance        │ │
│  │   - saldo_cuti     │ │    │  │ ├─ Request:                │ │
│  │   - jumlah_cuti    │ │    │  │ │  { nip, tahun }          │ │
│  │   - cuti_terpakai  │ │    │  │ └─ Response:               │ │
│  │   - last_updated   │ │    │  │    { SALDOCUTI, ... }      │ │
│  │                    │ │    │  │                            │ │
│  │ • leave_details    │ │    │  │ POST /leave/details        │ │
│  │   - id (PK)        │ │    │  │ ├─ Request:                │ │
│  │   - nip            │ │    │  │ │  { nip, tahun }          │ │
│  │   - tahun          │ │    │  │ └─ Response:               │ │
│  │   - mulai          │ │    │  │    [{ ID, MULAI, ... }]    │ │
│  │   - akhir          │ │    │  │                            │ │
│  │   - status         │ │    │  │ POST /leave/submit         │ │
│  │   - keterangan     │ │    │  │ ├─ Request:                │ │
│  │   - potong_gaji    │ │    │  │ │  { nip, tgla, tgle,      │ │
│  │   - potong_cuti    │ │    │  │ │    status, keterangan,   │ │
│  │   - ...            │ │    │  │ │    atasan1, atasan2 }    │ │
│  │   - last_updated   │ │    │  │ └─ Response:               │ │
│  │                    │ │    │  │    { id, key, status }     │ │
│  └────────────────────┘ │    │  └────────────────────────────┘ │
│                          │    │                                  │
│  DAOs:                   │    │  All endpoints use:              │
│  • LeaveBalanceDao       │    │  Query param: ?pt=<pt>           │
│  • LeaveDetailsDao       │    │  (A=DBS, B=DLB, C=Logistik)      │
│                          │    │                                  │
└──────────────────────────┘    └──────────────────────────────────┘
```

## Data Flow Diagrams

### 1. Get Leave Balance Flow

```
User Action                ViewModel               Repository           Local DB        Remote API
    │                          │                       │                   │                │
    │ Open Leave Screen        │                       │                   │                │
    ├─────────────────────────►│                       │                   │                │
    │                          │ getLeaveBalance()     │                   │                │
    │                          ├──────────────────────►│                   │                │
    │                          │                       │ Query cache       │                │
    │                          │                       ├──────────────────►│                │
    │                          │                       │ Return cached     │                │
    │                          │ Emit Loading          │◄──────────────────┤                │
    │◄─────────────────────────┤                       │                   │                │
    │ Show Progress            │                       │                   │                │
    │                          │ Emit Success(cache)   │                   │                │
    │◄─────────────────────────┤                       │                   │                │
    │ Display Balance          │                       │                   │                │
    │                          │                       │ Fetch from API    │                │
    │                          │                       ├───────────────────┼───────────────►│
    │                          │                       │                   │ GET /balance   │
    │                          │                       │ Response          │                │
    │                          │                       │◄──────────────────┼────────────────┤
    │                          │                       │ Update cache      │                │
    │                          │                       ├──────────────────►│                │
    │                          │ Emit Success(fresh)   │                   │                │
    │◄─────────────────────────┤                       │                   │                │
    │ Update Display           │                       │                   │                │
    └──────────────────────────┴───────────────────────┴───────────────────┴────────────────┘
```

### 2. Submit Leave Request Flow

```
User Action                ViewModel               Repository           Remote API        Local DB
    │                          │                       │                   │                │
    │ Fill Form                │                       │                   │                │
    ├─────────────────────────►│                       │                   │                │
    │ Select Leave Type        │ selectLeaveType()     │                   │                │
    ├─────────────────────────►│                       │                   │                │
    │ Select Dates             │ setStartDate()        │                   │                │
    ├─────────────────────────►│ setEndDate()          │                   │                │
    │ Enter Description        │ setDescription()      │                   │                │
    ├─────────────────────────►│                       │                   │                │
    │ Tap Submit               │                       │                   │                │
    ├─────────────────────────►│ submitLeaveRequest()  │                   │                │
    │                          ├──────────────────────►│                   │                │
    │                          │                       │ Validate          │                │
    │                          │                       │ Build Request     │                │
    │                          │                       │ Call API          │                │
    │                          │                       ├──────────────────►│                │
    │                          │                       │                   │ POST /submit   │
    │                          │ Emit Loading          │                   │                │
    │◄─────────────────────────┤                       │                   │                │
    │ Show Progress            │                       │ Response          │                │
    │ Disable Button           │                       │◄──────────────────┤                │
    │                          │                       │ Refresh cache     │                │
    │                          │                       ├───────────────────┼───────────────►│
    │                          │ Emit Success          │                   │                │
    │◄─────────────────────────┤                       │                   │                │
    │ Show Success Toast       │                       │                   │                │
    │ Close Activity           │                       │                   │                │
    │ Return to History        │                       │                   │                │
    └──────────────────────────┴───────────────────────┴───────────────────┴────────────────┘
```

### 3. Offline Mode Flow

```
User Action                ViewModel               Repository           Local DB        Remote API
    │                          │                       │                   │                │
    │ [No Internet]            │                       │                   │                │
    │ Open Leave Screen        │                       │                   │                │
    ├─────────────────────────►│ getLeaveBalance()     │                   │                │
    │                          ├──────────────────────►│                   │                │
    │                          │                       │ Query cache       │                │
    │                          │                       ├──────────────────►│                │
    │                          │                       │ Return cached     │                │
    │                          │                       │◄──────────────────┤                │
    │                          │ Emit Success(cache)   │                   │                │
    │◄─────────────────────────┤                       │                   │                │
    │ Display Cached Data      │                       │                   │                │
    │ (Shows old data)         │                       │ Try API           │                │
    │                          │                       ├───────────────────┼─── ✗ Failed    │
    │                          │                       │ (No Internet)     │                │
    │                          │                       │                   │                │
    │ User tries to Submit     │                       │                   │                │
    ├─────────────────────────►│ submitLeaveRequest()  │                   │                │
    │                          ├──────────────────────►│ Call API          │                │
    │                          │                       ├───────────────────┼─── ✗ Failed    │
    │                          │ Emit Error            │                   │                │
    │◄─────────────────────────┤                       │                   │                │
    │ Show Error Toast         │                       │                   │                │
    │ "No internet connection" │                       │                   │                │
    └──────────────────────────┴───────────────────────┴───────────────────┴────────────────┘
```

## Component Interaction Map

```
┌─────────────────────────────────────────────────────────────────┐
│                      UI COMPONENTS                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  LeaveHistoryActivity                LeaveSubmissionActivity    │
│  ┌────────────────────┐              ┌─────────────────────┐   │
│  │ Toolbar            │              │ Toolbar             │   │
│  │ SwipeRefreshLayout │              │ ScrollView          │   │
│  │ ┌────────────────┐ │              │ ┌─────────────────┐ │   │
│  │ │ Balance Card   │ │              │ │ Balance Info    │ │   │
│  │ │ - Saldo Cuti   │ │              │ └─────────────────┘ │   │
│  │ │ - Total        │ │              │ ┌─────────────────┐ │   │
│  │ │ - Terpakai     │ │              │ │ Leave Type      │ │   │
│  │ │ - Year Filter  │ │              │ │ Selector        │ │   │
│  │ └────────────────┘ │              │ └─────────────────┘ │   │
│  │ ┌────────────────┐ │              │ ┌─────────────────┐ │   │
│  │ │ Submit Button  │ │              │ │ Date Pickers    │ │   │
│  │ └────────────────┘ │              │ │ - Start Date    │ │   │
│  │ ┌────────────────┐ │              │ │ - End Date      │ │   │
│  │ │ RecyclerView   │ │              │ └─────────────────┘ │   │
│  │ │ LeaveHistory   │ │              │ ┌─────────────────┐ │   │
│  │ │ Adapter        │ │              │ │ Description     │ │   │
│  │ └────────────────┘ │              │ │ TextInput       │ │   │
│  └────────────────────┘              │ └─────────────────┘ │   │
│                                       │ ┌─────────────────┐ │   │
│                                       │ │ Atasan Info     │ │   │
│                                       │ │ Card            │ │   │
│                                       │ └─────────────────┘ │   │
│                                       │ ┌─────────────────┐ │   │
│                                       │ │ Submit Button   │ │   │
│                                       │ └─────────────────┘ │   │
│                                       └─────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      STATE MANAGEMENT                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  LeaveViewModel (Hilt @HiltViewModel)                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ LiveData Observables:                                    │   │
│  │ • leaveBalance ───► UI updates balance card              │   │
│  │ • leaveDetails ───► UI updates recycler view             │   │
│  │ • submitResult ───► UI shows toast & navigation          │   │
│  │ • selectedLeaveType ───► UI shows/hides date fields      │   │
│  │ • startDate ───► UI updates start date button            │   │
│  │ • endDate ───► UI updates end date button                │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Leave Type Decision Tree

```
                         User Selects Leave Type
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
    DT/PC/MP                   C/I/S                   B/G/K
  (No dates)              (Strict validation)        (Any dates)
         │                        │                        │
         ▼                        ▼                        ▼
   Use today's           ┌────────┴────────┐        Show date
      date               │                 │          pickers
         │           Cuti/Izin         Sakit              │
         │               │                 │              │
         │               ▼                 ▼              │
         │      Min: Today          Max: Today           │
         │      (Future only)       (Past only)          │
         │               │                 │              │
         └───────────────┴─────────────────┴──────────────┘
                                  │
                                  ▼
                         Calculate Deduction
                                  │
                     ┌────────────┴────────────┐
                     │                         │
              Balance > 0                 Balance = 0
                     │                         │
                     ▼                         ▼
              pcuti = "Y"                pcuti = "N"
              pgaji = "N"                pgaji = "Y"
           (Deduct from leave)      (Deduct from salary)
                     │                         │
                     └────────────┬────────────┘
                                  │
                                  ▼
                          Submit to Backend
                                  │
                     ┌────────────┴────────────┐
                     │                         │
                 Success                    Error
                     │                         │
                     ▼                         ▼
            Refresh cache              Show error message
            Show success toast         Keep form open
            Navigate back              Enable retry
```

## Complete Tech Stack

```
┌─────────────────────────────────────────────────────────┐
│                  TECHNOLOGY STACK                       │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Language:          Kotlin 1.9                          │
│  Android SDK:       API 24+ (Android 7.0+)              │
│  Build Tool:        Gradle 8.0                          │
│                                                          │
│  Architecture:                                           │
│  • Pattern:         MVVM (Model-View-ViewModel)         │
│  • DI:              Hilt (Dependency Injection)          │
│  • Async:           Kotlin Coroutines + Flow            │
│  • Reactive:        LiveData                             │
│                                                          │
│  Data Layer:                                             │
│  • Local DB:        Room 2.5                             │
│  • Network:         Retrofit 2.9 + OkHttp 4.11          │
│  • Serialization:   Gson 2.10                            │
│                                                          │
│  UI Layer:                                               │
│  • Design:          Material Design 3                    │
│  • View Binding:    Android View Binding                │
│  • RecyclerView:    AndroidX RecyclerView                │
│  • Navigation:      Android Navigation Component        │
│                                                          │
│  Testing:                                                │
│  • Unit Tests:      JUnit 4                              │
│  • UI Tests:        Espresso                             │
│  • Mocking:         Mockito                              │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

**Diagram Version:** 1.0  
**Last Updated:** December 18, 2025  
**Created by:** Qoder AI Assistant
