package com.dakotagroupstaff.utils

import com.dakotagroupstaff.data.remote.response.SalaryByMonth
import com.dakotagroupstaff.data.remote.response.SalaryByYear
import com.dakotagroupstaff.data.remote.response.SalarySlipData

/**
 * Helper class for processing and grouping salary data
 */
object SalaryDataHelper {
    
    /**
     * Group salary slips by year
     * @param salarySlips List of salary slips from API
     * @return List of SalaryByYear, sorted by year (descending - newest first)
     */
    fun groupByYear(salarySlips: List<SalarySlipData>): List<SalaryByYear> {
        return salarySlips
            .groupBy { it.getYear() }
            .map { (year, slips) ->
                SalaryByYear(
                    year = year,
                    salarySlips = slips.sortedByDescending { it.getMonth() }
                )
            }
            .sortedByDescending { it.year }
    }
    
    /**
     * Group salary slips by year and month
     * @param salarySlips List of salary slips from API
     * @return List of SalaryByMonth, sorted by year and month (descending)
     */
    fun groupByYearAndMonth(salarySlips: List<SalarySlipData>): List<SalaryByMonth> {
        return salarySlips
            .groupBy { Pair(it.getYear(), it.getMonth()) }
            .map { (yearMonth, slips) ->
                val (year, month) = yearMonth
                SalaryByMonth(
                    year = year,
                    month = month,
                    monthName = slips.firstOrNull()?.getMonthName() ?: "",
                    salarySlips = slips
                )
            }
            .sortedWith(compareByDescending<SalaryByMonth> { it.year }.thenByDescending { it.month })
    }
    
    /**
     * Get available years from salary slips
     * @param salarySlips List of salary slips
     * @return List of unique years, sorted descending
     */
    fun getAvailableYears(salarySlips: List<SalarySlipData>): List<Int> {
        return salarySlips
            .map { it.getYear() }
            .distinct()
            .sortedDescending()
    }
    
    /**
     * Get salary slips for a specific year
     * @param salarySlips List of salary slips
     * @param year Year to filter
     * @return List of salary slips for the specified year
     */
    fun filterByYear(salarySlips: List<SalarySlipData>, year: Int): List<SalarySlipData> {
        return salarySlips.filter { it.getYear() == year }
    }
    
    /**
     * Get salary slips for a specific year and month
     * @param salarySlips List of salary slips
     * @param year Year to filter
     * @param month Month to filter (1-12)
     * @return List of salary slips for the specified year and month
     */
    fun filterByYearAndMonth(
        salarySlips: List<SalarySlipData>,
        year: Int,
        month: Int
    ): List<SalarySlipData> {
        return salarySlips.filter { it.getYear() == year && it.getMonth() == month }
    }
    
    /**
     * Get months available for a specific year
     * @param salarySlips List of salary slips
     * @param year Year to check
     * @return Map of month number to month name, sorted by month descending
     */
    fun getMonthsForYear(
        salarySlips: List<SalarySlipData>,
        year: Int
    ): Map<Int, String> {
        return salarySlips
            .filter { it.getYear() == year }
            .groupBy { it.getMonth() }
            .mapValues { (_, slips) -> slips.firstOrNull()?.getMonthName() ?: "" }
            .toSortedMap(compareByDescending { it })
    }
    
    /**
     * Parse PRIODE string to extract year and month
     * @param priode PRIODE string in format "M/D/YYYY" or "MM/DD/YYYY"
     * @return Pair of (year, month) or null if parsing fails
     */
    fun parsePriode(priode: String): Pair<Int, Int>? {
        return try {
            val parts = priode.split("/")
            if (parts.size == 3) {
                val month = parts[0].toIntOrNull() ?: return null
                val year = parts[2].toIntOrNull() ?: return null
                Pair(year, month)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format currency to Indonesian Rupiah format
     * @param amount Amount to format
     * @return Formatted string (e.g., "Rp 1.000.000")
     */
    fun formatCurrency(amount: Int): String {
        return "Rp ${String.format("%,d", amount).replace(",", ".")}"
    }
    
    /**
     * Get summary statistics for a list of salary slips
     * @param salarySlips List of salary slips
     * @return SalarySummary object with statistics
     */
    fun getSummary(salarySlips: List<SalarySlipData>): SalarySummary {
        val totalIncome = salarySlips.sumOf { it.getTotalIncome() }
        val totalDeductions = salarySlips.sumOf { it.getTotalDeductions() }
        val netSalary = salarySlips.sumOf { it.getNetSalary() }
        
        return SalarySummary(
            totalIncome = totalIncome,
            totalDeductions = totalDeductions,
            netSalary = netSalary,
            slipCount = salarySlips.size
        )
    }
}

/**
 * Data class for salary summary statistics
 */
data class SalarySummary(
    val totalIncome: Int,
    val totalDeductions: Int,
    val netSalary: Int,
    val slipCount: Int
) {
    fun getFormattedTotalIncome(): String = SalaryDataHelper.formatCurrency(totalIncome)
    fun getFormattedTotalDeductions(): String = SalaryDataHelper.formatCurrency(totalDeductions)
    fun getFormattedNetSalary(): String = SalaryDataHelper.formatCurrency(netSalary)
    fun getAverageSalary(): Int = if (slipCount > 0) netSalary / slipCount else 0
    fun getFormattedAverageSalary(): String = SalaryDataHelper.formatCurrency(getAverageSalary())
}
