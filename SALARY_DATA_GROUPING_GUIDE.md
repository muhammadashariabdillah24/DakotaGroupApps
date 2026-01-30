# Salary Data Grouping - Usage Examples

## Overview
This document explains how to parse and group salary data from the API response, specifically extracting Year and Month from the `PRIODE` field.

## API Response Structure
The API endpoint `/salary/slips?pt=<pt>` returns salary data with the following structure:

```json
{
  "success": true,
  "message": "Salary slips retrieved successfully from PT Logistik",
  "data": [
    {
      "NAMA": "MUHAMMAD ASHARI ABDILLAH",
      "NIP": "0012404003",
      "ID": "00106250014",
      "PRIODE": "6/28/2025",
      "BULAN": "June",
      "TAHUN": "2025",
      "GAPOK": 0,
      "INSENTIF": 0,
      // ... other fields
    }
  ]
}
```

## PRIODE Field Format
- **Format**: `"M/D/YYYY"` or `"MM/DD/YYYY"`
- **Examples**: 
  - `"6/28/2025"` → Month: 6 (June), Year: 2025
  - `"12/30/2024"` → Month: 12 (December), Year: 2024
  - `"5/30/2025"` → Month: 5 (May), Year: 2025

## Usage Examples

### 1. Basic Parsing - Extract Year and Month

```kotlin
// Parse individual salary slip
val salarySlip: SalarySlipData = // ... from API

// Get year (Int)
val year = salarySlip.getYear() // Returns 2025

// Get month number (1-12)
val month = salarySlip.getMonth() // Returns 6 for June

// Get month name
val monthName = salarySlip.getMonthName() // Returns "June"

// Get formatted period
val period = salarySlip.getFormattedPeriod() // Returns "June 2025"
```

### 2. Group by Year

```kotlin
// Assume you have a list of salary slips from API
val salarySlips: List<SalarySlipData> = apiResponse.data ?: emptyList()

// Group by year
val groupedByYear = SalaryDataHelper.groupByYear(salarySlips)

// Result: List<SalaryByYear>
// [
//   SalaryByYear(year=2025, salarySlips=[...]),
//   SalaryByYear(year=2024, salarySlips=[...])
// ]

// Access data
groupedByYear.forEach { yearGroup ->
    println("Year: ${yearGroup.year}")
    println("Number of slips: ${yearGroup.salarySlips.size}")
    println("Total net salary: ${yearGroup.getTotalNetSalary()}")
    
    // Get months for this year
    val monthlyData = yearGroup.groupByMonth()
    monthlyData.forEach { (month, slips) ->
        println("  Month $month: ${slips.size} slips")
    }
}
```

### 3. Group by Year and Month

```kotlin
val salarySlips: List<SalarySlipData> = apiResponse.data ?: emptyList()

// Group by year and month
val groupedByYearAndMonth = SalaryDataHelper.groupByYearAndMonth(salarySlips)

// Result: List<SalaryByMonth>
// [
//   SalaryByMonth(year=2025, month=6, monthName="June", salarySlips=[...]),
//   SalaryByMonth(year=2025, month=5, monthName="May", salarySlips=[...]),
//   SalaryByMonth(year=2025, month=4, monthName="April", salarySlips=[...]),
//   ...
// ]

// Display in RecyclerView or UI
groupedByYearAndMonth.forEach { monthGroup ->
    println("${monthGroup.getFormattedPeriod()}: ${monthGroup.getTotalNetSalary()}")
}
```

### 4. Filter by Specific Year

```kotlin
val salarySlips: List<SalarySlipData> = apiResponse.data ?: emptyList()

// Get all available years
val availableYears = SalaryDataHelper.getAvailableYears(salarySlips)
// Returns: [2025, 2024]

// Filter slips for specific year
val slips2025 = SalaryDataHelper.filterByYear(salarySlips, 2025)

// Get months available for year 2025
val monthsFor2025 = SalaryDataHelper.getMonthsForYear(salarySlips, 2025)
// Returns: Map<Int, String> = {6="June", 5="May", 4="April", ...}
```

### 5. Filter by Specific Year and Month

```kotlin
val salarySlips: List<SalarySlipData> = apiResponse.data ?: emptyList()

// Get salary slips for June 2025
val june2025Slips = SalaryDataHelper.filterByYearAndMonth(
    salarySlips = salarySlips,
    year = 2025,
    month = 6
)

// Calculate totals for June 2025
june2025Slips.forEach { slip ->
    println("Total Income: ${slip.getTotalIncome()}")
    println("Total Deductions: ${slip.getTotalDeductions()}")
    println("Net Salary: ${slip.getNetSalary()}")
}
```

### 6. Get Summary Statistics

```kotlin
val salarySlips: List<SalarySlipData> = apiResponse.data ?: emptyList()

// Get summary for all slips
val summary = SalaryDataHelper.getSummary(salarySlips)

println("Total Income: ${summary.getFormattedTotalIncome()}")
println("Total Deductions: ${summary.getFormattedTotalDeductions()}")
println("Net Salary: ${summary.getFormattedNetSalary()}")
println("Average Salary: ${summary.getFormattedAverageSalary()}")
println("Number of slips: ${summary.slipCount}")

// Get summary for specific year
val slips2025 = SalaryDataHelper.filterByYear(salarySlips, 2025)
val summary2025 = SalaryDataHelper.getSummary(slips2025)
```

