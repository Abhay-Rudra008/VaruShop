package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _reviewSuccess = MutableSharedFlow<Unit>()
    val reviewSuccess = _reviewSuccess.asSharedFlow()

    fun submitReview(productId: Int, rating: Int, comment: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.submitProductReview(productId, rating, comment)
                if (response.isSuccessful && response.body()?.success == true) {
                    _reviewSuccess.emit(Unit)
                } else {
                    val errorMsg =
                        response.message().takeIf { it.isNotBlank() } ?: response.body()?.message
                        ?: "Could not submit review"
                    _error.emit(errorMsg)
                }
            } catch (e: Exception) {
                _error.emit("Network Error: Please try again later")
            } finally {
                _isLoading.value = false
            }
        }
    }
}