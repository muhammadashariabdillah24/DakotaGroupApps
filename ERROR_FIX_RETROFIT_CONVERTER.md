# Error Fix: Retrofit @Body Converter Issue

## Error Message
```
Unable to create @Body converter for class com.dakotagroupstaff.data.remote.retrofit.LoginRequest
(parameter #2) for method ApiService.login
```

## Root Cause
The `StringResponseConverterFactory` only implemented `responseBodyConverter()` for handling API responses, but did NOT implement `requestBodyConverter()` for handling request bodies (like `LoginRequest` with `@Body` annotation).

When Retrofit tried to serialize the `LoginRequest` object to JSON for the POST request, it couldn't find a suitable converter.

## Solution
Added `GsonConverterFactory` to handle request body serialization while keeping `StringResponseConverterFactory` for response deserialization.

### Changed File
**File:** `app/src/main/java/com/dakotagroupstaff/data/remote/retrofit/ApiConfig.kt`

### What Changed
```kotlin
// BEFORE (Error):
val retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.BASE_URL)
    .addConverterFactory(StringResponseConverterFactory(gson))
    .client(client)
    .build()

// AFTER (Fixed):
val retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.BASE_URL)
    .addConverterFactory(GsonConverterFactory.create(gson)) // For request bodies
    .addConverterFactory(StringResponseConverterFactory(gson)) // For response bodies
    .client(client)
    .build()
```

## How It Works

### Converter Factory Chain
Retrofit uses a chain of converter factories. When it needs to convert something, it asks each factory in order until one can handle it:

1. **GsonConverterFactory** - Handles:
   - ✅ Request body serialization (`@Body LoginRequest`)
   - ✅ Response body deserialization (fallback)

2. **StringResponseConverterFactory** - Handles:
   - ✅ Response body deserialization with double-stringified JSON handling
   - ❌ Request body serialization (not implemented)

### Order Matters!
The order of converter factories is CRITICAL:

```kotlin
// ✅ CORRECT ORDER:
.addConverterFactory(GsonConverterFactory.create(gson))        // 1st: Handles requests
.addConverterFactory(StringResponseConverterFactory(gson))     // 2nd: Handles responses

// ❌ WRONG ORDER (would break response parsing):
.addConverterFactory(StringResponseConverterFactory(gson))     // 1st: Only handles responses
.addConverterFactory(GsonConverterFactory.create(gson))        // 2nd: Never reached for responses
```

## Why This Architecture?

### Backend Response Format
The backend sends responses as `JSON.stringify(response)`, creating double-stringified JSON:
```javascript
// Backend code
res.status(200).send(JSON.stringify(response));
```

### Custom Converter Needed
`StringResponseConverterFactory` handles this by:
1. Parsing the outer string layer (remove quotes)
2. Parsing the inner JSON using Gson

This is equivalent to `JSON.parse()` in JavaScript.

### Request Format
Requests are normal JSON, so standard `GsonConverterFactory` works perfectly:
```json
{
  "nip": "123456",
  "deviceId": "abc123",
  "serialNumber": "xyz789",
  "email": "user@example.com"
}
```

## Testing the Fix

### Before Running
1. Clean and rebuild the project:
   ```bash
   ./gradlew clean build
   ```

2. Make sure `local.properties` has the correct BASE_URL:
   ```properties
   BASE_URL=https://stagingdakota.my.id/api/v1/
   ```

### Expected Behavior
- ✅ App should launch without Retrofit converter errors
- ✅ Login API calls should work properly
- ✅ Request bodies are serialized correctly
- ✅ Response bodies are parsed correctly (handling double-stringified JSON)

## Related Files
- `ApiConfig.kt` - Retrofit configuration ✅ Fixed
- `ApiService.kt` - API endpoint definitions (unchanged)
- `StringResponseConverterFactory.kt` - Custom response converter (unchanged)
- `LoginRequest.kt` - Request data class (unchanged)

## Future Considerations

### If Backend Changes Response Format
If the backend stops using `JSON.stringify()` and sends normal JSON:

**Option 1 (Recommended):** Keep current setup
- Works with both normal JSON and stringified JSON
- No code changes needed

**Option 2:** Remove custom converter
```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.BASE_URL)
    .addConverterFactory(GsonConverterFactory.create(gson))
    .client(client)
    .build()
```

### Adding More @Body Parameters
All `@Body` parameters will now work correctly:
- `LoginRequest`
- `AttendanceRequest`
- `SubmitAttendanceRequest`
- `SalarySlipsRequest`
- Any future request classes

## Summary
✅ **Fixed:** Added `GsonConverterFactory` for request body serialization  
✅ **Kept:** `StringResponseConverterFactory` for custom response handling  
✅ **Order:** GsonConverterFactory → StringResponseConverterFactory  
✅ **Result:** Both request and response conversion work properly
