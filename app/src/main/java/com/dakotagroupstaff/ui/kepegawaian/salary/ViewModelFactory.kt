package com.dakotagroupstaff.ui.kepegawaian.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dakotagroupstaff.data.repository.SalaryRepository

/**
 * ViewModelFactory for creating SalaryViewModel with dependencies
 */
class ViewModelFactory(
    private val salaryRepository: SalaryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SalaryViewModel::class.java) -> {
                SalaryViewModel(salaryRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
