package com.dakotagroupstaff.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dakotagroupstaff.data.repository.AssignmentRepository
import com.dakotagroupstaff.data.repository.DeliveryRepository
import com.dakotagroupstaff.ui.operasional.assignment.AssignmentViewModel
import com.dakotagroupstaff.ui.operasional.loper.LoperViewModel

/**
 * Generic ViewModelFactory for creating ViewModels with repository dependencies
 */
class ViewModelFactory private constructor(
    private val assignmentRepository: AssignmentRepository? = null,
    private val deliveryRepository: DeliveryRepository? = null,
    private val loperRepository: com.dakotagroupstaff.data.repository.LoperRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AssignmentViewModel::class.java) -> {
                requireNotNull(assignmentRepository) { "AssignmentRepository is required for AssignmentViewModel" }
                AssignmentViewModel(assignmentRepository) as T
            }
            modelClass.isAssignableFrom(LoperViewModel::class.java) -> {
                requireNotNull(deliveryRepository) { "DeliveryRepository is required for LoperViewModel" }
                requireNotNull(loperRepository) { "LoperRepository is required for LoperViewModel" }
                LoperViewModel(deliveryRepository, loperRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ViewModelFactory? = null

        fun getInstance(
            context: Context,
            assignmentRepository: AssignmentRepository? = null,
            deliveryRepository: DeliveryRepository? = null,
            loperRepository: com.dakotagroupstaff.data.repository.LoperRepository? = null
        ): ViewModelFactory {
            return ViewModelFactory(
                assignmentRepository = assignmentRepository,
                deliveryRepository = deliveryRepository,
                loperRepository = loperRepository
            )
        }
    }
}
