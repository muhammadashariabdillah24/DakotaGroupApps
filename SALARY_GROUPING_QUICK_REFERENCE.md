# Salary Data Grouping - Quick Reference

## Summary
This guide explains how to extract and group Year and Month from the `PRIODE` property in the salary API response.

## Key Information

### PRIODE Format
- **Pattern**: `"M/D/YYYY"` or `"MM/DD/YYYY"`
- **Example**: `"6/28/2025"` means June 28, 2025
- **Parts**:
  - Part 1: Month (1-12)
  - Part 2: Day
  - Part 3: Year (YYYY)

### Files Created

1. **SalaryResponse.kt** - Data classes for salary API response
   - `SalarySlipData` - Main data class for salary slip
   - `SalarySlipsRequest` - Request body for API call
   - `SalaryByYear` - Helper class for year grouping
   - `SalaryByMonth` - Helper class for month grouping

2. **SalaryDataHelper.kt** - Utility functions for grouping and filtering
   - `groupByYear()` - Group slips by year
   - `groupByYearAndMonth()` - Group slips by year and month
   - `filterByYear()` - Get slips for specific year
   - `filterByYearAndMonth()` - Get slips for specific year and month
   - `getAvailableYears()` - Get list of available years
   - `getMonthsForYear()` - Get available months for a year
   - `parsePriode()` - Parse PRIODE string
   - `formatCurrency()` - Format amount to Rupiah

3. **SalaryGroupingExample.kt** - Complete usage examples

## Quick Usage

### Extract Year and Month from Single Slip
```kotlin
val salarySlip: SalarySlipData = // from API

val year = salarySlip.getYear()          // Int: 2025
val month = salarySlip.getMonth()        // Int: 6
val monthName = salarySlip.getMonthName() // String: "June"
val period = salarySlip.getFormattedPeriod() // String: "June 2025"
```

### Group All Slips by Year
```kotlin
val salarySlips: List<SalarySlipData> = apiResponse.data ?: emptyList()

val groupedByYear = SalaryDataHelper.groupByYear(salarySlips)
// Returns: List<SalaryByYear>

groupedByYear.forEach { yearGroup ->
    println("Year: ${yearGroup.year}")
    println("Slips: ${yearGroup.salarySlips.size}")
    println("Total: ${yearGroup.getTotalNetSalary()}")
}
```

### Group All Slips by Year and Month
```kotlin
val groupedByYearAndMonth = SalaryDataHelper.groupByYearAndMonth(salarySlips)
// Returns: List<SalaryByMonth>

groupedByYearAndMonth.forEach { monthGroup ->
    println("${monthGroup.getFormattedPeriod()}: ${monthGroup.getTotalNetSalary()}")
}
```

### Get Available Years
```kotlin
val years = SalaryDataHelper.getAvailableYears(salarySlips)
// Returns: [2025, 2024] (sorted descending)
```

### Filter by Specific Year
```kotlin
val slips2025 = SalaryDataHelper.filterByYear(salarySlips, 2025)
```

### Filter by Year and Month
```kotlin
val june2025 = SalaryDataHelper.filterByYearAndMonth(salarySlips, 2025, 6)
```

### Get Months for Specific Year
```kotlin
val months = SalaryDataHelper.getMonthsForYear(salarySlips, 2025)
// Returns: Map<Int, String> = {6="June", 5="May", 4="April", ...}
```

### Parse PRIODE String Directly
```kotlin
val (year, month) = SalaryDataHelper.parsePriode("6/28/2025") ?: Pair(0, 0)
// year = 2025, month = 6
```

### Calculate Salary Totals
```kotlin
val slip: SalarySlipData = // from API

val totalIncome = slip.getTotalIncome()
val totalDeductions = slip.getTotalDeductions()
val netSalary = slip.getNetSalary()

// Formatted
val formatted = SalaryDataHelper.formatCurrency(netSalary)
// Returns: "Rp 1.000.000"
```

