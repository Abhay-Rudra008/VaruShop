package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _statsState = MutableStateFlow<AccountUiState>(AccountUiState.Idle)
    val statsState = _statsState.asStateFlow()

    fun fetchUserStats() {
        viewModelScope.launch {
            _statsState.value = AccountUiState.Loading
            try {
                val response = repository.getUserStats()
                if (response.isSuccessful && response.body()?.success == true) {
                    val stats = response.body()?.data ?: UserStats(0, 0, 0)
                    _statsState.value = AccountUiState.Success(stats)
                } else {
                    val errorMsg = response.message().takeIf { it.isNotBlank() } ?: response.body()?.message ?: "Unknown Error"
                    _statsState.value = AccountUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _statsState.value = AccountUiState.Error("Network Failed")
            }
        }
    }

    sealed interface AccountUiState {
        data object Idle : AccountUiState
        data object Loading : AccountUiState
        data class Success(val stats: UserStats) : AccountUiState
        data class Error(val message: String) : AccountUiState
    }
}