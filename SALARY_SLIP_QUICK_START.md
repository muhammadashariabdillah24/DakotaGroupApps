# Salary Slip Feature - Quick Start Guide

## Overview
This guide helps you quickly test the new Salary Slip feature migrated from the old React Native system.

## Prerequisites
- DakotaGroupStaff app installed on Android device/emulator
- Valid user credentials (NIP and password)
- Backend API running and accessible
- Internet connection

## Step-by-Step Testing

### 1. Login
```
1. Open DakotaGroupStaff app
2. Enter your NIP
3. Enter your password
4. Click "Login"
```

### 2. Navigate to Salary Slip
```
1. From main menu, click "Kepegawaian" card
2. Click "Slip Gaji" card
3. You should see the Salary Slip List screen
```

### 3. View Salary Slips
**Expected Behavior:**
- Shows salary slips for current year by default
- Each item shows:
  - Month abbreviation (JUN, MAY, etc.)
  - Period (e.g., "Juni 2025")
  - Net salary amount
- List is sorted by month (newest first)

### 4. Filter by Year
```
1. Click on the year filter card (shows "TAHUN - 2025")
2. Dialog appears with available years
3. Select a different year
4. List updates to show slips for selected year
```

### 5. View Slip Detail
```
1. Click on any salary slip item
2. Detail screen shows:
   - Employee info (NIP, Name, Division, Position, Area)
   - Period
   - Income breakdown (left column)
   - Deduction breakdown (right column)
   - Total salary at bottom
```

### 6. Export to PDF
```
1. On detail screen, click the download FAB (floating action button)
2. If on Android 9 or below, grant storage permission when prompted
3. PDF is generated and saved
4. Toast shows save location
```

**PDF Location:**
- **Android 10+**: App-specific Documents folder
  ```
  /Android/data/com.dakotagroupstaff/files/Documents/SlipGaji-{Month}-{Year}.pdf
  ```
- **Android 9-**: Public Downloads folder
  ```
  /Download/SlipGaji-{Month}-{Year}.pdf
  ```

### 7. Pull to Refresh
```
1. On list screen, swipe down from top
2. Loading indicator appears
3. Data refreshes from API
```

## Common Scenarios

### Scenario 1: No Salary Slips Available
**Steps:**
1. Navigate to salary slip list
2. Select a year with no data

**Expected:**
- Empty state shows
- Message: "Tidak ada slip gaji"
- Icon displayed

### Scenario 2: Network Error
**Steps:**
1. Disconnect from internet
2. Navigate to salary slip list
3. Pull to refresh

**Expected:**
- Error toast appears
- Previous data (if any) remains visible

### Scenario 3: PDF Permission Denied (Android 9-)
**Steps:**
1. Click download on Android 9 device
2. Deny permission

**Expected:**
- Toast: "Izin penyimpanan diperlukan"
- PDF not generated

## Testing Checklist

### List Screen
- [ ] Displays salary slips for current year
- [ ] Month labels show 3-letter abbreviation (JUN, MAY, etc.)
- [ ] Net salary displays correctly
- [ ] Year filter opens dialog
- [ ] Can select different years
- [ ] List updates after year selection
- [ ] Pull to refresh works
- [ ] Empty state shows when no data
- [ ] Loading indicator appears during fetch
- [ ] Can navigate back

### Detail Screen
- [ ] Employee info displays correctly
- [ ] Period formatted as MM/YYYY
- [ ] Income items show correct amounts
- [ ] Deduction items show correct amounts
- [ ] Total salary calculated correctly
- [ ] Download button visible
- [ ] Can navigate back

### PDF Export
- [ ] PDF generates successfully
- [ ] Toast shows save location
- [ ] File exists at specified location
- [ ] PDF content matches screen
- [ ] Permission requested on Android 9-
- [ ] Works without permission on Android 10+

### Error Handling
- [ ] Network errors show toast
- [ ] Invalid NIP shows error
- [ ] Missing data handled gracefully
- [ ] App doesn't crash on errors

## Sample Test Data

### Expected API Response
```json
{
  "success": true,
  "message": "Salary slips retrieved successfully",
  "data": [
    {
      "NIP": "0012404003",
      "NAMA": "MUHAMMAD ASHARI ABDILLAH",
      "DIVISI": "Sistem Informasi",
      "NAMAJABATAN": "Staf",
      "AREA": "DLI PUSAT",
      "PRIODE": "6/28/2025",
      "BULAN": "June",
      "TAHUN": "2025",
      "GAPOK": 5000000,
      "INSENTIF": 1000000,
      // ... other fields
    }
  ]
}
```

### Sample NIP for Testing
(Use actual test NIPs provided by the team)

## Troubleshooting

### Problem: No salary slips displayed
**Solution:**
1. Check if logged in with correct NIP
2. Verify backend API is running
3. Check network connectivity
4. Look at Logcat for API errors

### Problem: PDF not saving
**Solution:**
1. Check storage permission (Android 9-)
2. Verify app has write access
3. Check available storage space
4. Look at Logcat for file system errors

### Problem: Year filter shows no years
**Solution:**
1. Verify API returns data with TAHUN field
2. Check if data is being parsed correctly
3. Look at Logcat for parsing errors

### Problem: Wrong salary calculations
**Solution:**
1. Verify API data is correct
2. Check SalarySlipData calculation methods
3. Compare with backend calculations

## Logcat Commands

### View App Logs
```bash
adb logcat -s DakotaGroupStaff
```

### View Salary-Specific Logs
```bash
adb logcat | grep -i "salary"
```

### View API Logs
```bash
adb logcat | grep -i "SalaryRepository"
```

### View ViewModel Logs
```bash
adb logcat | grep -i "SalaryViewModel"
```

## Performance Testing

### Load Testing
1. Test with 1 salary slip
2. Test with 10 salary slips
3. Test with 50+ salary slips
4. Test with multiple years (5+ years)

**Expected:**
- Smooth scrolling
- No lag when filtering
- Fast PDF generation (<2 seconds)

### Memory Testing
1. Open/close detail screen 10 times
2. Switch between years 10 times
3. Generate 5 PDFs in a row

**Expected:**
- No memory leaks
- No crashes
- Consistent performance

## Integration Points to Verify

### 1. SessionManager
- [ ] getPt() returns correct value
- [ ] getNip() returns correct value

### 2. SalaryRepository
- [ ] API call succeeds
- [ ] Data parsed correctly
- [ ] Errors handled properly

### 3. SalaryViewModel
- [ ] LiveData updates
- [ ] Loading states correct
- [ ] Error messages shown

### 4. Navigation
- [ ] Can navigate from Kepegawaian menu
- [ ] Can navigate to detail
- [ ] Back button works

## Known Issues / Limitations

1. **Watermark**: PDF doesn't include company logo watermark (can be added if needed)
2. **Preview**: No PDF preview before saving (saves directly)
3. **Share**: No share functionality (can be added if needed)

## Next Steps

After successful testing:
1. Report any bugs found
2. Suggest UI/UX improvements
3. Request additional features
4. Verify with production data

## Support

For issues or questions:
1. Check [SALARY_SLIP_MIGRATION.md](./SALARY_SLIP_MIGRATION.md) for technical details
2. Review [SALARY_DATA_GROUPING_GUIDE.md](./SALARY_DATA_GROUPING_GUIDE.md) for data structure
3. Contact development team

---

**Last Updated**: January 2025
**Version**: 1.0
