package com.dakotagroupstaff.examples

import com.dakotagroupstaff.data.remote.response.SalarySlipData
import com.dakotagroupstaff.utils.SalaryDataHelper

/**
 * Example demonstrating how to parse and group salary data
 * from the PRIODE field in the API response
 */
object SalaryGroupingExample {
    
    /**
     * Example 1: Parse PRIODE from a single salary slip
     */
    fun example1_ParseSingleSlip() {
        // Sample data from API
        val salarySlip = SalarySlipData(
            nama = "MUHAMMAD ASHARI ABDILLAH",
            nip = "0012404003",
            id = "00106250014",
            gapok = 0,
            insentif = 0,
            pengembalianPo = 0,
            insentifPph = 0,
            thr = "0.00",
            bpjsp = null,
            transport = 0,
            kesehatan = 0,
            keluarga = 0,
            jabatan = 0,
            asuransi = 0,
            jamsostek = 160303,
            koperasi = 0,
            klaim = 0,
            bpjsk = null,
            pph21 = 0,
            absensi = 0,
            lain = 0,
            bulan = "June",
            divisi = "Sistem Informasi",
            namaJabatan = "Staf",
            area = "DLI PUSAT",
            priode = "6/28/2025",
            tahun = "2025",
            iuranPaguyuban = 0
        )
        
        // Extract year and month
        val year = salarySlip.getYear()        // Returns: 2025
        val month = salarySlip.getMonth()      // Returns: 6
        val monthName = salarySlip.getMonthName() // Returns: "June"
        
        println("Year: $year")
        println("Month: $month")
        println("Month Name: $monthName")
        println("Formatted Period: ${salarySlip.getFormattedPeriod()}")
        
        // Output:
        // Year: 2025
        // Month: 6
        // Month Name: June
        // Formatted Period: June 2025
    }
    
    /**
     * Example 2: Group salary slips by year
     */
    fun example2_GroupByYear(salarySlips: List<SalarySlipData>) {
        // Group by year
        val groupedByYear = SalaryDataHelper.groupByYear(salarySlips)
        
        // Display results
        groupedByYear.forEach { yearGroup ->
            println("\n=== Year ${yearGroup.year} ===")
            println("Total slips: ${yearGroup.salarySlips.size}")
            println("Total net salary: ${SalaryDataHelper.formatCurrency(yearGroup.getTotalNetSalary())}")
            
            // Show months in this year
            val monthlyData = yearGroup.groupByMonth()
            monthlyData.forEach { (monthNum, slips) ->
                val monthName = slips.firstOrNull()?.getMonthName() ?: ""
                println("  $monthName ($monthNum): ${slips.size} slip(s)")
            }
        }
        
        // Example Output:
        // === Year 2025 ===
        // Total slips: 6
        // Total net salary: Rp -961.818
        //   June (6): 1 slip(s)
        //   May (5): 1 slip(s)
        //   April (4): 1 slip(s)
        //   ...
        //
        // === Year 2024 ===
        // Total slips: 9
        // Total net salary: Rp -1.229.990
        //   December (12): 1 slip(s)
        //   November (11): 1 slip(s)
        //   ...
    }
    
    /**
     * Example 3: Group salary slips by year and month
     */
    fun example3_GroupByYearAndMonth(salarySlips: List<SalarySlipData>) {
        // Group by year and month
        val groupedByYearAndMonth = SalaryDataHelper.groupByYearAndMonth(salarySlips)
        
        // Display results
        println("\n=== Salary Slips by Month ===")
        groupedByYearAndMonth.forEach { monthGroup ->
            val period = monthGroup.getFormattedPeriod()
            val totalSalary = SalaryDataHelper.formatCurrency(monthGroup.getTotalNetSalary())
            
            println("$period: $totalSalary (${monthGroup.salarySlips.size} slip(s))")
        }
        
        // Example Output:
        // === Salary Slips by Month ===
        // June 2025: Rp -160.303 (1 slip(s))
        // May 2025: Rp -160.303 (1 slip(s))
        // April 2025: Rp -160.303 (1 slip(s))
        // March 2025: Rp -160.303 (1 slip(s))
        // February 2025: Rp -160.303 (1 slip(s))
        // January 2025: Rp -160.303 (1 slip(s))
        // December 2024: Rp -160.303 (1 slip(s))
        // ...
    }
    
    /**
     * Example 4: Get available years
     */
    fun example4_GetAvailableYears(salarySlips: List<SalarySlipData>) {
        // Get all available years
        val years = SalaryDataHelper.getAvailableYears(salarySlips)
        
        println("\n=== Available Years ===")
        years.forEach { year ->
            val slipsCount = SalaryDataHelper.filterByYear(salarySlips, year).size
            println("$year: $slipsCount slip(s)")
        }
        
        // Example Output:
        // === Available Years ===
        // 2025: 6 slip(s)
        // 2024: 9 slip(s)
    }
    
