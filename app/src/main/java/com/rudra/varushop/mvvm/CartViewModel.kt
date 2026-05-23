package com.rudra.varushop.mvvm


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.cart.AddToCartRequest
import com.rudra.varushop.modal.cart.CartItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _cartState = MutableStateFlow<CartUiState>(CartUiState.Idle)
    val cartState = _cartState.asStateFlow()

    private val _isInCart = MutableStateFlow(false)
    val isInCart = _isInCart.asStateFlow()

    private val _totalPrice = MutableStateFlow(0.0)
    val totalPrice = _totalPrice.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()


    fun addToCart(productId: Int, quantity: Int) {
        viewModelScope.launch {
            _cartState.value = CartUiState.Loading
            try {
                val request = AddToCartRequest(productId, quantity)
                val response = repository.addToCart(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _isInCart.value = true
                    _cartState.value = CartUiState.Success(emptyList())
                } else {
                    val errorMsg = response.message().takeIf { it.isNotBlank() } ?: "Failed to add"
                    _cartState.value = CartUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _cartState.value = CartUiState.Error(e.localizedMessage ?: "Network Error")
            }
        }
    }

    fun checkIfProductInCart(productId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.checkCartStatus(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    // 🔥 Dig into the data object!
                    _isInCart.value = response.body()?.data?.isInCart ?: false
                } else {
                    _isInCart.value = false
                }
            } catch (e: Exception) {
                _isInCart.value = false
            }
        }
    }


    fun getCart() {
        viewModelScope.launch {
            _cartState.value = CartUiState.Loading
            try {
                val response = repository.getCart()
                if (response.isSuccessful && response.body()?.success == true) {
                    val items = response.body()?.data?.items ?: emptyList()
                    _cartState.value = CartUiState.Success(items)
                    calculateTotal(items)
                } else {
                    _cartState.value = CartUiState.Error("Could not load cart")
                }
            } catch (e: Exception) {
                _cartState.value = CartUiState.Error(e.localizedMessage ?: "Network Error")
            }
        }
    }

    private fun calculateTotal(items: List<CartItem>) {
        val total = items.sumOf { it.price * it.quantity }
        _totalPrice.value = total
    }


    fun updateCartQuantity(productId: Int, newQuantity: Int) {
        viewModelScope.launch {
            try {
                val request = AddToCartRequest(productId, newQuantity)
                val response = repository.addToCart(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    getCart() // Refresh cart list to reflect the new total
                } else {
                    _snackbarMessage.emit("Failed to update: ${response.message()}")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Check your internet connection")
            }
        }
    }

    fun removeItem(productId: Int) {
        viewModelScope.launch {
            _cartState.value = CartUiState.Loading
            try {
                val response = repository.deleteItem(productId)
                if (response.isSuccessful) {
                    _snackbarMessage.emit("Item removed")
                    getCart() // Refresh cart list
                } else {
                    _cartState.value = CartUiState.Error("Failed to remove")
                }
            } catch (e: Exception) {
                _cartState.value = CartUiState.Error(e.localizedMessage ?: "Network Error")
            }
        }
    }

    sealed interface CartUiState {
        data object Idle : CartUiState
        data object Loading : CartUiState
        data class Success(val items: List<CartItem>) : CartUiState
        data class Error(val message: String) : CartUiState
    }
}