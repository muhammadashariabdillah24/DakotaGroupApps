package com.dakotagroupstaff.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dakotagroupstaff.data.repository.MenuRepository
import kotlinx.coroutines.launch

class MainViewModel(private val menuRepository: MenuRepository) : ViewModel() {

    val recentMenus = menuRepository.getRecentMenus().asLiveData()

    fun saveMenuToHistory(menuId: String, name: String, iconRes: Int, activityClass: String) {
        viewModelScope.launch {
            menuRepository.saveToHistory(menuId, name, iconRes, activityClass)
        }
    }

    fun clearMenuHistory() {
        viewModelScope.launch {
            menuRepository.clearHistory()
        }
    }
}
