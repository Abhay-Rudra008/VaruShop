package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.user.DataWrapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel // 🔥 1. Add Hilt
class RegisterViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _registrationState = MutableStateFlow<UiState>(UiState.Idle)
    val registrationState = _registrationState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun register(
        name: RequestBody,
        email: RequestBody,
        pass: RequestBody,
        image: MultipartBody.Part?
    ) {
        viewModelScope.launch {
            _registrationState.value = UiState.Loading
            try {
                val response = repository.register(name, email, pass, image)

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    if (body?.data != null) {
                        // 💡 REMINDER: Access the token/refresh_token here using body.data.userId.token
                        _registrationState.value = UiState.Success(body.data, body.message ?: "Success")
                    } else {
                        _registrationState.value = UiState.Error("Registration failed: No data returned")
                    }
                } else {
                    // Extract exact error message from backend
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        JSONObject(errorBody).optString("message", "Registration failed")
                    } else {
                        response.body()?.message ?: "Server Error: ${response.code()}"
                    }
                    _registrationState.value = UiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _registrationState.value = UiState.Error(e.localizedMessage ?: "Connection error")
            }
        }
    }


    sealed interface NavigationEvent {
        data object ToLogin : NavigationEvent
    }

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data class Success(val authData: DataWrapper, val message: String) : UiState
        data class Error(val message: String) : UiState
    }
}