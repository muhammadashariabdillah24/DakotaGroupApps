package com.dakotagroupstaff.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.Result
import com.dakotagroupstaff.data.local.model.UserSession
import com.dakotagroupstaff.data.remote.response.LoginData
import com.dakotagroupstaff.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    fun login(
        pt: String,
        nip: String,
        deviceId: String,
        serialNumber: String,
        email: String
    ): LiveData<Result<LoginData>> {
        return authRepository.login(pt, nip, deviceId, serialNumber, email)
    }

    fun getSession(): LiveData<UserSession> {
        return authRepository.getSession().asLiveData()
    }

    suspend fun logout() {
        authRepository.logout()
    }
}
