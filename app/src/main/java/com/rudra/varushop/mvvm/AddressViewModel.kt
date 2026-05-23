package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.AddressEntity
import com.rudra.varushop.modal.AddressRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _addresses = MutableStateFlow<List<AddressEntity>>(emptyList())
    val addresses = _addresses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _addressSavedEvent = MutableSharedFlow<Unit>()
    val addressSavedEvent = _addressSavedEvent.asSharedFlow()


    fun fetchAddresses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getAddressesFromCloud()
                if (response.isSuccessful && response.body()?.success == true) {
                    _addresses.value = response.body()?.data ?: emptyList()
                } else {
                    val errorMsg =
                        response.message().takeIf { it.isNotBlank() } ?: "Failed to fetch addresses"
                    _error.emit(errorMsg)
                }
            } catch (e: Exception) {
                _error.emit(e.localizedMessage ?: "Connection Error")
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun addAddress(request: AddressRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.addAddressToCloud(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _addressSavedEvent.emit(Unit)
                } else {
                    _error.emit(response.body()?.message ?: "Failed to save address")
                }
            } catch (e: Exception) {
                _error.emit("Network error. Please try again.")
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun updateAddress(addressId: Int, request: AddressRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.updateAddressInCloud(addressId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _addressSavedEvent.emit(Unit)
                } else {
                    _error.emit(response.body()?.message ?: "Update failed")
                }
            } catch (e: Exception) {
                _error.emit("Network Error: Could not update address")
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun deleteAddressFromCloud(addressId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.deleteAddressFromCloud(addressId)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Instantly refresh the local list
                    fetchAddresses()
                } else {
                    _error.emit(response.body()?.message ?: "Could not delete address")
                }
            } catch (e: Exception) {
                _error.emit("Network error: Check your connection")
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun setDefaultAddressInCloud(addressId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.setDefaultAddressInCloud(addressId)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Refresh so the "isSelected" logic updates in the Adapter
                    fetchAddresses()
                } else {
                    _error.emit("Could not update default address")
                }
            } catch (e: Exception) {
                _error.emit("Check your internet connection")
            }
        }
    }
}