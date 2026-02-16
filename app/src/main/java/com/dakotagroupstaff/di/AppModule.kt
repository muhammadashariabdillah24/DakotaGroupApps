package com.dakotagroupstaff.di

import androidx.room.Room
import com.dakotagroupstaff.data.local.pref.SessionManager
import com.dakotagroupstaff.data.local.preferences.UserPreferences
import com.dakotagroupstaff.data.local.preferences.dataStore
import com.dakotagroupstaff.data.local.room.AppDatabase
import com.dakotagroupstaff.data.remote.retrofit.ApiConfig
import com.dakotagroupstaff.data.repository.AuthRepository
import com.dakotagroupstaff.ui.login.LoginViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin modules for Dependency Injection
 * Following pattern from MyQuranApp (Expert level)
 */

val networkModule = module {
    single { ApiConfig.getApiService(userPreferences = get()) }
}

val databaseModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    
    // Provide DAOs
    single { get<AppDatabase>().employeeBioDao() }
    single { get<AppDatabase>().agentLocationDao() }
    single { get<AppDatabase>().leaveBalanceDao() }
    single { get<AppDatabase>().attendanceHistoryDao() }
    single { get<AppDatabase>().leaveDetailsDao() }
    single { get<AppDatabase>().deliveryListDao() }
    single { get<AppDatabase>().recentMenuDao() }
}

val dataStoreModule = module {
    single { androidContext().dataStore }
    single { UserPreferences.getInstance(get()) }
    single { SessionManager(androidContext()) }
}

val repositoryModule = module {
    single { AuthRepository.getInstance(get(), get()) }
    single { 
        com.dakotagroupstaff.data.repository.AttendanceRepository.getInstance(
            get(), get(), get()
        ) 
    }
    single {
        com.dakotagroupstaff.data.repository.SalaryRepository.getInstance(get())
    }
    single {
        com.dakotagroupstaff.data.repository.LeaveRepository(
            apiService = get(),
            leaveBalanceDao = get(),
            leaveDetailsDao = get()
        )
    }
    single {
        com.dakotagroupstaff.data.repository.AssignmentRepository(
            apiService = get()
        )
    }
    single {
        com.dakotagroupstaff.data.repository.LetterOfAssignRepository(
            apiService = get(),
            userPreferences = get()
        )
    }
    single { com.dakotagroupstaff.data.repository.MenuRepository.getInstance(get()) }
}

val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { 
        com.dakotagroupstaff.ui.kepegawaian.attendance.AttendanceViewModel(get()) 
    }
    viewModel {
        com.dakotagroupstaff.ui.kepegawaian.salary.SalaryViewModel(get())
    }
    viewModel {
        com.dakotagroupstaff.ui.kepegawaian.leave.LeaveViewModel(get())
    }
    viewModel {
        com.dakotagroupstaff.ui.operasional.assignment.AssignmentViewModel(get())
    }
    viewModel {
        com.dakotagroupstaff.ui.operasional.letterofassign.LetterOfAssignViewModel(get())
    }
    viewModel { com.dakotagroupstaff.ui.main.MainViewModel(get()) }
}
