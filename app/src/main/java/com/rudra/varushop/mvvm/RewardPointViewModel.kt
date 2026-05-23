package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.modal.PointsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class RewardPointViewModel @Inject constructor(
    private val repository: HomeRepository, private val prefManager: PrefManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RewardPointUiState>(RewardPointUiState.Loading)
    val uiState: StateFlow<RewardPointUiState> = _uiState.asStateFlow()

    fun fetchPoints(hasInternet: Boolean) {
        if (!hasInternet) {
            _uiState.value = RewardPointUiState.NoInternet
            return
        }

        _uiState.value = RewardPointUiState.Loading
        val userId = prefManager.userId

        viewModelScope.launch {
            try {
                val response = repository.getPointsHistoryFromCloud(userId)

                if (response.isSuccessful && response.body() != null) {
                    val baseResponse = response.body()!!

                    if (baseResponse.success && baseResponse.data != null) {
                        val pointsData = baseResponse.data

                        // Check if the transactions list is empty
                        if (pointsData.transactions.isEmpty()) {
                            _uiState.value = RewardPointUiState.Empty
                        } else {
                            _uiState.value = RewardPointUiState.Success(pointsData)
                        }
                    } else {
                        _uiState.value = RewardPointUiState.Error(baseResponse.message)
                    }
                } else {
                    _uiState.value = RewardPointUiState.Error("Server error: ${response.code()}")
                }

            } catch (e: Exception) {
                _uiState.value =
                    RewardPointUiState.Error(e.localizedMessage ?: "Unknown network error")
            }
        }
    }

    sealed interface RewardPointUiState {
        data object Loading : RewardPointUiState

        // Changed to hold PointsData
        data class Success(val pointsData: PointsData) : RewardPointUiState
        data object Empty : RewardPointUiState
        data object NoInternet : RewardPointUiState
        data class Error(val message: String) : RewardPointUiState
    }
}