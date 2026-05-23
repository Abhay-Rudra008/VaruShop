package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.user.UserDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import dagger.hilt.android.lifecycle.HiltViewModel
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {
    private val _removeStatus = MutableSharedFlow<String>()
    val removeStatus = _removeStatus.asSharedFlow()

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val profileState = _profileState.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()

    private val _updateStatus = MutableSharedFlow<String>()
    val updateStatus = _updateStatus.asSharedFlow()

    fun fetchProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            try {
                val response = repository.getProfileDetail()
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { userDto ->
                        _profileState.value = ProfileUiState.Success(userDto)
                    } ?: run {
                        _profileState.value = ProfileUiState.Error("Profile data is missing")
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Failed to load profile"
                    _profileState.value = ProfileUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _profileState.value = ProfileUiState.Error("Connection Error: ${e.localizedMessage}")
            }
        }
    }

    fun removeProfilePhoto() {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                val response = repository.removeProfileImage()
                if (response.isSuccessful && response.body()?.success == true) {
                    _removeStatus.emit("Success")
                } else {
                    _removeStatus.emit("Failed to remove photo")
                }
            } catch (e: Exception) {
                _removeStatus.emit("Error: ${e.message}")
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun changePassword(passData: Map<String, String>) {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                val response = repository.changePassword(passData)
                if (response.isSuccessful && response.body()?.success == true) {
                    _updateStatus.emit(response.body()?.message ?: "Password Changed Successfully")
                } else {
                    val errorObj = response.errorBody()?.string()
                    val message = if (errorObj != null) {
                        JSONObject(errorObj).optString("message", "Failed to update")
                    } else {
                        response.body()?.message ?: "An error occurred"
                    }
                    _updateStatus.emit(message)
                }
            } catch (e: Exception) {
                _updateStatus.emit("Connection error: ${e.localizedMessage}")
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun updateProfile(name: RequestBody, image: MultipartBody.Part?) {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                val response = repository.updateProfile(name, image)
                if (response.isSuccessful && response.body()?.success == true) {
                    _updateStatus.emit(response.body()?.message ?: "Profile Updated Successfully")
                } else {
                    _updateStatus.emit(response.body()?.message ?: "Update failed")
                }
            } catch (e: Exception) {
                _updateStatus.emit("Error: ${e.message}")
            } finally {
                _isUpdating.value = false
            }
        }
    }

    sealed interface ProfileUiState {
        data object Idle : ProfileUiState
        data object Loading : ProfileUiState
        data class Success(val user: UserDto) : ProfileUiState
        data class Error(val message: String) : ProfileUiState
    }
}