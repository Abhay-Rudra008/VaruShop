package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.product.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _wishlistState = MutableStateFlow<WishlistUiState>(WishlistUiState.Idle)
    val wishlistState = _wishlistState.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _isWishlisted = MutableStateFlow(false)

    fun fetchWishlist() {
        viewModelScope.launch {
            _wishlistState.value = WishlistUiState.Loading
            try {
                val response = repository.getWishlist()
                if (response.isSuccessful && response.body()?.success == true) {

                    val items = response.body()?.data?.products ?: emptyList()

                    if (items.isEmpty()) {
                        _wishlistState.value = WishlistUiState.Empty
                    } else {
                        items.forEach { it.isWishlisted = true }
                        _products.value = items // Sync the local list state
                        _wishlistState.value = WishlistUiState.Success(items)
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Failed to fetch wishlist"
                    _wishlistState.value = WishlistUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _wishlistState.value = WishlistUiState.Error("Connection Error")
            }
        }
    }


    fun toggleWishlist(productId: Int, isFromWishlistScreen: Boolean = false) {
        val oldList = _products.value

        val updatedList = if (isFromWishlistScreen) {
            oldList.filter { it.id != productId }
        } else {
            oldList.map {
                if (it.id == productId) it.copy(isWishlisted = !it.isWishlisted)
                else it
            }
        }

        _products.value = updatedList
        if (isFromWishlistScreen) {
            _wishlistState.value =
                if (updatedList.isEmpty()) WishlistUiState.Empty else WishlistUiState.Success(
                    updatedList
                )
        } else {
            _isWishlisted.value = !_isWishlisted.value
        }

        viewModelScope.launch {
            try {
                val response = repository.toggleWishlist(productId)
                if (!response.isSuccessful || response.body()?.success != true) {
                    _products.value = oldList
                    if (isFromWishlistScreen) {
                        _wishlistState.value =
                            if (oldList.isEmpty()) WishlistUiState.Empty else WishlistUiState.Success(
                                oldList
                            )
                    } else {
                        _isWishlisted.value = !_isWishlisted.value
                    }
                    _error.emit(response.body()?.message ?: "Failed to update wishlist")
                } else {
                    _isWishlisted.value = response.body()?.data?.isWishlisted ?: false
                }
            } catch (e: Exception) {
                _products.value = oldList
                if (isFromWishlistScreen) {
                    _wishlistState.value =
                        if (oldList.isEmpty()) WishlistUiState.Empty else WishlistUiState.Success(
                            oldList
                        )
                } else {
                    _isWishlisted.value = !_isWishlisted.value
                }
                _error.emit("Connection error")
            }
        }
    }

    sealed interface WishlistUiState {
        data object Idle : WishlistUiState
        data object Loading : WishlistUiState
        data object Empty : WishlistUiState
        data class Success(val items: List<Product>) : WishlistUiState
        data class Error(val message: String) : WishlistUiState
    }
}