    /**
     * Example 5: Filter by specific year
     */
    fun example5_FilterByYear(salarySlips: List<SalarySlipData>, year: Int) {
        // Get slips for specific year
        val yearSlips = SalaryDataHelper.filterByYear(salarySlips, year)
        
        println("\n=== Salary Slips for $year ===")
        println("Total slips: ${yearSlips.size}")
        
        // Get months available for this year
        val months = SalaryDataHelper.getMonthsForYear(salarySlips, year)
        println("Months available:")
        months.forEach { (monthNum, monthName) ->
            println("  $monthName ($monthNum)")
        }
        
        // Calculate summary
        val summary = SalaryDataHelper.getSummary(yearSlips)
        println("\nSummary for $year:")
        println("  Total Income: ${summary.getFormattedTotalIncome()}")
        println("  Total Deductions: ${summary.getFormattedTotalDeductions()}")
        println("  Net Salary: ${summary.getFormattedNetSalary()}")
        println("  Average Salary: ${summary.getFormattedAverageSalary()}")
    }
    
    /**
     * Example 6: Filter by specific year and month
     */
    fun example6_FilterByYearAndMonth(
        salarySlips: List<SalarySlipData>,
        year: Int,
        month: Int
    ) {
        // Get slips for specific year and month
        val monthSlips = SalaryDataHelper.filterByYearAndMonth(salarySlips, year, month)
        
        println("\n=== Salary Slips for $year-$month ===")
        println("Total slips: ${monthSlips.size}")
        
        // Display each slip
        monthSlips.forEach { slip ->
            println("\nSlip ID: ${slip.id}")
            println("  Period: ${slip.getFormattedPeriod()}")
            println("  Total Income: ${SalaryDataHelper.formatCurrency(slip.getTotalIncome())}")
            println("  Total Deductions: ${SalaryDataHelper.formatCurrency(slip.getTotalDeductions())}")
            println("  Net Salary: ${SalaryDataHelper.formatCurrency(slip.getNetSalary())}")
        }
    }
    
    /**
     * Example 7: Parse PRIODE string directly
     */
    fun example7_ParsePriodeString() {
        val priodeExamples = listOf(
            "6/28/2025",
            "12/30/2024",
            "5/29/2024",
            "1/30/2025"
        )
        
        println("\n=== Parse PRIODE Strings ===")
        priodeExamples.forEach { priode ->
            val (year, month) = SalaryDataHelper.parsePriode(priode) ?: Pair(0, 0)
            println("$priode → Year: $year, Month: $month")
        }
        
        // Example Output:
        // === Parse PRIODE Strings ===
        // 6/28/2025 → Year: 2025, Month: 6
        // 12/30/2024 → Year: 2024, Month: 12
        // 5/29/2024 → Year: 2024, Month: 5
        // 1/30/2025 → Year: 2025, Month: 1
    }
    
    /**
     * Example 8: Complete workflow - from API response to grouped data
     */
    fun example8_CompleteWorkflow(apiResponseData: List<SalarySlipData>) {
        println("\n=== Complete Workflow ===")
        println("Total salary slips received: ${apiResponseData.size}\n")
        
        // Step 1: Get available years
        val years = SalaryDataHelper.getAvailableYears(apiResponseData)
        println("Available years: ${years.joinToString(", ")}")
        
        // Step 2: Group by year
        val groupedByYear = SalaryDataHelper.groupByYear(apiResponseData)
        println("\nGrouped by year:")
        groupedByYear.forEach { yearGroup ->
            println("  ${yearGroup.year}: ${yearGroup.salarySlips.size} slips, " +
                    "Total: ${SalaryDataHelper.formatCurrency(yearGroup.getTotalNetSalary())}")
        }
        
        // Step 3: For each year, show months
        groupedByYear.forEach { yearGroup ->
            println("\nMonths in ${yearGroup.year}:")
            val months = SalaryDataHelper.getMonthsForYear(apiResponseData, yearGroup.year)
            months.forEach { (monthNum, monthName) ->
                val monthSlips = SalaryDataHelper.filterByYearAndMonth(
                    apiResponseData, 
                    yearGroup.year, 
                    monthNum
                )
                val monthTotal = monthSlips.sumOf { it.getNetSalary() }
                println("  $monthName: ${SalaryDataHelper.formatCurrency(monthTotal)}")
            }
        }
        
        // Step 4: Overall summary
        val overallSummary = SalaryDataHelper.getSummary(apiResponseData)
        println("\n=== Overall Summary ===")
        println("Total Slips: ${overallSummary.slipCount}")
        println("Total Income: ${overallSummary.getFormattedTotalIncome()}")
        println("Total Deductions: ${overallSummary.getFormattedTotalDeductions()}")
        println("Total Net Salary: ${overallSummary.getFormattedNetSalary()}")
        println("Average Monthly Salary: ${overallSummary.getFormattedAverageSalary()}")
    }
    
    /**
     * Demo function to run all examples
     */
    fun runAllExamples(sampleData: List<SalarySlipData>) {
        println("========================================")
        println("SALARY DATA GROUPING EXAMPLES")
        println("========================================")
        
        example1_ParseSingleSlip()
        example2_GroupByYear(sampleData)
        example3_GroupByYearAndMonth(sampleData)
        example4_GetAvailableYears(sampleData)
        example5_FilterByYear(sampleData, 2025)
        example6_FilterByYearAndMonth(sampleData, 2025, 6)
        example7_ParsePriodeString()
        example8_CompleteWorkflow(sampleData)
        
        println("\n========================================")
        println("END OF EXAMPLES")
        println("========================================")
    }
}