### 7. Parse PRIODE String Directly

```kotlin
// Parse PRIODE string to get year and month
val priode = "6/28/2025"
val (year, month) = SalaryDataHelper.parsePriode(priode) ?: Pair(0, 0)

println("Year: $year")   // 2025
println("Month: $month") // 6
```

## ViewModel Example

```kotlin
class SalaryViewModel : ViewModel() {
    
    private val _salarySlips = MutableLiveData<List<SalarySlipData>>()
    val salarySlips: LiveData<List<SalarySlipData>> = _salarySlips
    
    private val _groupedByYear = MutableLiveData<List<SalaryByYear>>()
    val groupedByYear: LiveData<List<SalaryByYear>> = _groupedByYear
    
    private val _availableYears = MutableLiveData<List<Int>>()
    val availableYears: LiveData<List<Int>> = _availableYears
    
    fun loadSalaryData(nip: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getSalarySlips(
                    pt = "Logistik",
                    request = SalarySlipsRequest(nip = nip)
                )
                
                if (response.success && response.data != null) {
                    _salarySlips.value = response.data
                    
                    // Group by year
                    _groupedByYear.value = SalaryDataHelper.groupByYear(response.data)
                    
                    // Get available years
                    _availableYears.value = SalaryDataHelper.getAvailableYears(response.data)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun getMonthsForYear(year: Int): Map<Int, String> {
        val slips = _salarySlips.value ?: emptyList()
        return SalaryDataHelper.getMonthsForYear(slips, year)
    }
    
    fun getSalaryForMonth(year: Int, month: Int): List<SalarySlipData> {
        val slips = _salarySlips.value ?: emptyList()
        return SalaryDataHelper.filterByYearAndMonth(slips, year, month)
    }
}
```

## RecyclerView Adapter Example

```kotlin
class YearAdapter(
    private val onYearClick: (Int) -> Unit
) : RecyclerView.Adapter<YearAdapter.YearViewHolder>() {
    
    private var yearList: List<SalaryByYear> = emptyList()
    
    fun submitList(list: List<SalaryByYear>) {
        yearList = list
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearViewHolder {
        // Inflate layout
        return YearViewHolder(/* binding */)
    }
    
    override fun onBindViewHolder(holder: YearViewHolder, position: Int) {
        val yearData = yearList[position]
        holder.bind(yearData)
    }
    
    override fun getItemCount() = yearList.size
    
    inner class YearViewHolder(/* binding */) : RecyclerView.ViewHolder(/* view */) {
        fun bind(yearData: SalaryByYear) {
            // binding.textYear.text = yearData.year.toString()
            // binding.textSlipCount.text = "${yearData.salarySlips.size} slips"
            // binding.textTotalSalary.text = SalaryDataHelper.formatCurrency(yearData.getTotalNetSalary())
            
            itemView.setOnClickListener {
                onYearClick(yearData.year)
            }
        }
    }
}
```

## Complete Flow Example

```kotlin
// In Activity or Fragment
class SalaryActivity : AppCompatActivity() {
    
    private lateinit var viewModel: SalaryViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load data
        viewModel.loadSalaryData(nip = "0012404003")
        
        // Observe grouped data
        viewModel.groupedByYear.observe(this) { yearGroups ->
            // Display year list
            yearAdapter.submitList(yearGroups)
        }
        
        // Observe available years
        viewModel.availableYears.observe(this) { years ->
            // Update year picker
            setupYearPicker(years)
        }
    }
    
    private fun onYearSelected(year: Int) {
        // Get months for selected year
        val months = viewModel.getMonthsForYear(year)
        
        // Display month list
        displayMonths(months)
    }
    
    private fun onMonthSelected(year: Int, month: Int) {
        // Get salary slips for selected month
        val slips = viewModel.getSalaryForMonth(year, month)
        
        // Display salary details
        displaySalaryDetails(slips)
    }
}
```

## Key Points

1. **PRIODE Format**: The `PRIODE` field uses format `"M/D/YYYY"` where:
   - First part = Month (1-12)
   - Second part = Day
   - Third part = Year (4 digits)

2. **Grouping Options**:
   - By Year only: Use `SalaryDataHelper.groupByYear()`
   - By Year and Month: Use `SalaryDataHelper.groupByYearAndMonth()`
   - Custom filtering: Use `filterByYear()` or `filterByYearAndMonth()`

3. **Calculations**:
   - Each `SalarySlipData` has methods to calculate:
     - `getTotalIncome()`: Sum of all income fields
     - `getTotalDeductions()`: Sum of all deduction fields
     - `getNetSalary()`: Total income minus total deductions

4. **Sorting**:
   - Data is automatically sorted in descending order (newest first)
   - Years: 2025, 2024, 2023...
   - Months: 12, 11, 10... 1

5. **Currency Formatting**:
   - Use `SalaryDataHelper.formatCurrency(amount)` to format amounts
   - Returns format: "Rp 1.000.000" (Indonesian Rupiah format)