### Get Summary Statistics
```kotlin
val summary = SalaryDataHelper.getSummary(salarySlips)

println(summary.getFormattedTotalIncome())
println(summary.getFormattedTotalDeductions())
println(summary.getFormattedNetSalary())
println(summary.getFormattedAverageSalary())
println("Count: ${summary.slipCount}")
```

## Common Patterns

### Display Year List
```kotlin
val years = SalaryDataHelper.getAvailableYears(salarySlips)
years.forEach { year ->
    val yearSlips = SalaryDataHelper.filterByYear(salarySlips, year)
    val total = yearSlips.sumOf { it.getNetSalary() }
    println("$year: ${SalaryDataHelper.formatCurrency(total)}")
}
```

### Display Month List for Selected Year
```kotlin
fun showMonthsForYear(year: Int) {
    val months = SalaryDataHelper.getMonthsForYear(salarySlips, year)
    months.forEach { (monthNum, monthName) ->
        val monthSlips = SalaryDataHelper.filterByYearAndMonth(salarySlips, year, monthNum)
        val total = monthSlips.sumOf { it.getNetSalary() }
        println("$monthName: ${SalaryDataHelper.formatCurrency(total)}")
    }
}
```

### Show Details for Selected Month
```kotlin
fun showMonthDetails(year: Int, month: Int) {
    val slips = SalaryDataHelper.filterByYearAndMonth(salarySlips, year, month)
    slips.forEach { slip ->
        println("Slip ID: ${slip.id}")
        println("Period: ${slip.getFormattedPeriod()}")
        println("Net: ${SalaryDataHelper.formatCurrency(slip.getNetSalary())}")
    }
}
```

## Data Flow Example

```
API Response
    ↓
List<SalarySlipData>
    ↓
┌─────────────────────┬─────────────────────┬─────────────────────┐
│   Group by Year     │ Group by Year+Month │    Filter           │
├─────────────────────┼─────────────────────┼─────────────────────┤
│ List<SalaryByYear>  │ List<SalaryByMonth> │ Custom filters      │
│   - 2025: [...]     │   - June 2025: [...] │   - By year        │
│   - 2024: [...]     │   - May 2025: [...]  │   - By year+month  │
│                     │   - April 2025: [...] │                    │
└─────────────────────┴─────────────────────┴─────────────────────┘
                            ↓
                    Display in RecyclerView
                    or other UI components
```

## Important Methods Reference

### SalarySlipData Methods
- `getYear(): Int` - Extract year from PRIODE
- `getMonth(): Int` - Extract month from PRIODE (1-12)
- `getMonthName(): String` - Get month name from BULAN
- `getFormattedPeriod(): String` - Get "MonthName YYYY" format
- `getTotalIncome(): Int` - Calculate total income
- `getTotalDeductions(): Int` - Calculate total deductions
- `getNetSalary(): Int` - Calculate net salary

### SalaryDataHelper Methods
- `groupByYear(List): List<SalaryByYear>`
- `groupByYearAndMonth(List): List<SalaryByMonth>`
- `getAvailableYears(List): List<Int>`
- `filterByYear(List, Int): List<SalarySlipData>`
- `filterByYearAndMonth(List, Int, Int): List<SalarySlipData>`
- `getMonthsForYear(List, Int): Map<Int, String>`
- `parsePriode(String): Pair<Int, Int>?`
- `formatCurrency(Int): String`
- `getSummary(List): SalarySummary`

### SalaryByYear Methods
- `groupByMonth(): Map<Int, List<SalarySlipData>>`
- `getTotalNetSalary(): Int`

### SalaryByMonth Methods
- `getTotalNetSalary(): Int`
- `getFormattedPeriod(): String`

## Testing

To test the implementation, see the example file:
```
app/src/main/java/com/dakotagroupstaff/examples/SalaryGroupingExample.kt
```

Run `SalaryGroupingExample.runAllExamples(sampleData)` with your API data.

## Notes

1. **Data Sorting**: All grouping functions automatically sort data in descending order (newest first)
2. **Null Safety**: All functions handle null values safely
3. **Performance**: Grouping operations are efficient using Kotlin's built-in collection functions
4. **Format Consistency**: All currency formatting uses Indonesian Rupiah format with dot separators
