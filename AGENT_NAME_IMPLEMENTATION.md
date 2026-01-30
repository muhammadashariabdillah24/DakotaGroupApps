# Agent Name Display Implementation

## Overview
This document describes the implementation for displaying agent/branch names in the "Start Agen" field on the Assignment (Surat Tugas) page.

## Requirement
The "Start Agen" field in the "Detail Informasi" section should display the agent or branch name fetched from the API endpoint:
```
GET /api/v1/agent/locations?pt=<pt>
```

## Implementation Details

### 1. Repository Layer - `AssignmentRepository.kt`

Added a new method to fetch agent locations:

```kotlin
/**
 * Get agent locations to map agent codes to names
 * @param pt Company code (DBS, DLB, Logistik)
 * @return Flow of Result containing list of AgentLocationData
 */
fun getAgentLocations(
    pt: String
): Flow<Result<List<AgentLocationData>>> = flow {
    emit(Result.Loading)
    
    try {
        val response = apiService.getAgentLocations(pt = pt)
        
        if (response.success) {
            emit(Result.Success(response.data ?: emptyList()))
        } else {
            emit(Result.Error(response.message ?: "Failed to get agent locations"))
        }
    } catch (e: Exception) {
        emit(Result.Error(e.message ?: "Failed to get agent locations"))
    }
}.flowOn(Dispatchers.IO)
```

**Location:** Lines 20-43

### 2. ViewModel Layer - `AssignmentViewModel.kt`

#### Added LiveData and Cache
```kotlin
private val _agentLocations = MutableLiveData<Result<List<AgentLocationData>>>()
val agentLocations: LiveData<Result<List<AgentLocationData>>> = _agentLocations

// Cache for agent locations to avoid repeated API calls
private var agentLocationsCache: List<AgentLocationData>? = null
```

**Location:** Lines 34-38

#### Added Methods

**1. Fetch Agent Locations (with caching)**
```kotlin
/**
 * Get agent locations for mapping agent codes to names
 * @param pt Company code (DBS, DLB, Logistik)
 */
fun getAgentLocations(pt: String) {
    // Return cached data if available
    if (agentLocationsCache != null) {
        _agentLocations.value = Result.Success(agentLocationsCache!!)
        return
    }
    
    viewModelScope.launch {
        assignmentRepository.getAgentLocations(pt).collect { result ->
            _agentLocations.postValue(result)
            // Cache successful results
            if (result is Result.Success) {
                agentLocationsCache = result.data
            }
        }
    }
}
```

**2. Get Agent Name by Code**
```kotlin
/**
 * Get agent name by agent code
 * @param agentCode Agent code (KodeAgen)
 * @return Agent name or null if not found
 */
fun getAgentNameByCode(agentCode: Int): String? {
    return agentLocationsCache?.find { it.kodeAgen == agentCode }?.namaAgen
}
```

**Location:** Lines 71-101

### 3. Activity Layer - `AssignmentActivity.kt`

#### Observer Setup
Added observer to monitor agent location loading:

```kotlin
private fun setupObservers() {
    // Observe agent locations for mapping agent codes to names
    viewModel.agentLocations.observe(this) { result ->
        // Agent locations loaded silently in background
        // Used for mapping startAgen code to agent name
        if (result is Result.Error) {
            android.util.Log.w("AssignmentActivity", "Failed to load agent locations: ${result.message}")
        }
    }
    
    // Observe assignment data...
}
```

**Location:** Lines 78-86

#### Display Logic
Updated the display logic to show agent name with code:

```kotlin
// Display start agent with name from API
val agentText = if (data.startAgen == 0) {
    "-"
} else {
    // Try to get agent name from cache
    val agentName = viewModel.getAgentNameByCode(data.startAgen)
    if (agentName != null) {
        "$agentName (Agen ${data.startAgen})"
    } else {
        "Agen ${data.startAgen}"
    }
}
binding.tvStartAgent.text = agentText
```

**Location:** Lines 141-153

#### Data Loading
Updated `loadData()` to fetch agent locations first:

```kotlin
private fun loadData() {
    currentPt?.let { pt ->
        // Load agent locations first for mapping agent codes to names
        viewModel.getAgentLocations(pt)
        
        currentNip?.let { nip ->
            viewModel.getLetterOfAssignment(pt, nip)
        }
    }
}
```

**Location:** Lines 243-251

## API Response Structure

### Agent Locations API Response
```json
{
  "success": true,
  "message": "Retrieved agent locations",
  "data": [
    {
      "KodeAgen": 1,
      "NamaAgen": "Cabang Jakarta",
      "AgenLat": "-6.2088",
      "AgenLon": "106.8456",
      "AgenMD5": "abc123",
      "AgenRange": "500"
    }
  ]
}
```

### Data Model - `AgentLocationData`
```kotlin
data class AgentLocationData(
    @SerializedName("KodeAgen")
    val kodeAgen: Int,
    
    @SerializedName("NamaAgen")
    val namaAgen: String,
    
    @SerializedName("AgenLat")
    val agenLat: String,
    
    @SerializedName("AgenLon")
    val agenLon: String,
    
    @SerializedName("AgenMD5")
    val agenMd5: String? = null,
    
    @SerializedName("AgenRange")
    val agenRange: String
)
```

## Display Format

The "Start Agen" field will display in the following formats:

1. **When startAgen is 0 (no agent):**
   ```
   -
   ```

2. **When agent name is found:**
   ```
   Cabang Jakarta (Agen 1)
   ```

3. **When agent name is not found (fallback):**
   ```
   Agen 1
   ```

## Benefits

1. **Better UX**: Users see meaningful agent/branch names instead of just numeric codes
2. **Performance**: Agent locations are cached to avoid repeated API calls
3. **Graceful Degradation**: If agent name cannot be found, falls back to showing the agent code
4. **Silent Loading**: Agent locations load in the background without blocking the main UI

## Testing

To test this implementation:

1. Open the Assignment (Surat Tugas) page
2. Navigate to the "Detail Informasi" card
3. Check the "Start Agen" field
4. Expected: Should display agent name with code, e.g., "Cabang Jakarta (Agen 1)"
5. If no agent name is available: Should display "Agen X" where X is the agent code
6. If startAgen is 0: Should display "-"

## Build Status

**Build Result:** ✅ BUILD SUCCESSFUL in 2s

All 43 tasks completed successfully with no compilation errors.

## Files Modified

1. **AssignmentRepository.kt**
   - Added `getAgentLocations()` method
   - Lines: 20-43

2. **AssignmentViewModel.kt**
   - Added `_agentLocations` LiveData
   - Added `agentLocationsCache` for caching
   - Added `getAgentLocations()` method
   - Added `getAgentNameByCode()` method
   - Lines: 34-38, 71-101

3. **AssignmentActivity.kt**
   - Added agent locations observer in `setupObservers()`
   - Updated agent name display logic
   - Updated `loadData()` to fetch agent locations
   - Lines: 78-86, 141-153, 243-251

## Related Files (Unchanged)

- **AgentLocationResponse.kt**: Contains `AgentLocationData` model
- **ApiService.kt**: Already has `getAgentLocations()` API method defined
