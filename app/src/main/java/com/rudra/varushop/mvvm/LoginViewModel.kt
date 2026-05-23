package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.user.AuthData
import com.rudra.varushop.modal.user.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = repository.login(LoginRequest(email, pass))

                if (response.isSuccessful && response.body()?.success == true) {
                    val authDataInside = response.body()?.data

                    if (authDataInside != null) {
                        _loginState.value = LoginState.Success(authDataInside)
                    } else {
                        _loginState.value = LoginState.Error("Invalid response data")
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()

                    val errorMsg = try {
                        if (!errorBodyString.isNullOrBlank()) {
                            JSONObject(errorBodyString).getString("message")
                        } else {
                            "Invalid Email or Password"
                        }
                    } catch (e: Exception) {
                        response.message().takeIf { it.isNotBlank() } ?: "Invalid Email or Password"
                    }

                    _loginState.value = LoginState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _loginState.value =
                    LoginState.Error(e.localizedMessage ?: "Network failure. Check your internet.")
            }
        }
    }

    sealed interface LoginState {
        data object Idle : LoginState
        data object Loading : LoginState
        data class Success(val data: AuthData) : LoginState
        data class Error(val message: String) : LoginState
    }
}