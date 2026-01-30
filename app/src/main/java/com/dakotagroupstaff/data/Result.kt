package com.dakotagroupstaff.data

/**
 * Sealed class for representing different states of data operations
 * Similar to Resource pattern but with simplified naming
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
